/*
 * Copyright (c) 2022 the original author or authors.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

package at.fh.hagenberg.aist.gce.optimization.operators;

import at.fh.hagenberg.aist.gce.optimization.ProfileKeeper;
import at.fh.hagenberg.aist.gce.optimization.TruffleOptimizationProblem;
import at.fh.hagenberg.aist.gce.optimization.TruffleOptimizationSolution;
import at.fh.hagenberg.aist.gce.optimization.operators.selection.RandomTruffleTreeSelector;
import at.fh.hagenberg.aist.gce.optimization.util.*;
import at.fh.hagenberg.aist.gce.optimization.util.strategy.*;
import at.fh.hagenberg.aist.gce.optimization.util.strategy.initializer.TruffleEntryPointStrategy;
import at.fh.hagenberg.aist.gce.optimization.util.strategy.random.NodeInjectingSubtreeStrategy;
import at.fh.hagenberg.aist.gce.optimization.util.strategy.selection.ChooseOption;
import at.fh.hagenberg.aist.gce.optimization.util.strategy.selection.RandomChooser;
import science.aist.seshat.Logger;
import at.fh.hagenberg.machinelearning.analytics.TruffleGraphAnalytics;
import at.fh.hagenberg.machinelearning.analytics.graph.nodes.*;
import at.fh.hagenberg.machinelearning.core.Solution;
import at.fh.hagenberg.machinelearning.core.SolutionGene;
import at.fh.hagenberg.machinelearning.core.options.Descriptor;
import com.oracle.truffle.api.nodes.Node;
import org.springframework.beans.factory.annotation.Required;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * Created by Oliver Krauss on 10.02.2017.
 */
public class TruffleSingleNodeMutator implements ConfigurableMutator<TruffleOptimizationSolution, TruffleOptimizationProblem> {

    /**
     * Analytics service for analyzing a run
     */
    private TruffleGraphAnalytics analyticsService;

    /**
     * Selection strategy for point in tree that will be mutated
     */
    private TruffleTreeSelector selector = new RandomTruffleTreeSelector();

    /**
     * Choice of which subclass will be chosen as replacement
     */
    private ChooseOption<TruffleClassInitializer> mutationChoice = new RandomChooser<>();

    /**
     * Strategy for creating entirely new trees
     */
    private TruffleHierarchicalStrategy<Node> fullTreeStrategy;

    /**
     * Strategy for creating new subtrees
     */
    private TruffleHierarchicalStrategy<Node> subtreeStrategy;

    private Logger logger = Logger.getInstance();

    private TruffleLanguageInformation information;

    /**
     * Terminal strategies for creating values
     */
    private Map<String, TruffleVerifyingStrategy> strategies;

    private TruffleLanguageInformation getTruffleLanguageInformation(TruffleOptimizationSolution solution) {
        if (information != null) {
            return information;
        }
        return information = TruffleLanguageInformation.getLanguageInformationMinimal(solution.getProblem().getLanguage());
    }

    @Override
    public Solution<TruffleOptimizationSolution, TruffleOptimizationProblem> mutate(Solution<TruffleOptimizationSolution, TruffleOptimizationProblem> solution) {
        long start = ProfileKeeper.profiler.start();
        // select gene for mutation (only one anyway)
        SolutionGene<TruffleOptimizationSolution, TruffleOptimizationProblem> gene = solution.getSolutionGenes().get(0);
        Solution<TruffleOptimizationSolution, TruffleOptimizationProblem> newSolution = new Solution<>();

        newSolution.addGene(new SolutionGene<>(mutate(gene.getGene()), gene.getProblemGenes()));
        ProfileKeeper.profiler.profile("TruffleSingleNodeMutator.mutate", start);
        return newSolution;
    }

    /**
     * Utility class for weighing
     */
    private NodeWrapperWeightUtil weightUtil;

    private NodeWrapperWeightUtil getWeightUtil(TruffleOptimizationSolution solution) {
        if (weightUtil == null) {
            weightUtil = new NodeWrapperWeightUtil(solution.getProblem().getLanguage());
        }
        return weightUtil;
    }

