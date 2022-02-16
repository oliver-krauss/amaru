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
import at.fh.hagenberg.aist.gce.lang.GraalBackdoorSystem;
import at.fh.hagenberg.aist.gce.minic.language.MinicLanguage;
import at.fh.hagenberg.aist.gce.minic.nodes.impl.function.MinicFunctionLiteralNode;
import at.fh.hagenberg.aist.gce.minic.nodes.impl.function.MinicFunctionNode;
import at.fh.hagenberg.aist.gce.optimization.TruffleEvaluatorImpl;
import at.fh.hagenberg.aist.gce.optimization.TruffleOptimizationProblem;
import at.fh.hagenberg.aist.gce.optimization.TruffleOptimizationSolution;
import at.fh.hagenberg.aist.gce.optimization.TruffleOptimizationSolutionRepository;
import at.fh.hagenberg.aist.gce.optimization.cachet.PerformanceCachetEvaluator;
import at.fh.hagenberg.aist.gce.optimization.executor.MessageExecutor;
import at.fh.hagenberg.aist.gce.optimization.language.util.EngineConfig;
import at.fh.hagenberg.aist.gce.optimization.run.TruffleAlgorithmFactory;
import at.fh.hagenberg.aist.gce.optimization.runtime.RuntimeProfile;
import at.fh.hagenberg.aist.gce.optimization.runtime.SystemInformation;
import at.fh.hagenberg.aist.gce.optimization.test.*;
import at.fh.hagenberg.aist.gce.optimization.util.ExtendedNodeUtil;
import at.fh.hagenberg.aist.gce.optimization.util.JavaAssistUtil;
import at.fh.hagenberg.aist.gce.optimization.util.TruffleLanguageInformation;
import at.fh.hagenberg.aist.gce.pattern.TrufflePatternDetector;
import at.fh.hagenberg.aist.gce.pattern.constraint.CachetConstraint;
import at.fh.hagenberg.aist.gce.pattern.constraint.ProblemConstraint;
import at.fh.hagenberg.aist.gce.pattern.selection.TrufflePatternSearchSpaceDefinition;
import at.fh.hagenberg.aist.gce.science.statistics.data.template.NodeWrapperPrinter;
import at.fh.hagenberg.aist.gce.science.statistics.data.template.RuntimeProfilePrinter;
import at.fh.hagenberg.machinelearning.analytics.graph.SystemInformationRepository;
import at.fh.hagenberg.machinelearning.analytics.graph.nodes.NodeWrapper;
import at.fh.hagenberg.machinelearning.core.Problem;
import at.fh.hagenberg.machinelearning.core.ProblemGene;
import at.fh.hagenberg.machinelearning.core.Solution;
import at.fh.hagenberg.machinelearning.core.SolutionGene;
import at.fh.hagenberg.machinelearning.core.fitness.CachetEvaluator;
import at.fh.hagenberg.util.Pair;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.RootNode;
import org.neo4j.driver.Values;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import science.aist.neo4j.reflective.ReflectiveNeo4JNodeRepositoryImpl;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Performance Evaluation pipeline.
 * Exists to add performance values to already existing stuff in the database
 *
 * @author Oliver Krauss on 11.07.2020
 */
public class MinicSequentialVsParallelPerformanceEvaluation {

    // TODO #74 this should not be hardcoded
    protected String PATH = EngineConfig.ROOT_LOCATION + "/Amaru/gce/optimization-optimizer/src/test/resources/testCases/";
    private static final String REPORT_LOCATION = EngineConfig.ROOT_LOCATION + "/Amaru.LOGS.wiki/PerformanceProfiles/";

    private static RuntimeProfilePrinter printer;

    private static Map<String, Map<String, Pair<RuntimeProfile, long[]>>> runtimes = new HashMap<>();
    private static Map<String, Map<String, Pair<RuntimeProfile, long[]>>> optRuntimes = new HashMap<>();

    private TruffleOptimizationSolutionRepository solutionRepository;
    private ReflectiveNeo4JNodeRepositoryImpl<TruffleOptimizationTestResult> truffleOptimizationTestResultRepository;

    /**
     * Tracing graal can NOT happen via the message workers.
     * If you wanna trace
     */
    public static boolean TRACE_GRAAL = false;

