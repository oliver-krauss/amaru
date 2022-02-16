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
import at.fh.hagenberg.machinelearning.algorithm.ga.Selector;
import at.fh.hagenberg.machinelearning.analytics.TruffleGraphAnalytics;
import at.fh.hagenberg.machinelearning.analytics.graph.nodes.*;
import at.fh.hagenberg.machinelearning.core.Solution;
import at.fh.hagenberg.machinelearning.core.SolutionGene;
import at.fh.hagenberg.machinelearning.core.options.Descriptor;
import at.fh.hagenberg.util.Pair;
import com.oracle.truffle.api.nodes.Node;
import org.springframework.beans.factory.annotation.Required;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by Oliver Krauss on 10.02.2017.
 */
public class TrufflePatternAdheringTreeCrossover implements ConfigurableCrossover<TruffleOptimizationSolution, TruffleOptimizationProblem> {

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
     * Strategy that we can use to load the requirements (e.g. patterns and data flow)
     */
    private TruffleMasterStrategy masterStrategy;

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

    public TrufflePatternAdheringTreeCrossover() {
        // default to everything being safe
        safelyAssignableToplevelClasses.add(Object.class);
    }

    @Override
    public Solution<TruffleOptimizationSolution, TruffleOptimizationProblem> breed(List<Solution<TruffleOptimizationSolution, TruffleOptimizationProblem>> list, Selector<TruffleOptimizationSolution, TruffleOptimizationProblem> selector) {
        Solution<TruffleOptimizationSolution, TruffleOptimizationProblem> a = selector.select(list);
        Solution<TruffleOptimizationSolution, TruffleOptimizationProblem> b = selector.select(list);
        // ensure that we don't do a self-crossover
        int retries = 3;
        while (b.getId().equals(a.getId()) && retries > 0) {
            b = selector.select(list);
            retries--;
        }
        TruffleOptimizationProblem problem = a.getSolutionGenes().get(0).getProblemGenes().get(0).getGene();

        Solution<TruffleOptimizationSolution, TruffleOptimizationProblem> solution = new Solution<>();
        TruffleOptimizationSolution truffleSolution = breed(a.getSolutionGenes().get(0).getGene(), b.getSolutionGenes().get(0).getGene());
        solution.addGene(new SolutionGene<>(truffleSolution, a.getSolutionGenes().get(0).getProblemGenes()));

        return solution;
    }

