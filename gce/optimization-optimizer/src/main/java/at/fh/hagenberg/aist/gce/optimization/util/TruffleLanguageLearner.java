/*
 * Copyright (c) 2022 the original author or authors.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

package at.fh.hagenberg.aist.gce.optimization.util;

import at.fh.hagenberg.aist.gce.context.ApplicationContextProvider;
import at.fh.hagenberg.aist.gce.optimization.executor.*;
import at.fh.hagenberg.aist.gce.optimization.run.TruffleLanguageOptimizer;
import at.fh.hagenberg.aist.gce.optimization.runtime.SystemInformation;
import at.fh.hagenberg.aist.gce.optimization.util.strategy.*;
import at.fh.hagenberg.aist.gce.optimization.util.strategy.initializer.TruffleEntryPointStrategy;
import at.fh.hagenberg.aist.gce.optimization.util.strategy.other.NonCheckingFrameSlotStrategy;
import at.fh.hagenberg.aist.gce.science.statistics.data.template.ExceptionPrinter;
import science.aist.neo4j.repository.AbstractNeo4JRepository;
import science.aist.seshat.Logger;
import at.fh.hagenberg.machinelearning.analytics.graph.TruffleLanguageInformationRepository;
import at.fh.hagenberg.machinelearning.analytics.graph.nodes.*;
import at.fh.hagenberg.util.Pair;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.MaterializedFrame;
import com.oracle.truffle.api.nodes.Node;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Array;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * The Truffle Language Learner (TLL) is responsible for identifying the necessary information that
 * {@link TruffleLanguageInformation} should contain but can't be loaded quickly with just simple static analysis.
 * Currently it is responsible for:
 * - Finding the node cost (weight) of a node
 * - Finding which reads depend on which writes
 * - Finding out which readArgumentNodes accept what types
 */
public abstract class TruffleLanguageLearner {

    /**
     * How often the execution is repeated.
     * Do NOT change below 200.000 as this makes the results unreliable
     */
    private static final int REPEATS = 1000000;

    /**
     * How often a node class is duplicated (the more duplications the fewer side effects from other nodes)
     */
    private static final int NODE_DUPLICATIONS = 10000;

    /**
     * Logger for logging errors
     */
    private Logger logger = Logger.getInstance();

    /**
     * Language information that shall be learned
     */
    private TruffleLanguageInformation information;

    /**
     * Exceptions collected during weight calculation
     */
    private Map<Class, Exception> weightingExceptions = new HashMap<>();


    /**
     * Exceptions collected during weight calculation
     */
    private Map<Class, Exception> writeReadPairExceptions = new HashMap<>();


    /**
     * Several classes in the language are Truffle specifics (such as message resolutions)
     * that we don't really want to touch in an optimization context
     */
    private List<Class> notTrulyInstantiable = new LinkedList<>();

    /**
     * Executor for testing
     * NOTE: for debugging switching to InternalExecutor here is a good idea!
     */
    private AbstractExecutor exec;

    /**
     * Executor for producing traces containing additional information.
     */
    private JavassistExecutor traceExec;

    /**
     * Executor for testing internally (everything that is NOT performance related)
     */
    private InternalExecutor internalExec;

    /**
     * Helper for learning the child-weights of already measured nodes
     */
    private NodeWrapperWeightUtil weightUtil;

    /**
     * If the results shall be saved to the database immediately after being produced
     */
    private boolean saveToDB = false;

    /**
     * If learner is set to fast it will not attempt to find the weights
     * This option is exclusively to quickly get the core features of the language
     */
    private boolean fast = false;

    public TruffleLanguageLearner(TruffleLanguageInformation information) {
        this.information = information;
    }

