/*
 * Copyright (c) 2022 the original author or authors.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

package at.fh.hagenberg.aist.gce.science.statistics.difference;

import at.fh.hagenberg.aist.gce.science.statistics.StatisticalTest;

/**
 * Tests of this class test if groups of samples originate from the same distribution.
 * If the test succeeds it does means that the groups are different.
 *
 * Note: Several tests implementing this interface may have preconditions such as the amount of groups (2 or *).
 *
 * @author Oliver Krauss on 16.10.2019
 */
public interface DifferentDistributionsTest extends StatisticalTest<double[][]> {

    /**
     * Checks if the given groups are not from the same distribution
     * @param values to be checked [groups][valuesInGroup]
     * @return true if different distributions, false if from same distribution
     */
    boolean isFromDifferentDistributions(double[][] values);

    /**
     * Checks if the given groups are not from the same distribution
     * @param values to be checked [groups][valuesInGroup]
     * @param pThreshold probability threshold to be tested against
     * @return true if different distributions, false if from same distribution
     */
    boolean isFromDifferentDistributions(double[][] values, double pThreshold);

    /**
     * Calculates the probability that the given values are not from the same distribution
     * @param values to be checked [groups][valuesInGroup]
     * @return the pValue without interpretation
     */
    double testForDifferentDistributions(double[][] values);
}
