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
import at.fh.hagenberg.aist.gce.optimization.util.strategy.CreationConfiguration;
import at.fh.hagenberg.aist.gce.optimization.util.strategy.CreationInformation;
import at.fh.hagenberg.aist.gce.optimization.util.strategy.RequirementInformation;
import at.fh.hagenberg.aist.gce.optimization.util.strategy.TruffleVerifyingStrategy;
import at.fh.hagenberg.machinelearning.algorithm.ga.Selector;
import at.fh.hagenberg.machinelearning.analytics.TruffleGraphAnalytics;
import at.fh.hagenberg.machinelearning.analytics.graph.nodes.*;
import at.fh.hagenberg.machinelearning.core.Solution;
import at.fh.hagenberg.machinelearning.core.SolutionGene;
import at.fh.hagenberg.machinelearning.core.options.Descriptor;
import at.fh.hagenberg.util.Pair;
import com.oracle.truffle.api.nodes.Node;
import org.springframework.beans.factory.annotation.Required;

import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Created by Oliver Krauss on 10.02.2017.
 */
public class TruffleTreeCrossover implements ConfigurableCrossover<TruffleOptimizationSolution, TruffleOptimizationProblem> {

    /**
     * Analytics service for analyzing a run
     */
    private TruffleGraphAnalytics analyticsService;

    /**
     * Selection strategy for point in tree that will be mutated
     */
    private ContractualTruffleTreeSelector selector = new RandomTruffleTreeSelector();

    /**
     * Classes that are safe for toplevel-replacement of nodes
     */
    private Collection<Class> safelyAssignableToplevelClasses = new ArrayList<>();

    /**
     * Utility class for weighing
     */
    private NodeWrapperWeightUtil weightUtil;

