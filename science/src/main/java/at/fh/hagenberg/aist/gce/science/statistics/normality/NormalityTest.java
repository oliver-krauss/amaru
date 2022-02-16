/*
 * Copyright (c) 2022 the original author or authors.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

package at.fh.hagenberg.aist.gce.science.statistics.normality;

import at.fh.hagenberg.aist.gce.science.statistics.StatisticalTest;

/**
 * Normality tests check if sample distributions are.
 * Values to be checked may need to apply specific conditinos for implementations of this interface.
 * @author Oliver Krauss on 15.10.2019
 */
public interface NormalityTest extends StatisticalTest<double[]> {

    /**
     * Checks if the given group of values is from a normal distribution
     * @param values to be checked
     * @return true if normal distribution, false if not according to pValue
     */
    boolean isNormalDistributed(double[] values);

    /**
     * Checks if the given group of values is from a normal distribution distribution
     * @param values to be checked
     * @param pThreshold probability threshold to be tested against
     * @return true if normal distribution, false if not according to pValue
     */
    boolean isNormalDistributed(double[] values, double pThreshold);

    /**
     * Calculates the probability that the given values are in a normal distribution
     * @param values to be checked for normal distribution
     * @return the pValue without interpretation
     */
    double testNormalDistribution(double[] values);
}
