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

import at.fh.hagenberg.aist.gce.science.statistics.AbstractStatisticalTest;
import at.fh.hagenberg.aist.gce.science.statistics.Report;
import at.fh.hagenberg.aist.gce.science.statistics.distributions.ChiSquareDistribution;
import at.fh.hagenberg.aist.gce.science.statistics.distributions.NormalDistribution;

import java.util.Arrays;

/**
 * Implementation of the Kruskal-Wallis Test for multiple non-parametric groups
 *
 * @author Oliver Krauss on 16.10.2019
 */
public class KruskalWallis extends AbstractStatisticalTest<double[][]> implements DifferentDistributionsTest {

    @Override
    public String getName() {
        return "Different distributions test - Kruskal-Wallis";
    }

    @Override
    public String getHypothesisHumanReadable() {
        return "areDifferentDistributions";
    }

    @Override
    protected boolean getHypothesis() {
        // tests below the pThreshold are from DIFFERENT distributions
        return true;
    }

    @Override
    public boolean isFromDifferentDistributions(double[][] values) {
        return isFromDifferentDistributions(values, this.pThreshold);
    }

    @Override
    public boolean isFromDifferentDistributions(double[][] values, double pThreshold) {
        return interpretValues(values, pThreshold, null);
    }

    @Override
    public double testForDifferentDistributions(double[][] values) {
        return statisticalTest(values, null);
    }

    @Override
    protected double statisticalTest(double[][] values, Report r) {
        // do sanity checks
        if (values.length < 3) {
            throw new IllegalArgumentException("Kruskal Wallis can only perform with 3 or more groups");
        }
        int dataPoints = values[0].length;
        for (int i = 0; i < values.length; i++) {
            if (values[i].length != dataPoints) {
                throw new IllegalArgumentException("All groups must have same amount of values");
            }
        }
        dataPoints = values.length;
        values = ArrayUtils.flip(values);

        // prepare arrays
        int allValues = values.length * dataPoints;
        double[] flatValues = ArrayUtils.flatten(values);
        Arrays.sort(flatValues);
        double[] ranks = ArrayUtils.rank(flatValues);

        int i, sample;
        double value;
        boolean found;
        int position;
        double term1, term2;

        // calculate the rank sums
        double[] sumRanks = new double[values[0].length];
        double[] avgRanks = new double[values[0].length];
        for (sample = 0; sample < values[0].length; sample++) {
            for (int j = 0; j < values.length; j++) {
                value = values[j][sample];
                found = false;
                position = -1;
                for (int k = 0; !found; k++) {
                    if (flatValues[k] == value) {
                        found = true;
                        position = k;
                    }
                }
                sumRanks[sample] += ranks[position];
            }
        }

        // calculate the avg ranks
        for (i = 0; i < values[0].length; i++) {
            avgRanks[i] = sumRanks[i] / (double) values.length;
        }

        // calculate the H value
        term1 = 12.0 / (allValues * (allValues + 1) * (allValues / sumRanks.length));
        term2 = 0.0;
        for (i = 0; i < sumRanks.length; i++) {
            value = (sumRanks[i] * sumRanks[i]);
            term2 += value;
        }
        double H = (term1 * term2) - (3.0 * (allValues + 1.0));
        int dF = sumRanks.length - 1;

        if (r != null) {
            r.addReport("count", allValues);
            r.addReport("H", H);
            r.addReport("degreesOfFreedom", dF);

            for (int i1 = 0; i1 < dataPoints; i1++) {
                Report subReport = r.getReport(i1);
                subReport.addReport("count", values.length);
                subReport.addReport("rankSum", sumRanks[i1]);
            }
        }

        // get the pValue
        ChiSquareDistribution chi = new ChiSquareDistribution(dF);
        return chi.cumulativeProbability(H);
    }
}
