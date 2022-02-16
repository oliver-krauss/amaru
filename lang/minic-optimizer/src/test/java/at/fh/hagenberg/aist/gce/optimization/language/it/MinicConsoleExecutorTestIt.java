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

import at.fh.hagenberg.aist.gce.minic.nodes.impl.control.MinicReturnNode;
import at.fh.hagenberg.aist.gce.minic.nodes.impl.literals.MinicSimpleLiteralNode;
import at.fh.hagenberg.aist.gce.optimization.executor.ConsoleExecutor;
import at.fh.hagenberg.aist.gce.optimization.executor.ExecutionResult;
import at.fh.hagenberg.aist.gce.optimization.language.ConsoleWorker;
import at.fh.hagenberg.aist.gce.optimization.language.util.CommandProcessor;
import at.fh.hagenberg.aist.gce.optimization.runtime.RuntimeProfile;
import at.fh.hagenberg.aist.gce.science.statistics.AutoStatistics;
import at.fh.hagenberg.aist.gce.science.statistics.Report;
import at.fh.hagenberg.aist.gce.science.statistics.data.Dataset;
import at.fh.hagenberg.aist.gce.science.statistics.data.template.DatasetReportTransformer;
import at.fh.hagenberg.machinelearning.analytics.graph.nodes.NodeWrapper;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.*;
import java.util.Arrays;
import java.util.Base64;

/**
 * @author Oliver Krauss on 31.10.2019
 */

public class MinicConsoleExecutorTestIt {

    @Test
    public void testConsoleWorker() throws FileNotFoundException {
        // given
        String[] args = {
            "c",
            "int main() {\n" +
                "    print(3 + 5);\n" +
                "    return 0;\n" +
                "}",
            "main",
            "1000",
            "null",
            "int:3"
        };
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        ConsoleWorker.setOut(new PrintStream(stream));

        // when
        ConsoleWorker.main(args);
        String result = stream.toString();
        String[] out = result.split(System.lineSeparator());

        // then
        Assert.assertEquals(out.length, 3);
        Assert.assertTrue(out[0].contains("returnValue:int:0"));
        Assert.assertTrue(out[1].contains("performance:"));
        Assert.assertEquals(out[1].split(",").length, 1000);
        Assert.assertTrue(out[2].contains("out:8"));
    }

