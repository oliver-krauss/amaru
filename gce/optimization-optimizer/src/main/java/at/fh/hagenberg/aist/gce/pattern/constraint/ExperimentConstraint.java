/*
 * Copyright (c) 2022 the original author or authors.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

package at.fh.hagenberg.aist.gce.pattern.constraint;

import at.fh.hagenberg.aist.gce.pattern.TrufflePatternProblem;

/**
 * A experiment constraint is used in the {@link TrufflePatternProblem} and relates to the {@link at.fh.hagenberg.machinelearning.analytics.graph.nodes.AnalyticsNode}
 * it defines which experiment (by title) the solution must be from.
 *
 * @author Oliver Krauss on 06.02.2019
 */
public class ExperimentConstraint {

    /**
     * Title of experiment
     */
    private String title;

    public ExperimentConstraint(String title) {
        this.title = title;
    }

    public String getTitle() {
        return title;
    }

}
