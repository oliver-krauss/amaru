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
import at.fh.hagenberg.aist.gce.optimization.language.util.EngineConfig;
import at.fh.hagenberg.aist.gce.pattern.*;
import at.fh.hagenberg.aist.gce.pattern.algorithm.metric.*;
import at.fh.hagenberg.aist.gce.pattern.constraint.SolutionConstraint;
import at.fh.hagenberg.aist.gce.pattern.constraint.TestResultConstraint;
import at.fh.hagenberg.aist.gce.pattern.selection.TrufflePatternSearchSpaceDefinition;
import at.fh.hagenberg.aist.gce.science.statistics.data.template.PatternAnalysisPrinter;
import at.fh.hagenberg.machinelearning.analytics.graph.nodes.NodeWrapper;
import at.fh.hagenberg.machinelearning.analytics.graph.nodes.OrderedRelationship;
import at.fh.hagenberg.machinelearning.core.Solution;
import at.fh.hagenberg.util.Pair;
import org.neo4j.driver.Result;
import org.neo4j.driver.Values;
import org.w3c.dom.Node;
import science.aist.neo4j.transaction.TransactionManager;

import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Class that exists to analyze Minc Experiments in the domain of {@link at.fh.hagenberg.aist.gce.optimization.test.TruffleOptimizationTestResult exception}
 * <p>
 * Fault loation info:
 * If you want to search for the operator that created the fault, you can turn on debug and match all trees that are offending
 * with MATCH (n)<-[:OUTPUT]-(m) WHERE id(n) IN [TREES] RETURN (m) you can find the operation
 * RETURN distinct m.operation, count(m.operation) lets you check who is offending
 *
 * @author Oliver Krauss on 21.07.2020
 */
public class MInicFaultDetectionCoLocated {

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
    private static final String LOCATION = EngineConfig.ROOT_LOCATION + "/Amaru.wiki/FAULT/";

    private static final int LIMIT = 10000;

    private static final int FINAL = 100;

    private static final int GROW_CNT = 3000;

