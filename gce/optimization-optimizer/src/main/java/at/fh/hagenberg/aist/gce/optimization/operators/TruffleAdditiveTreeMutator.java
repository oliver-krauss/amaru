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
import at.fh.hagenberg.aist.gce.optimization.operators.selection.ContractualTruffleTreeSelector;
import at.fh.hagenberg.aist.gce.optimization.operators.selection.RandomTruffleTreeSelector;
import at.fh.hagenberg.aist.gce.optimization.util.*;
import at.fh.hagenberg.aist.gce.optimization.util.strategy.*;
import at.fh.hagenberg.aist.gce.optimization.util.strategy.initializer.TruffleEntryPointStrategy;
import at.fh.hagenberg.aist.gce.optimization.util.strategy.selection.ChooseOption;
import at.fh.hagenberg.aist.gce.optimization.util.strategy.selection.RandomChooser;
import at.fh.hagenberg.machinelearning.analytics.TruffleGraphAnalytics;
import at.fh.hagenberg.machinelearning.analytics.graph.nodes.*;
import at.fh.hagenberg.machinelearning.core.Solution;
import at.fh.hagenberg.machinelearning.core.SolutionGene;
import at.fh.hagenberg.machinelearning.core.options.Descriptor;
import com.oracle.truffle.api.nodes.Node;
import org.springframework.beans.factory.annotation.Required;
import science.aist.seshat.Logger;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Attempts to only add statements instead of replacing
 * Created by Oliver Krauss on 10.02.2017.
 * TODO #4 IMPLEMENT FOR PATTERN REVIE
 */
public class TruffleAdditiveTreeMutator implements ConfigurableMutator<TruffleOptimizationSolution, TruffleOptimizationProblem> {

    /**
     * Analytics service for analyzing a run
     */
    private TruffleGraphAnalytics analyticsService;

    /**
     * Strategy for creating new subtrees
     */
    private TruffleHierarchicalStrategy<Node> subtreeStrategy;

    /**
     * Strategy for creating entirely new trees
     */
    private TruffleCombinedStrategy<Node> fullTreeStrategy;

    /**
     * Selection strategy for point in tree that will be mutated
     */
    private ContractualTruffleTreeSelector selector = new RandomTruffleTreeSelector();

    /**
     * Choice of which subclass will be chosen as replacement
     */
    private ChooseOption<Class> mutationChoice = new RandomChooser<>();

    private Logger logger = Logger.getInstance();

    /**
     * Randomizer
     */
    private Random random = new Random();

    /**
     * Utility class for weighing
     */
    private NodeWrapperWeightUtil weightUtil;

    /**
     * String that must be contained in the created mutation, otherwise it doesn't count.
     * Mostly useful to debug fix / prevention patterns
     */
    private Class check;

    private NodeWrapperWeightUtil getWeightUtil(TruffleOptimizationSolution solution) {
        if (weightUtil == null) {
            weightUtil = new NodeWrapperWeightUtil(solution.getProblem().getLanguage());
        }
        return weightUtil;
    }

    private TruffleLanguageInformation information;

    private TruffleLanguageInformation getTruffleLanguageInformation(TruffleOptimizationSolution solution) {
        if (information != null) {
            return information;
        }
        return information = TruffleLanguageInformation.getLanguageInformationMinimal(solution.getProblem().getLanguage());
    }

    @Override
    public Solution<TruffleOptimizationSolution, TruffleOptimizationProblem> mutate(Solution<TruffleOptimizationSolution, TruffleOptimizationProblem> solution) {
        // select gene for mutation (only one anyway)
        SolutionGene<TruffleOptimizationSolution, TruffleOptimizationProblem> gene = solution.getSolutionGenes().get(0);
        Solution<TruffleOptimizationSolution, TruffleOptimizationProblem> newSolution = new Solution<>();

        newSolution.addGene(new SolutionGene<>(mutate(gene.getGene()), gene.getProblemGenes()));
        return newSolution;
    }

