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

package org.apache.ignite.internal.placementdriver.leases;

import java.util.Collection;
import org.apache.ignite.internal.versioned.VersionedSerialization;

/**
 * Representation of leases batch.
 */
public class LeaseBatch {
    private final Collection<Lease> leases;

    public LeaseBatch(Collection<Lease> leases) {
        this.leases = leases;
    }

    public Collection<Lease> leases() {
        return leases;
    }

    public byte[] bytes() {
        return VersionedSerialization.toBytes(this, LeaseBatchSerializer.INSTANCE);
    }

    public static LeaseBatch fromBytes(byte[] bytes) {
        return VersionedSerialization.fromBytes(bytes, LeaseBatchSerializer.INSTANCE);
    }
}
