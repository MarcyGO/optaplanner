/*
 * Copyright 2020 Red Hat, Inc. and/or its affiliates.
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
package org.optaplanner.core.impl.score.director.drl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

import org.junit.jupiter.api.Test;
import org.kie.api.runtime.KieSession;
import org.kie.internal.event.rule.RuleEventManager;
import org.optaplanner.core.api.score.buildin.simple.SimpleScore;
import org.optaplanner.core.impl.domain.solution.descriptor.SolutionDescriptor;
import org.optaplanner.core.impl.score.buildin.SimpleScoreDefinition;

public class DrlScoreDirectorTest {

    @Test
    public void illegalStateExceptionThrownWhenConstraintMatchNotEnabled() {
        DrlScoreDirector<Object, ?> director = new DrlScoreDirector<>(mockDroolsScoreDirectorFactory(), false, false);
        director.setWorkingSolution(new Object());
        assertThatIllegalStateException()
                .isThrownBy(director::getConstraintMatchTotalMap)
                .withMessageContaining("constraintMatchEnabled");
    }

    @Test
    public void constraintMatchTotalsNeverNull() {
        DrlScoreDirector<Object, ?> director = new DrlScoreDirector<>(mockDroolsScoreDirectorFactory(), false, true);
        director.setWorkingSolution(new Object());
        assertThat(director.getConstraintMatchTotalMap()).isNotNull();
        assertThat(director.getConstraintMatchTotalMap()).isNotNull();
    }

    @Test
    public void indictmentMapNeverNull() {
        DrlScoreDirector<Object, ?> director = new DrlScoreDirector<>(mockDroolsScoreDirectorFactory(), false, true);
        director.setWorkingSolution(new Object());
        assertThat(director.getIndictmentMap()).isNotNull();
    }

    @SuppressWarnings("unchecked")
    private DrlScoreDirectorFactory<Object, SimpleScore> mockDroolsScoreDirectorFactory() {
        DrlScoreDirectorFactory<Object, SimpleScore> factory = mock(DrlScoreDirectorFactory.class);
        when(factory.getScoreDefinition()).thenReturn(new SimpleScoreDefinition());
        when(factory.getSolutionDescriptor()).thenReturn(mock(SolutionDescriptor.class));
        when(factory.newKieSession()).thenReturn(
                mock(KieSession.class, withSettings().extraInterfaces(RuleEventManager.class)));
        return factory;
    }
}
