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
 * A problem constraint is used in the {@link TrufflePatternProblem} and relates to the {@link at.fh.hagenberg.aist.gce.optimization.TruffleOptimizationProblem}
 * it defines which problem (human readable) the search space must be from.
 *
 * @author Oliver Krauss on 04.12.2018
 */
public class ProblemConstraint {

    /**
     * Human readable name of problem
     */
    private String name;

    public ProblemConstraint(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

}
