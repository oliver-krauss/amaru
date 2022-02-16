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

import at.fh.hagenberg.aist.gce.optimization.executor.ConsoleExecutor;
import at.fh.hagenberg.aist.gce.optimization.executor.ExecutionResult;
import at.fh.hagenberg.aist.gce.optimization.executor.JavassistExecutor;
import at.fh.hagenberg.aist.gce.optimization.executor.TraceExecutionResult;
import at.fh.hagenberg.aist.gce.optimization.language.XESWorker;
import at.fh.hagenberg.aist.gce.optimization.language.util.EngineConfig;
import at.fh.hagenberg.aist.gce.optimization.util.TruffleLanguageInformation;
import science.aist.seshat.LogConfiguration;
import at.fh.hagenberg.machinelearning.analytics.graph.nodes.NodeWrapper;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;
import java.io.FileNotFoundException;

/**
 * @author Oliver Krauss on 31.10.2019
 */

public class JavassistConsoleExecutorTestIt {

    @BeforeClass
    public void setUp() {
        // TODO #194
        //new File("../../dists/analysis-access-c.jar").delete();
        int i = 1;
    }

    @Test
    public void testJavassistExecutor() throws FileNotFoundException {
        // given
        String language = "c";
        String code = "int main() {\n" +
            "    print(3 + 5);\n" +
            "    return 0;\n" +
            "}";
        String function = "main";

        // when
        JavassistExecutor test = new JavassistExecutor(language, code, function, function, null);

        // then
        Assert.assertNotNull(test.getMain());
        Assert.assertNotNull(test.getOut());
        Assert.assertNotNull(test.getOrigin());
        Assert.assertNotNull(test.getRoot());
        Assert.assertTrue(new File("../../dists/analysis-access-c.jar").exists());
    }

    @Test
    public void testExecuteJavassistExecutor() {
        // given
        String language = "c";
        String code = "int main() {\n" +
            "    print(3 + 5);\n" +
            "    return 0;\n" +
            "}";
        String function = "main";

        // when
        JavassistExecutor test = new JavassistExecutor(language, code, function, function, null);
        TraceExecutionResult result = test.traceTest(test.getOrigin(), null);

        // then
        Assert.assertNotNull(result);
        Assert.assertEquals(result.getReturnValue(), 0);
        Assert.assertEquals(result.getOutStreamValue(), "8\n");
        Assert.assertEquals(result.getNumberOfExecutedNodes(), 11);
        Assert.assertEquals(result.getNumberofSpecializedNodes(), 3);
        Assert.assertTrue(result.getNodeExecutions().values().stream().allMatch(x -> x == 1));
    }

    @Test
    public void testExecuteXESLog() {
        // given
        String language = "c";
        String code = "int main() {\n" +
            "    print(3 + 5);\n" +
            "    return 0;\n" +
            "}";
        String function = "main";

        // when
        JavassistExecutor test = new JavassistExecutor(language, code, function, function, null);
        test.setWorker(XESWorker.class);
        TraceExecutionResult result = test.traceTest(test.getOrigin(), null);

        // then
        Assert.assertNotNull(result);
        Assert.assertEquals(result.getReturnValue(), 0);
        Assert.assertEquals(result.getOutStreamValue(), "8\n");
        Assert.assertEquals(result.getNumberOfExecutedNodes(), 11);
        Assert.assertEquals(result.getNumberofSpecializedNodes(), 3);
        Assert.assertTrue(result.getNodeExecutions().values().stream().allMatch(x -> x == 1));
        Assert.assertTrue(new File(LogConfiguration.LOG_LOCATION + "/test.xes").exists());
    }
}
