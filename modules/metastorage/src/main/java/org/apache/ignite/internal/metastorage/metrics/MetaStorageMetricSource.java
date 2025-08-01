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

package org.apache.ignite.internal.metastorage.metrics;

import java.util.List;
import org.apache.ignite.internal.metastorage.metrics.MetaStorageMetricSource.Holder;
import org.apache.ignite.internal.metrics.AbstractMetricSource;
import org.apache.ignite.internal.metrics.AtomicIntMetric;
import org.apache.ignite.internal.metrics.LongGauge;
import org.apache.ignite.internal.metrics.LongMetric;
import org.apache.ignite.internal.metrics.Metric;

/**
 * Metric source which provides MetaStorage-related metrics.
 */
public class MetaStorageMetricSource extends AbstractMetricSource<Holder> {
    private static final String SOURCE_NAME = "metastorage";

    private final MetaStorageMetrics metaStorageMetrics;

    /**
     * Constructor.
     */
    public MetaStorageMetricSource(MetaStorageMetrics metaStorageMetrics) {
        super(SOURCE_NAME);

        this.metaStorageMetrics = metaStorageMetrics;
    }

    @Override
    protected Holder createHolder() {
        return new Holder();
    }

    /**
     * Is called on the change of idempotent commands' cache size.
     *
     * @param newSize New size of the cache.
     */
    public void onIdempotentCacheSizeChange(int newSize) {
        Holder holder = holder();
        if (holder != null) {
            holder.idempotentCacheSize.value(newSize);
        }
    }

    /** Holder. */
    protected class Holder implements AbstractMetricSource.Holder<Holder> {
        private final LongMetric safeTimeLag = new LongGauge(
                "SafeTimeLag",
                "Number of milliseconds the local MetaStorage SafeTime lags behind the local logical clock.",
                metaStorageMetrics::safeTimeLag
        );

        private final AtomicIntMetric idempotentCacheSize = new AtomicIntMetric(
                "IdempotentCacheSize",
                "The current size of the cache of idempotent commands' results."
        );

        private final List<Metric> metrics = List.of(
                safeTimeLag,
                idempotentCacheSize
        );

        @Override
        public Iterable<Metric> metrics() {
            return metrics;
        }
    }
}
