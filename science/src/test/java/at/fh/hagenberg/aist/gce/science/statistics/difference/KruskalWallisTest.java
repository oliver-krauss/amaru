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
import at.fh.hagenberg.aist.gce.science.statistics.normality.ShapiroWilk;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * @author Oliver Krauss on 15.10.2019
 */
public class KruskalWallisTest {

    KruskalWallis stat = new KruskalWallis();


    // The following values are copied verbatim from the LookupTable-Excel which is a proven implementation of ShapiroWilk.
    @Test
    public void testKruskalWallis() {
        // given
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

        // when
        Report report = stat.report(values);

        // then
        Assert.assertEquals(report.getValue("pThreshold"), "0.05");
        Assert.assertEquals(report.getValue("pValue"), "1.0808837047626696E-8");
        Assert.assertEquals(report.getValue("areDifferentDistributions"), "true");
        Assert.assertEquals(report.getValue("count"), "300");
        Assert.assertEquals(report.getValue("H"), "201.0685880398671");
        Assert.assertEquals(report.getValue("degreesOfFreedom"), "2");

        Assert.assertEquals(report.getValue("0.count"), "100");
        Assert.assertEquals(report.getValue("0.rankSum"), "9242.5");
        Assert.assertEquals(report.getValue("1.count"), "100");
        Assert.assertEquals(report.getValue("1.rankSum"), "25050.0");
        Assert.assertEquals(report.getValue("2.count"), "100");
        Assert.assertEquals(report.getValue("2.rankSum"), "10857.5");
    }

}