    @Override
    public TruffleOptimizationSolution breed(TruffleOptimizationSolution a, TruffleOptimizationSolution b) {
        long start = ProfileKeeper.profiler.start();
        // ensure that we get no problems with modding the original ASTs
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
     * TODO #25 this crossover should really be expressed as a selection strategy
     * This function selects one node from left and one from right, tries to match them and switches out the trees, returning the LEFT tree with the crossed subtree from right
     *
     * @param a             left crossover tree
     * @param b             right crossover tree
     * @param configuration configuration for the subtree creation protocol
     * @return a copy of the left node with a sub-tree removed and selected from the right tree
     */
    public Node crossover(TruffleOptimizationSolution a, TruffleOptimizationSolution b, CreationConfiguration configuration) {
        // deep copy, because otherwise we would have nodes in multiple trees (right) or accidentally modify original versions for further use (left)
        Node left = a.getNode().deepCopy();
        Node right = b.getNode().deepCopy();
        Logger.log(Logger.LogLevel.DEBUG, "cross");
        Logger.log(Logger.LogLevel.TRACE, left);
        Logger.log(Logger.LogLevel.TRACE, right);

        // try to find a valid crossoverpoint 10 times
        int hardTries = 5; // attempts that do a fully random crossover
        int backupTries = 5; // attempts that try to back up via doing non-commital merges (those that don't have antipatterns in the first place)

        Map<Node, LoadedRequirementInformation> leftRQI = masterStrategy.loadRequirements(left);
        Map<Node, LoadedRequirementInformation> rightRQI = masterStrategy.loadRequirements(right);

        // exclude the root from being replaced (that would not be a crossover!)
        leftRQI.remove(left);
        ArrayList<Node> leftChoices = new ArrayList<>(leftRQI.keySet());
        ArrayList<Node> rightChoices = new ArrayList<>(rightRQI.keySet());


        // our target is <= amount of failed antipattern locations.
        long fails = leftRQI.values().stream().filter(LoadedRequirementInformation::isFailed).count();

        Node leftCrossoverPoint = selector.selectSubtreeFromChoices(leftChoices);
        Node rightCrossoverPoint = selector.selectSubtreeFromChoices(rightChoices);

        while (hardTries > 0 && !replaceIsSafe(left, right, leftCrossoverPoint, rightCrossoverPoint, configuration, fails)) {
            if (RandomUtil.random.nextDouble() < 0.5) {
                leftCrossoverPoint = selector.selectSubtreeFromChoices(leftChoices);
            } else {
                rightCrossoverPoint = selector.selectSubtreeFromChoices(rightChoices);
            }
            hardTries--;
        }
        if (hardTries <= 0) {
            Logger.log(Logger.LogLevel.DEBUG, "Conducting backup crossover with locations that have no patterns anyways.");
            leftChoices = (ArrayList<Node>) leftRQI.entrySet().stream().filter(e -> e.getValue().getRequirementInformation().fullfillsAll()).map(Map.Entry::getKey).collect(Collectors.toList());
            rightChoices = (ArrayList<Node>) leftRQI.entrySet().stream().filter(e -> e.getValue().getRequirementInformation().fullfillsAll()).map(Map.Entry::getKey).collect(Collectors.toList());
            while (backupTries > 0 && !replaceIsSafe(left, right, leftCrossoverPoint, rightCrossoverPoint, configuration, fails)) {
                if (RandomUtil.random.nextDouble() < 0.5) {
                    leftCrossoverPoint = selector.selectSubtreeFromChoices(leftChoices);
                } else {
                    rightCrossoverPoint = selector.selectSubtreeFromChoices(rightChoices);
                }
                backupTries--;
            }
        }


        // try to actually crossbreed
        if (replaceIsSafe(left, right, leftCrossoverPoint, rightCrossoverPoint, configuration, fails)) {
            Logger.log(Logger.LogLevel.INFO, "Conducting Crossover");
            leftCrossoverPoint.replace(rightCrossoverPoint.deepCopy());
        }

        Logger.log(Logger.LogLevel.DEBUG, "cross finished");
        Logger.log(Logger.LogLevel.TRACE, left);
        return left;
    }

    // TODO #191 -> this will become irrelevant
    private boolean replaceIsSafe(Node leftSource, Node rightSource, Node left, Node right, CreationConfiguration configuration, long fails) {
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

        if (left.isSafelyReplaceableBy(right)) {
            // simulate a crossover and see if the fail-count increased
            Node leftTree = leftSource.deepCopy();
            Node copyRight = right.deepCopy();
            leftTree.adoptChildren();
            // find left again
            Node newLeft = findCopy(left, leftTree, leftSource);
            newLeft.replace(copyRight);
            leftTree.adoptChildren();

            Map<Node, LoadedRequirementInformation> newReqs = masterStrategy.loadRequirements(leftTree);

            // replace only works if we violate fewer patterns than before
            return  fails >= newReqs.values().stream().filter(LoadedRequirementInformation::isFailed).count();
        }

        return false;
    }

    private Node findCopy(Node left, Node leftTree, Node leftOrigin) {
        int i = ExtendedNodeUtil.flatten(leftOrigin).collect(Collectors.toList()).indexOf(left);
        return ExtendedNodeUtil.flatten(leftTree).collect(Collectors.toList()).get(i);
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

    @Required
    public void setMasterStrategy(TruffleMasterStrategy masterStrategy) {
        this.masterStrategy = masterStrategy;
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
