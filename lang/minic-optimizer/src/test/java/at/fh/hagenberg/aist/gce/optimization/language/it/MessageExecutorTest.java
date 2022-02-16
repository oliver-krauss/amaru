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

import at.fh.hagenberg.aist.gce.minic.language.MinicLanguage;
import at.fh.hagenberg.aist.gce.minic.nodes.base.MinicExpressionNode;
import at.fh.hagenberg.aist.gce.minic.nodes.impl.control.MinicBlockNode;
import at.fh.hagenberg.aist.gce.minic.nodes.impl.control.MinicReturnNode;
import at.fh.hagenberg.aist.gce.minic.nodes.impl.control.MinicWhileNode;
import at.fh.hagenberg.aist.gce.minic.nodes.impl.function.MinicFunctionLiteralNode;
import at.fh.hagenberg.aist.gce.minic.nodes.impl.function.MinicInvokeNodeFactory;
import at.fh.hagenberg.aist.gce.minic.nodes.impl.literals.MinicSimpleLiteralNode;
import at.fh.hagenberg.aist.gce.optimization.executor.*;
import at.fh.hagenberg.aist.gce.optimization.infrastructure.MessageBroker;
import at.fh.hagenberg.aist.gce.optimization.infrastructure.MessageCommandModule;
import at.fh.hagenberg.aist.gce.optimization.language.MessageWorker;
import at.fh.hagenberg.aist.gce.optimization.runtime.RuntimeProfile;
import at.fh.hagenberg.aist.gce.optimization.util.TruffleLanguageInformation;
import at.fh.hagenberg.aist.gce.science.statistics.AutoStatistics;
import at.fh.hagenberg.aist.gce.science.statistics.Report;
import at.fh.hagenberg.aist.gce.science.statistics.data.Dataset;
import at.fh.hagenberg.aist.gce.science.statistics.data.template.DatasetReportTransformer;
import com.oracle.truffle.api.nodes.Node;
import org.junit.After;
import org.junit.AfterClass;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * NOTE: The entire test has to be disabled because it can't run in parallel with the CommandPlaneTest,
 * and TestNG ALWAYS runs all SetUp methods in parallel because it doesnt understand what parallel=False means
 *
 * Leave this class alive for debugging purposes please!
 *
 * @author Oliver Krauss on 31.10.2019
 */
public class MessageExecutorTest {

    private Thread broker = new Thread(() -> MessageBroker.main(new String[]{"frontend=15557", "backend=15558", "command=15559"}));
    private Thread worker = new Thread(() -> MessageWorker.main(new String[]{"broker=localhost:15558"}));
    private Thread worker2 = new Thread(() -> MessageWorker.main(new String[]{"broker=localhost:15558"}));
    private Thread worker3 = new Thread(() -> MessageWorker.main(new String[]{"broker=localhost:15558"}));

    @BeforeClass(enabled = false)
    public void setup() {
        ProcessHandle.allProcesses().forEach(process -> MessageCommandModule.purgeProcess(process));
        // since we are already in the same thread the workers will load the TLI in parallel which may lead to caching issues.
        TruffleLanguageInformation.getLanguageInformationMinimal(MinicLanguage.ID);

        // start the broker and worker so the test wont fail (on different from usual ports to not screw up running experiments)
        broker.start();
        worker.start();
        worker2.start();
        worker3.start();
    }

    @AfterClass
    public void teardown() {
        broker.stop();
        worker.stop();
        worker2.stop();
        worker3.stop();
    }

    @Test(enabled = false)
    public void testMessageExecutor() throws FileNotFoundException {
        // given
        String language = "c";
        String code = "int main() {\n" +
                "    print(3 + 5);\n" +
                "    return 0;\n" +
                "}";
        String function = "main";
        Node run = new MinicReturnNode(new MinicSimpleLiteralNode.MinicIntLiteralNode(333));

        // when
        MessageExecutor executor = new MessageExecutor(language, code, function, function, "localhost:15557");
        ExecutionResult result = executor.test(run, new Object[]{2});

        // then
        Assert.assertNotNull(result);
        Assert.assertTrue(result.isSuccess());
        Assert.assertEquals(result.getReturnValue(), 333);
        Assert.assertEquals(result.getOutStreamValue(), "");
    }

    @Test(enabled = false, dependsOnMethods = {"testParallelExecution", "testMessageExecutor"})
    public void testConfigureMessageExecutor() throws FileNotFoundException {
        // given
        String language = "c";
        String code = "int main() {\n" +
                "    print(3 + 5);\n" +
                "    return 0;\n" +
                "}";
        String function = "main";
        Node run = new MinicReturnNode(new MinicSimpleLiteralNode.MinicIntLiteralNode(333));

        // when
        MessageExecutor executor = new MessageExecutor(language, code, function, function, "localhost:15557");
        executor.setRepeats(11);
        executor.setTimeout(1111);
        ExecutionResult result = executor.conductTest(run, null);

        // then
        Assert.assertNotNull(result);
        Assert.assertTrue(result.isSuccess());
        Assert.assertEquals(result.getReturnValue(), 333);
        Assert.assertEquals(result.getOutStreamValue(), "");
    }

    // This test MUST be the last running in the class
    @Test(enabled = false, dependsOnMethods = {"testParallelExecution", "testMessageExecutor", "testConfigureMessageExecutor"})
    public void testCrashRecovery() {
        // given
        String language = "c";
        String code = "int main() {\n" +
                "    print(3 + 5);\n" +
                "    return 0;\n" +
                "}";
        String function = "main";
        Node run = new MinicReturnNode(new MinicSimpleLiteralNode.MinicIntLiteralNode(333));

        // when
        MessageExecutor executor = new MessageExecutor(language, code, function, function, "localhost:15557");
        // intentionally kill all workers so the broker THINKS they exist, but don't
        worker.stop();
        worker2.stop();
        worker3.stop();
        // still get a message back with what happened
        ExecutionResult result = executor.test(run, new Object[]{2});

        // then
        Assert.assertNotNull(result);
        Assert.assertFalse(result.isSuccess());
        Assert.assertNotNull(result.getReturnValue());
    }

    // Just a simple test to see what happens if more than one request is sent
    @Test(enabled = false)
    public void testParallelExecution() {
        // given
        String language = "c";
        String code = "int main() {\n" +
                "    print(3 + 5);\n" +
                "    return 0;\n" +
                "}";
        String function = "main";

        // when
        MessageExecutor executor = new MessageExecutor(language, code, function, function, "localhost:15557");

        // bench
        List<Integer> list = new ArrayList<>();
        for (int i = 0; i < 50000; i++) {
            list.add(i);
        }

        list.parallelStream().forEach(x -> {
            Node run = new MinicReturnNode(new MinicSimpleLiteralNode.MinicIntLiteralNode(333));
            ExecutionResult result = executor.test(run, new Object[]{2});
            Assert.assertNotNull(result);
            Assert.assertTrue(result.isSuccess());
            Assert.assertEquals(result.getReturnValue(), 333);
            Assert.assertEquals(result.getOutStreamValue(), "");
        });
    }


}
