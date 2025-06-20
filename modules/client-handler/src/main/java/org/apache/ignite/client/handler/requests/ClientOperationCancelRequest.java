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

package org.apache.ignite.client.handler.requests;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import org.apache.ignite.client.handler.ResponseWriter;
import org.apache.ignite.internal.client.proto.ClientMessageUnpacker;
import org.apache.ignite.internal.util.CompletableFutures;
import org.apache.ignite.lang.CancelHandle;

/**
 * Request to cancel execution of any operation previously initiated on the same connection.
 */
public class ClientOperationCancelRequest {
    /**
     * Processes the request.
     */
    public static CompletableFuture<ResponseWriter> process(
            ClientMessageUnpacker in,
            Map<Long, CancelHandle> cancelHandleMap
    ) {
        long correlationToken = in.unpackLong();
        CancelHandle cancelHandle = cancelHandleMap.get(correlationToken);

        if (cancelHandle != null) {
            return cancelHandle.cancelAsync().thenApply(x -> null);
        }

        return CompletableFutures.nullCompletedFuture();
    }
}
