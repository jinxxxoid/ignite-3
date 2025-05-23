/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ignite.internal.compute;

import static java.util.concurrent.CompletableFuture.failedFuture;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.apache.ignite.compute.JobExecution;
import org.apache.ignite.compute.JobState;
import org.apache.ignite.compute.JobStatus;
import org.apache.ignite.internal.logger.IgniteLogger;
import org.apache.ignite.internal.logger.Loggers;
import org.apache.ignite.network.ClusterNode;
import org.jetbrains.annotations.Nullable;

/**
 * Fail-safe wrapper for the {@link JobExecution} that should be returned to the client. This wrapper holds the original job execution
 * object. This object can be updated during the lifetime of {@link FailSafeJobExecution}.
 *
 * <p>The problem that is solved by this wrapper is the following: client can join the {@link JobExecution#resultAsync()}
 * future but this original future will never be completed in case the remote worker node has left the topology. By returning
 * {@link FailSafeJobExecution} to the client we can update the original job execution object when it is restarted on another node but the
 * client will still be able to join the original future.
 *
 */
class FailSafeJobExecution implements CancellableJobExecution<ComputeJobDataHolder> {
    private static final IgniteLogger LOG = Loggers.forClass(FailSafeJobExecution.class);

    /**
     * Exception that was thrown during the job execution. It can be set only once.
     */
    private final AtomicReference<Throwable> exception = new AtomicReference<>(null);

    /**
     * The future that is returned as {@link JobExecution#resultAsync()} and will be resolved when the job is completed.
     */
    private final CompletableFuture<ComputeJobDataHolder> resultFuture;

    /**
     * The state of the first job execution attempt. It is used to preserve the original job creation time.
     */
    private JobState capturedState;

    /**
     * Job id of the execution.
     */
    private final UUID jobId = UUID.randomUUID();

    /**
     * Link to the current job execution object. It can be updated when the job is restarted on another node.
     */
    private CancellableJobExecution<ComputeJobDataHolder> runningJobExecution;

    private CompletableFuture<ComputeJobDataHolder> completeHook;

    FailSafeJobExecution(CancellableJobExecution<ComputeJobDataHolder> runningJobExecution) throws RuntimeException {
        this.resultFuture = new CompletableFuture<>();
        this.runningJobExecution = runningJobExecution;

        captureState(runningJobExecution);

        registerCompleteHook();
    }

    private void captureState(JobExecution<ComputeJobDataHolder> runningJobExecution) {
        runningJobExecution.stateAsync()
                .completeOnTimeout(failedState(), 10, TimeUnit.SECONDS)
                .whenComplete((state, e) -> capturedState = state != null ? state : failedState());
    }

    private JobState failedState() {
        return JobStateImpl.builder().id(jobId).createTime(Instant.now()).status(JobStatus.FAILED).build();
    }

    /**
     * Registers a hook for the future that is returned to the user. This future will be completed when the job is completed.
     */
    private void registerCompleteHook() {
        completeHook = runningJobExecution.resultAsync().whenComplete((res, err) -> {
            if (err == null) {
                resultFuture.complete(res);
            } else {
                resultFuture.completeExceptionally(err);
            }
        });
    }

    void updateJobExecution(CancellableJobExecution<ComputeJobDataHolder> jobExecution) {
        LOG.debug("Updating job execution: {}", jobExecution);

        CancellableJobExecution<ComputeJobDataHolder> previousRunningJobExecution = runningJobExecution;
        CompletableFuture<ComputeJobDataHolder> previousCompleteHook = completeHook;

        runningJobExecution = jobExecution;
        registerCompleteHook();

        // Cancel the hook so that the cancelling the execution doesn't trigger it
        previousCompleteHook.cancel(true);
        cleanRunningJobExecution(previousRunningJobExecution);
    }

    /**
     * Transforms the state by modifying the fields that should be always the same regardless of the job execution attempt. The job creation
     * time and job id should be the same for all attempts.
     *
     * <p>Can update {@link #capturedState} as a side-effect if the one is null.
     *
     * @param jobState current job state.
     * @return transformed job state.
     */
    private @Nullable JobState transformState(@Nullable JobState jobState) {
        if (jobState == null) {
            return null;
        }

        if (capturedState == null) {
            capturedState = jobState;
        }

        return JobStateImpl.toBuilder(jobState)
                .createTime(capturedState.createTime())
                .id(jobId)
                .build();
    }

    @Override
    public CompletableFuture<ComputeJobDataHolder> resultAsync() {
        return resultFuture;
    }

    /**
     * Returns the transformed state of the running job execution. The transformation is needed because we do not want to change some
     * fields of the state (e.g. creation time) when the job is restarted.
     *
     * @return the transformed state.
     */
    @Override
    public CompletableFuture<@Nullable JobState> stateAsync() {
        if (exception.get() != null) {
            return failedFuture(exception.get());
        }

        return runningJobExecution.stateAsync()
                .thenApply(this::transformState);
    }

    @Override
    public CompletableFuture<@Nullable Boolean> cancelAsync() {
        if (exception.get() != null) {
            return failedFuture(exception.get());
        }

        return runningJobExecution.cancelAsync();
    }

    @Override
    public CompletableFuture<@Nullable Boolean> changePriorityAsync(int newPriority) {
        if (exception.get() != null) {
            return failedFuture(exception.get());
        }

        return runningJobExecution.changePriorityAsync(newPriority);
    }

    @Override
    public ClusterNode node() {
        return runningJobExecution.node();
    }

    /**
     * Completes the future with the exception. This method can be called only once.
     *
     * @param ex the exception that should be set to the future.
     */
    void completeExceptionally(Exception ex) {
        if (exception.compareAndSet(null, ex)) {
            resultFuture.completeExceptionally(ex);
            cleanRunningJobExecution(runningJobExecution);
        } else {
            throw new IllegalStateException("Job is already completed exceptionally.");
        }
    }

    /**
     * Cancels the running job result future, triggering the execution manager clean process.
     */
    private static void cleanRunningJobExecution(CancellableJobExecution<ComputeJobDataHolder> previousRunningJobExecution) {
        previousRunningJobExecution.resultAsync().cancel(true);
    }
}
