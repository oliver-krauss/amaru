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
 * A cachet constraint, is used in the {@link TrufflePatternProblem} and relates to the {@link at.fh.hagenberg.machinelearning.core.fitness.Cachet}
 * it defines the cachet that a pattern should apply to / does apply to.
 *
 * @author Oliver Krauss on 04.12.2018
 */
public class CachetConstraint extends RangeConstraint<Double> {

    /**
     * Qualifying name of cachet (with version)
     */
    private String name;

    public CachetConstraint(String name) {
        this.name = name;
    }

    /**
     * Setter for upperLimit >= cachet >= lowerLimit (either can be null)
     *
     * @param name       of Cachet
     * @param upperLimit of quality value (or null)
     * @param lowerLimit of quality value (or null)
     */
    public CachetConstraint(String name, Double upperLimit, Double lowerLimit) {
        super(upperLimit, lowerLimit);
        this.name = name;
    }

    public CachetConstraint(String name, Double exactly) {
        super(exactly);
        this.name = name;
    }

    public String getName() {
        return name;
    }

}
