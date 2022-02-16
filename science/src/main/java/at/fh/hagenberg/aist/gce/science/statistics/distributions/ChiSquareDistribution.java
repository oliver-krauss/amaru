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
 * Chi²Distribution
 *
 * @author Oliver Krauss on 16.10.2019
 */
public class ChiSquareDistribution implements Distribution {

    /**
     * Amount of iterations the results will be refined
     */
    private static final int ITERATIONS = 1000;

    /**
     * "negligible" error after which results wont be refined anymore
     */
    private static final double EPSILON = 10e-9;

    /**
     * Degrees of Freedom
     */
    int degreesOfFreedom;

    /**
     * Chi² is a spezialisation of the Gamma distribution
     */
    private GammaDistribution gamma = new GammaDistribution();

    public ChiSquareDistribution() {
    }

    public ChiSquareDistribution(int degreesOfFreedom) {
        setDegreesOfFreedom(degreesOfFreedom);
    }

    @Override
    public double probability(double value) {
        return gamma.probability(value);
    }

    @Override
    public double cumulativeProbability(double value) {
        return 1.0 - rightTailProbability(value);
    }

    public double rightTailProbability(double value) {
        double q, p, k, t, a;

        if ((degreesOfFreedom == 1) & (value > 1000.0)) {
            return 0;
        }

        if ((value > 1000) || (degreesOfFreedom > ITERATIONS)) {
            q = rightTailProbability((value - degreesOfFreedom) * (value - degreesOfFreedom) / (2 * degreesOfFreedom)) / 2;
            if (value > degreesOfFreedom) {
                return q;
            }
            return 1 - q;
        }

        p = Math.exp(-0.5 * value);

        if ((degreesOfFreedom % 2) == 1) {
            p = p * Math.sqrt(2 * value / Math.PI);
        }

        k = degreesOfFreedom;

        while (k >= 2) {
            p = p * value / k;
            k = k - 2;
        }

        t = p;
        a = degreesOfFreedom;

        while (t > EPSILON * p) {
            a = a + 2;
            t = t * value / a;
            p = p + t;
        }

        return p;
    }

    public int getDegreesOfFreedom() {
        return degreesOfFreedom;
    }

    public void setDegreesOfFreedom(int degreesOfFreedom) {
        if (degreesOfFreedom > 0) {
            this.degreesOfFreedom = degreesOfFreedom;
            gamma.setAlpha((double) degreesOfFreedom / 2.0);
            gamma.setBeta(2.0);
        }
    }
}