    public static void main(String[] args) throws IOException {
        System.out.println("Starting: " + new Date().toString());
        manager = ApplicationContextProvider.getCtx().getBean("transactionManager", TransactionManager.class);

        // Identify the differences in the algorithms with their distinct types
        detector.setHierarchyFloor(1);
        detector.setHierarchyCeil(1);
        detector.setMaxPatternSize(5000);
        detector.setEmbedded(false);
        detector.setGrouping(SignificanceType.MAX);
        printer.setDebug(false);
        printer.setFormat("html");
        printer.setOmitSearchSpace(false);
//        printer.setOmitPatternsPerProblem(true);
        printer.setOmitTracability(false);

        AtomicInteger i = new AtomicInteger(-1);

        // find the ORIGINAL space
        TrufflePatternSearchSpaceDefinition successfulSpace = new TrufflePatternSearchSpaceDefinition();
        successfulSpace.setSolutionSpace(false);
        // we want all solutions that do not throw exceptions in any test, and also do not return the wrong datatype
//        successfulSpace.setSolution(new SolutionConstraint(0.0, -1.0));
        TrufflePatternProblem successfulSolutions = new TrufflePatternProblem(MinicLanguage.ID, detector.findSearchSpace(successfulSpace), "Sucessful");

        // FAILED TO SERIALIZE MESSAGE
//        // Clear out all solutions not containing an allocate node
//        successfulSolutions.getSearchSpace().getSearchSpace().removeIf(x -> Arrays.stream(x.getKey()).noneMatch(y -> y.getType().contains("Allocate")));
//        // make allocate the root and remove all other
//        LinkedList<Pair<NodeWrapper[], OrderedRelationship[]>> searchSpace = successfulSolutions.getSearchSpace().getSearchSpace();
//        String rootType = "Allocate";
//        processSearchSpace(rootType, searchSpace);

        //   java.lang.ArrayIndexOutOfBoundsException -> read
        // Clear out all solutions not containing an read array node
//        successfulSolutions.getSearchSpace().getSearchSpace().removeIf(x -> Arrays.stream(x.getKey()).noneMatch(y -> y.getType().contains("ArrayReadNode")));
//        // make read the root and remove all other
//        LinkedList<Pair<NodeWrapper[], OrderedRelationship[]>> searchSpace = successfulSolutions.getSearchSpace().getSearchSpace();
//        String rootType = "ArrayReadNode";
//        processSearchSpace(rootType, searchSpace);

        //   java.lang.ArrayIndexOutOfBoundsException -> write
        successfulSolutions.getSearchSpace().getSearchSpace().removeIf(x -> Arrays.stream(x.getKey()).noneMatch(y -> y.getType().contains("ArrayWriteNode")));
        // make read the root and remove all other
        LinkedList<Pair<NodeWrapper[], OrderedRelationship[]>> searchSpace = successfulSolutions.getSearchSpace().getSearchSpace();
        String rootType = "ArrayWriteNode";
        processSearchSpace(rootType, searchSpace);

        // this is DEDICATED TESTING for co located patterns
        String ex = "java.lang.ArrayIndexOutOfBoundsException";

        Random rnd = new Random();
        while (successfulSolutions.getSearchSpace().getSearchSpace().size() > LIMIT) {
            successfulSolutions.getSearchSpace().getSearchSpace().remove(rnd.nextInt(successfulSolutions.getSearchSpace().getSearchSpace().size()));
        }

        // Analytics

        try {
            String readable = extractException(ex);

            // find the exception space
            TrufflePatternSearchSpaceDefinition definition = new TrufflePatternSearchSpaceDefinition();
            definition.setSolutionSpace(true);
            List<TestResultConstraint> testResults = new LinkedList<>();
            testResults.add(new TestResultConstraint(ex));
            definition.setTestResult(testResults);
            TrufflePatternProblem problem = new TrufflePatternProblem(MinicLanguage.ID, detector.findSearchSpace(definition), ex);
            processSearchSpace(rootType, problem.getSearchSpace().getSearchSpace());

            if (problem.getSearchSpace().getSearchSpace().size() > 3) {
                while (problem.getSearchSpace().getSearchSpace().size() > LIMIT) {
                    problem.getSearchSpace().getSearchSpace().remove(rnd.nextInt(problem.getSearchSpace().getSearchSpace().size()));
                }

                // we analyze every issue that has occured at least 3 times
                i.getAndIncrement();

                System.out.println("FAULT " + i.get() + " " + ex + " " + problem.getSearchSpace().getSearchSpace().size() + " (fails) / " + successfulSolutions.getSearchSpace().getSearchSpace().size());


                // mine often occuring in good space
                successfulSolutions.getSearchSpace().reset();
                problem.getSearchSpace().reset();
                detector.clearMetrics();
                detector.injectMetric(successfulSolutions, new TopNMetric(new SupportMetric(Collections.singletonList(successfulSolutions), 0.0, 1.0), GROW_CNT, FINAL, false));
                System.out.println("SEARCHING FAULTS " + LocalDateTime.now());
                Solution<TrufflePattern, TrufflePatternProblem> significantPatterns = detector.findSignificantPatterns(successfulSolutions);

                if (!significantPatterns.getSolutionGenes().isEmpty()) {
                    // The diff serves to check if we have any patterns exclusive to the negative space that aren't occuring in the positive space
                    writer = new FileWriter(LOCATION + "often_" + i.get() + "_solution_" + readable + ".html");
                    printer.setWriter(writer);
                    printer.printPatternSolution(significantPatterns);
                    writer.close();
                }

                // mine often occuring in bad space
                successfulSolutions.getSearchSpace().reset();
                problem.getSearchSpace().reset();
                detector.clearMetrics();
                detector.injectMetric(problem, new TopNMetric(new SupportMetric(Collections.singletonList(problem), 0.0, 1.0), GROW_CNT, FINAL, false));
                System.out.println("SEARCHING FAULTS " + LocalDateTime.now());
                significantPatterns = detector.findSignificantPatterns(problem);

                if (!significantPatterns.getSolutionGenes().isEmpty()) {
                    // The diff serves to check if we have any patterns exclusive to the negative space that aren't occuring in the positive space
                    writer = new FileWriter(LOCATION + "often_" + i.get() + "_bug_" + readable + ".html");
                    printer.setWriter(writer);
                    printer.printPatternSolution(significantPatterns);
                    writer.close();
                }

//                // mine faults
//                List<TrufflePatternProblem> problems = new LinkedList<>();
//                problems.add(problem);
//                problems.add(successfulSolutions);
//                successfulSolutions.getSearchSpace().reset();
//                problem.getSearchSpace().reset();
//                detector.clearMetrics();
//                detector.injectMetric(problem, new TopNMetric(new FaultFindingMetric(successfulSolutions, problem, 0), GROW_CNT, 10, true));
//                detector.injectMetric(problems, 0.25, 1); // make sure the pattern occurs in at least 10% of trees otherwise we get a lot of nonsense
//                System.out.println("SEARCHING FAULTS " + LocalDateTime.now());
//                Solution<TruffleDifferentialPatternSolution, TrufflePatternProblem> significantDiff = detector.comparePatterns(problems);
//
//                if (!significantDiff.getSolutionGenes().get(0).getGene().getDifferential().isEmpty()) {
//                    // The diff serves to check if we have any patterns exclusive to the negative space that aren't occuring in the positive space
//                    writer = new FileWriter(LOCATION + "colocated_" + i.get() + "_bug_" + readable + ".html");
//                    printer.setWriter(writer);
//                    printer.printDifferentialPattern(significantDiff.getSolutionGenes().get(0).getGene());
//                    writer.close();
//                }
//
//                // mine inverse faults faults
//                problems = new LinkedList<>();
//                problems.add(problem);
//                problems.add(successfulSolutions);
//                successfulSolutions.getSearchSpace().reset();
//                problem.getSearchSpace().reset();
//                detector.clearMetrics();
//                detector.injectMetric(problem, new TopNMetric(new FaultFindingMetric(problem, successfulSolutions, 0), GROW_CNT, 10, true));
//                detector.injectMetric(problems, 0.25, 1); // make sure the pattern occurs in at least 10% of trees otherwise we get a lot of nonsense
//                System.out.println("SEARCHING OMISSION FAULTS " + LocalDateTime.now());
//                significantDiff = detector.comparePatterns(problems);
//
//                if (!significantDiff.getSolutionGenes().get(0).getGene().getDifferential().isEmpty()) {
//                    // The diff serves to check if we have any patterns exclusive to the negative space that aren't occuring in the positive space
//                    writer = new FileWriter(LOCATION + "colocated_" + i.get() + "_omission_" + readable + ".html");
//                    printer.setWriter(writer);
//                    printer.printDifferentialPattern(significantDiff.getSolutionGenes().get(0).getGene());
//                    writer.close();
//                }


            } else {
                System.out.println("Skipping too small set " + ex + " " + problem.getSearchSpace().getSearchSpace().size());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }


        System.out.println("Ending: " + new Date().toString());
        ApplicationContextProvider.getCtx().close();
    }

    private static void processSearchSpace(String rootType, LinkedList<Pair<NodeWrapper[], OrderedRelationship[]>> searchSpace) {
        LinkedList<Pair<NodeWrapper[], OrderedRelationship[]>> newSearchSpace = new LinkedList<>();

        searchSpace.forEach(x -> {
            for (int pos = 0; pos < x.getKey().length; pos++) {
                NodeWrapper root = x.getKey()[pos];
                if (root.getType().contains(rootType)) {
                    List<NodeWrapper> keyList = new ArrayList<>();
                    keyList.add(root);
                    List<OrderedRelationship> valueList = new ArrayList<>();
                    processTree(x, root, keyList, valueList);
                    NodeWrapper[] key = new NodeWrapper[keyList.size()];
                    keyList.toArray(key);
                    OrderedRelationship[] value = new OrderedRelationship[valueList.size()];
                    valueList.toArray(value);
                    Pair<NodeWrapper[], OrderedRelationship[]> newRoot = new Pair<>(key, value);
                    newSearchSpace.add(newRoot);
                }
            }
        });

        // replace search space
        searchSpace.clear();
        searchSpace.addAll(newSearchSpace);
    }

    private static void processTree(Pair<NodeWrapper[], OrderedRelationship[]> tree, NodeWrapper root, List<NodeWrapper> keyList, List<OrderedRelationship> valueList) {
        List<OrderedRelationship> collect = Arrays.stream(tree.getValue()).filter(x -> x.getParent().getId().equals(root.getId())).collect(Collectors.toList());
        if (root.getType().contains("ArrayWriteNode"))  {
            collect.removeIf(x -> !x.getField().equals("arrayPosition"));
        }
        valueList.addAll(collect);
        List<NodeWrapper> children = collect.stream().map(OrderedRelationship::getChild).collect(Collectors.toList());
        keyList.addAll(children);
        children.forEach(x -> processTree(tree, x, keyList, valueList));
    }

    private static String extractException(String ex) {
        String search = "";
        if (ex.contains("Exception")) {
            search = "Exception";
        } else {
            search = "Error";
        }
        return ex.substring(0, ex.indexOf(search) + search.length());
    }

}
