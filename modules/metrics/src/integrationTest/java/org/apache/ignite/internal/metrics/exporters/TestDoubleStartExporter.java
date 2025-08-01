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

package org.apache.ignite.internal.metrics.exporters;

import static java.util.Collections.emptyMap;

import com.google.auto.service.AutoService;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import org.apache.ignite.internal.metrics.MetricProvider;
import org.apache.ignite.internal.metrics.exporters.configuration.ExporterView;

/**
 * Simple metrics exporter for test purposes.
 * It has a trivial API to receive all available metrics as map: (sourceName -> [(metricName -> metricValue), ...])
 */
@AutoService(MetricExporter.class)
public class TestDoubleStartExporter extends BasicMetricExporter {
    /** Exporter name. */
    static final String EXPORTER_NAME = "doubleStart";

    /** An amount of the exporter start attempts. */
    private static final AtomicInteger START_COUNTER = new AtomicInteger(0);

    /**
     * Returns all metrics as map (sourceName -> [(metricName -> metricValue), ...]).
     *
     * @return All available metrics.
     */
    Map<String, Map<String, String>> pull() {
        return emptyMap();
    }

    @Override
    public void start(MetricProvider metricsProvider, ExporterView configuration, Supplier<UUID> clusterIdSupplier,
            String nodeName) {
        super.start(metricsProvider, configuration, clusterIdSupplier, nodeName);

        START_COUNTER.incrementAndGet();
    }

    @Override
    public void stop() {
        // No-op
    }

    @Override
    public void reconfigure(ExporterView newValue) {
    }

    @Override
    public String name() {
        return EXPORTER_NAME;
    }

    /**
     * Return an amount of the exporter start attempts.
     */
    static int startCounter() {
        return START_COUNTER.get();
    }
}
