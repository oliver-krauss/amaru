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
import at.fh.hagenberg.aist.gce.minic.nodes.base.MinicExpressionNode;
import at.fh.hagenberg.aist.gce.minic.nodes.base.MinicRootNode;
import at.fh.hagenberg.aist.gce.minic.nodes.impl.cast.MinicToFloatNode;
import at.fh.hagenberg.aist.gce.minic.nodes.impl.function.MinicFunctionLiteralNode;
import at.fh.hagenberg.aist.gce.minic.nodes.impl.function.MinicFunctionNode;
import at.fh.hagenberg.aist.gce.optimization.ProfileKeeper;
import at.fh.hagenberg.aist.gce.optimization.TruffleEvaluatorImpl;
import at.fh.hagenberg.aist.gce.optimization.TruffleOptimizationProblem;
import at.fh.hagenberg.aist.gce.optimization.TruffleOptimizationSolution;
import at.fh.hagenberg.aist.gce.optimization.cachet.AccuracyCachetEvaluator;
import at.fh.hagenberg.aist.gce.optimization.executor.ExecutionResult;
import at.fh.hagenberg.aist.gce.optimization.executor.Executor;
import at.fh.hagenberg.aist.gce.optimization.executor.InternalExecutor;
import at.fh.hagenberg.aist.gce.optimization.executor.MessageExecutor;
import at.fh.hagenberg.aist.gce.optimization.language.util.EngineConfig;
import at.fh.hagenberg.aist.gce.optimization.run.TruffleAlgorithmFactory;
import at.fh.hagenberg.aist.gce.optimization.runtime.RuntimeProfile;
import at.fh.hagenberg.aist.gce.optimization.runtime.SystemInformation;
import at.fh.hagenberg.aist.gce.optimization.test.TruffleOptimizationTestComplexity;
import at.fh.hagenberg.aist.gce.optimization.test.ValueDefinitions;
import at.fh.hagenberg.aist.gce.optimization.util.ExtendedNodeUtil;
import at.fh.hagenberg.aist.gce.optimization.util.JavaAssistUtil;
import at.fh.hagenberg.aist.gce.optimization.util.MinicLanguageLearner;
import at.fh.hagenberg.aist.gce.optimization.util.TruffleLanguageInformation;
import at.fh.hagenberg.aist.gce.science.statistics.data.template.ExperimentPrinter;
import at.fh.hagenberg.aist.gce.science.statistics.data.template.IndividualInformation;
import at.fh.hagenberg.aist.gce.science.statistics.data.template.NodeWrapperPrinter;
import at.fh.hagenberg.aist.gce.science.statistics.data.template.RuntimeProfilePrinter;
import at.fh.hagenberg.machinelearning.analytics.graph.NodeRepository;
import at.fh.hagenberg.machinelearning.analytics.graph.SystemInformationRepository;
import at.fh.hagenberg.machinelearning.analytics.graph.nodes.AnalyticsNode;
import at.fh.hagenberg.machinelearning.analytics.graph.nodes.NodeWrapper;
import at.fh.hagenberg.machinelearning.analytics.graph.nodes.StepNode;
import at.fh.hagenberg.machinelearning.core.Problem;
import at.fh.hagenberg.machinelearning.core.ProblemGene;
import at.fh.hagenberg.machinelearning.core.experiment.Experiment;
import at.fh.hagenberg.machinelearning.core.experiment.ExperimentResult;
import at.fh.hagenberg.machinelearning.core.experiment.FixedChoice;
import at.fh.hagenberg.util.Pair;
import com.oracle.truffle.api.RootCallTarget;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.RootNode;
import org.neo4j.driver.Values;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import science.aist.neo4j.reflective.ReflectiveNeo4JNodeRepositoryImpl;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Core class for conducting experiments. Look at how it works, or just extend it
 *
 * @author Oliver Krauss on 11.07.2020
 */
public class MinicExperiments {


    private static final String REPORT_LOCATION = EngineConfig.ROOT_LOCATION + "/Amaru.LOGS/PerformanceProfiles/";

    private static RuntimeProfilePrinter printer;

    private static ExperimentPrinter experimentPrinter;

    private static Map<String, Pair<RuntimeProfile, long[]>> runtimes = new HashMap<>();
    private static Map<String, Pair<RuntimeProfile, long[]>> optRuntimes = new HashMap<>();

    /**
     * Tracing graal can NOT happen via the message workers.
     * If you wanna trace
     */
    public static boolean TRACE_GRAAL = false;

