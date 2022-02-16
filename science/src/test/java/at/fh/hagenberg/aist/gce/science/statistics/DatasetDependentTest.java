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

import at.fh.hagenberg.aist.gce.science.statistics.Report;
import at.fh.hagenberg.aist.gce.science.statistics.data.Dataset;
import at.fh.hagenberg.aist.gce.science.statistics.difference.KruskalWallis;

/**
 * @author Oliver Krauss on 24.10.2019
 */

public abstract class DatasetDependentTest {


    /**
     * Dataset for testing
     * @return dataset
     */
    protected Dataset getDataset() {
        double[] valuesA = {488.0, 486.0, 492.0, 490.0, 489.0, 491.0, 488.0, 490.0, 496.0, 487.0, 487.0, 493.0, 491.0,
            490.0, 494.0, 492.0, 485.0, 490.0, 495.0, 488.0, 487.0, 493.0, 490.0, 493.0, 488.0, 493.0, 490.0, 494.0,
            493.0, 489.0, 493.0, 486.0, 487.0, 493.0, 497.0, 494.0, 491.0, 493.0, 492.0, 488.0, 490.0, 496.0, 488.0,
            484.0, 491.0, 492.0, 490.0, 490.0, 487.0, 490.0, 489.0, 492.0, 493.0, 486.0, 488.0, 494.0, 488.0, 487.0,
            490.0, 481.0, 491.0, 489.0, 485.0, 489.0, 488.0, 490.0, 492.0, 491.0, 485.0, 493.0, 490.0, 486.0, 484.0,
            488.0, 492.0, 492.0, 495.0, 494.0, 495.0, 491.0, 487.0, 492.0, 496.0, 495.0, 492.0, 492.0, 484.0, 490.0,
            494.0, 497.0, 489.0, 494.0, 493.0, 495.0, 488.0, 497.0, 487.0, 491.0, 493.0, 492.0};
        double[] valuesB = {512.0, 512.0, 512.0, 512.0, 512.0, 512.0, 512.0, 512.0, 512.0, 512.0, 512.0, 512.0, 512.0,
            512.0, 512.0, 512.0, 512.0, 512.0, 512.0, 512.0, 512.0, 512.0, 512.0, 512.0, 512.0, 512.0, 512.0, 512.0,
            512.0, 512.0, 512.0, 512.0, 512.0, 512.0, 512.0, 512.0, 512.0, 512.0, 512.0, 512.0, 512.0, 512.0, 512.0,
            512.0, 512.0, 512.0, 512.0, 512.0, 512.0, 512.0, 512.0, 512.0, 512.0, 512.0, 512.0, 512.0, 512.0, 512.0,
            512.0, 512.0, 512.0, 512.0, 512.0, 512.0, 512.0, 512.0, 512.0, 512.0, 512.0, 512.0, 512.0, 512.0, 512.0,
            512.0, 512.0, 512.0, 512.0, 512.0, 512.0, 512.0, 512.0, 512.0, 512.0, 512.0, 512.0, 512.0, 512.0, 512.0,
            512.0, 512.0, 512.0, 512.0, 512.0, 512.0, 512.0, 512.0, 512.0, 512.0, 512.0, 512.0};
        double[] valuesC = {489.0, 490.0, 486.0, 493.0, 491.0, 486.0, 490.0, 495.0, 491.0, 489.0, 492.0, 495.0, 488.0,
            492.0, 488.0, 488.0, 489.0, 488.0, 491.0, 494.0, 488.0, 492.0, 491.0, 486.0, 493.0, 493.0, 496.0, 494.0,
            492.0, 489.0, 491.0, 488.0, 488.0, 493.0, 491.0, 490.0, 493.0, 491.0, 493.0, 494.0, 489.0, 489.0, 497.0,
            491.0, 492.0, 496.0, 494.0, 495.0, 490.0, 488.0, 496.0, 493.0, 487.0, 492.0, 494.0, 492.0, 492.0, 494.0,
            492.0, 489.0, 491.0, 495.0, 494.0, 490.0, 486.0, 495.0, 491.0, 490.0, 491.0, 488.0, 491.0, 494.0, 496.0,
            497.0, 497.0, 490.0, 493.0, 496.0, 485.0, 495.0, 492.0, 492.0, 487.0, 496.0, 491.0, 497.0, 493.0, 488.0,
            494.0, 488.0, 487.0, 490.0, 496.0, 491.0, 494.0, 486.0, 486.0, 494.0, 492.0, 497.0};
        double[][] values = {valuesA, valuesB, valuesC};
        String[] titles = {"A", "B", "C"};
        return new Dataset(values, titles);
    }

