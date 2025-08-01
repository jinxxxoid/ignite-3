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
package org.apache.ignite.raft.jraft.util.timer;

import java.util.concurrent.TimeUnit;
import org.apache.ignite.internal.logger.IgniteLogger;
import org.apache.ignite.internal.logger.Loggers;
import org.apache.ignite.internal.thread.IgniteThreadFactory;
import org.apache.ignite.raft.jraft.core.Scheduler;
import org.apache.ignite.raft.jraft.core.TimerManager;

/**
 * RAFT timers and schedulers factory.
 */
public class DefaultRaftTimerFactory implements RaftTimerFactory {
    private static final IgniteLogger LOG = Loggers.forClass(DefaultRaftTimerFactory.class);

    @Override
    public Timer getElectionTimer(final String name) {
        return createTimer(name);
    }

    @Override
    public Timer getVoteTimer(final String name) {
        return createTimer(name);
    }

    @Override
    public Timer getStepDownTimer(final String name) {
        return createTimer(name);
    }

    @Override
    public Timer getSnapshotTimer(final String name) {
        return createTimer(name);
    }

    @Override
    public Timer createTimer(final String name) {
        return new HashedWheelTimer(IgniteThreadFactory.createWithFixedPrefix(name, true, LOG), 1, TimeUnit.MILLISECONDS, 2048);
    }

    @Override
    public Scheduler createScheduler(final int workerNum, final String name) {
        return new TimerManager(workerNum, name);
    }
}
