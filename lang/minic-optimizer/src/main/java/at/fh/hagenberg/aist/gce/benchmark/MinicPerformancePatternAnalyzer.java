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

import at.fh.hagenberg.aist.gce.benchmark.mutators.TruffleDivisionFixingMutator;
import at.fh.hagenberg.aist.gce.benchmark.mutators.TruffleDoubleFixingMutator;
import at.fh.hagenberg.aist.gce.context.ApplicationContextProvider;
import at.fh.hagenberg.aist.gce.minic.language.MinicContext;
import at.fh.hagenberg.aist.gce.minic.language.MinicLanguage;
import at.fh.hagenberg.aist.gce.minic.nodes.builtin.ReadNode;
import at.fh.hagenberg.aist.gce.minic.nodes.builtin.ReadNodeFactory;
import at.fh.hagenberg.aist.gce.minic.nodes.impl.cast.MinicToFloatNode;
import at.fh.hagenberg.aist.gce.minic.nodes.impl.cast.MinicToStringNode;
import at.fh.hagenberg.aist.gce.minic.nodes.impl.control.MinicIfNode;
import at.fh.hagenberg.aist.gce.minic.nodes.impl.function.MinicFunctionLiteralNode;
import at.fh.hagenberg.aist.gce.minic.nodes.impl.function.MinicInvokeNode;
import at.fh.hagenberg.aist.gce.minic.nodes.impl.function.MinicInvokeNodeFactory;
import at.fh.hagenberg.aist.gce.minic.nodes.impl.literals.MinicSimpleLiteralNode;
import at.fh.hagenberg.aist.gce.minic.nodes.impl.op.arith.floating.MinicDoubleArithmeticNode;
import at.fh.hagenberg.aist.gce.minic.nodes.impl.op.arith.floating.MinicFloatArithmeticNodeFactory;
import at.fh.hagenberg.aist.gce.minic.nodes.impl.op.arith.integer.signed.MinicCharArithmeticNode;
import at.fh.hagenberg.aist.gce.minic.nodes.impl.op.arith.integer.signed.MinicIntArithmeticNode;
import at.fh.hagenberg.aist.gce.minic.nodes.impl.op.arith.integer.signed.MinicIntArithmeticNodeFactory;
import at.fh.hagenberg.aist.gce.minic.nodes.impl.vars.*;
import at.fh.hagenberg.aist.gce.minic.types.floating.MinicFloatNode;
import at.fh.hagenberg.aist.gce.optimization.TruffleEvaluatorImpl;
import at.fh.hagenberg.aist.gce.optimization.TruffleOptimizationProblem;
import at.fh.hagenberg.aist.gce.optimization.TruffleOptimizationSolution;
import at.fh.hagenberg.aist.gce.optimization.TruffleOptimizationSolutionRepository;
import at.fh.hagenberg.aist.gce.optimization.cachet.AccuracyCachetEvaluator;
import at.fh.hagenberg.aist.gce.optimization.executor.InternalExecutor;
import at.fh.hagenberg.aist.gce.optimization.executor.MessageExecutor;
import at.fh.hagenberg.aist.gce.optimization.language.util.EngineConfig;
import at.fh.hagenberg.aist.gce.optimization.operators.ConfigurableMutator;
import at.fh.hagenberg.aist.gce.optimization.operators.TruffleFaultFixingTreeMutator;
import at.fh.hagenberg.aist.gce.optimization.operators.selection.DepthWidthRestrictedTruffleTreeSelector;
import at.fh.hagenberg.aist.gce.optimization.runtime.RuntimeProfile;
import at.fh.hagenberg.aist.gce.optimization.test.*;
import at.fh.hagenberg.aist.gce.optimization.util.ExtendedNodeUtil;
import at.fh.hagenberg.aist.gce.optimization.util.MinicLanguageLearner;
import at.fh.hagenberg.aist.gce.optimization.util.TruffleLanguageInformation;
import at.fh.hagenberg.aist.gce.optimization.util.TruffleLanguageSearchSpace;
import at.fh.hagenberg.aist.gce.optimization.util.strategy.*;
import at.fh.hagenberg.aist.gce.optimization.util.strategy.initializer.TruffleEntryPointStrategy;
import at.fh.hagenberg.aist.gce.optimization.util.strategy.other.*;
import at.fh.hagenberg.aist.gce.optimization.util.strategy.random.MinicAntiFunctionLiteralStrategy;
import at.fh.hagenberg.aist.gce.optimization.util.strategy.random.MinicCantInvokeStrategy;
import at.fh.hagenberg.aist.gce.optimization.util.strategy.random.MinicFunctionLiteralStrategy;
import at.fh.hagenberg.aist.gce.optimization.util.strategy.random.MinicInvokeStrategy;
import at.fh.hagenberg.aist.gce.pattern.SignificanceType;
import at.fh.hagenberg.aist.gce.pattern.TrufflePatternDetector;
import at.fh.hagenberg.aist.gce.pattern.Wildcard;
import at.fh.hagenberg.aist.gce.pattern.constraint.CachetConstraint;
import at.fh.hagenberg.aist.gce.pattern.constraint.ProblemConstraint;
import at.fh.hagenberg.aist.gce.pattern.constraint.SolutionConstraint;
import at.fh.hagenberg.aist.gce.pattern.constraint.TestResultConstraint;
import at.fh.hagenberg.aist.gce.pattern.encoding.BitwisePatternMeta;
import at.fh.hagenberg.aist.gce.pattern.encoding.BitwisePatternMetaLoader;
import at.fh.hagenberg.aist.gce.pattern.selection.TrufflePatternSearchSpace;
import at.fh.hagenberg.aist.gce.pattern.selection.TrufflePatternSearchSpaceDefinition;
import at.fh.hagenberg.aist.gce.science.statistics.data.template.PatternConfidencePrinter;
import at.fh.hagenberg.machinelearning.analytics.graph.MinicPatternRepository;
import at.fh.hagenberg.machinelearning.analytics.graph.PatternRepository;
import at.fh.hagenberg.machinelearning.analytics.graph.TruffleProblemGeneRepository;
import at.fh.hagenberg.machinelearning.analytics.graph.TruffleTestCaseRepository;
import at.fh.hagenberg.machinelearning.analytics.graph.nodes.NodeWrapper;
import at.fh.hagenberg.machinelearning.analytics.graph.nodes.OrderedRelationship;
import at.fh.hagenberg.machinelearning.core.ProblemGene;
import at.fh.hagenberg.machinelearning.core.Solution;
import at.fh.hagenberg.machinelearning.core.SolutionGene;
import at.fh.hagenberg.machinelearning.core.fitness.CachetEvaluator;
import at.fh.hagenberg.machinelearning.core.options.Descriptor;
import at.fh.hagenberg.util.Pair;
import com.oracle.truffle.api.frame.MaterializedFrame;
import com.oracle.truffle.api.nodes.Node;
import org.neo4j.driver.Result;
import org.neo4j.driver.Values;
import science.aist.neo4j.transaction.TransactionManager;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Class that exists to analyze patterns found by performance detection
 *
 * @author Oliver Krauss on 21.07.2020
 */
