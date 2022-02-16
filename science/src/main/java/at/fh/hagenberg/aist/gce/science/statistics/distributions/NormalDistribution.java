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
public class NormalDistribution implements Distribution {

    /**
     * the "center point" of the distribution
     */
    private double mean = 0.0;

    /**
     * the "highest point" of the distribution
     */
    private double sigma = 1.0;

    public NormalDistribution() {
    }

    public NormalDistribution(double mean, double sigma) {
        this.mean = mean;
        this.sigma = sigma;
    }

    @Override
    public double probability(double value) {
        double result = Math.pow(Math.E, -(value - mean) * (value - mean) / (2.0 * sigma * sigma));
        result /= Math.sqrt(2.0 * Math.PI * sigma * sigma);
        return result;
    }

    @Override
    public double cumulativeProbability(double value) {
        double mean = this.mean;
        if (mean < 0) {
            mean *= -1;
            value *= -1;
        }
        double z = (value - mean) / sigma;
        return getTipifiedProbability(z, false);

    }

    public double getTipifiedProbability(double z, boolean upper) {
        double ltone = 7.0,
            utzero = 18.66,
            con = 1.28,
            a1 = 0.398942280444,
            a2 = 0.399903438504,
            a3 = 5.75885480458,
            a4 = 29.8213557808,
            a5 = 2.62433121679,
            a6 = 48.6959930692,
            a7 = 5.92885724438,
            b1 = 0.398942280385,
            b2 = 3.8052e-8,
            b3 = 1.00000615302,
            b4 = 3.98064794e-4,
            b5 = 1.986153813664,
            b6 = 0.151679116635,
            b7 = 5.29330324926,
            b8 = 4.8385912808,
            b9 = 15.1508972451,
            b10 = 0.742380924027,
            b11 = 30.789933034,
            b12 = 3.99019417011;

        double y, alnorm;

        if (z < 0) {
            upper = !upper;
            z = -z;
        }
        if (z <= ltone || upper && z <= utzero) {
            y = 0.5 * z * z;
            if (z > con) {
                alnorm = b1 * Math.exp(-y) /
                    (z - b2 +
                        b3 /
                            (z + b4 +
                                b5 /
                                    (z - b6 +
                                        b7 / (z + b8 - b9 / (z + b10 + b11 / (z + b12))))));
            } else {
                alnorm = 0.5 -
                    z *
                        (a1 - a2 * y / (y + a3 - a4 / (y + a5 + a6 / (y + a7))));
            }
        } else {
            alnorm = 0;
        }
        if (!upper) {
            alnorm = 1 - alnorm;
        }

        return alnorm;
    }

    public double inverseNormalDistribution(double value) {
        double a1 = -3.969683028665376e+01;
        double a2 = 2.209460984245205e+02;
        double a3 = -2.759285104469687e+02;
        double a4 = 1.383577518672690e+02;
        double a5 = -3.066479806614716e+01;
        double a6 = 2.506628277459239e+00;

        double b1 = -5.447609879822406e+01;
        double b2 = 1.615858368580409e+02;
        double b3 = -1.556989798598866e+02;
        double b4 = 6.680131188771972e+01;
        double b5 = -1.328068155288572e+01;

        double c1 = -7.784894002430293e-03;
        double c2 = -3.223964580411365e-01;
        double c3 = -2.400758277161838e+00;
        double c4 = -2.549732539343734e+00;
        double c5 = 4.374664141464968e+00;
        double c6 = 2.938163982698783e+00;

        double d1 = 7.784695709041462e-03;
        double d2 = 3.224671290700398e-01;
        double d3 = 2.445134137142996e+00;
        double d4 = 3.754408661907416e+00;

        double p_low = 0.02425;
        double p_high = 1.0 - p_low;

        double q;
        double x = 0.0;
        double r;

        if (value <= 0) {
            return Double.NEGATIVE_INFINITY;
        }

        if (value >= 1) {
            return Double.POSITIVE_INFINITY;
        }

        //Rational approximation for lower region.
        if (value < p_low) {
            q = Math.sqrt(-2.0 * Math.log(value));
            x = (((((c1 * q + c2) * q + c3) * q + c4) * q + c5) * q + c6) /
                ((((d1 * q + d2) * q + d3) * q + d4) * q + 1.0);

            return x;
        }

        //Rational approximation for central region.
        if (value <= p_high) {
            q = value - 0.5;
            r = q * q;
            x = (((((a1 * r + a2) * r + a3) * r + a4) * r + a5) * r + a6) * q /
                (((((b1 * r + b2) * r + b3) * r + b4) * r + b5) * r + 1.0);

            return x;
        }

        //Rational approximation for upper region.
        if (p_high < value) {
            q = Math.sqrt(-2.0 * Math.log(1.0 - value));
            x = -(((((c1 * q + c2) * q + c3) * q + c4) * q + c5) * q + c6) /
                ((((d1 * q + d2) * q + d3) * q + d4) * q + 1.0);

            return x;
        }

        return x;
    }


    public double getMean() {
        return mean;
    }

    public void setMean(double mean) {
        this.mean = mean;
    }

    public double getSigma() {
        return sigma;
    }

    public void setSigma(double sigma) {
        this.sigma = sigma;
    }
}
