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

import at.fh.hagenberg.aist.gce.science.statistics.AbstractStatisticalTest;
import at.fh.hagenberg.aist.gce.science.statistics.Report;
import at.fh.hagenberg.aist.gce.science.statistics.distributions.NormalDistribution;

import java.util.Arrays;

/**
 * Implementation of the Shapiro-Wilk Test for normal distributions
 * <p>
 * Note: This code was modeled after https://github.com/datumbox/datumbox-framework/blob/957560901f3c87d3e9f6760263644a4d70b0a3b8/datumbox-framework-core/src/main/java/com/datumbox/framework/core/statistics/nonparametrics/onesample/ShapiroWilk.java
 *
 * @author Oliver Krauss on 15.10.2019
 */
public class ShapiroWilk extends AbstractStatisticalTest<double[]> implements NormalityTest {

    @Override
    public String getName() {
        return "Normality test - Shapiro-Wilk";
    }

    @Override
    public String getHypothesisHumanReadable() {
        return "isNormal";
    }

    @Override
    protected boolean getHypothesis() {
        //  tests below the pThreshold are NOT in a normal distribution
        return false;
    }

    @Override
    public boolean isNormalDistributed(double[] values) {
        return isNormalDistributed(values, this.pThreshold);
    }

    @Override
    public boolean isNormalDistributed(double[] values, double pThreshold) {
        return interpretValues(values, pThreshold, null);
    }

    @Override
    public double testNormalDistribution(double[] values) {
        return statisticalTest(values, null);
    }

    @Override
    protected double statisticalTest(double[] values, Report r) {
        // pre sort the values
        Arrays.sort(values);

        // Sanity check
        int length = values.length;
        if (length < 3) {
            throw new IllegalArgumentException("Shapiro Wilk is not possible with less than 3 values");
        }

        // reuseable values
        int midPoint = length / 2;
        double[] a = new double[midPoint + 1];
        double minimalThreshold = 1e-19;

        // check if range is valid for shapiro wilk
        double range = values[length - 1] - values[0];
        if (range < minimalThreshold) {
            throw new IllegalArgumentException("The range is too small.");
        }

        // polynomial coefficients
        double g[] = {-2.273, 0.459};
        double c1[] = {0.0, 0.221157, -0.147981, -2.07119, 4.434685, -2.706056};
        double c2[] = {0.0, 0.042981, -0.293762, -1.752461, 5.682633, -3.582633};
        double c3[] = {0.544, -0.39978, 0.025054, -6.714e-4};
        double c4[] = {1.3822, -0.77857, 0.062767, -0.0020322};
        double c5[] = {-1.5861, -0.31082, -0.083751, 0.0038915};
        double c6[] = {-0.4803, -0.082676, 0.0030302};
        // variables for run
        int i, j, i1;
        double ssassx, summ2, ssumm2, gamma;
        double a1, a2, m, s, sa, xi, sx, xx, y, w1;
        double fac, asa, an25, ssa, sax, rsn, ssx, xsx;
        double pw;
        double variableCount = (double) length;

        if (length == 3) {
            a[1] = 0.70710678;/* = sqrt(1/2) */
        } else {
            an25 = variableCount + 0.25;
            summ2 = 0.0;
            for (i = 1; i <= midPoint; i++) {
                a[i] = normalQuantile((i - 0.375) / an25, 0, 1); // p(X <= x),
                summ2 += a[i] * a[i];
            }
            summ2 *= 2.0;
            ssumm2 = Math.sqrt(summ2);
            rsn = 1.0 / Math.sqrt(variableCount);
            a1 = poly(c1, 6, rsn) - a[1] / ssumm2;

            /* Normalize a[] */
            if (values.length > 5) {
                i1 = 3;
                a2 = -a[2] / ssumm2 + poly(c2, 6, rsn);
                fac = Math.sqrt((summ2 - 2.0 * (a[1] * a[1]) - 2.0 * (a[2] * a[2])) / (1.0 - 2.0 * (a1 * a1) - 2.0 * (a2 * a2)));
                a[2] = a2;
            } else {
                i1 = 2;
                fac = Math.sqrt((summ2 - 2.0 * (a[1] * a[1])) / (1.0 - 2.0 * (a1 * a1)));
            }
            a[1] = a1;
            for (i = i1; i <= midPoint; i++) {
                a[i] /= -fac;
            }
        }

        // Check for correct sort order on range - scaled X
        xx = values[0] / range;
        sx = xx;
        sa = -a[1];
        for (i = 1, j = length - 1; i < length; j--) {
            xi = values[i] / range;
            if (xx - xi > minimalThreshold) {
                throw new IllegalArgumentException("The xx - xi is too big.");
            }
            sx += xi;
            i++;
            if (i != j) {
                sa += sign(i - j) * a[Math.min(i, j)];
            }
            xx = xi;
        }

        // Calculate W statistic as squared correlation between data and coefficients
        sa /= length;
        sx /= length;
        ssa = ssx = sax = 0.;
        for (i = 0, j = length - 1; i < length; i++, j--) {
            if (i != j) {
                asa = sign(i - j) * a[1 + Math.min(i, j)] - sa;
            } else {
                asa = -sa;
            }
            xsx = values[i] / range - sx;
            ssa += asa * asa;
            ssx += xsx * xsx;
            sax += asa * xsx;
        }
        // W1 equals (1-W) calculated to avoid excessive rounding error for W very near 1 (a potential problem in very large samples)
        ssassx = Math.sqrt(ssa * ssx);
        w1 = (ssassx - sax) * (ssassx + sax) / (ssa * ssx);
        double w = 1.0 - w1;

        if (r != null) {
            r.addReport("w", w);
        }

        // Calculate significance level for W
        if (length == 3) {
            double pi6 = 6.0 / Math.PI;
            double stqr = Math.PI / 3.0;
            pw = pi6 * (Math.asin(Math.sqrt(w)) - stqr);
            if (pw < 0.) {
                pw = 0;
            }
            return pw;
        }
        y = Math.log(w1);
        xx = Math.log(variableCount);
        if (length <= 11) {
            gamma = poly(g, 2, variableCount);
            if (y >= gamma) {
                pw = 1e-99;
                return pw;
            }
            y = -Math.log(gamma - y);
            m = poly(c3, 4, variableCount);
            s = Math.exp(poly(c4, 4, variableCount));
        } else { /* n >= 12 */
            m = poly(c5, 4, xx);
            s = Math.exp(poly(c6, 3, xx));
        }

        return new NormalDistribution(m, s).cumulativeProbability(y);
    }

