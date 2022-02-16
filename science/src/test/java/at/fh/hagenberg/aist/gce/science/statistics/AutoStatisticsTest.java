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
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * @author Oliver Krauss on 24.10.2019
 */

public class AutoStatisticsTest extends DatasetDependentTest {

    @Test
    public void testAutoStatistics() {
        // given
        AutoStatistics statistics = new AutoStatistics();

        // when
        Report report = statistics.report(getDataset());

        // then
        Assert.assertNotNull(report);
        Assert.assertEquals(report.getValue("isParametric"), "true");
        Assert.assertEquals(report.getValue("isSignificant"), "true");
        Assert.assertNotNull(report.getReport("Different distributions test - Kruskal-Wallis"));
    }

    @Test
    public void testAutoStatisticsNotYetImplemented() {
        // given
        AutoStatistics statistics = new AutoStatistics();

        // when
        Report report = statistics.report(getNormalDataset());

        // then
        Assert.assertNotNull(report);
        Assert.assertEquals(report.getValue("isParametric"), "false");
        Assert.assertNotNull(report.getValue("ERROR"));
    }

    protected Dataset getNormalDataset() {
        double[] valuesA = {1, 2, 3, 2, 1};
        double[] valuesB = {1, 2, 3, 2, 1};
        double[] valuesC = {1, 2, 3, 2, 1};
        double[][] values = {valuesA, valuesB, valuesC};
        String[] titles = {"A", "B", "C"};
        return new Dataset(values, titles);
    }
}