    @Override
    public TruffleOptimizationSolution mutate(TruffleOptimizationSolution solution) {
        Node mutatedNode = solution.getNode().deepCopy();

        try {
            // find node to mutate
            Node mutationPoint = selector.selectSubtree(mutatedNode);
            double remainingNodeWeight = 0;
            if (solution.getProblem().getConfiguration().getMaxWeight() < Double.MAX_VALUE) {
                // the remaining weight is the total weight minus ONLY the node to be replaced
                remainingNodeWeight = weightUtil.weight(mutatedNode) - weightUtil.weight(new NodeWrapper(mutationPoint.getClass().getName()));
            }

            // find replacement
            // list of possibilities
            TruffleHierarchicalStrategy<Node> strategyForClasses = fullTreeStrategy;
            int depth = ExtendedNodeUtil.getRelativeDepth(mutatedNode, mutationPoint);
            CreationConfiguration configuration = solution.getProblem().getConfiguration();
            if (mutationPoint != mutatedNode) {
                TruffleLanguageSearchSpace context = solution.getProblem().getSearchSpace();
                strategyForClasses = new TruffleEntryPointStrategy(context, mutatedNode, mutationPoint, subtreeStrategy, new CreationConfiguration(configuration.getMaxDepth() - depth + 1, configuration.getMaxWidth(), configuration.getMaxWeight() - remainingNodeWeight));
            }

            Collection<Class> validParentClasses = strategyForClasses.getManagedClasses();
            Collection<TruffleClassInitializer> values = getTruffleLanguageInformation(solution).getInstantiableNodes().values().stream().filter(x -> validParentClasses.contains(x.getClazz())).flatMap(x -> x.getInitializersForCreation().stream()).collect(Collectors.toList());
            // clear all that can't take the child nodes
            mutationPoint.getChildren().forEach(x -> {
                values.removeIf(init ->
                    Arrays.stream(init.getParameters()).noneMatch(param ->
                        param.getClazz().isAssignableFrom(x.getClass())));
            });
            if (values.isEmpty()) {
                throw new RuntimeException("Node class can't be mutated " + mutationPoint.getClass());
            }

            TruffleClassInitializer initializer = mutationChoice.choose(values);

            // apply replacement -> move children to new node
            // Note: we are not providing the weight util here, as the mutator already restricted to the valid weight
            DataFlowGraph dataFlowGraph = DataFlowUtil.constructDataFlowGraph(getTruffleLanguageInformation(solution), mutatedNode, mutationPoint, TruffleFunctionAnalyzer.getSignature(solution.getNode().getRootNode()));
            Node newNode = new NodeInjectingSubtreeStrategy<Node>(initializer, strategies, subtreeStrategy, StreamSupport.stream(mutationPoint.getChildren().spliterator(), true).collect(Collectors.toList()), null).create(new CreationInformation(NodeWrapper.wrap(mutatedNode), NodeWrapper.wrap(mutationPoint), new RequirementInformation(null), dataFlowGraph, initializer.getClazz(), depth, configuration));

            if (values.isEmpty()) {
                throw new RuntimeException("Node class initialization failed " + mutationPoint.getClass());
            }

            if (mutationPoint == mutatedNode) {
                mutatedNode = newNode;
            }
            if (mutationPoint.getParent() != null) {
                // move mutation to correct parent
                mutationPoint.replace(newNode);
            }
        } catch (Exception e) {
            // make sure one failed mutation doesn't kill our run
            System.out.println("Mutation failed with node " + NodeWrapper.wrap(mutatedNode).getHash());
            e.printStackTrace();
        }

        // log node
        if (analyticsService != null) {
            OperationNode operation = new OperationNode("mutate-single-node");
            operation.addInput(solution.getTree()); // Note: The original node is manipulated during runtime in some languages. We wan't the pure parsed origin
            operation.addOutput(NodeWrapper.wrap(mutatedNode));
            analyticsService.saveOperation(operation);
        }

        // return mutated gene
        TruffleOptimizationSolution newGene = new TruffleOptimizationSolution(mutatedNode, solution.getProblem(), this);

        return newGene;
    }

    public void setAnalyticsService(TruffleGraphAnalytics analyticsService) {
        this.analyticsService = analyticsService;
    }

    public void setSelector(TruffleTreeSelector selector) {
        this.selector = selector;
    }

    public void setMutationChoice(ChooseOption<TruffleClassInitializer> mutationChoice) {
        this.mutationChoice = mutationChoice;
    }


    @Override
    public Map<String, Descriptor> getOptions() {
        Map<String, Descriptor> options = new HashMap<>();
        options.put("selector", new Descriptor<>(selector));
        options.put("chooser", new Descriptor<>(mutationChoice));
        return options;
    }

    @Override
    public boolean setOption(String name, Descriptor descriptor) {
        try {
            switch (name) {
                case "selector":
                    setSelector((TruffleTreeSelector) descriptor.getValue());
                    break;
                case "chooser":
                    setMutationChoice((ChooseOption<TruffleClassInitializer>) descriptor.getValue());
                    break;
            }
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    @Required
    @Override
    public void setFullTreeStrategy(TruffleCombinedStrategy<Node> fullTreeStrategy) {
        this.fullTreeStrategy = fullTreeStrategy;
    }

    @Required
    @Override
    public void setSubtreeStrategy(TruffleHierarchicalStrategy<Node> subtreeStrategy) {
        this.subtreeStrategy = subtreeStrategy;
    }

    @Required
    public void setStrategies(Map<String, TruffleVerifyingStrategy> strategies) {
        this.strategies = strategies;
    }
}
