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

package org.apache.ignite.internal.sql.engine.rule;

import static org.apache.ignite.internal.sql.engine.trait.IgniteDistributions.single;
import static org.apache.ignite.internal.sql.engine.trait.TraitUtils.distribution;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.apache.calcite.plan.RelOptCluster;
import org.apache.calcite.plan.RelOptRule;
import org.apache.calcite.plan.RelOptRuleCall;
import org.apache.calcite.plan.RelRule;
import org.apache.calcite.rel.RelCollation;
import org.apache.calcite.rel.RelDistribution.Type;
import org.apache.calcite.rel.RelNode;
import org.apache.calcite.rex.RexBuilder;
import org.apache.calcite.rex.RexExecutor;
import org.apache.calcite.rex.RexNode;
import org.apache.calcite.rex.RexUtil;
import org.apache.ignite.internal.sql.engine.rel.IgniteExchange;
import org.apache.ignite.internal.sql.engine.rel.IgniteLimit;
import org.apache.ignite.internal.sql.engine.rel.IgniteSort;
import org.apache.ignite.internal.sql.engine.sql.fun.IgniteSqlOperatorTable;
import org.apache.ignite.internal.sql.engine.trait.IgniteDistribution;
import org.apache.ignite.internal.util.ExceptionUtils;
import org.apache.ignite.internal.util.IgniteUtils;
import org.immutables.value.Value;
import org.jetbrains.annotations.Nullable;

/**
 * A rule that pushes {@link IgniteSort} node under {@link IgniteExchange}.
 */
@Value.Enclosing
public class SortExchangeTransposeRule extends RelRule<SortExchangeTransposeRule.Config> {
    public static final RelOptRule INSTANCE = Config.INSTANCE.toRule();

    private SortExchangeTransposeRule(Config cfg) {
        super(cfg);
    }

    @Override
    public boolean matches(RelOptRuleCall call) {
        IgniteExchange exchange = call.rel(1);

        return hashAlike(distribution(exchange.getInput())) && exchange.distribution() == single();
    }

    private static boolean hashAlike(IgniteDistribution distribution) {
        return distribution.getType() == Type.HASH_DISTRIBUTED
                || distribution.getType() == Type.RANDOM_DISTRIBUTED;
    }

    @Override
    public void onMatch(RelOptRuleCall call) {
        IgniteSort sort = call.rel(0);

        IgniteExchange exchange = call.rel(1);

        RelOptCluster cluster = sort.getCluster();
        RelCollation collation = sort.collation();
        RelNode input = exchange.getInput();
        RelNode newSort = new IgniteSort(
                cluster,
                input.getTraitSet().replace(sort.getCollation()),
                input,
                sort.getCollation(),
                null,
                createLimitForSort(cluster.getPlanner().getExecutor(), cluster.getRexBuilder(), sort.offset, sort.fetch)
        );

        if (sort.offset != null || sort.fetch != null) {
            call.transformTo(new IgniteLimit(
                    cluster,
                    exchange.getTraitSet()
                            .replace(collation),
                    convert(newSort, exchange.getTraitSet().replace(collation)),
                    sort.offset,
                    sort.fetch
            ));
        } else {
            call.transformTo(new IgniteExchange(
                    cluster,
                    exchange.getTraitSet()
                            .replace(collation),
                    newSort,
                    exchange.distribution()
            ), Map.of(newSort, sort));
        }
    }

    private static @Nullable RexNode createLimitForSort(
            @Nullable RexExecutor executor, RexBuilder builder, @Nullable RexNode offset, @Nullable RexNode fetch
    ) {
        if (fetch == null) {
            // Current implementation of SortNode cannot handle offset-only case.
            return null;
        }

        if (offset != null) {
            boolean shouldTryToSimplify = RexUtil.isLiteral(fetch, false)
                    && RexUtil.isLiteral(offset, false);

            fetch = builder.makeCall(IgniteSqlOperatorTable.PLUS, fetch, offset);

            if (shouldTryToSimplify && executor != null) {
                try {
                    List<RexNode> result = new ArrayList<>();
                    executor.reduce(builder, List.of(fetch), result);

                    assert result.size() <= 1 : result;

                    if (result.size() == 1) {
                        fetch = result.get(0);
                    }
                } catch (Exception ex) {
                    if (IgniteUtils.assertionsEnabled()) {
                        ExceptionUtils.sneakyThrow(ex);
                    }

                    // Just ignore the exception, we will deal with this expression later again,
                    // and next time we might have all the required context to evaluate it.
                }
            }
        }

        return fetch;
    }

    /** Configuration. */
    @SuppressWarnings({"ClassNameSameAsAncestorName", "InnerClassFieldHidesOuterClassField"})
    @Value.Immutable
    public interface Config extends RelRule.Config {
        Config INSTANCE = ImmutableSortExchangeTransposeRule.Config.of()
                .withDescription("SortExchangeTransposeRule")
                .withOperandSupplier(o0 ->
                        o0.operand(IgniteSort.class)
                                .oneInput(o1 ->
                                        o1.operand(IgniteExchange.class)
                                                .anyInputs()))
                .as(Config.class);

        /** {@inheritDoc} */
        @Override
        default SortExchangeTransposeRule toRule() {
            return new SortExchangeTransposeRule(this);
        }
    }
}