    public void experiment() {
//        new MinicSqrtBenchmarkSuite().benchmarkProblems().forEach(x -> execBenchmark(x, "sort_check"));


        new MinicMathBenchmarkSuite().benchmarkProblems().forEach(x -> execBenchmark(x, "math"));
        new MinicSortingAlgorithmBenchmarkSuite().benchmarkProblems().forEach(x -> execBenchmark(x, "sort"));
        new MinicNNBenchmarkSuite().benchmarkProblems().forEach(x -> execBenchmark(x, "neuralNet"));

//        new MinicFloatSortingAlgorithmBenchmarkSuite().benchmarkProblems().forEach(x -> execBenchmark(x, "sortFloat"));
//        new MinicMicroBenchmarkSuite().benchmarkProblems().forEach(x -> execBenchmark(x, "micro"));
    }

    private void execBenchmark(Pair<TruffleOptimizationProblem, TruffleAlgorithmFactory> run, String groupName) {
        File benchDir = new File(REPORT_LOCATION + groupName);
        if (!benchDir.exists()) {
            benchDir.mkdirs();
        }

        // load problem to experiment on
        LinkedList<ProblemGene<TruffleOptimizationProblem>> problemGenes = new LinkedList<>();
        problemGenes.add(new ProblemGene<>(run.getKey()));

        System.out.println("Beginning Experiment for " + run.getKey().getFunction());

        // validate the IN / OUT that was provided and the AST itself (will NOT work if we try to bugfix)
        TruffleOptimizationSolution gene = run.getKey().getOriginalSolution().getSolutionGenes().get(0).getGene();
        if (gene.getTestResults().isEmpty()) {
            run.getValue().getAnalytics().init();
            NodeWrapper.hash(gene.getTree());
            TruffleEvaluatorImpl truffleEvaluator = TruffleAlgorithmFactory.createTruffleEvaluator();
            InternalExecutor validationExec = new InternalExecutor("c", run.getKey().getCode(), run.getKey().getEntryPoint(), run.getKey().getFunction());
            truffleEvaluator.setExecutor(validationExec);
            truffleEvaluator.setTimeout(3000);
            truffleEvaluator.evaluateQuality(run.getKey().getOriginalSolution());
        }
        final boolean[] successful = {true};
        System.out.println("Test Report: ");
        gene.getTestResults().forEach(tr -> {
            System.out.println("  " + tr.getTest().getInput().stream().map(ti -> ValueDefinitions.valueToString(ti.getValue())).collect(Collectors.joining(", ")) +
                    " -> " + ValueDefinitions.valueToString(tr.getOutput().getValue()) + " diff " + tr.getOutput().compare(tr.getTest().getOutput()));
            if (tr.hasFailed()) {
                successful[0] = false;
            }
        });
        if (!successful[0]) {
            System.out.println("The original solution does not match all tests -> this is a bugfix experiment.");
        }

        // run the experiment
        Experiment<TruffleOptimizationSolution, TruffleOptimizationProblem> experiment = new Experiment<>();
        experiment.setRepeats(1);
        experiment.addAlgorithm(Experiment.createConfigurationChoices("ga", run.getValue().createAlgorithm()));
        experiment.addProblem(new FixedChoice<>(run.getKey().getDescription(), new Problem<>(problemGenes)));
        ExperimentResult<TruffleOptimizationSolution, TruffleOptimizationProblem> experimentResult = experiment.conductExperiment();
        System.out.println("CONDUCTED EXPERIMENTS");
        ProfileKeeper.profiler.report();
        ProfileKeeper.profiler.reset();

        // print the results
        System.out.println("Loading experiment info from DB");
        AnalyticsNode experimentNode = ((ReflectiveNeo4JNodeRepositoryImpl<AnalyticsNode>) ApplicationContextProvider.getCtx().getBean("analyticsRepository")).queryTyped("match (n:AnalyticsNode) return n order by n.title desc limit 1", null);
        // reload to also get the steps
        experimentNode = ((ReflectiveNeo4JNodeRepositoryImpl<AnalyticsNode>) ApplicationContextProvider.getCtx().getBean("analyticsRepository")).findById(experimentNode.getId());
        NodeRepository nodeRepository = ((NodeRepository) ApplicationContextProvider.getCtx().getBean("nodeRepository"));
        TruffleOptimizationSolution original = run.getKey().getOriginalSolution().getSolutionGenes().get(0).getGene();
        original.setTree(run.getKey().getWrappedNode());
        double originalQuality = nodeRepository.queryAll("match (s)-[:GENE]->()-[:RWGENE]->()-[:TREE]->(t) where id(t) = $ID return s.quality",
                Values.parameters("ID", original.getTree().getId()), Double.class).collect(Collectors.toList()).get(0);
        TruffleOptimizationSolution bestFound = experimentResult.getResults().values().iterator().next().get(0).getSolutionGenes().get(0).getGene();


        FileWriter writer = null;
        try {
            writer = new FileWriter(REPORT_LOCATION + groupName + "/" + run.getKey().getFunction() + "_EXPERIMENT.html");
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("COllecting experiment individuals");
        experimentPrinter.setWriter(writer);
        List<Map<String, Object>> stepParams = new ArrayList<>();
        List<List<IndividualInformation>> individuals = new ArrayList<>();
        experimentNode.getSteps().stream().sorted(Comparator.comparing(StepNode::getId)).forEach(x -> {
            stepParams.add(x.getParameters());
            List<Long> astIds = nodeRepository.queryAll("match (n)-[:POPULATION]->()-[:GENE]->()-[:RWGENE]->()-[:TREE]->(t) where id(n) = $ID return id(t)", Values.parameters("ID", x.getId()), Long.class).collect(Collectors.toList());
            List<Double> quality = nodeRepository.queryAll("match (n)-[:POPULATION]->(s)-[:GENE]->()-[:RWGENE]->()-[:TREE]->(t) where id(n) = $ID return s.quality", Values.parameters("ID", x.getId()), Double.class).collect(Collectors.toList());
            List<IndividualInformation> stepInfo = new ArrayList<>();
            for (int i = 0; i < astIds.size(); i++) {
                IndividualInformation info = new IndividualInformation(nodeRepository.findSubtree(astIds.get(i)), quality.get(i));
                stepInfo.add(info);
            }
            individuals.add(stepInfo);
        });
        System.out.println("Printing experiment log");
        try {
            // reload the OG and Best AST to ensure they have the node IDs set
            experimentPrinter.printExperiment(run.getKey().getFunction(), experimentNode.getParameters(), stepParams, individuals, nodeRepository.findSubtree(original.getTree().getId()),  nodeRepository.findSubtree(bestFound.getTree().getId()), originalQuality);
        } catch (Exception e) {
            System.out.println("Failed to print the experiment results");
            e.printStackTrace();
        }
    }

    /**
     * Entry point using the GraalBackdoorSystem to run the experiments
     *
     * @param args
     */
    public static void main(String[] args) {
        // Start Broker and Command Plane
//        System.out.println("AUTO-STARTING BROKER AND COMMAND PLANE. IF YOU DON'T WANT THIS, DISABLE THE CODE");
//        Thread broker = new Thread(() -> MessageBroker.main(new String[]{"frontend=15557", "backend=15558", "command=15559"}));
//        Thread commander = new Thread(() -> MessageCommandModule.main(new String[]{"backend=localhost:15558", "broker=localhost:15559", "workerLimit=5"}));
//        broker.start();
//        commander.start();

        // Caller for Experiments using the Graal Backdoor Language
        new GraalBackdoorSystem().breakIn(MinicExperiments.class, "enter", null);
    }

    /**
     * This function Sets up the optimization environment and starts the experiment function.
     */
    public static void enter() {
        printer = new RuntimeProfilePrinter();
        printer.setDebug(false);
        printer.setFormat("html");
        experimentPrinter = new ExperimentPrinter();
        experimentPrinter.setDebug(false);
        experimentPrinter.setFormat("html");

        // pre-load system information
        ClassPathXmlApplicationContext dbContext = ApplicationContextProvider.getCtx();
        SystemInformationRepository systemInformationRepository = dbContext.getBean(SystemInformationRepository.class);
        systemInformationRepository.save(SystemInformation.getCurrentSystem());

        // Prepare MiniC in case we haven't had the Language already in the DB
        // ENSURE that we have a fully learned language
        // TODO #196 load this from DB
        TruffleLanguageInformation tli = TruffleLanguageInformation.getLanguageInformation(MinicLanguage.ID);
        MinicLanguageLearner learner = new MinicLanguageLearner(tli);
        learner.setSaveToDB(true);
        learner.setFast(true);
        learner.learn();

        // Conduct Experiments
        new MinicExperiments().experiment();

        // shutdown
        ApplicationContextProvider.close();
    }

}
