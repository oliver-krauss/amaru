/*
 * Copyright (c) 2022 the original author or authors.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

package at.fh.hagenberg.aist.gce.benchmark.mutators;

import at.fh.hagenberg.aist.gce.minic.nodes.base.MinicExpressionNode;
import at.fh.hagenberg.aist.gce.minic.nodes.impl.cast.MinicToCharNodeFactory;
import at.fh.hagenberg.aist.gce.minic.nodes.impl.cast.MinicToDoubleNodeFactory;
import at.fh.hagenberg.aist.gce.minic.nodes.impl.cast.MinicToFloatNodeFactory;
import at.fh.hagenberg.aist.gce.minic.nodes.impl.cast.MinicToIntNodeFactory;
import at.fh.hagenberg.aist.gce.minic.nodes.impl.function.MinicInvokeNodeFactory;
import at.fh.hagenberg.aist.gce.minic.nodes.impl.function.MinicReadFunctionArgumentNodeFactory;
import at.fh.hagenberg.aist.gce.minic.nodes.impl.literals.MinicSimpleLiteralNode;
import at.fh.hagenberg.aist.gce.minic.nodes.impl.op.arith.floating.MinicDoubleArithmeticNodeFactory;
import at.fh.hagenberg.aist.gce.minic.nodes.impl.op.arith.floating.MinicFloatArithmeticNodeFactory;
import at.fh.hagenberg.aist.gce.minic.nodes.impl.op.arith.integer.signed.MinicCharArithmeticNodeFactory;
import at.fh.hagenberg.aist.gce.minic.nodes.impl.op.arith.integer.signed.MinicIntArithmeticNode;
import at.fh.hagenberg.aist.gce.minic.nodes.impl.op.arith.integer.signed.MinicIntArithmeticNodeFactory;
import at.fh.hagenberg.aist.gce.minic.nodes.impl.op.logical.floating.MinicDoubleLogicalNode;
import at.fh.hagenberg.aist.gce.minic.nodes.impl.op.logical.floating.MinicFloatLogicalNodeFactory;
import at.fh.hagenberg.aist.gce.minic.nodes.impl.op.rel.floating.MinicDoubleRelationalNodeFactory;
import at.fh.hagenberg.aist.gce.minic.nodes.impl.op.rel.floating.MinicFloatRelationalNodeFactory;
import at.fh.hagenberg.aist.gce.minic.nodes.impl.op.unary.floating.MinicDoubleUnaryNodeFactory;
import at.fh.hagenberg.aist.gce.minic.nodes.impl.op.unary.floating.MinicFloatUnaryNodeFactory;
import at.fh.hagenberg.aist.gce.minic.nodes.impl.vars.MinicReadArrayNodeFactory;
import at.fh.hagenberg.aist.gce.minic.nodes.impl.vars.MinicReadGlobalArrayNodeFactory;
import at.fh.hagenberg.aist.gce.minic.nodes.impl.vars.MinicReadGlobalNodeFactory;
import at.fh.hagenberg.aist.gce.minic.nodes.impl.vars.MinicReadNodeFactory;
import at.fh.hagenberg.aist.gce.minic.types.floating.MinicDoubleNode;
import at.fh.hagenberg.aist.gce.minic.types.floating.MinicFloatNode;
import at.fh.hagenberg.aist.gce.minic.types.integer.signed.MinicCharNode;
import at.fh.hagenberg.aist.gce.minic.types.integer.signed.MinicIntNode;
import at.fh.hagenberg.aist.gce.optimization.ProfileKeeper;
import at.fh.hagenberg.aist.gce.optimization.TruffleOptimizationProblem;
import at.fh.hagenberg.aist.gce.optimization.TruffleOptimizationSolution;
import at.fh.hagenberg.aist.gce.optimization.operators.ConfigurableMutator;
import at.fh.hagenberg.aist.gce.optimization.operators.selection.ContractualTruffleTreeSelector;
import at.fh.hagenberg.aist.gce.optimization.operators.selection.RandomTruffleTreeSelector;
import at.fh.hagenberg.aist.gce.optimization.util.ExtendedNodeUtil;
import at.fh.hagenberg.aist.gce.optimization.util.JavaAssistUtil;
import at.fh.hagenberg.aist.gce.optimization.util.TruffleLanguageInformation;
import at.fh.hagenberg.aist.gce.optimization.util.strategy.TruffleCombinedStrategy;
import at.fh.hagenberg.aist.gce.optimization.util.strategy.TruffleHierarchicalStrategy;
import at.fh.hagenberg.aist.gce.optimization.util.strategy.selection.ChooseOption;
import at.fh.hagenberg.aist.gce.optimization.util.strategy.selection.RandomChooser;
import at.fh.hagenberg.machinelearning.analytics.TruffleGraphAnalytics;
import at.fh.hagenberg.machinelearning.analytics.graph.nodes.NodeWrapper;
import at.fh.hagenberg.machinelearning.analytics.graph.nodes.NodeWrapperWeightUtil;
import at.fh.hagenberg.machinelearning.analytics.graph.nodes.OperationNode;
import at.fh.hagenberg.machinelearning.core.Solution;
import at.fh.hagenberg.machinelearning.core.SolutionGene;
import at.fh.hagenberg.machinelearning.core.options.Descriptor;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.MaterializedFrame;
import com.oracle.truffle.api.nodes.Node;
import org.springframework.beans.factory.annotation.Required;
import science.aist.seshat.Logger;

import java.util.*;
import java.util.stream.Collectors;

public class TruffleDivisionFixingMutator implements ConfigurableMutator<TruffleOptimizationSolution, TruffleOptimizationProblem> {

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
            boolean hasDouble = ExtendedNodeUtil.flatten(mutatedNode).anyMatch(x -> x.getClass().getName().contains("Div"));
            if (!hasDouble) {
                throw new RuntimeException("NO DOUBLE TO FIX");
            }
            mutatedNode = mutatedNode.deepCopy();
            List<Node> divNode = ExtendedNodeUtil.flatten(mutatedNode).filter(x -> x.getClass().getName().contains("Div")).collect(Collectors.toList());
            int switches = random.nextInt(3) + 1;
            while (!divNode.isEmpty() && switches > 0) {
                switches--;
                Node replaceMe = divNode.get(random.nextInt(divNode.size()));
                Node replacement = replace(replaceMe);
                replaceMe.replace(replacement);
                divNode = ExtendedNodeUtil.flatten(mutatedNode).filter(x -> x.getClass().getName().contains("Div")).collect(Collectors.toList());
            }
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

    private Node replace(Node node) {
        Node replacement = null;
        int type = random.nextInt(2);
        Iterator<Node> children = node.getChildren().iterator();
        if (node instanceof MinicIntArithmeticNodeFactory.MinicIntDivNodeGen) {
            switch (type) {
                case 0:
                    replacement = MinicIntArithmeticNodeFactory.MinicIntAddNodeGen.create((MinicIntNode) children.next(), (MinicIntNode) children.next());
                    break;
                default:
                    replacement = MinicIntArithmeticNodeFactory.MinicIntSubNodeGen.create((MinicIntNode) children.next(), (MinicIntNode) children.next());
            }
        } else if (node instanceof MinicDoubleArithmeticNodeFactory.MinicDoubleDivNodeGen) {
            switch (type) {
                case 0:
                    replacement = MinicDoubleArithmeticNodeFactory.MinicDoubleAddNodeGen.create((MinicDoubleNode) children.next(), (MinicDoubleNode) children.next());
                    break;
                default:
                    replacement = MinicDoubleArithmeticNodeFactory.MinicDoubleSubNodeGen.create((MinicDoubleNode) children.next(), (MinicDoubleNode) children.next());
            }
        } else if (node instanceof MinicFloatArithmeticNodeFactory.MinicFloatDivNodeGen) {
            switch (type) {
                case 0:
                    replacement = MinicFloatArithmeticNodeFactory.MinicFloatAddNodeGen.create((MinicFloatNode) children.next(), (MinicFloatNode) children.next());
                    break;
                default:
                    replacement = MinicFloatArithmeticNodeFactory.MinicFloatSubNodeGen.create((MinicFloatNode) children.next(), (MinicFloatNode) children.next());
            }
        } else if (node instanceof MinicCharArithmeticNodeFactory.MinicCharDivNodeGen) {
            switch (type) {
                case 0:
                    replacement = MinicCharArithmeticNodeFactory.MinicCharAddNodeGen.create((MinicCharNode) children.next(), (MinicCharNode) children.next());
                    break;
                default:
                    replacement = MinicCharArithmeticNodeFactory.MinicCharSubNodeGen.create((MinicCharNode) children.next(), (MinicCharNode) children.next());
            }
        } else {
            throw new RuntimeException("CAN'T HANDLE THIS NODE TYPE");
        }
        return replacement;

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