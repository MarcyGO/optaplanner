/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates.
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
package org.optaplanner.core.impl.score.director.drl.testgen;

import org.kie.api.runtime.KieSession;
import org.optaplanner.core.impl.score.director.drl.testgen.operation.TestGenKieSessionFireAllRules;
import org.optaplanner.core.impl.solver.event.AbstractEventSupport;

public class TestGenKieSessionEventSupport extends AbstractEventSupport<TestGenKieSessionListener>
        implements TestGenKieSessionListener {

    @Override
    public void afterFireAllRules(KieSession kieSession, TestGenKieSessionJournal journal,
            TestGenKieSessionFireAllRules fire) {
        for (TestGenKieSessionListener listener : eventListenerSet) {
            listener.afterFireAllRules(kieSession, journal, fire);
        }
    }

}