public class MinicPerformancePatternAnalyzer {

    /**
     * Algorithm to compare the different trees
     */
    static TrufflePatternDetector detector = new TrufflePatternDetector();

    static TransactionManager manager;

    static PatternConfidencePrinter printer = new PatternConfidencePrinter();

    /**
     * folder location where the output will be pushed to
     */
    protected static String PATH = EngineConfig.ROOT_LOCATION + "/Amaru/gce/optimization-optimizer/src/test/resources/testCases/";
    private static final String LOCATION = EngineConfig.ROOT_LOCATION + "/Amaru.LOGS/PatternsPerformanceAnalysis/";

    private static final String PROBLEM_FIND = "match (n)<-[:TREE]-()<-[:RWGENE]-()-[:SOLVES]->()-[:RWGENE]->(problem) where id(n) = $ID and not problem.description contains \"_BENCHMARK\" return problem";

    private static Map<String, ProblemHelperClass> loadedProblems = new HashMap<>();

    private static TruffleProblemGeneRepository truffleOptimizationProblemRepository;
    private static TruffleTestCaseRepository truffleTestCaseRepository;
    private static TruffleMasterStrategy masterStrategy;
    private static TruffleEntryPointStrategy entryPointStrategy;

    private static final int targetCount = 100;

    public enum EvalType {
        POSITIVE,
        NEGATIVE,
        PREVENT,
    }

    public static HashMap<String, Double> baseMedians;

