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
package org.optaplanner.core.impl.score.director.drl.testgen;

import java.io.File;
import java.util.List;

import org.kie.api.KieBase;
import org.optaplanner.core.api.score.Score;
import org.optaplanner.core.impl.domain.solution.descriptor.SolutionDescriptor;
import org.optaplanner.core.impl.score.director.drl.DrlScoreDirectorFactory;

public class TestGenDrlScoreDirectorFactory<Solution_, Score_ extends Score<Score_>>
        extends DrlScoreDirectorFactory<Solution_, Score_> {

    private final List<String> scoreDrlList;
    private final List<File> scoreDrlFileList;

    /**
     * @param solutionDescriptor never null
     * @param kieBase never null
     * @param scoreDrlList
     * @param scoreDrlFileList
     */
    public TestGenDrlScoreDirectorFactory(SolutionDescriptor<Solution_> solutionDescriptor,
            KieBase kieBase, List<String> scoreDrlList, List<File> scoreDrlFileList) {
        super(solutionDescriptor, kieBase);
        this.scoreDrlList = scoreDrlList;
        this.scoreDrlFileList = scoreDrlFileList;
    }

    @Override
    public TestGenDrlScoreDirector<Solution_, Score_> buildScoreDirector(
            boolean lookUpEnabled, boolean constraintMatchEnabledPreference) {
        return new TestGenDrlScoreDirector<>(this, lookUpEnabled, constraintMatchEnabledPreference, scoreDrlList,
                scoreDrlFileList);
    }

}
