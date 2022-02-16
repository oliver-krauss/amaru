/*
 * Copyright (c) 2022 the original author or authors.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

package at.fh.hagenberg.aist.gce.benchmark;

import at.fh.hagenberg.aist.gce.context.ApplicationContextProvider;
import at.fh.hagenberg.aist.gce.minic.language.MinicLanguage;
import at.fh.hagenberg.aist.gce.optimization.TruffleOptimizationProblem;
import at.fh.hagenberg.aist.gce.optimization.language.util.EngineConfig;
import at.fh.hagenberg.aist.gce.pattern.*;
import at.fh.hagenberg.aist.gce.pattern.algorithm.editor.EPMReproductionEditor;
import at.fh.hagenberg.aist.gce.pattern.algorithm.metric.*;
import at.fh.hagenberg.aist.gce.pattern.constraint.ProblemConstraint;
import at.fh.hagenberg.aist.gce.pattern.constraint.TestResultConstraint;
import at.fh.hagenberg.aist.gce.pattern.encoding.BitwisePatternMeta;
import at.fh.hagenberg.aist.gce.pattern.encoding.BitwisePatternMetaLoader;
import at.fh.hagenberg.aist.gce.pattern.selection.TrufflePatternSearchSpace;
import at.fh.hagenberg.aist.gce.pattern.selection.TrufflePatternSearchSpaceDefinition;
import at.fh.hagenberg.aist.gce.science.statistics.data.template.PatternAnalysisPrinter;
import at.fh.hagenberg.machinelearning.analytics.graph.TruffleProblemGeneRepository;
import at.fh.hagenberg.machinelearning.analytics.graph.nodes.NodeWrapper;
import at.fh.hagenberg.machinelearning.analytics.graph.nodes.OrderedRelationship;
import at.fh.hagenberg.machinelearning.core.Solution;
import at.fh.hagenberg.util.Pair;
import org.neo4j.driver.Result;
import org.neo4j.driver.Values;
import science.aist.neo4j.transaction.TransactionManager;

import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Class that exists to analyze Minic Experiments concerning performance
 * <p>
 *
 * @author Oliver Krauss on 21.07.2020
 */
public class MinicPerformanceDetection {

    /**
     * Algorithm to compare the different trees
     */
    static TrufflePatternDetector detector = new TrufflePatternDetector();

    static TransactionManager manager;

    /**
     * Reporting tools
     */
    static FileWriter writer;

    static PatternAnalysisPrinter printer = new PatternAnalysisPrinter();

    /**
     * folder location where the output will be pushed to
     */
    private static final String LOCATION = EngineConfig.ROOT_LOCATION + "/Amaru.LOGS/PatternsPerformance/";

    private static final int LIMIT = 10;
    private static final int EMBEDDED_LIMIT = 6;
    private static final boolean threeway = true;

    private static final int GROW_CNT = 3000;
    private static final int FINAL_CNT = 29;