    protected Dataset getShortDataset() {
        double[] valuesA = {488.0, 486.0, 492.0};
        double[] valuesB = {512.0, 512.0, 512.0};
        double[] valuesC = {489.0, 490.0, 486.0};
        double[][] values = {valuesA, valuesB, valuesC};
        String[] titles = {"A", "B", "C"};
        return new Dataset(values, titles);
    }

    protected Report getReport() {
        Report r = new Report("FreemarkerTestReport");
        r.addReport("A", new Report("A"));
        r.addReport("B", new Report("B"));
        r.addReport("C", new Report("C"));
        return new KruskalWallis().report(getDataset().getData(), r);
    }

    /**
     * DataSet as CSV
     * @return dataset
     */
    protected String getCSV() {
        return "A,B,C\n" +
            "488,512,489\n" +
            "486,512,490\n" +
            "492,512,486\n" +
            "490,512,493\n" +
            "489,512,491\n" +
            "491,512,486\n" +
            "488,512,490\n" +
            "490,512,495\n" +
            "496,512,491\n" +
            "487,512,489\n" +
            "487,512,492\n" +
            "493,512,495\n" +
            "491,512,488\n" +
            "490,512,492\n" +
            "494,512,488\n" +
            "492,512,488\n" +
            "485,512,489\n" +
            "490,512,488\n" +
            "495,512,491\n" +
            "488,512,494\n" +
            "487,512,488\n" +
            "493,512,492\n" +
            "490,512,491\n" +
            "493,512,486\n" +
            "488,512,493\n" +
            "493,512,493\n" +
            "490,512,496\n" +
            "494,512,494\n" +
            "493,512,492\n" +
            "489,512,489\n" +
            "493,512,491\n" +
            "486,512,488\n" +
            "487,512,488\n" +
            "493,512,493\n" +
            "497,512,491\n" +
            "494,512,490\n" +
            "491,512,493\n" +
            "493,512,491\n" +
            "492,512,493\n" +
            "488,512,494\n" +
            "490,512,489\n" +
            "496,512,489\n" +
            "488,512,497\n" +
            "484,512,491\n" +
            "491,512,492\n" +
            "492,512,496\n" +
            "490,512,494\n" +
            "490,512,495\n" +
            "487,512,490\n" +
            "490,512,488\n" +
            "489,512,496\n" +
            "492,512,493\n" +
            "493,512,487\n" +
            "486,512,492\n" +
            "488,512,494\n" +
            "494,512,492\n" +
            "488,512,492\n" +
            "487,512,494\n" +
            "490,512,492\n" +
            "481,512,489\n" +
            "491,512,491\n" +
            "489,512,495\n" +
            "485,512,494\n" +
            "489,512,490\n" +
            "488,512,486\n" +
            "490,512,495\n" +
            "492,512,491\n" +
            "491,512,490\n" +
            "485,512,491\n" +
            "493,512,488\n" +
            "490,512,491\n" +
            "486,512,494\n" +
            "484,512,496\n" +
            "488,512,497\n" +
            "492,512,497\n" +
            "492,512,490\n" +
            "495,512,493\n" +
            "494,512,496\n" +
            "495,512,485\n" +
            "491,512,495\n" +
            "487,512,492\n" +
            "492,512,492\n" +
            "496,512,487\n" +
            "495,512,496\n" +
            "492,512,491\n" +
            "492,512,497\n" +
            "484,512,493\n" +
            "490,512,488\n" +
            "494,512,494\n" +
            "497,512,488\n" +
            "489,512,487\n" +
            "494,512,490\n" +
            "493,512,496\n" +
            "495,512,491\n" +
            "488,512,494\n" +
            "497,512,486\n" +
            "487,512,486\n" +
            "491,512,494\n" +
            "493,512,492\n" +
            "492,512,497\n" +
            "";
    }

    protected String getReportString() {
        return "-----------\n" +
            "FreemarkerTestReport\n" +
            "        A       B       C       \n" +
            "rankSum 9242.5  25050.0 10857.5 \n" +
            "  count 100     100     100     \n" +
            "Results:\n" +
            "areDifferentDistributions = true\n" +
            "                    count = 300\n" +
            "                        H = 201.0685880398671\n" +
            "         degreesOfFreedom = 2\n" +
            "                   pValue = 1.0808837047626696E-8\n" +
            "               pThreshold = 0.05\n" +
            "-----------";
    }
}