    @Override
    public TruffleOptimizationSolution mutate(TruffleOptimizationSolution solution) {
        long start = ProfileKeeper.profiler.start();
        getWeightUtil(solution);
        getTruffleLanguageInformation(solution);
        Node mutatedNode = solution.getNode();

        try {
            // apply mutation to a single node
            mutatedNode = mutate(mutatedNode, solution.getProblem().getConfiguration(), solution.getProblem().getSearchSpace());
        } catch (Exception e) {
            // make sure one failed mutation doesn't kill our run
            System.out.println("Mutation failed with node " + NodeWrapper.wrap(mutatedNode).getHash());
            e.printStackTrace();
            throw new RuntimeException("MUTATOR COMPLETE FAIL");
        }
        start = ProfileKeeper.profiler.profile("TruffleTreeMutator.mutate", start);
        // log node
        if (analyticsService != null) {
            OperationNode operation = new OperationNode("mutate-subtree");
            operation.addInput(solution.getTree()); // Note: The original node is manipulated during runtime in some languages. We wan't the pure parsed origin
            operation.addOutput(NodeWrapper.wrap(mutatedNode));
            analyticsService.saveOperation(operation);
        }

        // return mutated gene
        TruffleOptimizationSolution newGene = new TruffleOptimizationSolution(mutatedNode, solution.getProblem(), this);

        ProfileKeeper.profiler.profile("TruffleTreeMutator.mutateLOG", start);
        return newGene;
    }

    private String path = "";

    /**
     * This function selects a node and then replaces it with a different one that still fits the tree structure
     * It uses a selection strategy to identify which part of the subtree will be mutated
     *
     * @param mutatedNode   copied structure with a single mutation
     * @param configuration configuration for the subtree creation protocol
     * @return the original tree, with one subtree mutated (original tree could be mutated fully)
     */
    private Node mutate(Node mutatedNode, CreationConfiguration configuration, TruffleLanguageContextProvider context) {
        logger.debug("mutate");
        logger.trace(mutatedNode.toString());
        TruffleFunctionSignature signature = TruffleFunctionAnalyzer.getSignature(mutatedNode.getRootNode());

        // mutate any random node in the tree
        mutatedNode = mutatedNode.deepCopy();
        mutatedNode.adoptChildren();
        Node backup = mutatedNode;

        int tries = 50;
        Node result = null;
        while (result == null && tries > 0) {
            // deep copy again to prevent bleedover from continous attempts
            Node attempt = mutatedNode.deepCopy();
            attempt.adoptChildren();
            result = attemptMutation(attempt, configuration, context, signature);
            tries--;
        }

        logger.debug("mutation finished");
        if (result == null) {
            logger.warn("Mutator failed in all tries to create a new individual!");
            result = backup;
            throw new RuntimeException("MUTATOR COMPLETE FAIL");
        }
        logger.trace("Mutated: " + result.toString());
        return result;
    }

