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

import java.util.concurrent.atomic.AtomicBoolean;
import org.apache.ignite.Ignite;
import org.apache.ignite.compute.JobExecutionContext;
import org.apache.ignite.internal.compute.loader.JobClassLoader;
import org.apache.ignite.table.partition.Partition;
import org.jetbrains.annotations.Nullable;

/**
 * Implementation of {@link JobExecutionContext}.
 */
public class JobExecutionContextImpl implements JobExecutionContext {
    private final Ignite ignite;

    private final AtomicBoolean isInterrupted;

    private final JobClassLoader classLoader;

    private final @Nullable Partition partition;

    /**
     * Constructor.
     *
     * @param ignite Ignite instance.
     * @param isInterrupted Interrupted flag.
     * @param classLoader Job class loader.
     * @param partition Partition associated with this job.
     */
    public JobExecutionContextImpl(Ignite ignite, AtomicBoolean isInterrupted, JobClassLoader classLoader, @Nullable Partition partition) {
        this.ignite = ignite;
        this.isInterrupted = isInterrupted;
        this.classLoader = classLoader;
        this.partition = partition;
    }

    @Override
    public Ignite ignite() {
        return ignite;
    }

    @Override
    public boolean isCancelled() {
        return isInterrupted.get();
    }

    @Override
    public @Nullable Partition partition() {
        return partition;
    }

    /**
     * Gets the job class loader.
     *
     * @return Job class loader.
     */
    public JobClassLoader classLoader() {
        return classLoader;
    }
}