    public static void main(String[] args) throws IOException {
        System.out.println("Starting: " + new Date().toString());

        // base medians for each experiment (too lazy to get from db, hardcoded)
        baseMedians = new HashMap<>();
        baseMedians.put("sqrt_java", 2553.0);
        baseMedians.put("sqrt_lookup", 26449.0);
        baseMedians.put("sqrt_nolookup", 147868.0);
        baseMedians.put("cbrt", 231039.0);
        baseMedians.put("surt", 316342.0);
        baseMedians.put("log", 282900.0);
        baseMedians.put("ln", 549841.0);
        baseMedians.put("invSqrt", 192140.0);
        baseMedians.put("bubbleSort", 3492836.0);
        baseMedians.put("heapSort", 304806.0);
        baseMedians.put("insertionSort", 3382195.0);
        baseMedians.put("mergeSort", 376408.0);
        baseMedians.put("mergeSortInlined", 83477.0);
        baseMedians.put("quickSort", 1297887.0);
        baseMedians.put("quickSortInlined", 1247939.0);
        baseMedians.put("selectionSort", 2772077.0);
        baseMedians.put("shakerSort", 11762.0);
        baseMedians.put("shellSort", 36909.0);
        baseMedians.put("nn_options", 1718321.0);
        baseMedians.put("nn_fullinline", 1982779.0);
        baseMedians.put("nn_sigmoid", 1565001.0);
        baseMedians.put("nn_tanh", 1683544.0);
        baseMedians.put("nn_swish", 1924867.0);
        baseMedians.put("nn_relu", 1574603.0);
        baseMedians.put("nn_lrelu", 1564061.0);


        // learn language as some strategies need the additional info
        TruffleLanguageInformation tli = TruffleLanguageInformation.getLanguageInformation(MinicLanguage.ID);
        MinicLanguageLearner learner = new MinicLanguageLearner(tli);
        learner.setSaveToDB(false);
        learner.setFast(true);
        learner.learn();

        manager = ApplicationContextProvider.getCtx().getBean("transactionManager", TransactionManager.class);

        // Identify the differences in the algorithms with their distinct types
        detector.setHierarchyFloor(0);
        detector.setHierarchyCeil(Integer.MAX_VALUE);
        detector.setMaxPatternSize(100);
        detector.setEmbedded(false);
        detector.setGrouping(SignificanceType.MAX);

        // the count how many ASTs will be checked -> if we don't have that many ASTs for testing we reduce to the maximum available
        // the exception to search (TODO #63 generalize this to injecting the search space definitions instead of hardcoding)
        BitwisePatternMeta meta = new BitwisePatternMeta(tli, true);
        BitwisePatternMeta metaContained = new BitwisePatternMeta(tli, false);
        List<String> datatypes = new ArrayList<>();
        datatypes.add("Char");
        datatypes.add("Int");
        datatypes.add("Float");
        datatypes.add("Double");
        datatypes.add("String");
        BitwisePatternMeta datatypeIndependentMeta = BitwisePatternMetaLoader.loadDatatypeIndependent(MinicLanguage.ID, datatypes, false);

        // get the problem loader and executor
        truffleOptimizationProblemRepository = ((TruffleProblemGeneRepository) ApplicationContextProvider.getCtx().getBean("truffleOptimizationProblemRepository"));
        truffleTestCaseRepository = ((TruffleTestCaseRepository) ApplicationContextProvider.getCtx().getBean("truffleTestCaseRepository"));

        // Antipattern DIVISION (1):
        // NOTE THIS ATTEMPT DOES NOT WORK:
//        NodeWrapper divAntipattern = new NodeWrapper("MinicDTArithmeticNode$MinicDTDivNode");
//        NodeWrapper leftDiv = new NodeWrapper("MinicDTArithmeticNode$MinicDTDivNode");
//        NodeWrapper rightDiv = new NodeWrapper("MinicDTArithmeticNode$MinicDTDivNode");
//        NodeWrapper llDiv = new NodeWrapper("MinicDTArithmeticNode$MinicDTDivNode");
//        NodeWrapper lrDiv = new NodeWrapper("MinicDTArithmeticNode$MinicDTDivNode");
//        NodeWrapper rlDiv = new NodeWrapper("MinicDTArithmeticNode$MinicDTDivNode");
//        NodeWrapper rrDiv = new NodeWrapper("MinicDTArithmeticNode$MinicDTDivNode");
//        divAntipattern.addChild(leftDiv, "leftNode", 0);
//        divAntipattern.addChild(rightDiv, "rightNode", 0);
//        leftDiv.addChild(llDiv, "leftNode", 0);
//        leftDiv.addChild(lrDiv, "rightNode", 0);
//        rightDiv.addChild(rlDiv, "leftNode", 0);
//        rightDiv.addChild(rrDiv, "rightNode", 0);
//        NodeWrapper litNodeForbiddenVal = new NodeWrapper(MinicSimpleLiteralNode.class.getName());
//        litNodeForbiddenVal.getValues().put("value:double", 0.0);
//        litNodeForbiddenVal.getValues().put("value:int", 0);
//        litNodeForbiddenVal.getValues().put("value:char", "\0");
//        litNodeForbiddenVal.getValues().put("value:float", 0.0f);
//        llDiv.addChild(litNodeForbiddenVal, "rightNode", 0);
//        lrDiv.addChild(litNodeForbiddenVal, "rightNode", 0);
//        rlDiv.addChild(new NodeWrapper(MinicSimpleLiteralNode.class.getName()), "leftNode", 0);
//        rlDiv.addChild(litNodeForbiddenVal, "rightNode", 0);
//        rrDiv.addChild(new NodeWrapper(MinicSimpleLiteralNode.class.getName()), "leftNode", 0);
//        rrDiv.addChild(litNodeForbiddenVal, "rightNode", 0);
        evaluatePattern("division", new ArrayList<>(), new ArrayList<>(), datatypeIndependentMeta, EvalType.NEGATIVE, MinicFloatArithmeticNodeFactory.MinicFloatDivNodeGen.class, false);
        // END BLOCK -> DIVISION (1) ------------------------------

        // Antipattern DOUBLE (4):
//        evaluatePattern("double", new ArrayList<>(), new ArrayList<>(), datatypeIndependentMeta, EvalType.NEGATIVE, MinicFloatArithmeticNodeFactory.MinicFloatDivNodeGen.class, false);
//        // END BLOCK -> DOUBLE (4) ------------------------------


        System.out.println("Ending: " + new Date().toString());
        ApplicationContextProvider.getCtx().close();
    }

