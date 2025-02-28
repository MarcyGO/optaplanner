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
package org.optaplanner.core.impl.score.director.drl.testgen.fact;

import java.util.Collections;
import java.util.List;

class TestGenExistingInstanceValueProvider extends TestGenAbstractValueProvider<Object> {

    private final String identifier;
    private List<TestGenFact> facts;

    public TestGenExistingInstanceValueProvider(Object value, String identifier, TestGenFact fact) {
        super(value);
        this.identifier = identifier;
        this.facts = Collections.singletonList(fact);
    }

    @Override
    public List<TestGenFact> getRequiredFacts() {
        return facts;
    }

    @Override
    public String toString() {
        return identifier;
    }

}
