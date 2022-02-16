/*
 * Copyright (c) 2022 the original author or authors.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

package at.fh.hagenberg.aist.gce.science.statistics;

/**
 * Normality tests check if sample distributions are.
 * Values to be checked may need to apply specific conditinos for implementations of this interface.
 * @author Oliver Krauss on 15.10.2019
 */
public interface StatisticalTest<T> {

    /**
     * Sets the probability value that is the THRESHOLD to Accept or Reject a hypothesis.
     * @param value p-value threshold
     */
    void setPThreshold(double value);

    /**
     * Creates a Report with whatever the statistical test shall be tested for.
     * @param values to be tested
     * @return       full Report of statistical values produced by the test
     */
    Report report(T values);

    /**
     * Creates a Report with whatever the statistical test shall be tested for.
     * @param values to be tested
     * @param report prepared report with TITLES of the datasets
     * @return       full Report of statistical values produced by the test
     */
    Report report(T values, Report report);

    /**
     * Human readable name
     * @return name of the test
     */
    public abstract String getName();

    /**
     * Human readable name of the hypothesis (ex. isNormalDistribution, isNotFromSameDistribution...)
     * @return name of hypothesis
     */
    String getHypothesisHumanReadable();
}