    /**
     * Learns all the information the TLL is responsible for and directly adds it to the given TLI
     */
    public void learn() {
        if (this.information.learned()) {
            return;
        }

        // initialize the executors
        // TODO #196 this should say "main" but then we can't find the pairings for the read args anymore
        exec = new WeightWatcherExecutor(information.getName(), getMinimalProgram(), "weight", "weight", null);
        exec.setRepeats(REPEATS);
        traceExec = new JavassistExecutor(information.getName(), getMinimalProgram(), "weight", "weight", null);
        internalExec = new InternalExecutor(information.getName(), getMinimalProgram(), "weight", "weight");
        weightUtil = new NodeWrapperWeightUtil(information);

        // should not be necessary but ENSURE that timeout is turned off for the learning
        exec.setTimeout(-1);
        traceExec.setTimeout(-1);
        internalExec.setTimeout(-1);

        // only re-learn if not already learned
        if (this.information.getInstantiableNodes().values().stream().allMatch(x -> x.getArgumentReadClasses().isEmpty() && x.getWritePairings().isEmpty())) {
            logger.info("Learning language basics");
            // learn which readArgument nodes accept what kind of argument type
            argumentTypes(information);
            // learn which reads can work with which writes
            writeReadPairs(information);
            // learn which classes can be safely replaced with each other
            replaceability(information);

            if (saveToDB) {
                System.out.println("SAVING");
                TruffleLanguageInformationRepository tliRepository = ApplicationContextProvider.getCtx().getBean(TruffleLanguageInformationRepository.class);
                tliRepository.save(information);
            }
        }

        // learn the assumed execution time of a node
        if (!fast) {
            weight(information);
        }

        // we cheat here to add the missing node infos TODO #196 when this issue finished should not be necessary anymore
        double average = information.getInstantiableNodes().values().stream().filter(x -> x.getSystemWeight() > 0).mapToDouble(TruffleClassInformation::getSystemWeight).average().orElse(1.0);
        information.getInstantiableNodes().values().stream().filter(x -> !x.getWeight().containsKey(SystemInformation.getCurrentSystem()))
                .forEach(x -> x.getWeight().put(SystemInformation.getCurrentSystem(), average));
    }

    /**
     * Learns which read-argument nodes can read what typeN
     *
     * @param information
     */
    private void argumentTypes(TruffleLanguageInformation information) {
        logger.info("Attempting to find input classes for argument-reading nodes");
        // get all write classes
        List<TruffleClassInformation> argumentClasses = information.getInstantiableNodes().values().stream().
                filter(x -> x.getProperties().contains(TruffleClassProperty.STATE_READ_ARGUMENT)
                        && !x.getProperties().contains(TruffleClassProperty.TRUFFLE_BOUNDARY)).collect(Collectors.toList());

        argumentClasses.forEach(arg -> {
            getSampleInput().forEach((key, input) -> {
                Object[] inParams = new Object[1];
                inParams[0] = input.create(null);
                ExecutionResult evaluate = evaluate(internalExec, loadOrPrepareNode(arg, false), 1, inParams);
                if (evaluate.isSuccess()) {
                    logger.info("found valid input for " + arg.getClazz().getName() + " / " + key);
                    arg.getArgumentReadClasses().add(key);
                }
            });
        });
    }

    /**
     * Learns which WRITES can happen before a READ for the read to work.
     *
     * @param information
     */
    private void writeReadPairs(TruffleLanguageInformation information) {
        // get all write classes
        List<TruffleClassInformation> writeClasses = information.getInstantiableNodes().values().stream().
                filter(x -> x.getProperties().contains(TruffleClassProperty.STATE_WRITE)
                        && !x.getProperties().contains(TruffleClassProperty.TRUFFLE_BOUNDARY)).collect(Collectors.toList());

        // get all read classes
        List<TruffleClassInformation> readClasses = information.getInstantiableNodes().values().stream()
                .filter(x -> x.getProperties().contains(TruffleClassProperty.STATE_READ)
                        && !x.getProperties().contains(TruffleClassProperty.STATE_READ_ARGUMENT)
                        && !x.getProperties().contains(TruffleClassProperty.TRUFFLE_BOUNDARY)).collect(Collectors.toList());

        // for all read classes determine which WRITE they can live with
        AtomicBoolean found = new AtomicBoolean(true);

        // repeat the process as long as we found new pairings that we can apply to our chaining process (doing this repeatedly as smaller chains can spawn larger ones)
        while (found.get()) {
            found.set(false);
            logger.info("Attempting to find read-write pairings");

            // test all classes for pairings
            readClasses.forEach(read -> {
                writeClasses.forEach(write -> {
                    // prevent mixing and matching local and global state nodes
                    if ((read.getProperties().contains(TruffleClassProperty.LOCAL_STATE) && !write.getProperties().contains(TruffleClassProperty.LOCAL_STATE))
                            || (read.getProperties().contains(TruffleClassProperty.GLOBAL_STATE) && !write.getProperties().contains(TruffleClassProperty.GLOBAL_STATE))) {
                        return;
                    }
                    // prevent rediscovery and self-chaining (and with that endless cycle!)
                    if (read.getWritePairings().contains(write) || read.equals(write)) {
                        return;
                    }

                    // add write pairing at FIRST position (as we always take the first position in the chain-creation)
                    read.getWritePairings().add(0, write);

                    // test and document
                    logger.debug("Testing " + read.getClazz().getName() + " -> " + write.getClazz().getName());
                    Node node = loadOrPrepareNode(read, false);
                    ExecutionResult test;
                    if (node != null) {
                        test = evaluate(internalExec, node, 1);
                    } else {
                        test = new ExecutionResult(null, null, null, false);
                    }
                    if (test.isSuccess()) {
                        logger.info("found pairable read write " + read.getClazz().getName() + " / " + write.getClazz().getName());
                        found.set(true);
                    } else {
                        // rollback the pairing
                        read.getWritePairings().remove(write);
                    }
                });
            });
        }

        // log all reads that failed
        readClasses.stream().filter(x -> x.getWritePairings().isEmpty()).forEach(x -> {
            writeReadPairExceptions.put(x.getClazz(), new RuntimeException("Read without ANY acceptable pairing found"));
        });
    }

