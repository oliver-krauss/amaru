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
import at.fh.hagenberg.machinelearning.core.Solution;
import org.neo4j.driver.Result;
import org.neo4j.driver.Values;
import science.aist.neo4j.transaction.TransactionManager;

import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

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
public class MinicFaultDetectionOtherMetric {

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
    private static final String LOCATION = EngineConfig.ROOT_LOCATION + "/Amaru.LOGS/FAULT/";

    private static final int LIMIT = 1000;

    private static final int GROW_CNT = 100;

    public static void main(String[] args) throws IOException {
        System.out.println("Starting: " + new Date().toString());
        manager = ApplicationContextProvider.getCtx().getBean("transactionManager", TransactionManager.class);

        // Identify the differences in the algorithms with their distinct types
        detector.setHierarchyFloor(1);
        detector.setHierarchyCeil(1);
        detector.setMaxPatternSize(5000);
        detector.setEmbedded(true);
        detector.setGrouping(SignificanceType.MAX);
        printer.setDebug(false);
        printer.setFormat("html");
        printer.setOmitSearchSpace(true);
//        printer.setOmitPatternsPerProblem(true);
        printer.setOmitTracability(true);

        Result exceptions = manager.executeRead((transaction) -> transaction.run("match (n:TruffleOptimizationTestResult) return distinct n.exception", new HashMap<>()));

        AtomicInteger i = new AtomicInteger(-1);

        // find the non-failing space
        TrufflePatternSearchSpaceDefinition successfulSpace = new TrufflePatternSearchSpaceDefinition();
        successfulSpace.setSolutionSpace(true);
        // we want all solutions that do not throw exceptions in any test, and also do not return the wrong datatype
        successfulSpace.setSolution(new SolutionConstraint(0.0, -1.0));
        TrufflePatternProblem successfulSolutions = new TrufflePatternProblem(MinicLanguage.ID, detector.findSearchSpace(successfulSpace), "Sucessful");

        Set<String> relevantExceptions = new HashSet<>();
        exceptions.forEachRemaining(x -> {
            if (!x.get(0).isNull()) {
                // trim to first line
                String ex = x.get(0).asString().replace("'", "\\'");
                if (ex.contains("\n")) {
                    ex = ex.substring(0, ex.indexOf("\n")).trim();
                }

                // special handling for the weirder messages
                if (ex.contains("An illegal reflective access operation has occurred")) {
                    relevantExceptions.add("An illegal reflective access operation has occurred");
                } else if (ex.contains("The worker crashed")) {
                    relevantExceptions.add("The worker crashed");
                }
                // reduce to text before : and @
                else if (ex.contains(":")) {
                    relevantExceptions.add(ex.substring(0, ex.indexOf(":")));
                } else if (ex.contains("@")) {
                    relevantExceptions.add(ex.substring(0, ex.indexOf("@")));
                } else {
                    relevantExceptions.add(ex);
                }
            }
        });

        System.out.println("Exceptions to be mined " + relevantExceptions.size());
        relevantExceptions.stream().sorted().forEach(x -> {
            Result res = manager.executeRead((transaction) -> transaction.run("match (n:TruffleOptimizationTestResult)<-[:TEST_RESULT]-(s) where n.exception contains $EXCEPTION with distinct(s) return count(s)", Values.parameters("EXCEPTION", x)));
            long size = res.next().get(0).asLong();
            System.out.println(size + " " + x);
        });

        Random rnd = new Random();
        while (successfulSolutions.getSearchSpace().getSearchSpace().size() > LIMIT) {
            successfulSolutions.getSearchSpace().getSearchSpace().remove(rnd.nextInt(successfulSolutions.getSearchSpace().getSearchSpace().size()));
        }

        // Analytics
        relevantExceptions.forEach(ex -> {
            try {
                String readable = extractException(ex);

                // find the exception space
                TrufflePatternSearchSpaceDefinition definition = new TrufflePatternSearchSpaceDefinition();
                definition.setSolutionSpace(true);
                List<TestResultConstraint> testResults = new LinkedList<>();
                testResults.add(new TestResultConstraint(ex));
                definition.setTestResult(testResults);
                TrufflePatternProblem problem = new TrufflePatternProblem(MinicLanguage.ID, detector.findSearchSpace(definition), ex);

                if (problem.getSearchSpace().getSearchSpace().size() > 3) {
                    while (problem.getSearchSpace().getSearchSpace().size() > LIMIT) {
                        problem.getSearchSpace().getSearchSpace().remove(rnd.nextInt(problem.getSearchSpace().getSearchSpace().size()));
                    }

                    // we analyze every issue that has occured at least 3 times
                    i.getAndIncrement();

                    System.out.println("FAULT " + i.get() + " " + ex + " " + problem.getSearchSpace().getSearchSpace().size() + " (fails) / " + successfulSolutions.getSearchSpace().getSearchSpace().size());

                    // mine faults
                    List<TrufflePatternProblem> problems = new LinkedList<>();
                    problems.add(problem);
                    problems.add(successfulSolutions);
                    problem.getSearchSpace().reset();
                    detector.setHierarchyCeil(Integer.MAX_VALUE);
                    detector.clearMetrics();
                    ArrayList<TrufflePatternProblem> opposite = new ArrayList<>();
                    opposite.add(successfulSolutions);
                    detector.injectMetric(problem, new TopNMetric(new DifferenceMustGrowMetric(problem, 0.25, 1, opposite), GROW_CNT, 20, true));
                    detector.injectMetric(problems, 0.25, 1); // make sure the pattern occurs in at least 10% of trees otherwise we get a lot of nonsense
                    System.out.println("SEARCHING FAULTS " + LocalDateTime.now());
                    Solution<TruffleDifferentialPatternSolution, TrufflePatternProblem> significantDiff = detector.comparePatterns(problems);

                    if (!significantDiff.getSolutionGenes().get(0).getGene().getDifferential().isEmpty()) {
                        // The diff serves to check if we have any patterns exclusive to the negative space that aren't occuring in the positive space
                        writer = new FileWriter(LOCATION + "fault_" + i.get() + "_bug_" + readable + ".html");
                        printer.setWriter(writer);
                        printer.printDifferentialPattern(significantDiff.getSolutionGenes().get(0).getGene());
                        writer.close();

//                        coLocatedMining("fault_" + i.get() + "_bug_" + readable, significantDiff);
                    }

                    // mine inverse faults faults
                    problems = new LinkedList<>();
                    problems.add(problem);
                    problems.add(successfulSolutions);
                    problem.getSearchSpace().reset();
                    detector.setHierarchyCeil(Integer.MAX_VALUE);
                    detector.clearMetrics();
                    detector.injectMetric(problem, new TopNMetric(new DifferenceMustGrowMetric(problem, 0.25, 1, opposite), GROW_CNT, 20, true));
                    detector.injectMetric(problems, 0.25, 1); // make sure the pattern occurs in at least 10% of trees otherwise we get a lot of nonsense
                    System.out.println("SEARCHING OMISSION FAULTS " + LocalDateTime.now());
                    significantDiff = detector.comparePatterns(problems);

                    if (!significantDiff.getSolutionGenes().get(0).getGene().getDifferential().isEmpty()) {
                        // The diff serves to check if we have any patterns exclusive to the negative space that aren't occuring in the positive space
                        writer = new FileWriter(LOCATION + "fault_" + i.get() + "_omission_" + readable + ".html");
                        printer.setWriter(writer);
                        printer.printDifferentialPattern(significantDiff.getSolutionGenes().get(0).getGene());
                        writer.close();

//                        coLocatedMining("fault_" + i.get() + "_bug_" + readable, significantDiff);
                    }

//                    // mine faults of omission
//                    problem.getSearchSpace().reset();
//                    detector.setHierarchyCeil(1);
//                    detector.clearMetrics();
//                    detector.injectMetric(problem, new TopNMetric(new FaultOfOmissionFindingMetric(successfulSolutions, problem, 0.1), 5, 20, false));
//                    detector.injectMetric(problems, 0.1, 1); // make sure the pattern occurs in at least 10% of trees otherwise we get a lot of nonsense
//
////                  Faults of Omission ONLY when we deem it necessary
//                    System.out.println("SEARCHING FAULTS OF OMISSION " + LocalDateTime.now());
//                    significantDiff = detector.comparePatterns(problems);
//
//                    if (!significantDiff.getSolutionGenes().get(0).getGene().getDifferential().isEmpty()) {
//                        // The diff serves to check if we have any patterns exclusive to the negative space that aren't occuring in the positive space
//                        writer = new FileWriter(LOCATION + "fault_" + i.get() + "_omission_" + readable + ".html");
//                        printer.setWriter(writer);
//                        printer.printDifferentialPattern(significantDiff.getSolutionGenes().get(0).getGene());
//                        writer.close();
//
////                        coLocatedMining("fault_" + i.get() + "_omission_" + readable, significantDiff);
//
//                        // TODO #246 I think this is the perfect location -> IF we get a _omssion_ _outOfCluster_ we create REWRITE TUPLES
//                        // Match them with a better contains
//                        // everything MISSING in contains add a NEG Node
//                        // Add the "Matching Nodes" Algorithm
//                        // Application of rewrite is 247 NOT part of this issue but rather interesting for the Fault Solver
//                    }


                } else {
                    System.out.println("Skipping too small set " + ex);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        });

        System.out.println("Ending: " + new Date().toString());
        ApplicationContextProvider.getCtx().close();
    }

    private static void coLocatedMining(String identity, Solution<TruffleDifferentialPatternSolution, TrufflePatternProblem> solution) throws IOException {
        // Co-Located Pattern Mining - in Cluster = JUST THAT ONE CLUSTER WITH HIGH SUPPORT
        List<TrufflePatternProblem> inClusterProblems = new LinkedList<>();

        // Looking per cluster individually
        solution.getSolutionGenes().get(0).getGene().getPatternsPerProblem().forEach((k, v) -> {
            TrufflePatternSearchSpaceDefinition singleSearchSpaceDef = new TrufflePatternSearchSpaceDefinition();
            v.forEach(singleSearchSpaceDef::includeTree);
            TrufflePatternProblem problem = new TrufflePatternProblem(MinicLanguage.ID, detector.findSearchSpace(singleSearchSpaceDef), k.getName());
            inClusterProblems.add(problem);
        });

        MustContainMetric mustContainMetric = new MustContainMetric(solution.getSolutionGenes().get(0).getGene().getDifferential().keySet().stream().map(TrufflePattern::getBitRepresentation).collect(Collectors.toList()));

        // use high support metric
        detector.clearMetrics();
        detector.injectMetric(inClusterProblems.get(0), mustContainMetric);
        detector.injectMetric(inClusterProblems.get(0), new TopNMetric(new SupportMetric(inClusterProblems, 0.8, 1), 200, 20, false));

        Solution<TruffleDifferentialPatternSolution, TrufflePatternProblem> inClusterSolution = detector.comparePatterns(inClusterProblems);

        if (!inClusterSolution.getSolutionGenes().get(0).getGene().getDifferential().isEmpty()) {
            writer = new FileWriter(LOCATION + identity + "_inClusterCoLocation.html");
            printer.setWriter(writer);
            printer.printDifferentialPattern(inClusterSolution.getSolutionGenes().get(0).getGene());
            writer.close();
        }

        // Co-Located Pattern Mining - outside Cluster = ORIGINAL CLUSTER + OTHERS WITH LITTLE DISCRIMINATIVE VALUE
        // use high overlap metric
        inClusterProblems.forEach(x -> x.getSearchSpace().reset());
        TrufflePatternProblem successful = inClusterProblems.stream().filter(x -> x.getName().equals("Sucessful")).findFirst().get();
        TrufflePatternProblem failing = inClusterProblems.stream().filter(x -> !x.getName().equals("Sucessful")).findFirst().get();
        detector.clearMetrics();
        detector.injectMetric(inClusterProblems.get(0), mustContainMetric);
        detector.injectMetric(inClusterProblems.get(0), new TopNMetric(new FaultOfOmissionFindingMetric(successful, failing, 0.1), 200, 20, false));

        Solution<TruffleDifferentialPatternSolution, TrufflePatternProblem> outsideClusterSolution = detector.comparePatterns(inClusterProblems);

        if (!outsideClusterSolution.getSolutionGenes().get(0).getGene().getDifferential().isEmpty()) {
            writer = new FileWriter(LOCATION + identity + "_outsideClusterCoLocation.html");
            printer.setWriter(writer);
            printer.printDifferentialPattern(outsideClusterSolution.getSolutionGenes().get(0).getGene());
            writer.close();
        }
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