    private Node attemptMutation(Node mutatedNode, CreationConfiguration configuration, TruffleLanguageContextProvider context, TruffleFunctionSignature signature) {
        Map<Node, LoadedRequirementInformation> rqi = ((TruffleMasterStrategy) subtreeStrategy).loadRequirements(mutatedNode);
        List<Node> choices = rqi.entrySet().stream().filter(x -> x.getValue().isFailed()).map(Map.Entry::getKey).collect(Collectors.toList());

        if (choices.isEmpty()) {
            choices = new ArrayList<>(rqi.keySet());
        }

        // select node to be mutated
        Node mutationNode = selector.selectSubtreeFromChoices(choices);
        double remainingNodeWeight = 0;
        if (configuration.getMaxWeight() < Double.MAX_VALUE) {
            remainingNodeWeight = weightUtil.weight(mutatedNode) - weightUtil.weight(mutationNode);
        }

        if (mutationNode.getParent() == null) {
            // we are replacing the top node.
            path = "FULL";
            // TODO #230 The data flow graph is not set. BUG for global variables
            if (fullTreeStrategy instanceof TruffleEntryPointStrategy) {
                ((TruffleEntryPointStrategy) fullTreeStrategy).setDataFlowGraph(new DataFlowGraph(null, null, null, signature));
            }
            Node next = fullTreeStrategy.next();
            if (check != null && next != null && ExtendedNodeUtil.flatten(next).noneMatch(x -> check.isAssignableFrom(x.getClass()) || (x.getClass().getSuperclass().getEnclosingClass() != null && check.isAssignableFrom(x.getClass().getSuperclass().getEnclosingClass())))) {
                System.out.println("Failed to inject the desired pattern. Not valid");
                next = null;
            }
            return next;
        } else {
            // we are replacing a child. Go at it with a smart entry point
            path = "HALF";
            int depth = ExtendedNodeUtil.getRelativeDepth(mutatedNode, mutationNode);
            int maxDepth = configuration.getMaxDepth() - depth;
            // The strategies have a tendency to create MASSIVE sub asts. We are probably better off restricting the width and the depth MORE than totally allowed.
            if (maxDepth > 10) {
                // randomly reduce to 10 or below
                maxDepth = random.nextInt(8) + 3;
            } else if (maxDepth > 3) {
                // chance for random reduction
                maxDepth = random.nextInt(maxDepth - 2) + 3;
            }
            final int[] width = {0};
            mutationNode.getParent().getChildren().iterator().forEachRemaining(x -> width[0]++);
            int maxWidth = configuration.getMaxWidth() - width[0] + 1;
            if (configuration.getMaxWidth() < maxWidth) {
                maxWidth = configuration.getMaxWidth();
            }
            if (maxWidth > 10) {
                // randomly reduce to 8 or below
                maxWidth = random.nextInt(6) + 3;
            } else if (maxWidth > 3) {
                // chance for random reduction
                int bound = maxWidth - 2;
                maxWidth = random.nextInt(bound) + 3;
            } else if (maxWidth < 1) {
                maxWidth = 1;
            }

            // TODO #106 we can speed this up by caching
            TruffleEntryPointStrategy strategy = new TruffleEntryPointStrategy(context, mutatedNode, mutationNode, subtreeStrategy, new CreationConfiguration(maxDepth, maxWidth, configuration.getMaxWeight() - remainingNodeWeight));
            strategy.setDataFlowGraph(DataFlowUtil.constructDataFlowGraph(information, mutatedNode, mutationNode, signature));
            strategy.setChooser(mutationChoice);
            if (strategy.canCreateNext()) {
                try {
                    // TODO #106 -> When we have multi objective optimization we can update the "maxWeight" here to restrict the search space during the run
                    if (check != null) {
                        strategy.getManagedClasses().removeIf(x -> !check.isAssignableFrom(x) && (x.getSuperclass().getEnclosingClass() == null || !check.isAssignableFrom(x.getSuperclass().getEnclosingClass())));
                    }
                    Node newNode = strategy.next();
                    if (check != null && newNode != null && ExtendedNodeUtil.flatten(newNode).noneMatch(x -> check.isAssignableFrom(x.getClass()) || (x.getClass().getSuperclass().getEnclosingClass() != null && check.isAssignableFrom(x.getClass().getSuperclass().getEnclosingClass())))) {
                        System.out.println("Failed to inject the desired pattern. Not valid");
                        newNode = null;
                    }
                    if (newNode != null) {
                        mutationNode.replace(newNode);
                        NodeWrapper.clearCache();
                        System.out.println("MUTATED WITH DEPTH " + ExtendedNodeUtil.maxDepth(mutatedNode) + " of allowed " + configuration.getMaxDepth());
                        System.out.println(NodeWrapper.wrap(newNode).humanReadableTree());
                        return mutatedNode;
                    } else {
                        logger.error("Class can't be mutated " + mutationNode.getClass());
                    }
                } catch (Exception e) {
                    logger.error("Mutation failed", e);
                }
            }
        }
        return null;
    }

    public void setAnalyticsService(TruffleGraphAnalytics analyticsService) {
        this.analyticsService = analyticsService;
    }

    @Required
    @Override
    public void setSubtreeStrategy(TruffleHierarchicalStrategy<Node> subtreeStrategy) {
        this.subtreeStrategy = subtreeStrategy;
    }

    public void setSelector(ContractualTruffleTreeSelector selector) {
        this.selector = selector;
    }

    @Required
    @Override
    public void setFullTreeStrategy(TruffleCombinedStrategy<Node> fullTreeStrategy) {
        this.fullTreeStrategy = fullTreeStrategy;
    }

    public void setMutationChoice(ChooseOption<Class> mutationChoice) {
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
                    setSelector((ContractualTruffleTreeSelector) descriptor.getValue());
                    break;
                case "chooser":
                    setMutationChoice((ChooseOption<Class>) descriptor.getValue());
                    break;
            }
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    public void setCheck(Class checkClass) {
        this.check = checkClass;
    }
}
