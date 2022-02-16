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

/**
 * Constraints inside can be (upperLimit &| lowerLimit) || exactly
 *
 * @author Oliver Krauss on 05.12.2018
 */

public class RangeConstraint<T> {

    /**
     * If set, constraint will satisfy <= uperLimit
     */
    protected T upperLimit;

    /**
     * If set, constraint will satisfy >= lowerLimit
     */
    protected T lowerLimit;

    /**
     * If set, constraint will satisfy == exactly
     * Note: Refrain from using, as floating point values have a high chance of never being "=="
     */
    protected T exactly;

    public RangeConstraint() {
    }

    public RangeConstraint(T upperLimit, T lowerLimit) {
        this.upperLimit = upperLimit;
        this.lowerLimit = lowerLimit;
    }

    public RangeConstraint(T exactly) {
        this.exactly = exactly;
    }

    public T getUpperLimit() {
        return upperLimit;
    }

    public T getLowerLimit() {
        return lowerLimit;
    }

    public T getExactly() {
        return exactly;
    }
}