    /**
     * checks if a value is positive or negative
     *
     * @param x value to check
     * @return positive=1, negative=-1 0=0
     */
    private static int sign(double x) {
        if (x == 0) {
            return 0;
        }
        return (x > 0) ? 1 : -1;
    }


    /**
     * Calculates the algebraic polynomial of order nord-1 with array of coefficients cc.
     * Zero order coefficient is cc(1) = cc[0]
     *
     * @param cc   coefficients
     * @param nord ?
     * @param x    ?
     * @return polynomial
     */
    private static double poly(double[] cc, int nord, double x) {
        double ret_val = cc[0];
        if (nord > 1) {
            double p = x * cc[nord - 1];
            for (int j = nord - 2; j > 0; --j) {
                p = (p + cc[j]) * x;
            }
            ret_val += p;
        }
        return ret_val;
    }


    /**
     * Compute the quantile function for the normal distribution. For small to moderate probabilities, algorithm referenced
     * below is used to obtain an initial approximation which is polished with a final Newton step. For very large arguments, an algorithm of Wichura is used.
     * Used by ShapiroWilk Test
     *
     * @param p     p value
     * @param mu    ?
     * @param sigma ?
     * @return
     */
    public static double normalQuantile(double p, double mu, double sigma) {
        // The inverse of cdf.
        if (sigma < 0) {
            throw new IllegalArgumentException("The sigma parameter must be positive.");
        } else if (sigma == 0) {
            return mu;
        }

        double r;
        double val;

        double q = p - 0.5;

        if (0.075 <= p && p <= 0.925) {
            r = 0.180625 - q * q;
            val = q * (((((((r * 2509.0809287301226727 + 33430.575583588128105) * r + 67265.770927008700853) * r
                + 45921.953931549871457) * r + 13731.693765509461125) * r + 1971.5909503065514427) * r + 133.14166789178437745) * r
                + 3.387132872796366608) / (((((((r * 5226.495278852854561 + 28729.085735721942674) * r + 39307.89580009271061) * r
                + 21213.794301586595867) * r + 5394.1960214247511077) * r + 687.1870074920579083) * r + 42.313330701600911252) * r + 1);
        } else { /* closer than 0.075 from {0,1} boundary */
            /* r = min(p, 1-p) < 0.075 */
            if (q > 0) {
                r = 1 - p;
            } else {
                r = p;/* = R_DT_Iv(p) ^=  p */
            }

            r = Math.sqrt(-Math.log(r)); /* r = sqrt(-log(r))  <==>  min(p, 1-p) = exp( - r^2 ) */

            if (r <= 5.0) { /* <==> min(p,1-p) >= exp(-25) ~= 1.3888e-11 */
                r += -1.6;
                val = (((((((r * 7.7454501427834140764e-4 + 0.0227238449892691845833) * r + 0.24178072517745061177) * r
                    + 1.27045825245236838258) * r + 3.64784832476320460504) * r + 5.7694972214606914055) * r
                    + 4.6303378461565452959) * r + 1.42343711074968357734) / (((((((r * 1.05075007164441684324e-9 + 5.475938084995344946e-4) * r
                    + 0.0151986665636164571966) * r + 0.14810397642748007459) * r + 0.68976733498510000455) * r + 1.6763848301838038494) * r
                    + 2.05319162663775882187) * r + 1.0);
            } else { /* very close to  0 or 1 */
                r += -5.0;
                val = (((((((r * 2.01033439929228813265e-7 + 2.71155556874348757815e-5) * r + 0.0012426609473880784386) * r
                    + 0.026532189526576123093) * r + 0.29656057182850489123) * r + 1.7848265399172913358) * r + 5.4637849111641143699) * r
                    + 6.6579046435011037772) / (((((((r * 2.04426310338993978564e-15 + 1.4215117583164458887e-7) * r
                    + 1.8463183175100546818e-5) * r + 7.868691311456132591e-4) * r + 0.0148753612908506148525) * r
                    + 0.13692988092273580531) * r + 0.59983220655588793769) * r + 1.0);
            }

            if (q < 0.0) {
                val = -val;
            }
            /* return (q >= 0.)? r : -r ;*/
        }
        return mu + sigma * val;
    }
}
