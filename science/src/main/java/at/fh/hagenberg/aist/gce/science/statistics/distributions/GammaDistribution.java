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
public class GammaDistribution implements Distribution {

    /**
     * Amount of iterations the results will be refined
     */
    private static final int ITERATIONS = 1000;

    /**
     * "negligible" error after which results wont be refined anymore
     */
    private static final double EPSILON = 10e-9;

    /**
     * The SHAPE parameter (k)
     */
    private double alpha = 1.0;

    /**
     * The scale parameter (θ)
     */
    private double beta = 1.0;

    @Override
    public double probability(double value) {
        double prob = Math.pow(value, alpha - 1) * Math.pow(Math.E, -value / beta);
        prob /= (Math.pow(beta, alpha) * Math.exp(logGamma(value)));
        return prob;
    }

    @Override
    public double cumulativeProbability(double value) {
        if (value <= 0.0) {
            return 0.0;
        } else if (Double.isInfinite(value)) {
            return 1.0;
        } else {
            return regularizedGammaP(alpha, value / beta);
        }
    }

    private double logGamma(double value) {
        // Lanczos coefficients.
        double[] coefficients = {0.99999999999999709182, 57.156235665862923517, -59.597960355475491248,
            14.136097974741747174, -0.49191381609762019978, 0.33994649984811888699e-4,
            0.46523628927048575665e-4, -0.98374475304879564677e-4, 0.15808870322491248884e-3,
            -0.21026444172410488319e-3, 0.21743961811521264320e-3, -0.16431810653676389022e-3,
            0.84418223983852743293e-4, -0.26190838401581408670e-4, 0.36899182659531622704e-5,
        };

        if (Double.isNaN(value) || (value <= 0.0)) {
            return Double.NaN;
        }

        double sum = 0.0;
        for (int i = 1; i < coefficients.length; ++i) {
            sum += (coefficients[i] / (value + i));
        }
        sum += coefficients[0];

        double tmp = value + (607.0 / 128.0) + 0.5;
        return ((value + 0.5) * Math.log(tmp)) - tmp + (0.5 * Math.log(Math.PI + Math.PI)) + Math.log(sum) - Math.log(value);
    }

    public double regularizedGammaP(double alpha, double value) {
        if (value == 0.0) {
            return 0.0;
        } else if (value >= (alpha + 1.0)) {
            return 1.0 - regularizedGammaQ(alpha, value);
        }

        double n = 0.0;
        double an = 1.0 / alpha;
        double sum = an;
        while (Math.abs(an) > EPSILON && n < ITERATIONS) {

            n += 1.0;
            an *= (value / (alpha + n));
            sum += an;
        }
        return Math.exp(-value + (alpha * Math.log(value)) - logGamma(alpha)) * sum;
    }

    public double regularizedGammaQ(double alpha, double value) {
        if (value == 0.0) {
            return 1.0;
        } else if (value < (alpha + 1.0)) {
            return 1.0 - regularizedGammaP(alpha, value);
        }

        // create continued fraction
        return Math.exp(-value + (alpha * Math.log(value)) - logGamma(alpha)) * (1.0 / continuedFraction(alpha, value));
    }


    public double getAlpha() {
        return alpha;
    }

    public void setAlpha(double alpha) {
        this.alpha = alpha;
    }

    public double getBeta() {
        return beta;
    }

    public void setBeta(double beta) {
        this.beta = beta;
    }

    private double continuedFraction(double alpha, double value) {
        double r, a, b, c, p0, p1, p2, q0, q1, q2;
        int n;
        double relativeError;

        p0 = 1.0;
        p1 = 1.0 - alpha + value;
        q0 = 0.0;
        q1 = 1.0;
        c = p1 / q1;
        n = 0;

        relativeError = Double.MAX_VALUE;
        while (n < ITERATIONS && relativeError > EPSILON) {
            n++;
            a = ((2.0 * n) + 1.0) - alpha + value;
            b = n * (alpha - n);
            p2 = a * p1 + b * p0;
            q2 = a * q1 + b * q0;

            if (Double.isInfinite(p2) || Double.isInfinite(q2)) {
                // need to scale
                if (a != 0.0) {
                    p2 = p1 + (b / a * p0);
                    q2 = q1 + (b / a * q0);
                } else if (b != 0) {
                    p2 = (a / b * p1) + p0;
                    q2 = (a / b * q1) + q0;

                } else {
                    return -1.0;
                }
            }

            r = p2 / q2;
            relativeError = Math.abs(r / c - 1.0);

            // next iteration
            c = p2 / q2;
            p0 = p1;
            p1 = p2;
            q0 = q1;
            q1 = q2;
        }

        return c;
    }
}
