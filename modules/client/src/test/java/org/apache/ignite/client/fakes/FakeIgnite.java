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

package org.apache.ignite.client.fakes;

import org.apache.ignite.Ignite;
import org.apache.ignite.catalog.IgniteCatalog;
import org.apache.ignite.client.handler.FakePlacementDriver;
import org.apache.ignite.compute.IgniteCompute;
import org.apache.ignite.internal.catalog.sql.IgniteCatalogSqlImpl;
import org.apache.ignite.internal.hlc.HybridClock;
import org.apache.ignite.internal.hlc.HybridClockImpl;
import org.apache.ignite.internal.hlc.HybridTimestampTracker;
import org.apache.ignite.internal.sql.engine.QueryProcessor;
import org.apache.ignite.internal.tx.TxManager;
import org.apache.ignite.internal.tx.impl.IgniteTransactionsImpl;
import org.apache.ignite.network.IgniteCluster;
import org.apache.ignite.sql.IgniteSql;
import org.apache.ignite.table.IgniteTables;
import org.apache.ignite.tx.IgniteTransactions;

/**
 * Fake Ignite.
 */
public class FakeIgnite implements Ignite {
    private final String name;

    private final HybridClock clock = new HybridClockImpl();

    private final FakeTxManager txMgr = new FakeTxManager(clock);

    /** Timestamp tracker. */
    private final HybridTimestampTracker hybridTimestampTracker = HybridTimestampTracker.atomicTracker(null);

    private final FakeCompute compute;

    private final IgniteTables tables;

    private final QueryProcessor queryProcessor;

    private final FakePlacementDriver placementDriver = new FakePlacementDriver(FakeInternalTable.PARTITIONS);

    /**
     * Default constructor.
     */
    public FakeIgnite() {
        this(null);
    }

    /**
     * Constructor.
     *
     * @param name Name.
     */
    public FakeIgnite(String name) {
        this.name = name;
        this.compute = new FakeCompute(name, this);
        this.tables = new FakeIgniteTables(compute, placementDriver);
        this.queryProcessor = new FakeIgniteQueryProcessor(name);
    }

    /** {@inheritDoc} */
    @Override
    public IgniteTables tables() {
        return tables;
    }

    public QueryProcessor queryEngine() {
        return queryProcessor;
    }

    /** {@inheritDoc} */
    @Override
    public IgniteTransactions transactions() {
        return new IgniteTransactionsImpl(txMgr, hybridTimestampTracker);
    }

    /** {@inheritDoc} */
    @Override
    public IgniteSql sql() {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    /** {@inheritDoc} */
    @Override
    public IgniteCompute compute() {
        return compute;
    }

    @Override
    public IgniteCatalog catalog() {
        return new IgniteCatalogSqlImpl(sql(), tables);
    }

    @Override
    public IgniteCluster cluster() {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    /** {@inheritDoc} */
    @Override
    public String name() {
        return name;
    }

    public FakePlacementDriver placementDriver() {
        return placementDriver;
    }

    public TxManager txManager() {
        return txMgr;
    }
}