    /**
     * Learns the WEIGHT of all nodes. The weight being the execution time for a single node
     *
     * @param information to be learned
     */
    private void weight(TruffleLanguageInformation information) {
        // TODO #196 -> Loop nodes should be constructed with a valid stopping condition
        logger.info("Attempting to weigh the node classes");

        // remove the adjustment value for weighing
        if (information.getWeightAdjustment().containsKey(SystemInformation.getCurrentSystem())) {
            Double oldAdjustment = information.getWeightAdjustment().get(SystemInformation.getCurrentSystem());
            // remove old adjustment
            information.getInstantiableNodes().values().stream().filter(x -> x.weight.containsKey(SystemInformation.getCurrentSystem())).forEach(x -> x.weight.put(SystemInformation.getCurrentSystem(), x.weight.get(SystemInformation.getCurrentSystem()) - oldAdjustment));
            if (saveToDB) {
                // update db with zeros so a crash does not invalidate the database and can be resumed
                information.getWeightAdjustment().put(SystemInformation.getCurrentSystem(), 0.0);
                TruffleLanguageInformationRepository tliRepository = ApplicationContextProvider.getCtx().getBean(TruffleLanguageInformationRepository.class);
                tliRepository.save(information);
            }
        }
        // remove the unoptimized adjustment value for weighing
        if (information.getWeightUnoptimizedAdjustment().containsKey(SystemInformation.getCurrentSystem())) {
            Double oldAdjustment = information.getWeightUnoptimizedAdjustment().get(SystemInformation.getCurrentSystem());
            // remove old adjustment
            information.getInstantiableNodes().values().stream().filter(x -> x.weightUnoptimized.containsKey(SystemInformation.getCurrentSystem())).forEach(x -> x.weightUnoptimized.put(SystemInformation.getCurrentSystem(), x.weightUnoptimized.get(SystemInformation.getCurrentSystem()) - oldAdjustment));
            if (saveToDB) {
                // update db with zeros so a crash does not invalidate the database and can be resumed
                information.getWeightUnoptimizedAdjustment().put(SystemInformation.getCurrentSystem(), 0.0);
                TruffleLanguageInformationRepository tliRepository = ApplicationContextProvider.getCtx().getBean(TruffleLanguageInformationRepository.class);
                tliRepository.save(information);
            }
        }

        // first evaluate the minimal subtree
        ExecutionResult originResult = exec.test(exec.getOrigin().deepCopy(), null);
        double unoptimizedBase = originResult.getPerformance()[0];
        double optimizedBase = originResult.getPerformance()[1];

        logger.error("Unoptimized Base: " + unoptimizedBase);
        logger.error("Optimized Base: " + optimizedBase);

        information.getInstantiableNodes().values().stream().filter(x -> !x.weight.containsKey(SystemInformation.getCurrentSystem())).sorted((o1, o2) -> {
            // sort reads at the end, as all writes must be weighted by that point
            int chain1 = getChainSize(o1), chain2 = getChainSize(o2);
            if (chain1 != chain2) {
                return Integer.compare(chain1, chain2);
            }
            return Integer.compare(o1.getMinimalSubtreeSize(), o2.getMinimalSubtreeSize());
        }).forEach(x -> {
            if (x.getProperties().contains(TruffleClassProperty.TRUFFLE_BOUNDARY)) {
                logger.info("NOT Weighing " + x.getClazz().getName() + " as we are not capable of handling truffle boundaries");
                notTrulyInstantiable.add(x.getClazz());
                return;
            }

            try {
                // weigh individual node
                weighNode(x, optimizedBase, unoptimizedBase);
            } catch (Exception e) {
                logger.error("Failed to weigh node " + x.getClazz().getName());
                weightingExceptions.put(x.getClazz(), e);
            }
        });

        // adjust weights to become positive
        OptionalDouble min = information.getInstantiableNodes().values().stream().filter(x -> x.weight.containsKey(SystemInformation.getCurrentSystem())).mapToDouble(x -> x.weight.get(SystemInformation.getCurrentSystem())).min();
        if (min.isPresent() && min.getAsDouble() < 0) {
            // adjust weights to be all positive
            double newAdjusmtent = min.getAsDouble() * -1 + 1;
            information.getWeightAdjustment().put(SystemInformation.getCurrentSystem(), newAdjusmtent);
            information.getInstantiableNodes().values().stream().filter(x -> x.weight.containsKey(SystemInformation.getCurrentSystem())).forEach(x -> x.weight.put(SystemInformation.getCurrentSystem(), x.weight.get(SystemInformation.getCurrentSystem()) + newAdjusmtent));
        }
        // adjust unoptimized weights to become positive
        OptionalDouble minUnopt = information.getInstantiableNodes().values().stream().filter(x -> x.weightUnoptimized.containsKey(SystemInformation.getCurrentSystem())).mapToDouble(x -> x.weightUnoptimized.get(SystemInformation.getCurrentSystem())).min();
        if (minUnopt.isPresent() && minUnopt.getAsDouble() < 0) {
            // adjust weights to be all positive
            double newAdjusmtent = minUnopt.getAsDouble() * -1 + 1;
            information.getWeightUnoptimizedAdjustment().put(SystemInformation.getCurrentSystem(), newAdjusmtent);
            information.getInstantiableNodes().values().stream().filter(x -> x.weightUnoptimized.containsKey(SystemInformation.getCurrentSystem())).forEach(x -> x.weightUnoptimized.put(SystemInformation.getCurrentSystem(), x.weightUnoptimized.get(SystemInformation.getCurrentSystem()) + newAdjusmtent));
        }

        if (saveToDB) {
            TruffleLanguageInformationRepository tliRepository = ApplicationContextProvider.getCtx().getBean(TruffleLanguageInformationRepository.class);
            tliRepository.save(information);
        }
    }