    public void experiment() {
        solutionRepository = (TruffleOptimizationSolutionRepository) ApplicationContextProvider.getCtx().getBean("truffleOptimizationSolutionRepository");
        truffleOptimizationTestResultRepository = (ReflectiveNeo4JNodeRepositoryImpl<TruffleOptimizationTestResult>) ApplicationContextProvider.getCtx().getBean("truffleOptimizationTestResultRepository");
        new MinicMathBenchmarkSuite().benchmarkProblems().forEach(x -> execBenchmark(x, "math", 0.0));
        printResults("math");
//        new MinicSortingAlgorithmBenchmarkSuite().benchmarkProblems().forEach(x -> execBenchmark(x, "sort", 0.0));
//        printResults("sort");
//        new MinicNNBenchmarkSuite().benchmarkProblems().forEach(x -> execBenchmark(x, "nn", 8.2E-38));
//        printResults("nn");
    }

    private void printResults(String name) {
        runtimes.forEach((k, v) -> {
            FileWriter writer = null;
            try {
                writer = new FileWriter(REPORT_LOCATION + name + "/ALL_" + k + ".html");
            } catch (IOException e) {
                e.printStackTrace();
            }
            printer.setWriter(writer);
            printer.printProfileGroup(name + "_" + k, v);
        });
        runtimes.clear();

        optRuntimes.forEach((k, v) -> {
            FileWriter writer = null;
            try {
                writer = new FileWriter(REPORT_LOCATION + name + "/ALL_OPT_" + k + ".html");
            } catch (IOException e) {
                e.printStackTrace();
            }
            printer.setWriter(writer);
            printer.printProfileGroup(name + "_" + k, v);
        });
        optRuntimes.clear();
    }

    private TrufflePatternDetector detector = new TrufflePatternDetector();

