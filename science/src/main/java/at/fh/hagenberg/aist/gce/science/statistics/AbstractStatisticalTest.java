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
 * Base class for statistical tests implementing some common features all statistical tests seem to adhere to
 *
 * @author Oliver Krauss on 16.10.2019
 */
public abstract class AbstractStatisticalTest<T> implements StatisticalTest<T> {

    /**
     * Results that are below the pThreshold are statistically significant.
     */
    protected double pThreshold = 0.05;

    @Override
    public void setPThreshold(double value) {
        this.pThreshold = value;
    }

    @Override
    public Report report(T values) {
        Report r = new Report(getName());
        interpretValues(values, this.pThreshold, r);
        return r;
    }

    @Override
    public Report report(T values, Report report) {
        interpretValues(values, this.pThreshold, report);
        return report;
    }

    protected boolean interpretValues(T values, double pValue, Report r) {
        boolean interpretation = !getHypothesis();

        // interpret
        double probability = statisticalTest(values, r);
        if (probability <= pValue || probability >= (1.0 - pValue)) {
            interpretation = !interpretation;
        }

        // report
        if (r != null) {
            r.addReport("pThreshold", pValue);
            r.addReport(getHypothesisHumanReadable(), interpretation);
            r.addReport("pValue", probability);
        }

        return interpretation;
    }


    /**
     * The hypothesis TRUE or FALSE if the pValue is below the pThreshold
     * @return assumption of p threshold
     */
    protected abstract boolean getHypothesis();

    /**
     * The core of the statistical test doing the actual statistics.
     * @param values that the statistics will be calculated for
     * @param r      report (can be null!) that will be generated
     * @return       the pValue for the test
     */
    protected abstract double statisticalTest(T values, Report r);
}