    /**
     * Weighing individual node in separate function due to java garbage collection issues
     *
     * @param x Class information to be assigned a weight
     */
    protected void weighNode(TruffleClassInformation x, double optimizedBase, double unoptimizedBase) {
        logger.error("Weighing " + x.getClazz().getName() + " " + java.time.LocalTime.now());

        // create child and evaluate
        Node prototype = loadOrPrepareNode(x, true);
        ExecutionResult evaluationResult = evaluate(exec, prototype, NODE_DUPLICATIONS);

        // make sure the evaluation did not FAIL
        if (!evaluationResult.isSuccess()) {
            logger.error("Weigting harness is wrong for " + x.getClazz().getName());
            weightingExceptions.put(x.getClazz(), new RuntimeException(evaluationResult.getReturnValue().toString()));
            return;
        }

        // get the runtime values
        double unoptimized = evaluationResult.getPerformance()[0];
        double optimized = evaluationResult.getPerformance()[1];

        // add trace for additional information
        TraceExecutionResult traceResult = (TraceExecutionResult) evaluate(traceExec, prototype, 1);
        if (!traceResult.isSuccess()) {
            // this should NEVER happen
            logger.error("Harness failed at javassist tracing for " + x.getClazz().getName());
            weightingExceptions.put(x.getClazz(), new RuntimeException(traceResult.getReturnValue().toString()));
        }

        // determine weight
        // determine the key of the insertNode for profile traces
        String traceKey = "";
        Node traceKeyNode = traceExec.getRoot();
        while (traceKeyNode != null && traceKeyNode.getChildren().iterator().hasNext()) {
            traceKey = traceKey + ".0";
            traceKeyNode = traceKeyNode.getChildren().iterator().next();
        }
        traceKey += "." + (getChainSize(x) - 1);
        String finalTraceKey = traceKey;
        final int[] i = {0};
        double childWeight = StreamSupport.stream(prototype.getChildren().spliterator(), true).mapToDouble(child -> weightUtil.weight(NodeWrapper.wrap(child), traceResult.getNodeExecutions(), finalTraceKey + "." + i[0]++)).sum();
        double childUnoptimizedWeight = StreamSupport.stream(prototype.getChildren().spliterator(), true).mapToDouble(child -> weightUtil.weight(NodeWrapper.wrap(child), traceResult.getNodeExecutions(), finalTraceKey)).sum();
        x.setSystemWeight(((optimized - optimizedBase) / (double) NODE_DUPLICATIONS) - childWeight);
        x.setSystemWeightUnoptimized(((unoptimized - unoptimizedBase) / (double) NODE_DUPLICATIONS) - childUnoptimizedWeight);

        if (saveToDB) {
            AbstractNeo4JRepository providedRepository = (AbstractNeo4JRepository) ApplicationContextProvider.getCtx().getBean("truffleClassInformationRepository");
            providedRepository.save(x);
        }

        logger.error("Unoptimized : " + x.getSystemWeightUnoptimized() + " (" + childUnoptimizedWeight + ") - " + unoptimized);
        logger.error("Optimized: " + x.getSystemWeight() + " (" + childWeight + ") - " + " - " + optimized);
    }


