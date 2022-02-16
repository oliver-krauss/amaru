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
import at.fh.hagenberg.aist.gce.minic.nodes.impl.control.MinicReturnNode;
import at.fh.hagenberg.aist.gce.minic.nodes.impl.literals.MinicSimpleLiteralNode;
import at.fh.hagenberg.aist.gce.optimization.executor.ExecutionResult;
import at.fh.hagenberg.aist.gce.optimization.executor.MessageExecutor;
import at.fh.hagenberg.aist.gce.optimization.infrastructure.MessageBroker;
import at.fh.hagenberg.aist.gce.optimization.infrastructure.MessageCommandModule;
import at.fh.hagenberg.aist.gce.optimization.language.MessageWorker;
import at.fh.hagenberg.aist.gce.optimization.util.TruffleLanguageInformation;
import com.oracle.truffle.api.nodes.Node;
import org.junit.AfterClass;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * @author Oliver Krauss on 31.10.2019
 */
public class MessageExecutorWithCommandPlaneTest {

    private Thread broker = new Thread(() -> MessageBroker.main(new String[]{"frontend=15557", "backend=15558", "command=15559"}));
    private Thread commander = new Thread(() -> MessageCommandModule.main(new String[]{"backend=localhost:15558", "broker=localhost:15559", "workerLimit=5"}));
    MessageExecutor executor;

    @BeforeClass
    public void setup() {
        // start the broker and worker so the test wont fail (on different from usual ports to not screw up running experiments)
        ProcessHandle.allProcesses().forEach(process -> MessageCommandModule.purgeProcess(process));
        broker.start();
        commander.start();
        waitCommander(300);
    }

    MessageExecutor getMessageExecutor(String language, String code, String entryPoint, String function) {
        if (executor != null) {
            return (MessageExecutor) executor.replace(language, code, entryPoint, function);
        }
        return executor = new MessageExecutor(language, code, entryPoint, function, "localhost:15557");
    }

    public void waitCommander(int length) {
        try {
            Thread.sleep(length);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @AfterClass
    public void teardown() {
        broker.stop();
        commander.stop();
        ProcessHandle.allProcesses().forEach(process -> MessageCommandModule.purgeProcess(process));
    }

    @Test
    public void testMessageExecutor() throws FileNotFoundException {
        // given
        String language = "c";
        String code = "int call(){\n" +
                "print(3 + 5);\n" +
                "return 0;\n" +
                "}\n" +
                "int main() {\n" +
                "    return call();\n" +
                "}";
        Node run = new MinicReturnNode(new MinicSimpleLiteralNode.MinicIntLiteralNode(333));

        // when
        executor = getMessageExecutor(language, code, "main", "call");
        ExecutionResult result = executor.test(run, new Object[]{2});

        // then
        // we want this to return 333 as main uses call and we modify call to give us 333
        Assert.assertNotNull(result);
        Assert.assertTrue(result.isSuccess());
        Assert.assertEquals(result.getReturnValue(), 333);
        Assert.assertEquals(result.getOutStreamValue(), "");
        // we have a concurrency issue with the init messages. Thus we must wait untill all messages are done
        waitCommander(2000);
    }

    @Test
    public void testMessageExecutorEntryPointVerify() throws FileNotFoundException {
        // given
        String language = "c";
        String code = "int call(){\n" +
                "print(3 + 5);\n" +
                "return 0;\n" +
                "}\n" +
                "int main() {\n" +
                "    return 1;\n" +
                "}";
        Node run = new MinicReturnNode(new MinicSimpleLiteralNode.MinicIntLiteralNode(333));

        // when
        executor = getMessageExecutor(language, code, "main", "call");
        ExecutionResult result = executor.test(run, new Object[]{2});

        // then
        // We want this to return 1 as we modify call but main never uses call.
        Assert.assertNotNull(result);
        Assert.assertTrue(result.isSuccess());
        Assert.assertEquals(result.getReturnValue(), 1);
        Assert.assertEquals(result.getOutStreamValue(), "");
        // we have a concurrency issue with the init messages. Thus we must wait untill all messages are done
        waitCommander(2000);
    }

    @Test
    public void testMessageExecutorSafeVM() throws FileNotFoundException {
        // given
        String language = "c";
        String code = "int main() {\n" +
                "    print(3 + 5);\n" +
                "    return 0;\n" +
                "}";
        String function = "main";
        Node run = new MinicReturnNode(new MinicSimpleLiteralNode.MinicIntLiteralNode(333));

        // when
        executor = getMessageExecutor(language, code, function, function);
        executor.setSafeVM(true);
        waitCommander(500);
        for (int i = 0; i < 20; i++) {
            ExecutionResult result = executor.test(run, new Object[]{2});
            Assert.assertNotNull(result);
            Assert.assertTrue(result.isSuccess());
            Assert.assertEquals(result.getReturnValue(), 333);
            Assert.assertEquals(result.getOutStreamValue(), "");
            System.out.println("DAI SUCCESSU");
            // we only use the safeVM for performance measurements -> we can't currently work with a resolution < heartbeat
            waitCommander(1000);
        }

        // then
        executor.setSafeVM(false);

        // we have a concurrency issue with the init messages. Thus we must wait untill all messages are done
        waitCommander(2000);
    }

    @Test(dependsOnMethods = {"testParallelExecution", "testMessageExecutor"})
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
        executor = getMessageExecutor(language, code, function, function);
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
    @Test(dependsOnMethods = {"testParallelExecution", "testMessageExecutor", "testConfigureMessageExecutor"})
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
        executor = getMessageExecutor(language, code, function, function);
        // wait for init to be complete
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        // intentionally kill all workers so the broker THINKS they exist, but don't
        ProcessHandle.allProcesses().forEach(process -> MessageCommandModule.purgeProcess(process));
        // still get a message back with what happened
        ExecutionResult result = executor.test(run, new Object[]{2});

        // then
        Assert.assertNotNull(result);
        Assert.assertFalse(result.isSuccess());
        System.out.println(result.getOutStreamValue());
        Assert.assertNotNull(result.getReturnValue());
    }

    // Just a simple test to see what happens if more than one request is sent
    @Test
    public void testParallelExecution() {
        // given
        String language = "c";
        String code = "int main() {\n" +
                "    print(3 + 5);\n" +
                "    return 0;\n" +
                "}";
        String function = "main";

        // when
        executor = getMessageExecutor(language, code, function, function);

        // bench
        List<Integer> list = new ArrayList<>();
        for (int i = 0; i < 500; i++) {
            list.add(i);
        }

        list.parallelStream().forEach(x -> {
            Node run = new MinicReturnNode(new MinicSimpleLiteralNode.MinicIntLiteralNode(333));
            ExecutionResult result = executor.test(run, new Object[]{2});
            Assert.assertNotNull(result);
            if (!result.isSuccess()) {
                System.out.println("RET " + result.getReturnValue());
                System.out.println("OUT: " + result.getOutStreamValue());
                System.out.println("----------------------------------");
            }
            Assert.assertTrue(result.isSuccess());
            Assert.assertEquals(result.getReturnValue(), 333);
            Assert.assertEquals(result.getOutStreamValue(), "");
        });
    }


}