    private NodeWrapperWeightUtil getWeightUtil(TruffleOptimizationSolution solution) {
        if (weightUtil == null) {
            weightUtil = new NodeWrapperWeightUtil(solution.getProblem().getLanguage());
            selector.setUtil(weightUtil);
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

    public TruffleTreeCrossover() {
        // default to everything being safe
        safelyAssignableToplevelClasses.add(Object.class);
    }

    @Override
    public Solution<TruffleOptimizationSolution, TruffleOptimizationProblem> breed(List<Solution<TruffleOptimizationSolution, TruffleOptimizationProblem>> list, Selector<TruffleOptimizationSolution, TruffleOptimizationProblem> selector) {
        Solution<TruffleOptimizationSolution, TruffleOptimizationProblem> a = selector.select(list);
        Solution<TruffleOptimizationSolution, TruffleOptimizationProblem> b = selector.select(list);
        TruffleOptimizationProblem problem = a.getSolutionGenes().get(0).getProblemGenes().get(0).getGene();

        Solution<TruffleOptimizationSolution, TruffleOptimizationProblem> solution = new Solution<>();
        TruffleOptimizationSolution truffleSolution = breed(a.getSolutionGenes().get(0).getGene(), b.getSolutionGenes().get(0).getGene());
        solution.addGene(new SolutionGene<>(truffleSolution, a.getSolutionGenes().get(0).getProblemGenes()));

        return solution;
    }

    @Override
    public TruffleOptimizationSolution breed(TruffleOptimizationSolution a, TruffleOptimizationSolution b) {
        long start = ProfileKeeper.profiler.start();
        Node left = a.getNode();
        Node right = b.getNode();
        Node crossedNode = left;
        try {
            crossedNode = crossover(a, b, a.getProblem().getConfiguration());
            System.out.println("CROSSED WITH DEPTH " + ExtendedNodeUtil.maxDepth(crossedNode) + " of allowed " + a.getProblem().getConfiguration().getMaxDepth());
        } catch (Exception e) {
            // make sure one failed crossover doesn't kill our run
            System.out.println("Crossover failed with nodes " + NodeWrapper.wrap(left).getHash() + " and " + NodeWrapper.wrap(right).getHash());
            e.printStackTrace();
        }
        TruffleOptimizationSolution truffleSolution = new TruffleOptimizationSolution(crossedNode, a.getProblem(), this);
        start = ProfileKeeper.profiler.profile("TruffleTreeCrossover.crossover", start);
        if (analyticsService != null) {
            OperationNode operation = new OperationNode("crossover");
            operation.addInput(a.getTree());
            operation.addInput(b.getTree());
            operation.addOutput(NodeWrapper.wrap(crossedNode));
            analyticsService.saveOperation(operation);
        }

        ProfileKeeper.profiler.profile("TruffleTreeCrossover.crossoverLOG", start);
        return truffleSolution;
    }

    /**
     * TODO #25 this is essentially a random crossover (with possible smart selection of nodes). We also wan't context preserving crossover etc.
     * This function selects one node from left and one from right, tries to match them and switches out the trees, returning the LEFT tree with the crossed subtree from right
     *
     * @param a             left crossover tree
     * @param b             right crossover tree
     * @param configuration configuration for the subtree creation protocol
     * @return a copy of the left node with a sub-tree removed and selected from the right tree
     */
    public Node crossover(TruffleOptimizationSolution a, TruffleOptimizationSolution b, CreationConfiguration configuration) {
        Node left = a.getNode();
        Node right = b.getNode();
        Logger.log(Logger.LogLevel.DEBUG, "cross");
        Logger.log(Logger.LogLevel.TRACE, left);
        Logger.log(Logger.LogLevel.TRACE, right);

        // deep copy, because otherwise we would have nodes in multiple trees (right) or modify original versions for further use (left)
        left = left.deepCopy();

        // try to find a valid crossoverpoint 10 times
        Integer tries = 10;

        Node rightCrossoverPoint;
        Pair<Node, Double> leftSelection = weightSensitiveSelection(left, a, tries);
        Node leftCrossoverPoint = leftSelection.getKey();
        Double leftWeight =leftSelection.getValue();
        DataFlowGraph dataFlowGraph = DataFlowUtil.constructDataFlowGraph(getTruffleLanguageInformation(a), left, leftCrossoverPoint, TruffleFunctionAnalyzer.getSignature(a.getNode().getRootNode()));
        NodeWrapper leftWrap = NodeWrapper.wrap(left);
        NodeWrapper leftPointWrap = NodeWrapper.wrap(leftCrossoverPoint);

        // TODO #229 replace with "get all valid crossover points from left choice" / also remove the weight sensitive selection
        rightCrossoverPoint = selector.selectSubtree(right, new CreationInformation(leftWrap, leftPointWrap, new RequirementInformation(null), dataFlowGraph, null, 0, a.getProblem().getConfiguration(), leftWeight));
        while (tries > 0 && !replaceIsSafe(left, leftCrossoverPoint, rightCrossoverPoint, configuration)) {
            if (RandomUtil.random.nextDouble() < 0.5) {
                leftSelection = weightSensitiveSelection(left, a, tries);
                leftCrossoverPoint = leftSelection.getKey();
                leftPointWrap = NodeWrapper.wrap(leftCrossoverPoint);
                leftWeight = leftSelection.getValue();
                dataFlowGraph = DataFlowUtil.constructDataFlowGraph(getTruffleLanguageInformation(a), left, leftCrossoverPoint, TruffleFunctionAnalyzer.getSignature(a.getNode().getRootNode()));
                // since the available data items are changing we MUST also re-select the right crossover point
                rightCrossoverPoint = selector.selectSubtree(right, new CreationInformation(leftWrap, leftPointWrap, new RequirementInformation(null), dataFlowGraph, null, 0, a.getProblem().getConfiguration(), leftWeight));
            } else {
                rightCrossoverPoint = selector.selectSubtree(right, new CreationInformation(leftWrap, leftPointWrap, new RequirementInformation(null), dataFlowGraph, null, 0, a.getProblem().getConfiguration(), leftWeight));
            }
            tries--;
        }

        // try to actually crossbreed
        if (replaceIsSafe(left, leftCrossoverPoint, rightCrossoverPoint, configuration)) {
            if (leftCrossoverPoint.getParent() == null) {
                left = rightCrossoverPoint;
            } else {
                leftCrossoverPoint.replace(rightCrossoverPoint.deepCopy());
            }
        }

        Logger.log(Logger.LogLevel.DEBUG, "cross finished");
        Logger.log(Logger.LogLevel.TRACE, left);
        return left;
    }

    /**
     * Helper function that enforces that the crossover still has a chance to find a right crossover point
     * Note that the function can still fail to ensure that the right side has a chance, but it reduces the likelyhood
     * @return crossover point with a chance for a valid right side equivalent
     */
    private Pair<Node, Double> weightSensitiveSelection(Node left, TruffleOptimizationSolution a, Integer tries) {
        Node leftCrossoverPoint = selector.selectSubtree(left);
        double leftWeight = Double.MAX_VALUE;
        if (a.getProblem().getConfiguration().getMaxWeight() < Double.MAX_VALUE && getWeightUtil(a).getTli().learned()) {
            leftWeight = weightUtil.weight(a.getTree()) - weightUtil.weight(leftCrossoverPoint);
            while ((leftWeight - weightUtil.getAverage()) > a.getProblem().getConfiguration().getMaxWeight() && tries > 0) {
                // if the weight is too large to let the right side select anything re-select the crossover point
                leftCrossoverPoint = selector.selectSubtree(left);
                leftWeight = weightUtil.weight(a.getTree()) - weightUtil.weight(leftCrossoverPoint);
                tries--;
            }
        }
        return new Pair<>(leftCrossoverPoint, leftWeight);
    }

    // TODO #191 -> this will become irrelevant
    private boolean replaceIsSafe(Node leftSource, Node left, Node right, CreationConfiguration configuration) {
        if (left == null || right == null) {
            return false;
        }
        // ensure that crossover doens't cross too deep
        if (ExtendedNodeUtil.getRelativeDepth(leftSource, left) + ExtendedNodeUtil.maxDepth(right) - 1 > configuration.getMaxDepth()) {
            return false;
        }

        if (left.getParent() == null) {
            return safelyAssignableToplevelClasses.stream().anyMatch(x -> x.isAssignableFrom(right.getClass()));
        }
        return left.isSafelyReplaceableBy(right);
    }

    public void setAnalyticsService(TruffleGraphAnalytics analyticsService) {
        this.analyticsService = analyticsService;
    }

    @Required
    public void setSelector(ContractualTruffleTreeSelector selector) {
        this.selector = selector;
    }

    public void setSafelyAssignableToplevelClasses(Collection<Class> safelyAssignableToplevelClasses) {
        this.safelyAssignableToplevelClasses = safelyAssignableToplevelClasses;
    }


    @Override
    public Map<String, Descriptor> getOptions() {
        Map<String, Descriptor> options = new HashMap<>();
        options.put("selector", new Descriptor<>(selector));
        return options;
    }

    @Override
    public boolean setOption(String name, Descriptor descriptor) {
        try {
            switch (name) {
                case "selector":
                    setSelector((ContractualTruffleTreeSelector) descriptor.getValue());
                    break;
            }
        } catch (Exception e) {
            return false;
        }
        return true;
    }
}
