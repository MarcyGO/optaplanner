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
package org.optaplanner.core.impl.score.director.drl.testgen.reproducer;

import org.optaplanner.core.api.score.Score;

public class TestGenCorruptedScoreException extends RuntimeException {

    private final Score<?> workingScore;
    private final Score<?> uncorruptedScore;

    public TestGenCorruptedScoreException(Score<?> workingScore, Score<?> uncorruptedScore) {
        super("Working: " + workingScore + ", uncorrupted: " + uncorruptedScore);
        this.workingScore = workingScore;
        this.uncorruptedScore = uncorruptedScore;
    }

    public Score<?> getWorkingScore() {
        return workingScore;
    }

    public Score<?> getUncorruptedScore() {
        return uncorruptedScore;
    }

}
