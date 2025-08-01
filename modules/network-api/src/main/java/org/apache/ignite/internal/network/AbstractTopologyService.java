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

package org.apache.ignite.internal.network;

import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Base class for {@link TopologyService} implementations.
 */
public abstract class AbstractTopologyService implements TopologyService {
    /** Registered event handlers. */
    private final Collection<TopologyEventHandler> eventHandlers = new CopyOnWriteArrayList<>();

    /** {@inheritDoc} */
    @Override
    public void addEventHandler(TopologyEventHandler handler) {
        eventHandlers.add(handler);
    }

    @Override
    public void removeEventHandler(TopologyEventHandler handler) {
        eventHandlers.remove(handler);
    }

    /**
     * Returns the registered topology event handlers.
     *
     * @return Registered event handlers.
     */
    protected Collection<TopologyEventHandler> getEventHandlers() {
        return Collections.unmodifiableCollection(eventHandlers);
    }
}