    @Test
    public void testConsoleWorkerSerial() throws IOException, ClassNotFoundException {
        // given
        String[] args = {
            "c",
            "int main() {\n" +
                "    print(3 + 5);\n" +
                "    return 0;\n" +
                "}",
            "main",
            "1000",
            "serial",
            "int:3"
        };
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        ConsoleWorker.setOut(new PrintStream(stream));
        new Thread(() -> {
            try {
                CommandProcessor.sendNode(new MinicReturnNode(new MinicSimpleLiteralNode.MinicIntLiteralNode(333)));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();

        // when
        new Thread(() -> {
            ConsoleWorker.main(args);
        }).start();
        ExecutionResult result = CommandProcessor.receiveExecutionResult();

        // then
        Assert.assertNotNull(result);
        Assert.assertEquals(result.getReturnValue(), 333);
        Assert.assertEquals(result.getPerformance().length, 1000);
    }

    @Test
    public void testConsoleWorkerParse() throws FileNotFoundException {
        // given
        String[] args = {
            "c",
            "int main() {\n" +
                "    print(3 + 5);\n" +
                "    return 0;\n" +
                "}",
            "main",
            "1000",
            NodeWrapper.serialize(NodeWrapper.wrap(new MinicReturnNode(new MinicSimpleLiteralNode.MinicIntLiteralNode(333)))),
            "int:3"
        };
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        ConsoleWorker.setOut(new PrintStream(stream));

        // when
        ConsoleWorker.main(args);
        String result = stream.toString();
        String[] out = result.split(System.lineSeparator());

        // then
        Assert.assertEquals(out.length, 3);
        Assert.assertTrue(out[0].contains("returnValue:int:333"));
        Assert.assertTrue(out[1].contains("performance:"));
        Assert.assertEquals(out[1].split(",").length, 1000);
        Assert.assertTrue(out[2].contains("out:"));
    }

    @Test
    public void testConsoleExecutor() throws FileNotFoundException {
        // given
        String language = "c";
        String code = "int main() {\n" +
            "    print(3 + 5);\n" +
            "    return 0;\n" +
            "}";
        String function = "main";

        // when
        ConsoleExecutor test = new ConsoleExecutor(language, code, function, function);

        // then
        Assert.assertNotNull(test.getMain());
        Assert.assertNotNull(test.getOut());
        Assert.assertNotNull(test.getOrigin());
        Assert.assertNotNull(test.getRoot());
    }

    @Test
    public void testWithConsoleExecutor() {
        // given
        String language = "c";
        String code = "int main() {\n" +
            "    print(3 + 5);\n" +
            "    return 0;\n" +
            "}";
        String function = "main";
        ConsoleExecutor test = new ConsoleExecutor(language, code, function, function);

        // when
        ExecutionResult result = test.test(new MinicReturnNode(new MinicSimpleLiteralNode.MinicIntLiteralNode(1234)), null);

        // then
        Assert.assertNotNull(result);
        Assert.assertEquals(result.getReturnValue(), 1234);
        Assert.assertEquals(result.getOutStreamValue(), "");
    }

    /**
     * This test is designed to check if Graal bleed over effects happen. It is very costly and disabled by default
     */
    @Test(enabled = false)
    public void benchmarkConsoleExecutor() {
        int repeats = 200000;
        // given
        String language = "c";
        String code = "int fibonacci(int n) {\n" +
            "    if (n == 0) {\n" +
            "        return 0;\n" +
            "    }\n" +
            "    if (n == 1) {\n" +
            "        return 1;\n" +
            "    }\n" +
            "    return fibonacci(n - 1) + fibonacci(n - 2);\n" +
            "}\n" +
            "\n" +
            "int main() {\n" +
            "    int i;\n" +
            "    i = 2;\n" +
            "    while (i < 13) {\n" +
            "        print(fibonacci(i));\n" +
            "        i = i + 1;\n" +
            "    }\n" +
            "    return 0;\n" +
            "}";
        String function = "main";
        ConsoleExecutor test = new ConsoleExecutor(language, code, function, function);
        test.setRepeats(repeats);

        // when
        ExecutionResult result = test.test(test.getOrigin(), null);
        ExecutionResult result2 = test.test(test.getOrigin(), null);

        // then
        Assert.assertNotNull(result);
        Assert.assertEquals(result.getReturnValue(), 0);
        Assert.assertEquals(result.getPerformance().length, repeats);
        Assert.assertEquals(result.getOutStreamValue(), "1\n" +
            "2\n" +
            "3\n" +
            "5\n" +
            "8\n" +
            "13\n" +
            "21\n" +
            "34\n" +
            "55\n" +
            "89\n" +
            "144\n");

        // NOTICE:
        // Interpret the following at your own discretion. AutoStat says consistently that these are DIFFERENT distributions
        // However when re-running the tests A and B consistently switch who has the best mean/median/...
        // So I think we removed the Graal side-effects but have got NOTHING on the regular CPU madness.

        // Check the quartiles
        DatasetReportTransformer transformer = new DatasetReportTransformer(null);
        transformer.setWriter(new PrintWriter(System.out));
        RuntimeProfile profileA = new RuntimeProfile(result.getPerformance());
        RuntimeProfile profileB = new RuntimeProfile(result2.getPerformance());
        Report rtReport = new Report("Runtime Profile Comparison");
        rtReport.addReport("A", toReport(profileA, "A"));
        rtReport.addReport("B", toReport(profileB, "B"));
        transformer.transform(rtReport);

        // Science
        AutoStatistics stat = new AutoStatistics();
        double[][] performance = new double[2][];
        performance[0] = Arrays.stream(result.getPerformance()).mapToDouble(x -> (double) x).toArray();
        performance[1] = Arrays.stream(result2.getPerformance()).mapToDouble(x -> (double) x).toArray();
        Report report = stat.report(new Dataset(performance, new String[]{"A", "B"}));

        transformer.transform(report.getReport(1));
        int i = 0;
    }

    private static Report toReport(RuntimeProfile p, String name) {
        Report r = new Report(name);
        r.addReport("minimum", p.getMinimum());
        r.addReport("firstQuartile", p.getFirstQuartile());
        r.addReport("mean", p.getMean());
        r.addReport("median", p.getMedian());
        r.addReport("thirdQuartile", p.getThirdQuartile());
        r.addReport("maximum", p.getMaximum());
        return r;
    }

}
