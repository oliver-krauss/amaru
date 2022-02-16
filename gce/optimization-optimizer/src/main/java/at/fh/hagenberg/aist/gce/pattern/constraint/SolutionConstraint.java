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

import at.fh.hagenberg.machinelearning.core.Solution;
import at.fh.hagenberg.aist.gce.pattern.TrufflePatternProblem;

/**
 * A solution Constraint is used in the {@link TrufflePatternProblem} and relates to the {@link Solution connected to the tree}
 * it defines constraints on the overall solution quality of that a pattern should apply to.
 *
 * @author Oliver Krauss on 05.12.2018
 */
public class SolutionConstraint extends RangeConstraint<Double> {

    public SolutionConstraint() {
    }

    public SolutionConstraint(Double upperLimit, Double lowerLimit) {
        super(upperLimit, lowerLimit);
    }

    public SolutionConstraint(Double exactly) {
        super(exactly);
    }
}