    /**
     * Global frame used in execution;
     */
    protected MaterializedFrame globalFrame;

    protected MaterializedFrame getGlobalFrame() {
        if (globalFrame != null) {
            return globalFrame;
        }
        if (getStrategyForExecutor(null).containsKey("com.oracle.truffle.api.frame.MaterializedFrame")) {
            TruffleVerifyingStrategy truffleStrategy = getStrategyForExecutor(null).get("com.oracle.truffle.api.frame.MaterializedFrame");
            globalFrame = (MaterializedFrame) truffleStrategy.create(null);
        }
        return globalFrame;
    }

    protected void clearGlobalFrame() {
        // load the materialized frame for resetting
        MaterializedFrame globalFrame = getGlobalFrame();

        // globals affect each other as the state remains after runs, so the global state must be cleared.
        if (globalFrame != null) {
            List<? extends FrameSlot> slots = globalFrame.getFrameDescriptor().getSlots();
            slots.forEach(slot -> {
                globalFrame.setObject(slot, null);
            });
        }
    }

    protected ExecutionResult evaluate(AbstractExecutor executor, Node n, int count) {
        // prepare input if we require it
        Object[] input = null;
        TruffleClassInformation tci = information.getTci(n.getClass());
        if (tci.getProperties().contains(TruffleClassProperty.STATE_READ_ARGUMENT)) {
            input = new Object[1];
            if (!tci.getArgumentReadClasses().isEmpty()) {
                Map<String, TruffleVerifyingStrategy> sampleInput = getSampleInput();
                Map.Entry<String, TruffleVerifyingStrategy> strategy = sampleInput.entrySet().stream().filter(x -> tci.getArgumentReadClasses().contains(x.getKey())).findFirst().orElseGet(null);
                if (strategy != null) {
                    input[0] = strategy.getValue().create(null);
                }
            }
        }
        return evaluate(executor, n, count, input);
    }

    protected ExecutionResult evaluate(AbstractExecutor executor, Node n, int count, Object[] input) {
        clearGlobalFrame();
        Node insertNode = executor.getOrigin().deepCopy();

        // determine size of object chain
        TruffleClassInformation classInfo = information.getTci(n.getClass());
        TruffleClassInformation chainInfo = classInfo;
        int chain = getChainSize(chainInfo);

        // create object chain (write-read chains if necessary)
        TruffleClassInitializer insertConstructor = information.getInstantiableNodes().get(insertNode.getClass()).getInitializersForCreation().get(0);
        Object[] array = (Object[]) Array.newInstance(insertConstructor.getParameters()[0].getClazz(), chain + count - 1); // count -1 because the chain will always be at least 1 in size
        // create the chain
        for (int i = chain - 1; i >= 0; i--) {
            array[i] = loadOrPrepareNode(chainInfo, false).deepCopy();
            chainInfo = chainInfo.getWritePairings().isEmpty() ? null : chainInfo.getWritePairings().get(0);
        }
        // create the node copies after the initializer chain
        for (int i = chain; i < array.length; i++) {
            array[i] = n.deepCopy();
        }

        // package nodes and create new insertNode
        Object[][] arrayContainer = new Object[1][];
        arrayContainer[0] = array;
        Node newInsertNode = (Node) insertConstructor.instantiate(arrayContainer);

        return executor.test(newInsertNode, input);
    }

