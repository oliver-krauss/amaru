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

import at.fh.hagenberg.aist.gce.science.statistics.Report;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.Random;

/**
 * @author Oliver Krauss on 15.10.2019
 */
public class ShapiroWilkTest {

    private ShapiroWilk stat = new ShapiroWilk();

    // The following values are copied verbatim from the LookupTable-Excel which is a proven implementation of ShapiroWilk.
    @Test
    public void testShapiroWilk() {
        // given
        double[] values = {488.0, 486.0, 492.0, 490.0, 489.0, 491.0, 488.0, 490.0, 496.0, 487.0, 487.0, 493.0, 491.0,
            490.0, 494.0, 492.0, 485.0, 490.0, 495.0, 488.0, 487.0, 493.0, 490.0, 493.0, 488.0, 493.0, 490.0, 494.0,
            493.0, 489.0, 493.0, 486.0, 487.0, 493.0, 497.0, 494.0, 491.0, 493.0, 492.0, 488.0, 490.0, 496.0, 488.0,
            484.0, 491.0, 492.0, 490.0, 490.0, 487.0, 490.0, 489.0, 492.0, 493.0, 486.0, 488.0, 494.0, 488.0, 487.0,
            490.0, 481.0, 491.0, 489.0, 485.0, 489.0, 488.0, 490.0, 492.0, 491.0, 485.0, 493.0, 490.0, 486.0, 484.0,
            488.0, 492.0, 492.0, 495.0, 494.0, 495.0, 491.0, 487.0, 492.0, 496.0, 495.0, 492.0, 492.0, 484.0, 490.0,
            494.0, 497.0, 489.0, 494.0, 493.0, 495.0, 488.0, 497.0, 487.0, 491.0, 493.0, 492.0};

        // when
        Report report = stat.report(values);

        // then
        Assert.assertEquals(report.getValue("pThreshold"), "0.05");
        Assert.assertEquals(report.getValue("pValue"), "0.21574011617084932");
        Assert.assertEquals(report.getValue("isNormal"), "true");
        Assert.assertEquals(report.getValue("w"), "0.9827330762465631");
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testShapiroWilk2() {
        // given
        double[] values = {512.0, 512.0, 512.0, 512.0, 512.0, 512.0, 512.0, 512.0, 512.0, 512.0, 512.0, 512.0, 512.0,
            512.0, 512.0, 512.0, 512.0, 512.0, 512.0, 512.0, 512.0, 512.0, 512.0, 512.0, 512.0, 512.0, 512.0, 512.0,
            512.0, 512.0, 512.0, 512.0, 512.0, 512.0, 512.0, 512.0, 512.0, 512.0, 512.0, 512.0, 512.0, 512.0, 512.0,
            512.0, 512.0, 512.0, 512.0, 512.0, 512.0, 512.0, 512.0, 512.0, 512.0, 512.0, 512.0, 512.0, 512.0, 512.0,
            512.0, 512.0, 512.0, 512.0, 512.0, 512.0, 512.0, 512.0, 512.0, 512.0, 512.0, 512.0, 512.0, 512.0, 512.0,
            512.0, 512.0, 512.0, 512.0, 512.0, 512.0, 512.0, 512.0, 512.0, 512.0, 512.0, 512.0, 512.0, 512.0, 512.0,
            512.0, 512.0, 512.0, 512.0, 512.0, 512.0, 512.0, 512.0, 512.0, 512.0, 512.0, 512.0};

        // when
        Report report = stat.report(values);
    }

    @Test
    public void testShapiroWilk3() {
        // given
        double[] values = {489.0, 490.0, 486.0, 493.0, 491.0, 486.0, 490.0, 495.0, 491.0, 489.0, 492.0, 495.0, 488.0,
            492.0, 488.0, 488.0, 489.0, 488.0, 491.0, 494.0, 488.0, 492.0, 491.0, 486.0, 493.0, 493.0, 496.0, 494.0,
            492.0, 489.0, 491.0, 488.0, 488.0, 493.0, 491.0, 490.0, 493.0, 491.0, 493.0, 494.0, 489.0, 489.0, 497.0,
            491.0, 492.0, 496.0, 494.0, 495.0, 490.0, 488.0, 496.0, 493.0, 487.0, 492.0, 494.0, 492.0, 492.0, 494.0,
            492.0, 489.0, 491.0, 495.0, 494.0, 490.0, 486.0, 495.0, 491.0, 490.0, 491.0, 488.0, 491.0, 494.0, 496.0,
            497.0, 497.0, 490.0, 493.0, 496.0, 485.0, 495.0, 492.0, 492.0, 487.0, 496.0, 491.0, 497.0, 493.0, 488.0,
            494.0, 488.0, 487.0, 490.0, 496.0, 491.0, 494.0, 486.0, 486.0, 494.0, 492.0, 497.0};

        // when
        Report report = stat.report(values);

        // then
        Assert.assertEquals(report.getValue("pThreshold"), "0.05");
        Assert.assertEquals(report.getValue("pValue"), "0.020104618626406747");
        Assert.assertEquals(report.getValue("isNormal"), "false");
        Assert.assertEquals(report.getValue("w"), "0.9694611431236898");
    }

}
