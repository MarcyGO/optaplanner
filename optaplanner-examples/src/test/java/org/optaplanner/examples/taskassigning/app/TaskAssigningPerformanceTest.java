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

package org.optaplanner.examples.taskassigning.app;

import java.util.stream.Stream;

import org.optaplanner.core.api.score.buildin.bendable.BendableScore;
import org.optaplanner.core.config.solver.EnvironmentMode;
import org.optaplanner.examples.common.app.SolverPerformanceTest;
import org.optaplanner.examples.taskassigning.domain.TaskAssigningSolution;

public class TaskAssigningPerformanceTest extends SolverPerformanceTest<TaskAssigningSolution, BendableScore> {

    private static final String UNSOLVED_DATA_FILE = "data/taskassigning/unsolved/50tasks-5employees.xml";

    @Override
    protected TaskAssigningApp createCommonApp() {
        return new TaskAssigningApp();
    }

    @Override
    protected Stream<TestData<BendableScore>> testData() {
        return Stream.of(
                testData(UNSOLVED_DATA_FILE, BendableScore.of(new int[] { 0 },
                        new int[] { -3925, -6293940, -7784, -20600 }), EnvironmentMode.REPRODUCIBLE),
                testData(UNSOLVED_DATA_FILE, BendableScore.of(new int[] { 0 },
                        new int[] { -3925, -6293940, -7883, -20476 }), EnvironmentMode.FAST_ASSERT));
    }
}