    private int getChainSize(TruffleClassInformation chainInfo) {
        int chain = 0;
        while (chainInfo != null) {
            chain++;
            chainInfo = chainInfo.getWritePairings().isEmpty() ? null : chainInfo.getWritePairings().get(0);
        }
        return chain;
    }

    /**
     * Map of nodes that were already prepared for learning purposes
     */
    protected Map<TruffleClassInformation, Node> preparedNodes = new HashMap<>();

    protected Node loadOrPrepareNode(TruffleClassInformation tci, boolean weightedOnly) {
        if (!preparedNodes.containsKey(tci)) {
            // prepare minimal configuration
            CreationConfiguration config = new CreationConfiguration(tci.getMinimalSubtreeSize(), 1, Double.MAX_VALUE);
            // exclude all classes that aren't "minimal"
            List<Class> excludes = new LinkedList<>();
            information.getInstantiableNodes().forEach((key, value) -> {
                if (!key.equals(tci.getClazz())) {
                    if (!value.getProperties().isEmpty()) {
                        excludes.add(key);
                    } else if (weightedOnly && value.getSystemWeight() < 0) {
                        excludes.add(key);
                    }
                }
            });

            // find node for insertion (the executor doesn't matter here, we just need to know which insert type we require)
            Node execNode = exec.getOrigin().getParent().deepCopy();
            Node insertNode = execNode.getChildren().iterator().next();

            TruffleLanguageSearchSpace writeTss = new TruffleLanguageSearchSpace(information, excludes);
            // TODO #196 inject specific strategies here if needed (invoke needs to invoke functions that exist!)
            TruffleMasterStrategy masterStrategy = TruffleMasterStrategy.createFromTLI(new CreationConfiguration(information.getInstantiableNodes().values().stream().mapToInt(TruffleClassInformation::getMinimalSubtreeSize).max().orElse(5), 1, Double.MAX_VALUE), writeTss, new ArrayList<>(), getStrategyForExecutor(tci));
            TruffleEntryPointStrategy strategy = new TruffleEntryPointStrategy(information, execNode, insertNode, masterStrategy, config);

            try {
                // create node
                logger.info("Preparing node " + tci.getClazz());

                // to ensure we can generate read nodes we add available data items
                CreationInformation creationInfo = new CreationInformation(null, null, new RequirementInformation(null), getFakeDataFlowGraph(), tci.getClazz(), 0, config);

                Node prototype = strategy.create(creationInfo);
                preparedNodes.put(tci, prototype);
            } catch (Exception e) {
                // on failure add null so we won't try again
                logger.error("Failed at node construction " + tci.getClazz().getName(), e);
                writeReadPairExceptions.put(tci.getClazz(), e);
                preparedNodes.put(tci, null);
            }
        }

        return preparedNodes.get(tci);
    }

    private DataFlowGraph fakeDataFlowGraph;

    private DataFlowGraph getFakeDataFlowGraph() {
        if (fakeDataFlowGraph == null) {
            fakeDataFlowGraph = new DataFlowGraph(null, new HashMap<>(), new HashMap<>(), null);
            ArrayList<Object> localList = new ArrayList<>();
            localList.add(exec.getRoot().getFrameDescriptor().getSlots().get(0));
            ArrayList<Object> globalList = new ArrayList<>();
            globalList.add(getGlobalFrame().getFrameDescriptor().getSlots().get(0));
            fakeDataFlowGraph.getAvailableDataItems().put(null, localList.stream().map(x -> new DataFlowNode(x, null)).collect(Collectors.toList()));
            fakeDataFlowGraph.getAvailableDataItems().put(getGlobalFrame(), globalList.stream().map(x -> new DataFlowNode(x, null)).collect(Collectors.toList()));
        }
        return fakeDataFlowGraph;
    }

    /**
     * Printer for implementation state diagnosis
     */
    private ExceptionPrinter printer = new ExceptionPrinter(null);

