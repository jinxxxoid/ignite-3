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

package org.apache.ignite.internal.sql.engine.prepare;

import javax.annotation.Nullable;
import org.apache.ignite.internal.sql.engine.SqlQueryType;
import org.apache.ignite.internal.sql.engine.prepare.partitionawareness.PartitionAwarenessMetadata;
import org.apache.ignite.sql.ResultSetMetadata;

/**
 * QueryPlan interface.
 * TODO Documentation https://issues.apache.org/jira/browse/IGNITE-15859
 */
public interface QueryPlan {
    /**
     * Get a unique identifier of a plan.
     */
    PlanId id();

    /**
     * Get query type, or {@code null} if this is a fragment.
     */
    SqlQueryType type();

    /**
     * Get fields metadata.
     */
    ResultSetMetadata metadata();

    /**
     * Returns parameters metadata.
     */
    ParameterMetadata parameterMetadata();

    /**
     * Returns partition-awareness metadata or {@code null} if it not present.
     */
    @Nullable PartitionAwarenessMetadata partitionAwarenessMetadata();
}