    private static void evaluatePattern(String name, List<NodeWrapper> patterns, List<NodeWrapper> antiPatterns, BitwisePatternMeta meta, EvalType evalType, Class checkClass, boolean rootMedian) {
        TrufflePatternSearchSpaceDefinition definition = new TrufflePatternSearchSpaceDefinition();
        Random random = new Random();
        definition = new TrufflePatternSearchSpaceDefinition();
        definition.setSolutionSpace(!rootMedian);
        if (!rootMedian) {
            List<CachetConstraint> cachetConstraints = new ArrayList<>();
            cachetConstraints.add(new CachetConstraint("Performance-0.3", 1.0, 0.0));
            definition.setCachets(cachetConstraints);
        }
        TrufflePatternSearchSpace solutions = detector.findSearchSpace(definition);

        TruffleEvaluatorImpl eval = createTruffleEvaluator();
        System.out.println("Examples space is " + solutions.getSearchSpace().size() + " ASTs");

        double confidence = 0;
        double softConfidence = 0;

        HashMap<String, Integer> performances = new HashMap<>();
        performances.put("efficient", 0);
        performances.put("inefficient", 0);
        performances.put("timeout", 0);
        List<Pair<String, Double>> values = new ArrayList<>(targetCount);


        int count = targetCount;

        // load problem and execute it again
        ConfigurableMutator<TruffleOptimizationSolution, TruffleOptimizationProblem> mutator;
        MessageExecutor executor;
        for (int i = 0; i < count; i++) {
            int pos = random.nextInt(solutions.getSearchSpace().size());
            NodeWrapper rootWrapper = solutions.getSearchSpace().get(pos).getKey()[0];
            TruffleOptimizationProblem problem = truffleOptimizationProblemRepository.queryTyped(PROBLEM_FIND, Values.parameters("ID", rootWrapper.getId()));
            while (problem == null) {
                solutions.getSearchSpace().remove(pos);
                pos = random.nextInt(solutions.getSearchSpace().size());
                rootWrapper = solutions.getSearchSpace().get(pos).getKey()[0];
                problem = truffleOptimizationProblemRepository.queryTyped(PROBLEM_FIND, Values.parameters("ID", rootWrapper.getId()));
            }
            // force load the tests
            String e = evalType.equals(EvalType.POSITIVE) ? "p" : "n";
            String key = e + problem.getId();
            if (loadedProblems.containsKey(key)) {
                ProblemHelperClass phc = loadedProblems.get(key);
                problem = phc.problem;
                mutator = phc.mutator;
            } else {
                eval.verifyExecutor(problem, false);
                executor = (MessageExecutor) eval.getExecutor();
                problem = repair(problem, executor.getOrigin());
                masterStrategy = TruffleMasterStrategy.createFromTLI(problem.getConfiguration(), getTruffleLanguageSearchSpace(), getStrategies(), getTerminalStrategies(problem));
                entryPointStrategy = new TruffleEntryPointStrategy(getTruffleLanguageSearchSpace(), problem.getNode(), problem.getNode(), masterStrategy, problem.getConfiguration());
                patterns.forEach(pattern -> {
                    masterStrategy.injectPattern(pattern, meta, 1.0);
                });
                antiPatterns.forEach(antipattern -> {
                    masterStrategy.injectAntiPattern(antipattern, meta);
                });

                if (name.equals("double")) {
                    mutator = createDoubleReplacingMutator();
                } else if (name.equals("division")) {
                    mutator = createDivisionReplacingMutator();
                } else {
                    mutator = createFaultFixingMutator();
                    ((TruffleFaultFixingTreeMutator) mutator).setCheck(checkClass);
                }
                loadedProblems.put(key, new ProblemHelperClass(problem, mutator));
            }
            eval.verifyExecutor(problem, true);
            // mutate before evaluation and then execute
            executor = (MessageExecutor) eval.getExecutor();
            NodeWrapper nodeWrapper = solutions.toAst(solutions.getSearchSpace().get(pos));
            long identifier = nodeWrapper.getId();
            Node node = NodeWrapper.unwrap(nodeWrapper, executor.getRoot().getFrameDescriptor(), executor.getGlobalScope(), MinicLanguage.ID);
            boolean prebug = false;
            if (prebug) {
                new InternalExecutor(problem.getLanguage(), problem.getCode(), problem.getEntryPoint(), problem.getFunction()).conductTest(node, problem.getTests().iterator().next().getTest().getInputArguments());
            }
            Solution<TruffleOptimizationSolution, TruffleOptimizationProblem> solution = createSolution(node, problem);
            try {
                solution = mutator.mutate(solution);
                eval.evaluateQuality(solution);

                // evaluate the confidence in the pattern
                // IF you want to just test without enforcing validity set to < 1
                if (solution.getQuality() == 0 || solution.getSolutionGenes().get(0).getGene().getProblem().getDescription().startsWith("nn") && validateNN(solution.getSolutionGenes().get(0).getGene().getTestResults().iterator().next())) {
                    System.out.println("SUCCESSFUL CANDIDATE " + problem.getDescription() + new Date().toString());
                    TruffleOptimizationProblem benchProblem = TruffleOptimizationProblem.copy(problem);
                    benchProblem.setBenchmark();
                    benchProblem.getTests().clear();
                    String groupName = "";
                    if (problem.getDescription().contains("Sort")) {
                        groupName = "sort";
                        benchProblem.setEntryPoint(benchProblem.getFunction());
                    } else if (problem.getDescription().startsWith("nn_")) {
                        groupName = "nn";
                        benchProblem.setEntryPoint("nn_entry");
                    } else {
                        groupName = "math";
                        benchProblem.setEntryPoint(benchProblem.getFunction() + "_benchmark");
                    }
                    benchProblem.getTests().addAll(getBenchmarkCases(groupName).stream().map(x -> new TruffleOptimizationTestComplexity(benchProblem, x)).collect(Collectors.toList()));

                    Solution<TruffleOptimizationSolution, TruffleOptimizationProblem> benchSolution = createSolution(solution.getSolutionGenes().get(0).getGene().getNode(), benchProblem);
                    long len = new File(EngineConfig.ROOT_LOCATION + "/Amaru.LOGS/PerformanceProfiles/runtimePATTERNVERIFY/").listFiles().length + 1;
                    TruffleOptimizationSolution truffleSolution = benchSolution.getSolutionGenes().get(0).getGene();
                    truffleSolution.getTree().setId(-1 * len);
                    eval.verifyExecutor(benchProblem, true);
                    if (groupName.equals("math")) {
                        eval.setTimeout(5 * 60 * 1000);
                    } else {
                        eval.setTimeout(30 * 60 * 1000);
                    }
                    double v = eval.evaluateQuality(benchSolution);
                    eval.setTimeout(10000);
                    File nodeSafety = new File(LOCATION + name + "/" + truffleSolution.getTree().getId() + ".node");
                    nodeSafety.getParentFile().mkdirs();
                    Files.writeString(nodeSafety.toPath(), NodeWrapper.serialize(truffleSolution.getTree()));
                    RuntimeProfile runtimeProfile = truffleSolution.getTestResults().iterator().next().getRuntimeProfile();
                    if (v > 1) {
                        System.out.println("BENCH FAIL " + problem.getDescription());
                        if (true) {
                            new InternalExecutor(benchProblem.getLanguage(), benchProblem.getCode(), benchProblem.getEntryPoint(), benchProblem.getFunction()).conductTest(truffleSolution.getNode(), benchProblem.getTests().iterator().next().getTest().getInputArguments());
                        }
                        // count timeout
                        if (benchSolution.getSolutionGenes().get(0).getGene().getTestResults().iterator().next().getException() != null && solution.getSolutionGenes().get(0).getGene().getTestResults().iterator().next().getException().contains("java.util.TimeoutException")) {
                            performances.put("timeout", performances.get("timeout") + 1);
                        } else {
                            // ignore other fails
                            i--;
                        }
                    } else {
                        System.out.println("GOT THE BENCH");
                        double diff = 0;
                        double origin = 0;
                        if (rootMedian) {
                            // compare bench with root
                            origin = baseMedians.get(problem.getDescription());
                            diff = runtimeProfile.getMedian() - origin;
                        } else {
                            // compare bench with AST that was just modified
                            Result median = manager.executeRead((transaction) -> transaction.run(
                                    "match (n)<-[:TREE]-()-[:TEST_RESULT]->()-[:RUNTIME]->(r) where id(n) = $ID and r.count = 100000 return r.median",
                                    Values.parameters("ID", identifier)));
                            origin = median.next().get(0).asDouble();
                            diff = runtimeProfile.getMedian() - origin;
                        }
                        if (diff <= 0) {
                            performances.put("efficient", performances.get("efficient") + 1);
                        } else {
                            performances.put("inefficient", performances.get("inefficient") + 1);
                        }
                        System.out.println("DIFF IS " + diff);
                        File performanceFile = new File(LOCATION + name + "/diffs.txt");
                        performanceFile.getParentFile().mkdirs();
                        values.add(new Pair<>(problem.getDescription() + ", " + identifier + ", " + origin, diff));
                        try {
                            Files.writeString(performanceFile.toPath(), values.stream().map(x -> x.getKey() + " - " + x.getValue()).collect(Collectors.joining(System.lineSeparator())));
                        } catch (IOException ioe) {
                            ioe.printStackTrace();
                        }

                    }
                } else if (solution.getSolutionGenes().get(0).getGene().getTestResults().iterator().next().getException() != null && solution.getSolutionGenes().get(0).getGene().getTestResults().iterator().next().getException().contains("java.util.TimeoutException")) {
                    // count timeout
                    File nodeSafety = new File(LOCATION + name + "/" + new File(LOCATION + name + "/").listFiles().length + "_timeout.node");
                    nodeSafety.getParentFile().mkdirs();
                    Files.writeString(nodeSafety.toPath(), NodeWrapper.serialize(solution.getSolutionGenes().get(0).getGene().getTree()));
                    performances.put("timeout", performances.get("timeout") + 1);
                } else {
                    solution.getSolutionGenes().get(0).getGene().getProblem().getDescription();
                    String ex = solution.getSolutionGenes().get(0).getGene().getTestResults().iterator().next().getException();
                    if (ex != null) {
                        // trim to first line
                        ex = ex.replace("'", "\\'");
                        if (ex.contains("\n")) {
                            ex = ex.substring(0, ex.indexOf("\n")).trim();
                        }

                        // special handling for the weirder messages
                        if (ex.contains("An illegal reflective access operation has occurred")) {
                            ex = "An illegal reflective access operation has occurred";
                        } else if (ex.contains("The worker crashed")) {
                            ex = "The worker crashed";
                        }
                        // reduce to text before : and @
                        else if (ex.contains(":")) {
                            ex = ex.substring(0, ex.indexOf(":"));
                        } else if (ex.contains("@")) {
                            ex = ex.substring(0, ex.indexOf("@"));
                        } else {
                            ex = ex;
                        }
                    }
                    System.out.println("NOT GOOD ENOUGH " + ex);
                    i--;
                }
            } catch (Exception ex) {
                System.out.println("RETRYING BECAUSE OF FAIL");
                i--;
            }
        }

        confidence = performances.get("efficient") / (double) targetCount;
        softConfidence = 1.0 - performances.get("timeout") / (double) targetCount;
        if (evalType.equals(EvalType.NEGATIVE)) {
            confidence = 1 - confidence;
            softConfidence = 1 - softConfidence;
        }

        System.out.println("Evaluated confidence pattern being responsible for performance on " + count + " trees");
        System.out.println("Confidence: " + confidence);
        System.out.println("Soft Confidence: " + softConfidence);
        print(name + "PERFORMANCE_CONFIDENCE", patterns, antiPatterns, performances, confidence, softConfidence);
        System.out.println("AVERAGE CHANGE " + values.stream().mapToDouble(Pair::getValue).average().orElse(0.0));
        File performanceFile = new File(LOCATION + name + "/diffs.txt");
        performanceFile.getParentFile().mkdirs();
        try {
            Files.writeString(performanceFile.toPath(), values.stream().map(x -> x.getKey() + " - " + x.getValue()).collect(Collectors.joining(System.lineSeparator())));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // TODO #74 this is duplicate with the Truffle language test file optimizer
    public static Set<TruffleOptimizationTest> getBenchmarkCases(String group) {
        Set<TruffleOptimizationTest> cases = new HashSet<>();

        List<String> in = readAllLines(Paths.get(PATH + group + "/BENCHMARK.input"));
        // NOTE - The benchark results are actually irrelevant. We ONLY record them so graal does not optimize the results away
        List<String> out = readAllLines(Paths.get(PATH + group + "/BENCHMARK.output"));

        Iterator<String> inIt = in.iterator();
        Iterator<String> outIt = out.iterator();

        while (inIt.hasNext() && outIt.hasNext()) {
            List<TruffleTestValue> inList = new ArrayList<TruffleTestValue>();
            inList.addAll(cast(inIt.next()));
            cases.add(new TruffleOptimizationTest(inList, cast(outIt.next()).get(0)));
        }

        return cases;
    }

    private static List<TruffleTestValue> cast(String s) {
        List<TruffleTestValue> inputs = new ArrayList<>();
        String[] split = s.split(";");
        for (String rawVal : split) {
            Pair<String, Object> val = ValueDefinitions.stringToValueTyped(rawVal);
            inputs.add(new TruffleTestValue(val.getValue(), val.getKey()));
        }
        return inputs;
    }

    private static List<String> readAllLines(Path file) {
        // fix line feeds for non unix os
        List<String> strings = null;
        try {
            strings = Files.readAllLines(file, Charset.defaultCharset());
        } catch (IOException e) {
            e.printStackTrace();
        }
        return strings;
    }

    private static boolean validateNN(TruffleOptimizationTestResult next) {
        if (next.getOutput().getValue() == null) {
            return false;
        }
        if (next.getOutput().getValue() instanceof float[]) {
            float[] value = (float[]) next.getOutput().getValue();
            if (value.length == 4) {
                // If you just wanna test if the patterns work -> value[0] < 0.5 && value[1] > 0.5 && value[2] > 0.5 && value[3] < 0.5
                if (value[0] < 0.01 && value[1] > 0.99 && value[2] > 0.99 && value[3] < 0.01) {
                    return true;
                }
            }
        }
        return false;
    }


    private static void print(String name, List<NodeWrapper> patterns, List<NodeWrapper> antiPatterns, HashMap<String, Integer> performances, double confidence, double softConfidence) {
        FileWriter writer = null;
        try {
            writer = new FileWriter(LOCATION + name + ".html");
        } catch (IOException e) {
            e.printStackTrace();
        }
        printer.setWriter(writer);
        printer.printConfidence(name, patterns, antiPatterns, performances, confidence, softConfidence);
    }

    private static TruffleOptimizationProblem repair(TruffleOptimizationProblem problem, Node nodeToOptimize) {
        // force load the tests
        TruffleOptimizationProblem loadedProblem = truffleOptimizationProblemRepository.findById(problem.getId());
        // force load the test values
        loadedProblem.getTests().forEach(x -> {
            x.setTest(truffleTestCaseRepository.findById(x.getTest().getId()));
        });

        // Set node
        loadedProblem.setOption("node", new Descriptor(nodeToOptimize));

        // set config
        loadedProblem.setOption("configuration", new Descriptor(new CreationConfiguration(ExtendedNodeUtil.maxDepth(nodeToOptimize) + 1, ExtendedNodeUtil.maxWidth(nodeToOptimize) + 1, Double.MAX_VALUE)));

        // set search space
        loadedProblem.setOption("searchSpace", new Descriptor(getTruffleLanguageSearchSpace()));

        return loadedProblem;
    }

    protected static Solution<TruffleOptimizationSolution, TruffleOptimizationProblem> createSolution(Node node, TruffleOptimizationProblem problem) {
        Solution<TruffleOptimizationSolution, TruffleOptimizationProblem> solution = new Solution<>();
        TruffleOptimizationSolution solutionGene = new TruffleOptimizationSolution(node, problem, null);
        SolutionGene<TruffleOptimizationSolution, TruffleOptimizationProblem> sg = new SolutionGene<>(solutionGene);
        sg.addProblemGene(new ProblemGene<>(problem));
        solution.addGene(sg);
        return solution;
    }

    public static TruffleEvaluatorImpl createTruffleEvaluator() {
        TruffleEvaluatorImpl evaluator = new TruffleEvaluatorImpl();
        evaluator.setTimeout(10000);
        // WE EXPLICITLY DO NOT WANT TO CONTAMINATE THE DB HERE -> evaluator.setAnalyticsService(getAnalytics());
        Map<CachetEvaluator<TruffleOptimizationSolution, TruffleOptimizationProblem>, Double> cachetMap = new HashMap<>();
        cachetMap.put(new AccuracyCachetEvaluator(), 1.0);
        evaluator.setCachetEvaluators(cachetMap);
        return evaluator;
    }

    public static ConfigurableMutator<TruffleOptimizationSolution, TruffleOptimizationProblem> createFaultFixingMutator() {
        TruffleFaultFixingTreeMutator truffleTreeMutator = new TruffleFaultFixingTreeMutator();
        DepthWidthRestrictedTruffleTreeSelector selector = new DepthWidthRestrictedTruffleTreeSelector();
        selector.setMaxDepth(3);
        selector.setMaxWidth(3);
        truffleTreeMutator.setSelector(selector);
        truffleTreeMutator.setSubtreeStrategy(masterStrategy);
        truffleTreeMutator.setFullTreeStrategy(entryPointStrategy);
        return truffleTreeMutator;
    }

    public static ConfigurableMutator<TruffleOptimizationSolution, TruffleOptimizationProblem> createDoubleReplacingMutator() {
        TruffleDoubleFixingMutator doubleReplacingMutator = new TruffleDoubleFixingMutator();
        DepthWidthRestrictedTruffleTreeSelector selector = new DepthWidthRestrictedTruffleTreeSelector();
        selector.setMaxDepth(3);
        selector.setMaxWidth(3);
        doubleReplacingMutator.setSelector(selector);
        doubleReplacingMutator.setSubtreeStrategy(masterStrategy);
        doubleReplacingMutator.setFullTreeStrategy(entryPointStrategy);
        return doubleReplacingMutator;
    }

    private static ConfigurableMutator<TruffleOptimizationSolution, TruffleOptimizationProblem> createDivisionReplacingMutator() {
        TruffleDivisionFixingMutator divisionFixingMutator = new TruffleDivisionFixingMutator();
        DepthWidthRestrictedTruffleTreeSelector selector = new DepthWidthRestrictedTruffleTreeSelector();
        selector.setMaxDepth(3);
        selector.setMaxWidth(3);
        divisionFixingMutator.setSelector(selector);
        divisionFixingMutator.setSubtreeStrategy(masterStrategy);
        divisionFixingMutator.setFullTreeStrategy(entryPointStrategy);
        return divisionFixingMutator;
    }


    // TODO # 63 all of the below is copy paste from the MINIC TESTFILE OPTIMIZER
    protected static TruffleLanguageSearchSpace getTruffleLanguageSearchSpace() {
        List<Class> excludes = new ArrayList<>();
        excludes.add(ReadNodeFactory.ReadNodeGen.class);
        excludes.add(ReadNode.class);
        TruffleLanguageInformation information = TruffleLanguageInformation.getLanguageInformation(MinicLanguage.ID);
        return new TruffleLanguageSearchSpace(information, excludes);
    }

    protected static List<TruffleHierarchicalStrategy> getStrategies() {
        MinicContext ctx = MinicLanguage.INSTANCE.getContextReference().get();
        List<TruffleHierarchicalStrategy> strategies = new ArrayList<>();
        strategies.add(new MinicFunctionLiteralStrategy(ctx));
        Set<Class> invokeClasses = new HashSet<>();
        invokeClasses.add(MinicInvokeNodeFactory.MinicInvokeIntNodeGen.class);
        invokeClasses.add(MinicInvokeNodeFactory.MinicInvokeFloatNodeGen.class);
        invokeClasses.add(MinicInvokeNodeFactory.MinicInvokeVoidNodeGen.class);
        invokeClasses.add(MinicInvokeNodeFactory.MinicInvokeArrayNodeGen.class);
        invokeClasses.add(MinicInvokeNodeFactory.MinicInvokeCharNodeGen.class);
        invokeClasses.add(MinicInvokeNodeFactory.MinicInvokeStringNodeGen.class);
        invokeClasses.add(MinicInvokeNodeFactory.MinicInvokeDoubleNodeGen.class);

        // create dedicated invoke strategies for each method
        ctx.getFunctionRegistry().getFunctions().stream().filter(x -> !x.getName().equals("main") && !x.getName().equals("read") && !x.getName().endsWith("_benchmark") && !x.getName().endsWith("_entry")).forEach(x -> {
            MinicInvokeStrategy invoke = new MinicInvokeStrategy(x);
            strategies.add(invoke);
            invokeClasses.remove(invoke.getInvokeClazz());
        });
        // prevent other invokes if they have no valid impl:
        invokeClasses.forEach(x -> {
            strategies.add(new MinicCantInvokeStrategy(ctx, x));
        });

        return strategies;
    }

    protected static List<TruffleHierarchicalStrategy> getPostitiveStrategies(String exception) {
        MinicContext ctx = MinicLanguage.INSTANCE.getContextReference().get();
        List<TruffleHierarchicalStrategy> strategies = new ArrayList<>();
        if (exception.equals("java.lang.RuntimeException")) {
            strategies.add(new MinicAntiFunctionLiteralStrategy(ctx));
        } else {
            strategies.add(new MinicFunctionLiteralStrategy(ctx));
        }
        Set<Class> invokeClasses = new HashSet<>();
        return strategies;
    }

    protected static Map<String, TruffleVerifyingStrategy> getPositiveTerminalStrategies(TruffleOptimizationProblem problem, String exception) {
        Map<String, TruffleVerifyingStrategy> strategies = new HashMap<>();
        if (MinicLanguage.INSTANCE.getContextReference().get().getGlobalStorage() != null) {
            strategies.put("com.oracle.truffle.api.frame.MaterializedFrame", new StaticObjectStrategy<MaterializedFrame>(MinicLanguage.INSTANCE.getContextReference().get().getGlobalStorage()));
        }
        strategies.put("at.fh.hagenberg.aist.gce.minic.language.MinicContext", new StaticObjectStrategy<MinicContext>(MinicLanguage.INSTANCE.getContextReference().get()));
        strategies.putAll(DefaultStrategyUtil.defaultStrategies());
        if (exception.equals("java.lang.IllegalArgumentException")) {
            strategies.put("com.oracle.truffle.api.frame.FrameSlot", new IllegalArgumentExceptionFrameSlotStrategy(problem.getNode().getRootNode().getFrameDescriptor()));
        } else if (exception.equals("java.lang.IllegalStateException") || exception.equals("java.lang.NullPointerException")) {
            strategies.put("com.oracle.truffle.api.frame.FrameSlot", new IllegalStateExceptionFrameSlotStrategy(problem.getNode().getRootNode().getFrameDescriptor()));
        } else if (exception.equals("FAILED TO SERIALIZE MESSAGE")) {
            // Allowing ONLY 0, since the patterns don't have a "I require this literal" option yet
            strategies.put("int", new KnownValueStrategy<>(new LinkedList<>(Arrays.asList(-1000))));
            strategies.put("com.oracle.truffle.api.frame.FrameSlot", new DefaultFrameSlotStrategy(problem.getNode().getRootNode().getFrameDescriptor()));
        } else if (exception.equals("java.lang.ArithmeticException")) {
            strategies.put("int", new KnownValueStrategy<>(new LinkedList<>(Arrays.asList(0))));
        } else if (exception.equals("java.lang.ArrayIndexOutOfBoundsException")) {
            strategies.put("int", new KnownValueStrategy<>(new LinkedList<>(Arrays.asList(-1000))));
            strategies.put("com.oracle.truffle.api.frame.FrameSlot", new DefaultFrameSlotStrategy(problem.getNode().getRootNode().getFrameDescriptor()));
        } else if (exception.equals("java.lang.ClassCastException")) {
            strategies.put("com.oracle.truffle.api.frame.FrameSlot", new ClassCastExceptionFrameSlotStrategy(problem.getNode().getRootNode().getFrameDescriptor()));
        } else {
            strategies.put("com.oracle.truffle.api.frame.FrameSlot", new NonCheckingFrameSlotStrategy(problem.getNode().getRootNode().getFrameDescriptor()));
        }
        return strategies;
    }

    protected static Map<String, TruffleVerifyingStrategy> getTerminalStrategies(TruffleOptimizationProblem problem) {
        Map<String, TruffleVerifyingStrategy> strategies = new HashMap<>();
        if (MinicLanguage.INSTANCE.getContextReference().get().getGlobalStorage() != null) {
            strategies.put("com.oracle.truffle.api.frame.MaterializedFrame", new StaticObjectStrategy<MaterializedFrame>(MinicLanguage.INSTANCE.getContextReference().get().getGlobalStorage()));
        }
        strategies.put("at.fh.hagenberg.aist.gce.minic.language.MinicContext", new StaticObjectStrategy<MinicContext>(MinicLanguage.INSTANCE.getContextReference().get()));
        strategies.putAll(DefaultStrategyUtil.defaultStrategies());
        strategies.put("com.oracle.truffle.api.frame.FrameSlot", new DefaultFrameSlotStrategy(problem.getNode().getRootNode().getFrameDescriptor()));
        return strategies;
    }

    private static class ProblemHelperClass {
        public TruffleOptimizationProblem problem;
        public ConfigurableMutator<TruffleOptimizationSolution, TruffleOptimizationProblem> mutator;

        public ProblemHelperClass(TruffleOptimizationProblem problem, ConfigurableMutator<TruffleOptimizationSolution, TruffleOptimizationProblem> mutator) {
            this.problem = problem;
            this.mutator = mutator;
        }
    }
}
