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

import at.fh.hagenberg.aist.gce.science.statistics.data.Dataset;
import at.fh.hagenberg.aist.gce.science.statistics.difference.KruskalWallis;
import at.fh.hagenberg.aist.gce.science.statistics.difference.MannWhitneyU;
import at.fh.hagenberg.aist.gce.science.statistics.normality.NormalityTest;
import at.fh.hagenberg.aist.gce.science.statistics.normality.ShapiroWilk;

/**
 * Class that automates statistical testing of given DataSets.
 * <p>
 * Warning: Don't be lazy. Thoroughly check the results!
 *
 * @author Oliver Krauss on 24.10.2019
 */
public class AutoStatistics implements StatisticalTest<Dataset> {

    /**
     * Threshold for reported pValues
     */
    private double pThreshold = 0.001;

    @Override
    public void setPThreshold(double value) {

    }

    @Override
    public Report report(Dataset values) {
        return report(values, new Report(this.getName()));
    }

    @Override
    public Report report(Dataset values, Report report) {
        StatisticalTest<double[][]> test = null;

        if (values.getData().length > 2) {
            // use tests for multiple groups
            if (isParametric(values, report)) {
                if (isIndependent(values, report)) {
                    test = new KruskalWallis();
                } else {
                    // TODO #145 -> Firendman for dependent variables
                    report.addReport("ERROR", "String", "Friedman Test not yet implemented");
                }

            } else {
                if (isIndependent(values, report)) {
                    // TODO #147 -> ANOVA for dependent variables
                    report.addReport("ERROR", "String", "ANOVA Test not yet implemented");
                } else {
                    // TODO #147 -> ANOVA for dependent variables
                    report.addReport("ERROR", "String", "ANOVA Test not yet implemented");
                }
            }
        } else {
            // use tests for 2 groups
            if (isParametric(values, report)) {
                if (isIndependent(values, report)) {
                    test = new MannWhitneyU();
                } else {
                    // TODO #146 -> Wilcoxson
                    report.addReport("ERROR", "String", "Wilcoxon test not yet implemented");
                }
            } else {
                if (isIndependent(values, report)) {
                    // TODO #148 -> t test for independent variables
                    report.addReport("ERROR", "String", "T Test not yet implemented");
                } else {
                    // TODO #148 -> t test for dependent variables
                    report.addReport("ERROR", "String", "T Test not yet implemented");
                }
            }
        }

        // conduct test
        if (test != null) {
            test.setPThreshold(pThreshold);
            report.addReport(test.getName(), test.report(values.getData()));
            report.addReport(this.getHypothesisHumanReadable(), "boolean", report.getReport(test.getName()).getValue(test.getHypothesisHumanReadable()));
        } else {
            report.addReport(this.getHypothesisHumanReadable(), "boolean", "false");
        }

        return report;
    }

    @Override
    public String getName() {
        return "Auto Statistics";
    }

    @Override
    public String getHypothesisHumanReadable() {
        return "isSignificant";
    }


    public boolean isIndependent(Dataset dataset, Report report) {
        // data groups that were made on different observations can never be dependent
        //   (ex. comparing results of two different fitness functions)
        //   (ex. comparing value X of the first 100 samples with the value X of 100 DIFFERENT samples)
        // data groups that were made on the SAME observation can be dependent
        //   (ex. comparing mutation rate and crossover rate in one fitness function made over same runs)
        //   to find out IF groups are dependent there is the Chi-Square Test of Independence (TODO #154 -> also add test to reports)

        // currently we ONLY support independent tests. If that changes we will likely have to let the user decide dependence
        return true;
    }

    public boolean isParametric(Dataset dataset, Report report) {
        NormalityTest normalDistributionTest = new ShapiroWilk();
        Report normalSubReport = new Report("Normal Distribution");

        for (int i = 0; i < dataset.getData().length; i++) {
            try {
                normalSubReport.addReport(dataset.getTitle(i), normalDistributionTest.report(dataset.getData(i)));
            } catch (IllegalArgumentException e) {
                // if range is too small the data can't be normal distributed
                Report r = new Report(dataset.getTitle(i));
                r.addReport("isNormal", false);
                normalSubReport.addReport(dataset.getTitle(i), r);
            }

        }

        boolean parametric = normalSubReport.getChildReports().values().stream().anyMatch(x -> x.getValue("isNormal").equals("false"));

        if (!parametric) {
            // TODO #144 next test would be to use Durbin-Watson to test for independence of residuals

            // TODO #144 if still not parametric we would need to check for "linearity" whatever that is.
            // TODO #144 According to the            internet if you test for Homoscedacity linearity automatically is included though

            // TODO #144 if still not parametric we would have to check for Homoscedacity
        }

        report.addReport("isParametric", parametric);
        normalSubReport.addReport("isParametric", parametric);
        report.addReport("Normal Distribution", normalSubReport);
        return parametric;
    }


}
