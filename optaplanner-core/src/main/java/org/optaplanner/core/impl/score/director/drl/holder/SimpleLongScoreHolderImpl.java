/*
 * Copyright 2021 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.optaplanner.core.impl.score.director.drl.holder;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

import org.kie.api.definition.rule.Rule;
import org.kie.api.runtime.rule.RuleContext;
import org.optaplanner.core.api.domain.constraintweight.ConstraintConfiguration;
import org.optaplanner.core.api.domain.constraintweight.ConstraintWeight;
import org.optaplanner.core.api.score.buildin.simplelong.SimpleLongScore;
import org.optaplanner.core.api.score.buildin.simplelong.SimpleLongScoreHolder;
import org.optaplanner.core.impl.score.holder.AbstractScoreHolder;

/**
 * @see SimpleLongScore
 */
public final class SimpleLongScoreHolderImpl extends AbstractScoreHolder<SimpleLongScore>
        implements SimpleLongScoreHolder {

    protected final Map<Rule, LongMatchExecutor> matchExecutorByNumberMap = new LinkedHashMap<>();

    protected long score;

    public SimpleLongScoreHolderImpl(boolean constraintMatchEnabled) {
        super(constraintMatchEnabled);
    }

    public long getScore() {
        return score;
    }

    // ************************************************************************
    // Setup methods
    // ************************************************************************

    @Override
    public void configureConstraintWeight(Rule rule, SimpleLongScore constraintWeight) {
        super.configureConstraintWeight(rule, constraintWeight);
        LongMatchExecutor matchExecutor;
        if (constraintWeight.isZero()) {
            matchExecutor = (RuleContext kcontext, long matchWeight) -> {
            };
        } else {
            matchExecutor = (RuleContext kcontext, long matchWeight) -> addConstraintMatch(kcontext,
                    constraintWeight.getScore() * matchWeight);
        }
        matchExecutorByNumberMap.put(rule, matchExecutor);
    }

    // ************************************************************************
    // Penalize and reward methods
    // ************************************************************************

    @Override
    public void penalize(RuleContext kcontext) {
        impactScore(kcontext, -1L);
    }

    @Override
    public void penalize(RuleContext kcontext, long weightMultiplier) {
        impactScore(kcontext, -weightMultiplier);
    }

    @Override
    public void reward(RuleContext kcontext) {
        impactScore(kcontext, 1L);
    }

    @Override
    public void reward(RuleContext kcontext, long weightMultiplier) {
        impactScore(kcontext, weightMultiplier);
    }

    @Override
    public void impactScore(RuleContext kcontext) {
        impactScore(kcontext, 1L);
    }

    @Override
    public void impactScore(RuleContext kcontext, int weightMultiplier) {
        impactScore(kcontext, (long) weightMultiplier);
    }

    @Override
    public void impactScore(RuleContext kcontext, long weightMultiplier) {
        Rule rule = kcontext.getRule();
        LongMatchExecutor matchExecutor = matchExecutorByNumberMap.get(rule);
        if (matchExecutor == null) {
            throw new IllegalStateException("The DRL rule (" + rule.getPackageName() + ":" + rule.getName()
                    + ") does not match a @" + ConstraintWeight.class.getSimpleName() + " on the @"
                    + ConstraintConfiguration.class.getSimpleName() + " annotated class.");
        }
        matchExecutor.accept(kcontext, weightMultiplier);
    }

    @Override
    public void impactScore(RuleContext kcontext, BigDecimal weightMultiplier) {
        throw new UnsupportedOperationException("In the rule (" + kcontext.getRule().getName()
                + "), the scoreHolder class (" + getClass()
                + ") does not support a BigDecimal weightMultiplier (" + weightMultiplier + ").\n"
                + "If you're using constraint streams, maybe switch from penalizeBigDecimal() to penalizeLong().");
    }

    // ************************************************************************
    // Other match methods
    // ************************************************************************

    @Override
    public void addConstraintMatch(RuleContext kcontext, long weight) {
        score += weight;
        registerConstraintMatch(kcontext,
                () -> score -= weight,
                () -> SimpleLongScore.of(weight));
    }

    @Override
    public SimpleLongScore extractScore(int initScore) {
        return SimpleLongScore.ofUninitialized(initScore, score);
    }

}