    public static void main(String[] args) throws IOException {
        System.out.println("Starting: " + new Date().toString());
        manager = ApplicationContextProvider.getCtx().getBean("transactionManager", TransactionManager.class);

        HashMap<String, Pair<Long, Long>> limits = new HashMap();
        // NOTE I put the values here manually, They are unless otherwise identified always the LOWER OUTER FENCE
        // and the UPPER OUTER FENCE to only mine outliers against each other.
        // Exceptions are only made if there are NO outliers
        limits.put("sqrt_java_BENCHMARK", new Pair<>(2690L, 5000L));
        limits.put("sqrt_lookup_BENCHMARK", new Pair<>(30000L, 50000L));
        limits.put("sqrt_nolookup_BENCHMARK", new Pair<>(139500L, 157409L));
        limits.put("cbrt_BENCHMARK", new Pair<>(218000L, 291256L));
        limits.put("surt_BENCHMARK", new Pair<>(310000L, 950000L));
        limits.put("invSqrt_BENCHMARK", new Pair<>(168176L, 204476L));
        limits.put("log_BENCHMARK", new Pair<>(270500L, 305000L));
        limits.put("ln_BENCHMARK", new Pair<>(515000L, 562000L));

        limits.put("bubbleSort_BENCHMARK", new Pair<>(3000000L, 7000000L));
        limits.put("heapSort_BENCHMARK", new Pair<>(285000L, 700000L));
        limits.put("insertionSort_BENCHMARK", new Pair<>(2770000L, 7616398L));
        limits.put("mergeSort_BENCHMARK", new Pair<>(440000L, 1500000L));
        limits.put("mergeSortInlined_BENCHMARK", new Pair<>(64000L, 1600000L));
        limits.put("quickSort_BENCHMARK", new Pair<>(1180000L, 1350000L));
        limits.put("quickSortInlined_BENCHMARK", new Pair<>(1180000L, 1300000L));
        limits.put("selectionSort_BENCHMARK", new Pair<>(2370000L, 4000000L));
        limits.put("shakerSort_BENCHMARK", new Pair<>(5800L, 3000000L));
        limits.put("shellSort_BENCHMARK", new Pair<>(6800L, 40000L));


        limits.put("nn_relu_BENCHMARK", new Pair<>(1000000L, 1986280L));
        limits.put("nn_lrelu_BENCHMARK", new Pair<>(1163349L, 1871114L)); // NOTE: also attempted with Lower limit of 500.000
        limits.put("nn_sigmoid_BENCHMARK", new Pair<>(1325614L,1560829L));
        limits.put("nn_swish_BENCHMARK", new Pair<>(1356825L, 2401090L)); // NOTE: swish was also done with Lower limit 10000L to analyze the outlier
        limits.put("nn_tanh_BENCHMARK", new Pair<>(1428738L, 1629928L));
        limits.put("nn_fullinline_BENCHMARK", new Pair<>(1427571L, 2314381L));
        limits.put("nn_options_BENCHMARK", new Pair<>(1201068L, 2075961L));


        // Identify the differences in the algorithms with their distinct types
        detector.setHierarchyFloor(1);
        detector.setHierarchyCeil(1);
        detector.setMaxPatternSize(LIMIT);
        detector.setEmbedded(false);
        detector.setGrouping(SignificanceType.MAX);
        printer.setDebug(false);
        printer.setFormat("html");
        printer.setOmitSearchSpace(false);
//        printer.setOmitPatternsPerProblem(true);
        printer.setOmitTracability(false);

        // TODO Find the positive / negative search spaces (negative )

        TruffleProblemGeneRepository truffleProblemGeneRepository = ApplicationContextProvider.getCtx().getBean("truffleOptimizationProblemRepository", TruffleProblemGeneRepository.class);
        List<TruffleOptimizationProblem> benchmarks = truffleProblemGeneRepository.findAllAsStream().filter(x -> x.getDescription().endsWith("_BENCHMARK")).collect(Collectors.toList());

        HashMap<String, TrufflePatternSearchSpace> goodSearchSpaces = new HashMap<>();
        HashMap<String, TrufflePatternSearchSpace> badSearchSpaces = new HashMap<>();
        HashMap<String, TrufflePatternSearchSpace> timeoutSearchSpaces = new HashMap<>();

        // find search spaces per alg
        benchmarks.forEach(space -> {
            // only for testing right now
            if (!limits.containsKey(space.getDescription())) {
                return;
            }
            // find good ones
            TrufflePatternSearchSpaceDefinition allSsd = new TrufflePatternSearchSpaceDefinition();
            ProblemConstraint pc = new ProblemConstraint(space.getDescription());
            allSsd.setProblems(Collections.singletonList(pc));
            allSsd.setSolutionSpace(true);
            HashMap<String, Object> constraints = new HashMap<>();
            Result goodTreesRes = manager.executeRead((transaction) -> transaction.run(
                    "match (p)<-[:RWGENE]-(pg)<-[:SOLVES]-(sg)<-[:RWGENE]->(s)-[:TEST_RESULT]->(t) " +
                            "where p.description = $DESC " +
                            "match (n:Node)<-[:TREE]-(s)-[:TEST_RESULT]->(t)-[:RUNTIME]->(r) " +
                            "where r.count = 100000 and r.median <= $MEDIAN " +
                            "return id(n)", Values.parameters("DESC", space.getDescription(), "MEDIAN", limits.get(space.getDescription()).getKey())));
            List<Long> goodTrees = goodTreesRes.stream().map(x -> x.get("id(n)").asLong()).collect(Collectors.toList());
            Result badTreesRes = manager.executeRead((transaction) -> transaction.run(
                    "match (p)<-[:RWGENE]-(pg)<-[:SOLVES]-(sg)<-[:RWGENE]->(s)-[:TEST_RESULT]->(t) " +
                            "where p.description = $DESC " +
                            "match (n:Node)<-[:TREE]-(s)-[:TEST_RESULT]->(t)-[:RUNTIME]->(r) " +
                            "where r.count = 100000 and r.median >= $MEDIAN " +
                            "return id(n)", Values.parameters("DESC", space.getDescription(), "MEDIAN", limits.get(space.getDescription()).getValue())));
            List<Long> badTrees = badTreesRes.stream().map(x -> x.get("id(n)").asLong()).collect(Collectors.toList());

            TrufflePatternSearchSpace allSearchSpace = detector.findSearchSpace(allSsd);
            TrufflePatternSearchSpace goodSearchSpace = new TrufflePatternSearchSpace();
            TrufflePatternSearchSpace badSearchSpace = new TrufflePatternSearchSpace();

            goodTrees.forEach(x ->
                    allSearchSpace.getSearchSpace().stream().filter(y -> y.getKey()[0].getId().equals(x)).forEach(y -> goodSearchSpace.addTree(y.getKey(), y.getValue()))
            );
            goodSearchSpaces.put(space.getDescription(), goodSearchSpace);
            badTrees.forEach(x ->
                    allSearchSpace.getSearchSpace().stream().filter(y -> y.getKey()[0].getId().equals(x)).forEach(y -> badSearchSpace.addTree(y.getKey(), y.getValue()))
            );
            badSearchSpaces.put(space.getDescription(), badSearchSpace);

            System.out.println("GOOD " + space.getDescription() + " " + goodSearchSpace.getSearchSpace().size());
            System.out.println("BAD " + space.getDescription() + " " + badSearchSpace.getSearchSpace().size());

            // Find timeouts
            TrufflePatternSearchSpaceDefinition timeouts = new TrufflePatternSearchSpaceDefinition();
            timeouts.setProblems(Collections.singletonList(pc));
            timeouts.setTestResult(Collections.singletonList(new TestResultConstraint("java.util.concurrent.TimeoutException")));
            timeouts.setSolutionSpace(true);
            TrufflePatternSearchSpace timeoutSearchSpace = detector.findSearchSpace(timeouts);
            if (!threeway) {
                timeoutSearchSpace.getSearchSpace().forEach(x -> badSearchSpace.addTree(x.getKey(), x.getValue()));
            }
            timeoutSearchSpaces.put(space.getDescription(), timeoutSearchSpace);
            System.out.println("TIMEOUT " + space.getDescription() + " " + timeoutSearchSpace.getSearchSpace().size());
        });

//         Combine groups of search space
//         Note: Activate this only if you wanna test just ONE total group
        limits.clear();
        List<String> groups = new LinkedList<>();
        groups.add("nn_");
        groups.add("Sort");
        groups.add("MATH");
        groups.forEach(group -> {
            TrufflePatternSearchSpace goodSearchSpace = new TrufflePatternSearchSpace();
            TrufflePatternSearchSpace badSearchSpace = new TrufflePatternSearchSpace();
            TrufflePatternSearchSpace timeoutSearchSpace = new TrufflePatternSearchSpace();

            goodSearchSpaces.forEach((k, v) -> {
                if (group.equals("nn_") && k.startsWith("nn_")) {
                    for (Pair<NodeWrapper[], OrderedRelationship[]> pair : v.getSearchSpace()) {
                        goodSearchSpace.addTree(pair.getKey(), pair.getValue());
                    }
                } else if (group.equals("Sort") && k.contains("Sort")) {
                    for (Pair<NodeWrapper[], OrderedRelationship[]> pair : v.getSearchSpace()) {
                        goodSearchSpace.addTree(pair.getKey(), pair.getValue());
                    }
                } else if (group.equals("MATH") && !k.contains("Sort") && !k.startsWith("nn_")) {
                    for (Pair<NodeWrapper[], OrderedRelationship[]> pair : v.getSearchSpace()) {
                        goodSearchSpace.addTree(pair.getKey(), pair.getValue());
                    }
                }
            });
            badSearchSpaces.forEach((k, v) -> {
                if (k.startsWith("nn_") && k.startsWith("nn_")) {
                    for (Pair<NodeWrapper[], OrderedRelationship[]> pair : v.getSearchSpace()) {
                        badSearchSpace.addTree(pair.getKey(), pair.getValue());
                    }
                } else if (group.equals("Sort") && k.contains("Sort")) {
                    for (Pair<NodeWrapper[], OrderedRelationship[]> pair : v.getSearchSpace()) {
                        badSearchSpace.addTree(pair.getKey(), pair.getValue());
                    }
                } else if (group.equals("MATH") && !k.contains("Sort") && !k.startsWith("nn_")) {
                    for (Pair<NodeWrapper[], OrderedRelationship[]> pair : v.getSearchSpace()) {
                        badSearchSpace.addTree(pair.getKey(), pair.getValue());
                    }
                }
            });
            timeoutSearchSpaces.forEach((k, v) -> {
                if (k.startsWith("nn_") && k.startsWith("nn_")) {
                    for (Pair<NodeWrapper[], OrderedRelationship[]> pair : v.getSearchSpace()) {
                        timeoutSearchSpace.addTree(pair.getKey(), pair.getValue());
                    }
                } else if (group.equals("Sort") && k.contains("Sort")) {
                    for (Pair<NodeWrapper[], OrderedRelationship[]> pair : v.getSearchSpace()) {
                        timeoutSearchSpace.addTree(pair.getKey(), pair.getValue());
                    }
                } else if (group.equals("MATH") && !k.contains("Sort") && !k.startsWith("nn_")) {
                    for (Pair<NodeWrapper[], OrderedRelationship[]> pair : v.getSearchSpace()) {
                        timeoutSearchSpace.addTree(pair.getKey(), pair.getValue());
                    }
                }
            });

            String grpName = "";
            if (group.equals("nn_")) {
                grpName = "ALL_NN";
            } else if (group.equals("Sort")) {
                grpName = "ALL_Sort";
            } else if (group.equals("MATH")) {
                grpName = "ALL_MATH";
            }
            goodSearchSpaces.put(grpName, goodSearchSpace);
            badSearchSpaces.put(grpName, badSearchSpace);
            timeoutSearchSpaces.put(grpName, timeoutSearchSpace);
            System.out.println("GOOD " + grpName + " " + goodSearchSpace.getSearchSpace().size());
            System.out.println("BAD " + grpName + " " + badSearchSpace.getSearchSpace().size());
            System.out.println("TIMEOUT " + grpName + " " + timeoutSearchSpace.getSearchSpace().size());
            limits.put(grpName, null);
        });

        // ALL ALL ONly comment in if you wanna compare everything with everything
        limits.clear();
        TrufflePatternSearchSpace goodSearchSpace = new TrufflePatternSearchSpace();
        TrufflePatternSearchSpace badSearchSpace = new TrufflePatternSearchSpace();
        TrufflePatternSearchSpace timeoutSearchSpace = new TrufflePatternSearchSpace();

        goodSearchSpaces.forEach((k, v) -> {
            if (k.startsWith("ALL_")) {
                for (Pair<NodeWrapper[], OrderedRelationship[]> pair : v.getSearchSpace()) {
                    goodSearchSpace.addTree(pair.getKey(), pair.getValue());
                }
            }
        });
        badSearchSpaces.forEach((k, v) -> {
            if (k.startsWith("ALL_")) {
                for (Pair<NodeWrapper[], OrderedRelationship[]> pair : v.getSearchSpace()) {
                    badSearchSpace.addTree(pair.getKey(), pair.getValue());
                }
            }
        });
        timeoutSearchSpaces.forEach((k, v) -> {
            if (k.startsWith("ALL_")) {
                for (Pair<NodeWrapper[], OrderedRelationship[]> pair : v.getSearchSpace()) {
                    timeoutSearchSpace.addTree(pair.getKey(), pair.getValue());
                }
            }
        });

        goodSearchSpaces.put("ALL_ALL", goodSearchSpace);
        badSearchSpaces.put("ALL_ALL", badSearchSpace);
        timeoutSearchSpaces.put("ALL_ALL", timeoutSearchSpace);
        limits.put("ALL_ALL", null);

        //                BitwisePatternMeta meta = new BitwisePatternMeta(TruffleLanguageInformation.getLanguageInformation(MinicLanguage.ID));
        List<String> datatypes = new ArrayList<>();
        datatypes.add("Char");
        datatypes.add("Int");
        datatypes.add("Float");
        datatypes.add("Double");
        datatypes.add("String");
        BitwisePatternMeta meta = BitwisePatternMetaLoader.loadDatatypeIndependent(MinicLanguage.ID, datatypes, false);

        // Conduct Pattern mining between the groups
        limits.keySet().forEach(space -> {
            try {
                String ending = ".html";
                if (threeway) {
                    ending = "_threeway.html";
                }
                writer = new FileWriter(LOCATION + space + ending);
                printer.setWriter(writer);

                List<TrufflePatternProblem> problems = new ArrayList<>();
                problems.add(new TrufflePatternProblem(MinicLanguage.ID,
                        goodSearchSpaces.get(space),
                        "good", meta));
                problems.add(new TrufflePatternProblem(MinicLanguage.ID,
                        badSearchSpaces.get(space),
                        "bad", meta));
                if (threeway & timeoutSearchSpaces.get(space).getSearchSpace().size() > 0) {
                    problems.add(new TrufflePatternProblem(MinicLanguage.ID,
                            timeoutSearchSpaces.get(space),
                            "timeout", meta));

                }

                // MINING METRICS
                detector.clearMetrics();
                detector.setMaxPatternSize(LIMIT);
                double min = 0.8;
                int size = Math.min(goodSearchSpaces.get(space).getSearchSpace().size(), badSearchSpaces.get(space).getSearchSpace().size());
                if (size < 10) {
                    min = (size - 1.0) / (double) size;
                }
                if (min < 0.1) {
                    // ensure single-AST analysis does not allow complete overlap
                    min = 0.8;
                }
                if (space.startsWith("ALL_")) {
                    min = 0.4;
                }
                MaxSupportPerGroupMetric metric = new MaxSupportPerGroupMetric(new AbsDifferenceMetric(null, min, 1.0, problems), GROW_CNT * 3, FINAL_CNT, false);
                metric.getOtherMetrics().add(new SupportMetric(null, min, 1.0));
                detector.injectMetric(null, metric);
                detector.setEditor(new EPMReproductionEditor(meta, 1, 1));

                Solution<TruffleDifferentialPatternSolution, TrufflePatternProblem> significantDiff = detector.comparePatterns(problems);
                printer.printDifferentialPattern(significantDiff.getSolutionGenes().get(0).getGene());
                System.out.println("compared " + space + " " + new Date().toString());
                writer.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });

        // Conduct pattern mining again with embedded
        detector.setEmbedded(true);
        limits.keySet().forEach(space -> {
            try {
                String ending = "_embedded.html";
                if (threeway) {
                    ending = "_embedded_threeway.html";
                }
                writer = new FileWriter(LOCATION + space + ending);
                printer.setWriter(writer);

                List<TrufflePatternProblem> problems = new ArrayList<>();
                problems.add(new TrufflePatternProblem(MinicLanguage.ID,
                        goodSearchSpaces.get(space),
                        "good", meta));
                problems.add(new TrufflePatternProblem(MinicLanguage.ID,
                        badSearchSpaces.get(space),
                        "bad", meta));
                if (threeway & timeoutSearchSpaces.get(space).getSearchSpace().size() > 0) {
                    problems.add(new TrufflePatternProblem(MinicLanguage.ID,
                            timeoutSearchSpaces.get(space),
                            "timeout", meta));

                }

                // MINING METRICS
                detector.clearMetrics();
                detector.setMaxPatternSize(EMBEDDED_LIMIT);
                double min = 0.8;
                int size = Math.min(goodSearchSpaces.get(space).getSearchSpace().size(), badSearchSpaces.get(space).getSearchSpace().size());
                if (size < 10) {
                    min = (size - 1.0) / (double) size;
                }
                if (min < 0.1) {
                    // ensure single-AST analysis does not allow complete overlap
                    min = 0.8;
                }
                if (space.startsWith("ALL_")) {
                    min = 0.4;
                }
                detector.injectMetric(null, min, 1.0);
                detector.injectMetric(null, new MaxSupportPerGroupMetric(new AbsDifferenceMetric(null, min, 1.0, problems), 30, FINAL_CNT, false));
                detector.setEditor(new EPMReproductionEditor(meta, 1, 1));

                Solution<TruffleDifferentialPatternSolution, TrufflePatternProblem> significantDiff = detector.comparePatterns(problems);
                printer.printDifferentialPattern(significantDiff.getSolutionGenes().get(0).getGene());
                System.out.println("compared " + space + " " + new Date().toString());
                writer.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });


        System.out.println("Ending: " + new Date().toString());
        ApplicationContextProvider.getCtx().close();
    }

}
