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
import at.fh.hagenberg.aist.gce.science.statistics.distributions.NormalDistribution;

import java.util.Arrays;

/**
 * Implementation of the Mann-Whitney-U Test for 2 non-parametric groups.
 *
 * The test does not support any ties correction (yet), and also no two tailing.
 *
 * If you NEED two-tailed correction just divide the pThreshold by half
 *   (a two tailed analysis is literally just 2*pValue which has the same outcome)
 *
 * @author Oliver Krauss on 16.10.2019
 */
public class MannWhitneyU extends AbstractStatisticalTest<double[][]> implements DifferentDistributionsTest {

    @Override
    public String getName() {
        return "Different distributions test - Mann-Whitney-U";
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
        if (values.length != 2) {
            throw new IllegalArgumentException("Mann-Whitney-U only works with 2 groups");
        }

        // pre-format
        for (int i = 0; i < values.length; i++) {
            Arrays.sort(values[i]);
        }

        // the sample with the least columns has to be left
        int left = 0;
        int right = 1;
        if (values[0].length > values[1].length) {
            left = 1;
            right = 0;
        }
        int sizeLeft = values[left].length;
        int sizeRight = values[right].length;

        double[] flatValues = ArrayUtils.flatten(values);
        Arrays.sort(flatValues);
        double[] ranks = ArrayUtils.rank(flatValues);

        // calculate rank sum
        double rankSumLeft = getRankSum(values, left, flatValues, ranks);
        double rankSumRight = getRankSum(values, right, flatValues, ranks);

        // calculate U
        double ULeft = sizeLeft * sizeRight + sizeLeft * (sizeLeft + 1) / 2.0 - rankSumLeft;
        double URight = sizeLeft * sizeRight + sizeRight * (sizeRight + 1) / 2.0 - rankSumRight;
        double U = ULeft <= URight ? ULeft : URight;

        double mean = sizeLeft * sizeRight / 2.0;
        double stdDev = Math.sqrt(mean * (sizeLeft + sizeRight + 1) / 6.0);
        double z = Math.abs(Math.abs(U - mean) - 1 / 2.0) / stdDev;
        double effectR = z / Math.sqrt(sizeLeft + sizeRight);

        if (r != null) {
            r.addReport("U", U);
            r.addReport("mean", mean);
            r.addReport("standardDeviation", stdDev);
            r.addReport("zScore", z);
            r.addReport("effectR", effectR);
        }

        // get the pValue
        return 1.0 - new NormalDistribution().cumulativeProbability(z);
    }

    private double getRankSum(double[][] values, int pos, double[] flatValues, double[] ranks) {
        double rankSum = 0.0;
        for (int sample = 0; sample < values[pos].length; sample++) {
            double value = values[pos][sample];
            boolean found = false;
            int position = -1;
            for (int k = 0; !found; k++) {
                if (flatValues[k] == value) {
                    found = true;
                    position = k;
                }
            }
            rankSum += ranks[position];
        }
        return rankSum;
    }
}
