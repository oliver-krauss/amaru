/*
 * Copyright (c) 2022 the original author or authors.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

package at.fh.hagenberg.aist.gce.science.statistics.distributions;

/**
 * Interface for distributions
 * @author Oliver Krauss on 16.10.2019
 */
public interface Distribution {

    /**
     * Probability of value in the distribution
     * @param value probablity will be calculated for
     * @return probability for value
     */
    double probability(double value);

    /**
     * Cumulative probability of value in the distribution
     * @param value probablity will be calculated for
     * @return probability for value
     */
    double cumulativeProbability(double value);

}
