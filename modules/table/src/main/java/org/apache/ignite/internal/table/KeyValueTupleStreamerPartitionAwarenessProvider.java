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

package org.apache.ignite.internal.table;

import java.util.Map.Entry;
import org.apache.ignite.internal.schema.Column;
import org.apache.ignite.internal.schema.SchemaDescriptor;
import org.apache.ignite.internal.schema.SchemaRegistry;
import org.apache.ignite.internal.util.ColocationUtils;
import org.apache.ignite.internal.util.HashCalculator;
import org.apache.ignite.table.Tuple;
import org.apache.ignite.table.TupleHelper;

/**
 * Partition awareness provider for data streamer.
 */
class KeyValueTupleStreamerPartitionAwarenessProvider extends AbstractClientStreamerPartitionAwarenessProvider<Entry<Tuple, Tuple>> {
    KeyValueTupleStreamerPartitionAwarenessProvider(SchemaRegistry schemaReg, int partitions) {
        super(schemaReg, partitions);
    }

    @Override
    int colocationHash(SchemaDescriptor schema, Entry<Tuple, Tuple> item) {
        HashCalculator hashCalc = new HashCalculator();

        for (Column c : schema.colocationColumns()) {
            // Colocation columns are always part of the key and can't be missing; serializer will check for nulls.
            Object val = TupleHelper.valueOrDefault(item.getKey(), c.name(), null);
            ColocationUtils.append(hashCalc, val, c.type());
        }

        return hashCalc.hash();
    }
}
