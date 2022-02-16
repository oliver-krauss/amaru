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

import at.fh.hagenberg.aist.gce.science.statistics.Report;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * @author Oliver Krauss on 15.10.2019
 */
public class MannWhitneyUTest {

    MannWhitneyU stat = new MannWhitneyU();


    // The following values are copied verbatim from the LookupTable-Excel which is a proven implementation of ShapiroWilk.
    @Test
    public void test() {
        // given
        double[] valuesA = {496.0, 496.0, 500.0, 493.0, 497.0, 493.0, 495.0, 498.0, 496.0, 498.0, 491.0, 498.0, 500.0,
            498.0, 497.0, 495.0, 497.0, 497.0, 495.0, 497.0, 496.0, 497.0, 497.0, 498.0, 499.0, 494.0, 498.0, 497.0,
            496.0, 495.0, 495.0, 497.0, 498.0, 498.0, 499.0, 499.0, 496.0, 498.0, 497.0, 495.0, 498.0, 494.0, 496.0,
            495.0, 494.0, 495.0, 497.0, 496.0, 498.0, 500.0, 494.0, 496.0, 497.0, 495.0, 497.0, 499.0, 496.0, 501.0,
            497.0, 494.0, 498.0, 494.0, 494.0, 494.0, 498.0, 496.0, 494.0, 499.0, 497.0, 494.0, 493.0, 497.0, 494.0,
            497.0, 498.0, 495.0, 495.0, 496.0, 496.0, 496.0, 495.0, 494.0, 502.0, 498.0, 493.0, 496.0, 495.0, 498.0,
            496.0, 498.0, 496.0, 497.0, 494.0, 493.0, 496.0, 498.0, 498.0, 497.0, 493.0, 496.0};

        double[] valuesB = {496.0, 497.0, 498.0, 496.0, 498.0, 497.0, 496.0, 496.0, 496.0, 499.0, 498.0, 497.0, 497.0,
            496.0, 496.0, 495.0, 498.0, 496.0, 494.0, 493.0, 495.0, 497.0, 494.0, 499.0, 496.0, 494.0, 495.0, 497.0,
            496.0, 496.0, 493.0, 495.0, 496.0, 497.0, 494.0, 495.0, 499.0, 497.0, 497.0, 497.0, 495.0, 493.0, 497.0,
            499.0, 492.0, 492.0, 496.0, 495.0, 494.0, 495.0, 494.0, 496.0, 497.0, 499.0, 495.0, 497.0, 498.0, 494.0,
            496.0, 494.0, 497.0, 496.0, 492.0, 496.0, 496.0, 498.0, 497.0, 495.0, 495.0, 495.0, 495.0, 497.0, 495.0,
            495.0, 495.0, 498.0, 496.0, 498.0, 498.0, 497.0, 493.0, 493.0, 497.0, 494.0, 494.0, 496.0, 497.0, 498.0,
            499.0, 496.0, 493.0, 496.0, 494.0, 496.0, 496.0, 495.0, 492.0, 498.0, 496.0, 497.0};
        double[][] values = {valuesA, valuesB};

        // when
        Report report = stat.report(values);

        // then
        Assert.assertEquals(report.getValue("pThreshold"), "0.05");
        Assert.assertEquals(report.getValue("pValue"), "0.05907956862279262");
        Assert.assertEquals(report.getValue("areDifferentDistributions"), "false");
        Assert.assertEquals(report.getValue("U"), "4360.0");
        Assert.assertEquals(report.getValue("mean"), "5000.0");
        Assert.assertEquals(report.getValue("standardDeviation"), "409.2676385936225");
        Assert.assertEquals(report.getValue("zScore"), "1.5625471933171438");
        Assert.assertEquals(report.getValue("effectR"), "0.11048877163185596");
    }

}



