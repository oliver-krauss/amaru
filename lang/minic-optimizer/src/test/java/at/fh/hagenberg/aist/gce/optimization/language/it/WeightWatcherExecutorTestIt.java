/*
 * Copyright (c) 2022 the original author or authors.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

package at.fh.hagenberg.aist.gce.optimization.language.it;

import at.fh.hagenberg.aist.gce.optimization.executor.WeightWatcherExecutor;
import at.fh.hagenberg.aist.gce.optimization.executor.ExecutionResult;
import at.fh.hagenberg.aist.gce.optimization.executor.JavassistExecutor;
import at.fh.hagenberg.aist.gce.optimization.executor.TraceExecutionResult;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;
import java.io.FileNotFoundException;

/**
 * @author Oliver Krauss on 31.10.2019
 */

public class WeightWatcherExecutorTestIt {

    @BeforeClass
    public void setUp() {
        // TODO #194
        //new File("../../dists/weight-access-c.jar").delete();
    }

    @Test
    public void testDummyExecutor() throws FileNotFoundException {
        // given
        String language = "c";
        String code = "int main() {\n" +
            "    print(3 + 5);\n" +
            "    return 0;\n" +
            "}";
        String function = "main";

        // when
        WeightWatcherExecutor test = new WeightWatcherExecutor(language, code, function, function, null);

        // then
        Assert.assertNotNull(test.getMain());
        Assert.assertNotNull(test.getOut());
        Assert.assertNotNull(test.getOrigin());
        Assert.assertNotNull(test.getRoot());
        Assert.assertTrue(new File("../../dists/weight-access-c.jar").exists());
    }

    @Test
    public void testExecuteDummyExecutor() {
        // given
        String language = "c";
        String code = "int main() {\n" +
            "    print(3 + 5);\n" +
            "    return 0;\n" +
            "}";
        String function = "main";

        // when
        WeightWatcherExecutor test = new WeightWatcherExecutor(language, code, function, function, null);
        ExecutionResult result = test.test(test.getOrigin(), null);

        // then
        Assert.assertNotNull(result);
        Assert.assertEquals(result.getReturnValue(), 0);
        Assert.assertEquals(result.getOutStreamValue(), "8\n");
    }
}
