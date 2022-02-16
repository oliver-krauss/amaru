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
 * The test value constraint, is used in the {@link TrufflePatternProblem} and relates to the {@link at.fh.hagenberg.aist.gce.optimization.test.TruffleTestValue}
 * It can either define values to be searched for, the type to be searched for or both.
 *
 * @author Oliver Krauss on 07.02.2019
 */
public class TestValueConstraint<T> extends RangeConstraint<T> {

    /**
     * Type of the value
     */
    private String type;

    /**
     * true -> Input;
     * false -> Output;
     * null -> both
     */
    private Boolean input;

    public TestValueConstraint(String type, Boolean input, T upperLimit, T lowerLimit) {
        super(upperLimit, lowerLimit);
        this.type = type;
        this.input = input;
    }

    public TestValueConstraint(String type, Boolean input, T exactly) {
        super(exactly);
        this.type = type;
        this.input = input;
    }

    public String getType() {
        return type;
    }

    public Boolean getInput() {
        return input;
    }
}
