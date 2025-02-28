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
package org.optaplanner.core.impl.score.director.drl.testgen.fact;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

class TestGenSetValueProvider extends TestGenAbstractValueProvider<Set<?>> {

    private final String identifier;
    private final Type typeArgument;
    private final Map<Object, TestGenFact> existingInstances;
    private final List<Class<?>> imports = new ArrayList<>();
    private final List<TestGenFact> requiredFacts;

    public TestGenSetValueProvider(Set<?> value, String identifier, Type genericType,
            Map<Object, TestGenFact> existingInstances) {
        super(value);
        this.identifier = identifier;
        this.typeArgument = genericType;
        this.existingInstances = existingInstances;
        imports.add(HashSet.class);
        imports.add((Class<?>) genericType);
        requiredFacts = value.stream()
                .map(existingInstances::get)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    @Override
    public List<TestGenFact> getRequiredFacts() {
        return requiredFacts;
    }

    @Override
    public List<Class<?>> getImports() {
        return imports;
    }

    @Override
    public void printSetup(StringBuilder sb) {
        String e = ((Class<?>) typeArgument).getSimpleName();
        sb.append(String.format("        HashSet<%s> %s = new HashSet<%s>();\n", e, identifier, e));
        for (Object item : value) {
            sb.append(String.format("        //%s\n", item));
            sb.append(String.format("        %s.add(%s);\n", identifier, existingInstances.get(item)));
        }
    }

    @Override
    public String toString() {
        return identifier;
    }

}