    /**
     * Produces a report of the implementation state of the truffle language
     *
     * @param reportLocation to be printed to. If null will be printed to the execution directory as "LANGUAGEID-implementationReport.md"
     */
    public void diagnoseImplementationState(String reportLocation) {
        reportLocation = reportLocation == null ? new File(".") + "/" + information.getName() + "-implementationReport.md" : reportLocation;
        List<Pair<String, Exception>> exceptions = new LinkedList<>();

        information.getUnreachableClasses().forEach((key, value) -> {
            exceptions.add(new Pair<>(value, new InstantiationException(key.getName())));
        });

        this.notTrulyInstantiable.forEach(x -> {
            exceptions.add(new Pair<>("Node is not suited for optimization " + x.getName(), new InstantiationException(x.getName())));
        });

        this.weightingExceptions.forEach((key, value) -> {
            exceptions.add(new Pair<>("Failed to weigh node " + key.getName(), value));
        });

        this.writeReadPairExceptions.forEach((key, value) -> {
            exceptions.add(new Pair<>("Failed to find write-read pairing " + key.getName(), value));
        });

        try {
            printer.setWriter(new FileWriter(reportLocation));
            printer.print(exceptions);
        } catch (IOException e) {
            logger.error("Failed to write report", e);
        }
    }

    private Map<String, TruffleVerifyingStrategy> getStrategyForExecutor(TruffleClassInformation information) {
        // prepare strategy for creating objects
        Map<String, TruffleVerifyingStrategy> strategies = DefaultStrategyUtil.defaultStrategies();

        if (information != null) {
            if (information.getProperties().contains(TruffleClassProperty.STATE_READ_ARGUMENT)) {
                // for ease of access we allow loading ONLY from the first position
                strategies.put("int", new KnownValueStrategy<>(new LinkedList<>(Collections.singletonList(0))));
            } else if (information.getProperties().contains(TruffleClassProperty.STATE_READ)) {
                // downsize reads for lower positions
                strategies.put("int", new KnownValueStrategy<>(new LinkedList<>(Arrays.asList(0, 1, 2))));
            } else if (information.getProperties().contains(TruffleClassProperty.STATE_WRITE)) {
                // increase size for writes to higher positions
                strategies.put("int", new KnownValueStrategy<>(new LinkedList<>(Arrays.asList(3, 4, 5))));
            } else {
                // Just to prevent things like "Loop 0" and "x / 0" we remove 0 from the rest of the run
                strategies.put("int", new KnownValueStrategy<>(new LinkedList<>(Arrays.asList(1, 2, 3))));
            }
        }

        strategies.put("com.oracle.truffle.api.frame.FrameSlot", new NonCheckingFrameSlotStrategy(exec.getRoot().getFrameDescriptor()));
        strategies.putAll(getStrategies());
        return strategies;
    }

    /**
     * Strategies for special "primitive" types that the language may have
     * Generally the same implementation as needed for {@link TruffleLanguageOptimizer#}getStrategies()
     *
     * @return primitive type strategies
     */
    protected abstract Map<String, TruffleVerifyingStrategy> getStrategies();

    /**
     * Strategies for input (can be ANYTHING the language requires)
     * The map is <Classname of Input, Strategy returning input>
     *
     * @return sample input strategies
     */
    protected abstract Map<String, TruffleVerifyingStrategy> getSampleInput();

    /**
     * program that the nodes will be inserted in for evaluation. The name of the function must be "weight"
     *
     * @return code as string
     */
    protected abstract String getMinimalProgram();

    /**
     * validation if the return value is invalid. If it is, there is a problem with the test harness but not the weighting process
     *
     * @param returnValue that was returned
     * @return if the value is not as expected
     */
    protected abstract boolean returnValueInvalid(Object returnValue);


    /**
     * Learns which nodes are safely replacable by other nodes. These are the nodes that are ALWAYS
     * safely replaceable. In some cases more replacement options can occur during runtime.
     *
     * @param information
     */
    private void replaceability(TruffleLanguageInformation information) {
        // check every class for replacements
        information.getInstantiableNodes().values().forEach(tci -> {
            // for this we need to compare the FIELDS (that are nodes!) of the class to the PARAMETERS of other class INITIALIZERS
            // TODO #21
        });
    }

    public boolean isSaveToDB() {
        return saveToDB;
    }

    public void setSaveToDB(boolean saveToDB) {
        this.saveToDB = saveToDB;
    }

    public boolean isFast() {
        return fast;
    }

    public void setFast(boolean fast) {
        this.fast = fast;
    }
}
