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

package org.apache.ignite.internal.cli.call.recovery.states;

import static java.util.stream.Collectors.toList;
import static org.apache.ignite.internal.lang.IgniteSystemProperties.colocationEnabled;

import jakarta.inject.Singleton;
import java.util.List;
import java.util.stream.Stream;
import org.apache.ignite.internal.cli.core.call.Call;
import org.apache.ignite.internal.cli.core.call.DefaultCallOutput;
import org.apache.ignite.internal.cli.core.exception.IgniteCliApiException;
import org.apache.ignite.internal.cli.core.rest.ApiClientFactory;
import org.apache.ignite.internal.cli.sql.table.Table;
import org.apache.ignite.rest.client.api.RecoveryApi;
import org.apache.ignite.rest.client.invoker.ApiException;
import org.apache.ignite.rest.client.model.GlobalPartitionStatesResponse;
import org.apache.ignite.rest.client.model.GlobalZonePartitionStatesResponse;
import org.apache.ignite.rest.client.model.LocalPartitionStatesResponse;
import org.apache.ignite.rest.client.model.LocalZonePartitionStatesResponse;

/** Call to get partition states. */
@Singleton
public class PartitionStatesCall implements Call<PartitionStatesCallInput, Table> {
    private final ApiClientFactory clientFactory;

    private static final List<String> GLOBAL_HEADERS = List.of("Zone name", "Schema name", "Table ID", "Table name", "Partition ID",
            "State");

    private static final List<String> LOCAL_HEADERS = Stream
            .concat(Stream.of("Node name"), GLOBAL_HEADERS.stream())
            .collect(toList());

    private static final List<String> ZONE_GLOBAL_HEADERS = List.of("Zone name", "Partition ID", "State");

    private static final List<String> ZONE_LOCAL_HEADERS = Stream
            .concat(Stream.of("Node name"), ZONE_GLOBAL_HEADERS.stream())
            .collect(toList());

    public PartitionStatesCall(ApiClientFactory clientFactory) {
        this.clientFactory = clientFactory;
    }

    @Override
    public DefaultCallOutput<Table> execute(PartitionStatesCallInput input) {
        RecoveryApi client = new RecoveryApi(clientFactory.getClient(input.clusterUrl()));

        try {
            if (input.local()) {
                return getLocalPartitionStatesOutput(client, input.zoneNames(), input);
            } else {
                return getGlobalPartitionStatesOutput(input, client, input.zoneNames());
            }
        } catch (ApiException e) {
            return DefaultCallOutput.failure(new IgniteCliApiException(e, input.clusterUrl()));
        }
    }

    private DefaultCallOutput<Table> getGlobalPartitionStatesOutput(
            PartitionStatesCallInput input,
            RecoveryApi client,
            List<String> zoneNames
    ) throws ApiException {
        // TODO: IGNITE-25637 - remove this.
        if (colocationEnabled()) {
            GlobalZonePartitionStatesResponse globalStates = client.getZoneGlobalPartitionStates(
                    zoneNames,
                    input.partitionIds()
            );

            List<String> content = globalStates.getStates().stream()
                    .flatMap(state -> Stream.of(
                                    state.getZoneName(),
                                    String.valueOf(state.getPartitionId()),
                                    state.getState()
                            )
                    )
                    .collect(toList());

            return DefaultCallOutput.success(new Table(ZONE_GLOBAL_HEADERS, content));
        }

        GlobalPartitionStatesResponse globalStates = client.getGlobalPartitionStates(
                zoneNames,
                input.partitionIds()
        );

        List<String> content = globalStates.getStates().stream()
                .flatMap(state -> Stream.of(
                                state.getZoneName(),
                                state.getSchemaName(),
                                String.valueOf(state.getTableId()),
                                state.getTableName(),
                                String.valueOf(state.getPartitionId()),
                                state.getState()
                        )
                )
                .collect(toList());

        return DefaultCallOutput.success(new Table(GLOBAL_HEADERS, content));
    }

    private DefaultCallOutput<Table> getLocalPartitionStatesOutput(
            RecoveryApi client,
            List<String> zoneNames,
            PartitionStatesCallInput input
    ) throws ApiException {
        // TODO: IGNITE-25637 - remove this.
        if (colocationEnabled()) {
            LocalZonePartitionStatesResponse localStates = client.getZoneLocalPartitionStates(
                    zoneNames,
                    input.nodeNames(),
                    input.partitionIds()
            );

            List<String> content = localStates.getStates().stream()
                    .flatMap(state -> Stream.of(
                                    state.getNodeName(),
                                    state.getZoneName(),
                                    String.valueOf(state.getPartitionId()),
                                    state.getState()
                            )
                    )
                    .collect(toList());

            return DefaultCallOutput.success(new Table(ZONE_LOCAL_HEADERS, content));
        }

        LocalPartitionStatesResponse localStates = client.getLocalPartitionStates(
                zoneNames,
                input.nodeNames(),
                input.partitionIds()
        );

        List<String> content;
        content = localStates.getStates().stream()
                .flatMap(state -> Stream.of(
                                state.getNodeName(),
                                state.getZoneName(),
                                state.getSchemaName(),
                                String.valueOf(state.getTableId()),
                                state.getTableName(),
                                String.valueOf(state.getPartitionId()),
                                state.getState()
                        )
                )
                .collect(toList());

        return DefaultCallOutput.success(new Table(LOCAL_HEADERS, content));
    }
}