    private void execBenchmark(Pair<TruffleOptimizationProblem, TruffleAlgorithmFactory> run, String groupName, double upperLimit) {
        File benchDir = new File(REPORT_LOCATION + groupName);
        if (!benchDir.exists()) {
            benchDir.mkdirs();
        }
        File nodeDir = new File(REPORT_LOCATION + groupName + "/NODE");
        if (!nodeDir.exists()) {
            nodeDir.mkdirs();
        }
        File reportDir = new File(REPORT_LOCATION + groupName + "/performance");
        if (!reportDir.exists()) {
            reportDir.mkdirs();
        }
        // override the test cases with the benchmark
        run.getKey().getTests().clear();
        run.getKey().getTests().addAll(getBenchmarkCases(groupName).stream().map(x -> new TruffleOptimizationTestComplexity(run.getKey(), x)).collect(Collectors.toList()));


        // Get the Evaluator
        TruffleEvaluatorImpl truffleEvaluator = run.getValue().createTruffleEvaluator();
        // set 30 min for profiling (probably too little)
        truffleEvaluator.setTimeout(30 * 60 * 1000);
        truffleEvaluator.setSafeVM(true);
        HashMap<CachetEvaluator<TruffleOptimizationSolution, TruffleOptimizationProblem>, Double> cachets = new HashMap<>();
        cachets.put(new PerformanceCachetEvaluator(), 1.0);
        truffleEvaluator.setCachetEvaluators(cachets);
        // ensure we got the context so run can switch to benchmark
        truffleEvaluator.verifyExecutor(run.getKey(), false);

        // Do performance evaluation for the given problem
        String function = run.getKey().getFunction();
        System.out.println("BENCHMARKING " + function + " " + java.time.LocalDateTime.now());
        run.getKey().setBenchmark();
        // remove the solution so it won't be logged with wrong data
        Solution<TruffleOptimizationSolution, TruffleOptimizationProblem> originalSolution = run.getKey().getOriginalSolution();
        run.getKey().setOriginalSolution(null);
        ArrayList<ProblemGene<TruffleOptimizationProblem>> genes = new ArrayList<>();
        genes.add(new ProblemGene<>(run.getKey()));
        // add the solution back in again
        run.getKey().setOriginalSolution(originalSolution);


        // calc runtime for every feasible solution
        TrufflePatternSearchSpaceDefinition successfulSpace = new TrufflePatternSearchSpaceDefinition();
        successfulSpace.setSolutionSpace(true);
        List<CachetConstraint> cachetConstraints = new ArrayList<>();
        // search all solutions that have a perfect accuracy + are of the given problem
        cachetConstraints.add(new CachetConstraint("Accuracy-0.4", upperLimit, 0.0));
        successfulSpace.setCachets(cachetConstraints);
        List<ProblemConstraint> problemConstraints = new ArrayList<>();
        problemConstraints.add(new ProblemConstraint(function));
        successfulSpace.setProblems(problemConstraints);

        // evaluate every single thingy
        List<NodeWrapper> searchSpace = detector.findSearchSpace(successfulSpace).asAsts();
        List<ProblemGene<TruffleOptimizationProblem>> problems = run.getKey().getOriginalSolution().getSolutionGenes().get(0).getProblemGenes();
        MessageExecutor executor = (MessageExecutor) truffleEvaluator.getExecutor();
        truffleEvaluator.verifyExecutor(run.getKey(), true);
        truffleEvaluator.setSafeVM(true);
        System.out.println("Evaluating " + searchSpace.size() + " solutions");

        ArrayList<NodeWrapper> selected = new ArrayList<NodeWrapper>();
        for (int i = 0; i < 10; i++) {
            selected.add(searchSpace.remove(new Random().nextInt(searchSpace.size())));
        }

        System.out.println("START SEQUENTIAL EVAL " + new Date());

        selected.forEach(x -> {
            for (int i = 0; i < 10; i++) {
                System.out.println("Evaluating " + x.getId() + " loop " + i);
                Node xNode = NodeWrapper.unwrap(x, executor.getRoot().getFrameDescriptor(), executor.getGlobalScope(), run.getKey().getLanguage());
                Solution<TruffleOptimizationSolution, TruffleOptimizationProblem> solution = new Solution();
                solution.addGene(new SolutionGene<>(new TruffleOptimizationSolution(xNode, run.getKey(), null), problems));
                solution.getSolutionGenes().get(0).getGene().getTree().setId(x.getId() * 100 + i);
                double v = truffleEvaluator.evaluateQuality(solution);
                System.out.println("Evaluated " + x.getId() + " -> " + v);
                System.out.println(x.humanReadableTree());
                TruffleOptimizationSolution solGene = solution.getSolutionGenes().get(0).getGene();
                printPerformance(solGene, executor, groupName, function, run.getKey());
            }
        });

        System.out.println("FINISHED SEQUENTIAL / STARTING PARALLEL EVAL " + new Date());

        ForkJoinPool customThreadPool = new ForkJoinPool(10);
        customThreadPool.submit(
                () -> selected.parallelStream().forEach(x -> {
                    for (int i = 0; i < 10; i++) {
                        System.out.println("Evaluating " + x.getId() + " loop " + i);
                        Node xNode = NodeWrapper.unwrap(x, executor.getRoot().getFrameDescriptor(), executor.getGlobalScope(), run.getKey().getLanguage());
                        Solution<TruffleOptimizationSolution, TruffleOptimizationProblem> solution = new Solution();
                        solution.addGene(new SolutionGene<>(new TruffleOptimizationSolution(xNode, run.getKey(), null), problems));
                        solution.getSolutionGenes().get(0).getGene().getTree().setId(-1 * x.getId() * 100 - i);
                        double v = truffleEvaluator.evaluateQuality(solution);
                        System.out.println("Evaluated " + x.getId() + " -> " + v);
                        System.out.println(x.humanReadableTree());
                        TruffleOptimizationSolution solGene = solution.getSolutionGenes().get(0).getGene();
                        printPerformance(solGene, executor, groupName, function, run.getKey());
                    }
                })
        );
        try {
            customThreadPool.awaitTermination(1, TimeUnit.HOURS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        customThreadPool.shutdown();

        System.out.println("FINISHED PARALLEL EVAL " + new Date());

        System.out.println("FINISHED BENCH " + java.time.LocalDateTime.now());
    }

    private TruffleOptimizationSolution repair(Solution<TruffleOptimizationSolution, TruffleOptimizationProblem> solution, TruffleOptimizationProblem key, TruffleOptimizationSolution gene) {
        // find the solution
        TruffleOptimizationSolution loaded = solutionRepository.queryTyped("match (n:TruffleOptimizationSolution)<-[:RWGENE]-()<-[:GENE]-(s) where id(s) = $parentSolutionId return n limit 1", Values.parameters("parentSolutionId", solution.getId()));
        // find the test results
        loaded = solutionRepository.findById(loaded.getId());
        // replace the tests with those from the problem
        Set<TruffleOptimizationTestResult> testResults = new HashSet<>(loaded.getTestResults());
        loaded.getTestResults().clear();
        TruffleOptimizationSolution finalLoaded = loaded;
        testResults.forEach(x -> {
            TruffleOptimizationTestResult result = truffleOptimizationTestResultRepository.findById(x.getId());
            result.setTest(key.getTests().stream().map(TruffleOptimizationTestComplexity::getTest).filter(t -> t.getId().equals(result.getTest().getId())).findFirst().get());
            finalLoaded.getTestResults().add(result);
        });
        return loaded;
    }

    // TODO #74 this is duplicate with the Truffle language test file optimizer
    public Set<TruffleOptimizationTest> getBenchmarkCases(String group) {
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

    private List<TruffleTestValue> cast(String s) {
        List<TruffleTestValue> inputs = new ArrayList<>();
        String[] split = s.split(";");
        for (String rawVal : split) {
            Pair<String, Object> val = ValueDefinitions.stringToValueTyped(rawVal);
            inputs.add(new TruffleTestValue(val.getValue(), val.getKey()));
        }
        return inputs;
    }

    private List<String> readAllLines(Path file) {
        // fix line feeds for non unix os
        List<String> strings = null;
        try {
            strings = Files.readAllLines(file, Charset.defaultCharset());
        } catch (IOException e) {
            e.printStackTrace();
        }
        return strings;
    }

    private void printPerformance(TruffleOptimizationSolution solution, MessageExecutor executor, String groupName, String function, TruffleOptimizationProblem problem) {

        Map<String, Pair<RuntimeProfile, long[]>> testRuntimes = new HashMap<>();
        Map<String, Pair<RuntimeProfile, long[]>> testOptRuntimes = new HashMap<>();
        Map<String, String> testInfo = new HashMap<>();

        // NOTE: the following code is only for getting the nice performance values.
        for (TruffleOptimizationTestResult testResult : solution.getTestResults()) {
            String testId = String.valueOf(testResult.getTest().getId());
            File runtimeReportDir = new File(EngineConfig.ROOT_LOCATION + "/Amaru.LOGS/PerformanceProfiles/runtimeProfiles/" + solution.getTree().getId() + "_" + testId + ".rtp");
            try {
                String content = Files.readString(runtimeReportDir.toPath(), StandardCharsets.US_ASCII);
                long[] performance = Arrays.stream(content.split(",")).mapToLong(Long::valueOf).toArray();
                testInfo.put(testId, humanReadableTestResult(testResult));

                // evaluate performance
                long[] copy = new long[performance.length];
                System.arraycopy(performance, 0, copy, 0, performance.length);
                RuntimeProfile profile = new RuntimeProfile(copy);
                long[] halfPerformance = new long[100000];
                long[] optCopy = new long[100000];
                System.arraycopy(performance, 100000, halfPerformance, 0, 100000);
                System.arraycopy(performance, 100000, optCopy, 0, 100000);
                RuntimeProfile optimizedProfile = new RuntimeProfile(optCopy);
                System.out.println("Full Profile: ");
                profile.report();
                System.out.println("after 100.000 runs only: ");
                optimizedProfile.report();
                testRuntimes.put(testId, new Pair<>(profile, performance));
                testOptRuntimes.put(testId, new Pair<>(optimizedProfile, halfPerformance));

                // only doing this for the OG Solution
                if (solution.getTree().getId() != null && solution.getTree().getId().equals(problem.getWrappedNode().getId())) {
                    if (!runtimes.containsKey(testId)) {
                        runtimes.put(testId, new HashMap<>());
                        optRuntimes.put(testId, new HashMap<>());
                    }
                    runtimes.get(testId).put(function, new Pair<>(profile, performance));
                    optRuntimes.get(testId).put(function, new Pair<>(optimizedProfile, halfPerformance));
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        // write reports
        FileWriter writer = null;
        try {
            writer = new FileWriter(REPORT_LOCATION + groupName + "/performance/" + function + "_" + solution.getTree().getId() + ".html");
        } catch (IOException e) {
            e.printStackTrace();
        }
        printer.setWriter(writer);
        printer.printProfile(function, testInfo, testRuntimes);
        // write opt reports
        try {
            writer = new FileWriter(REPORT_LOCATION + groupName + "/performance/" + function + "_" + solution.getTree().getId() + "_opt.html");
        } catch (IOException e) {
            e.printStackTrace();
        }
        printer.setWriter(writer);
        printer.printProfile(function, testInfo, testOptRuntimes);


        NodeWrapperPrinter nodeWrapperPrinter = new NodeWrapperPrinter(null);
        nodeWrapperPrinter.setDebug(false);
        nodeWrapperPrinter.setFormat("html");
        try {
            nodeWrapperPrinter.setWriter(new FileWriter(REPORT_LOCATION + groupName + "/NODE/" + function + ".html"));
        } catch (IOException e) {
            e.printStackTrace();
        }

        // print ALL (used) ASTS
        Set<Node> invokes = new HashSet<>();
        invokes.add(executor.getOrigin().getParent().getParent());
        findInvocations(invokes.iterator().next(), invokes);
        nodeWrapperPrinter.printNodeWrapperGroup(invokes.stream().map(NodeWrapper::wrap).collect(Collectors.toList()));
    }

    private String humanReadableTestResult(TruffleOptimizationTestResult tr) {
        return tr.getTest().getInput().stream().map(ti -> ValueDefinitions.valueToString(ti.getValue())).collect(Collectors.joining(", ")) +
                " -> " + ValueDefinitions.valueToString(tr.getTest().getOutput().getValue()) + " RESULT: " + ValueDefinitions.valueToString(tr.getOutput().getValue());
    }

    /**
     * Find all methods called by the given AST and add them to the invoke list (recursive!)
     *
     * @param ast     to be invoked
     * @param invokes to add to
     */
    private void findInvocations(Node ast, Set<Node> invokes) {
        List<Iterable<Node>> minicInvoke = ExtendedNodeUtil.flatten(ast).filter(x -> x.getClass().getName().contains("MinicInvoke")).map(Node::getChildren).collect(Collectors.toList());
        minicInvoke.forEach(x -> {
            x.iterator().forEachRemaining(y -> {
                if (y.getClass().getName().contains("MinicFunctionLiteral")) {
                    MinicFunctionNode cachedFunction = (MinicFunctionNode) JavaAssistUtil.safeFieldAccess("cachedFunction", y);
                    RootNode rootNode;
                    if (cachedFunction == null) {
                        rootNode = MinicLanguage.getCurrentContext().getFunctionRegistry().lookup(((MinicFunctionLiteralNode) y).getName()).getCallTarget().getRootNode();
                    } else {
                        rootNode = cachedFunction.getCallTarget().getRootNode();
                    }
                    if (invokes.add(rootNode)) {
                        findInvocations(rootNode, invokes);
                    }
                }
            });
        });
    }

    /**
     * Entry point using the GraalBackdoorSystem to run the experiments
     *
     * @param args
     */
    public static void main(String[] args) {
        File runtimeReportDir = new File(REPORT_LOCATION + "runtimeProfiles/");
        if (!runtimeReportDir.exists()) {
            runtimeReportDir.mkdirs();
        }

        // Start Broker and Command Plane
//        System.out.println("AUTO-STARTING BROKER AND COMMAND PLANE. IF YOU DON'T WANT THIS, DISABLE THE CODE");
//        Thread broker = new Thread(() -> MessageBroker.main(new String[]{"frontend=15557", "backend=15558", "command=15559"}));
//        Thread commander = new Thread(() -> MessageCommandModule.main(new String[]{"backend=localhost:15558", "broker=localhost:15559", "workerLimit=5"}));
//        broker.start();
//        commander.start();

        // Caller for Experiments using the Graal Backdoor Language
        new GraalBackdoorSystem().breakIn(MinicSequentialVsParallelPerformanceEvaluation.class, "enter", null);
    }

    /**
     * This function Sets up the optimization environment and starts the experiment function.
     */
    public static void enter() {
        MinicTestfileOptimizer.PERFORMANCE_PIPE = true;

        printer = new RuntimeProfilePrinter();
        printer.setDebug(false);
        printer.setFormat("html");

        // pre-load system information
        ClassPathXmlApplicationContext dbContext = ApplicationContextProvider.getCtx();
        SystemInformationRepository systemInformationRepository = dbContext.getBean(SystemInformationRepository.class);
        systemInformationRepository.save(SystemInformation.getCurrentSystem());

        // Pre load MiniC (no language learning needed for performance eval)
        // TODO #196 load this from DB
        TruffleLanguageInformation tli = TruffleLanguageInformation.getLanguageInformation(MinicLanguage.ID);

        // Conduct Experiments
        new MinicSequentialVsParallelPerformanceEvaluation().experiment();

        // shutdown
        ApplicationContextProvider.close();
        MinicTestfileOptimizer.PERFORMANCE_PIPE = false;
    }

}
