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
import at.fh.hagenberg.aist.gce.minic.nodes.impl.op.logical.floating.MinicDoubleLogicalNode;
import at.fh.hagenberg.aist.gce.minic.nodes.impl.op.logical.floating.MinicFloatLogicalNode;
import at.fh.hagenberg.aist.gce.minic.nodes.impl.op.logical.floating.MinicFloatLogicalNodeFactory;
import at.fh.hagenberg.aist.gce.minic.nodes.impl.op.rel.floating.MinicDoubleRelationalNodeFactory;
import at.fh.hagenberg.aist.gce.minic.nodes.impl.op.rel.floating.MinicFloatRelationalNodeFactory;
import at.fh.hagenberg.aist.gce.minic.nodes.impl.op.unary.floating.MinicDoubleUnaryNodeFactory;
import at.fh.hagenberg.aist.gce.minic.nodes.impl.op.unary.floating.MinicFloatUnaryNodeFactory;
import at.fh.hagenberg.aist.gce.minic.nodes.impl.vars.MinicReadArrayNodeFactory;
import at.fh.hagenberg.aist.gce.minic.nodes.impl.vars.MinicReadGlobalArrayNodeFactory;
import at.fh.hagenberg.aist.gce.minic.nodes.impl.vars.MinicReadGlobalNodeFactory;
import at.fh.hagenberg.aist.gce.minic.nodes.impl.vars.MinicReadNodeFactory;
import at.fh.hagenberg.aist.gce.minic.types.floating.MinicFloatNode;
import at.fh.hagenberg.aist.gce.minic.types.integer.signed.MinicCharNode;
import at.fh.hagenberg.aist.gce.minic.types.integer.signed.MinicIntNode;
import at.fh.hagenberg.aist.gce.optimization.ProfileKeeper;
import at.fh.hagenberg.aist.gce.optimization.TruffleOptimizationProblem;
import at.fh.hagenberg.aist.gce.optimization.TruffleOptimizationSolution;
import at.fh.hagenberg.aist.gce.optimization.operators.ConfigurableMutator;
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
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.MaterializedFrame;
import com.oracle.truffle.api.nodes.Node;
import org.springframework.beans.factory.annotation.Required;
import science.aist.seshat.Logger;

import java.util.*;
import java.util.stream.Collectors;

public class TruffleDoubleFixingMutator implements ConfigurableMutator<TruffleOptimizationSolution, TruffleOptimizationProblem> {

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
            boolean hasDouble = ExtendedNodeUtil.flatten(mutatedNode).anyMatch(x -> x.getClass().getName().contains("Double"));
            if (!hasDouble) {
                throw new RuntimeException("NO DOUBLE TO FIX");
            }
            mutatedNode = mutatedNode.deepCopy();
            Optional<Node> doubleNode = ExtendedNodeUtil.flatten(mutatedNode).filter(x -> x.getClass().getName().contains("Double")).findFirst();
            while (doubleNode.isPresent()) {
                Node replaceMe = doubleNode.get();
                Node replacement = replace(replaceMe);
                replaceMe.replace(replacement);
                doubleNode = ExtendedNodeUtil.flatten(mutatedNode).filter(x -> x.getClass().getName().contains("Double")).findFirst();
            }
            int i = 345;
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
        if (node instanceof MinicDoubleArithmeticNodeFactory.MinicDoubleAddNodeGen) {
            Iterator<Node> children = node.getChildren().iterator();
            replacement = MinicFloatArithmeticNodeFactory.MinicFloatAddNodeGen.create((MinicFloatNode) replace(children.next()), (MinicFloatNode) replace(children.next()));
        } else if (node instanceof MinicDoubleArithmeticNodeFactory.MinicDoubleModNodeGen) {
            Iterator<Node> children = node.getChildren().iterator();
            replacement = MinicFloatArithmeticNodeFactory.MinicFloatModNodeGen.create((MinicFloatNode) replace(children.next()), (MinicFloatNode) replace(children.next()));
        } else if (node instanceof MinicDoubleArithmeticNodeFactory.MinicDoubleMulNodeGen) {
            Iterator<Node> children = node.getChildren().iterator();
            replacement = MinicFloatArithmeticNodeFactory.MinicFloatMulNodeGen.create((MinicFloatNode) replace(children.next()), (MinicFloatNode) replace(children.next()));
        } else if (node instanceof MinicDoubleArithmeticNodeFactory.MinicDoubleSubNodeGen) {
            Iterator<Node> children = node.getChildren().iterator();
            replacement = MinicFloatArithmeticNodeFactory.MinicFloatSubNodeGen.create((MinicFloatNode) replace(children.next()), (MinicFloatNode) replace(children.next()));
        } else if (node instanceof MinicDoubleArithmeticNodeFactory.MinicDoubleDivNodeGen) {
            Iterator<Node> children = node.getChildren().iterator();
            replacement = MinicFloatArithmeticNodeFactory.MinicFloatDivNodeGen.create((MinicFloatNode) replace(children.next()), (MinicFloatNode) replace(children.next()));
        } else if (node instanceof MinicInvokeNodeFactory.MinicInvokeDoubleNodeGen) {
            replacement = MinicInvokeNodeFactory.MinicInvokeFloatNodeGen.create(
                    (MinicExpressionNode[]) JavaAssistUtil.safeFieldAccess("argumentNodes", node),
                    (MinicExpressionNode) JavaAssistUtil.safeFieldAccess("functionNode_", node)
            );
        } else if (node instanceof MinicReadFunctionArgumentNodeFactory.MinicReadFunctionArgumentDoubleNodeGen) {
            replacement = MinicReadFunctionArgumentNodeFactory.MinicReadFunctionArgumentFloatNodeGen.create((int) JavaAssistUtil.safeFieldAccess("index", node));
        } else if (node instanceof MinicToDoubleNodeFactory.MinicFloatToDoubleNodeGen) {
            replacement = node.getChildren().iterator().next(); // float does not need to be cast to float
        } else if (node instanceof MinicToFloatNodeFactory.MinicDoubleToFloatNodeGen) {
            replacement = replace(node.getChildren().iterator().next()); // float does not need to be cast to float
        } else if (node instanceof MinicToIntNodeFactory.MinicDoubleToIntNodeGen) {
            replacement = MinicToIntNodeFactory.MinicFloatToIntNodeGen.create((MinicFloatNode) replace(node.getChildren().iterator().next()));
        } else if (node instanceof MinicToCharNodeFactory.MinicDoubleToCharNodeGen) {
            replacement = MinicToCharNodeFactory.MinicFloatToCharNodeGen.create((MinicFloatNode) replace(node.getChildren().iterator().next()));
        } else if (node instanceof MinicToDoubleNodeFactory.MinicCharToDoubleNodeGen) {
            replacement = MinicToFloatNodeFactory.MinicCharToFloatNodeGen.create((MinicCharNode) node.getChildren().iterator().next());
        } else if (node instanceof MinicToDoubleNodeFactory.MinicIntToDoubleNodeGen) {
            replacement = MinicToFloatNodeFactory.MinicIntToFloatNodeGen.create((MinicIntNode) node.getChildren().iterator().next());
        } else if (node instanceof MinicSimpleLiteralNode.MinicDoubleLiteralNode) {
            replacement = new MinicSimpleLiteralNode.MinicFloatLiteralNode(((Double) JavaAssistUtil.safeFieldAccess("value", node)).floatValue());
        } else if (node instanceof MinicReadGlobalArrayNodeFactory.MinicDoubleArrayReadGlobalNodeGen) {
            replacement = MinicReadGlobalArrayNodeFactory.MinicFloatArrayReadGlobalNodeGen.create(
                    (MinicIntNode[]) JavaAssistUtil.safeFieldAccess("arrayPosition", node),
                    (FrameSlot) JavaAssistUtil.safeFieldAccess("slot", node),
                    (MaterializedFrame) JavaAssistUtil.safeFieldAccess("globalFrame", node));
        } else if (node instanceof MinicReadGlobalNodeFactory.MinicDoubleReadGlobalNodeGen) {
            replacement = MinicReadGlobalNodeFactory.MinicFloatReadGlobalNodeGen.create(
                    (FrameSlot) JavaAssistUtil.safeFieldAccess("slot", node),
                    (MaterializedFrame) JavaAssistUtil.safeFieldAccess("globalFrame", node));
        } else if (node instanceof MinicReadArrayNodeFactory.MinicDoubleArrayReadNodeGen) {
            replacement = MinicReadArrayNodeFactory.MinicFloatArrayReadNodeGen.create(
                    (MinicIntNode[]) JavaAssistUtil.safeFieldAccess("arrayPosition", node),
                    (FrameSlot) JavaAssistUtil.safeFieldAccess("slot", node));
        } else if (node instanceof MinicReadNodeFactory.MinicDoubleReadNodeGen) {
            replacement = MinicReadNodeFactory.MinicFloatReadNodeGen.create((FrameSlot) JavaAssistUtil.safeFieldAccess("slot", node));
        } else if (node instanceof MinicDoubleRelationalNodeFactory.MinicDoubleGtNodeGen) {
            Iterator<Node> children = node.getChildren().iterator();
            replacement = MinicFloatRelationalNodeFactory.MinicFloatGtNodeGen.create((MinicFloatNode) replace(children.next()), (MinicFloatNode) replace(children.next()));
        } else if (node instanceof MinicDoubleRelationalNodeFactory.MinicDoubleGtENodeGen) {
            Iterator<Node> children = node.getChildren().iterator();
            replacement = MinicFloatRelationalNodeFactory.MinicFloatGtENodeGen.create((MinicFloatNode) replace(children.next()), (MinicFloatNode) replace(children.next()));
        } else if (node instanceof MinicDoubleRelationalNodeFactory.MinicDoubleEqualsNodeGen) {
            Iterator<Node> children = node.getChildren().iterator();
            replacement = MinicFloatRelationalNodeFactory.MinicFloatEqualsNodeGen.create((MinicFloatNode) replace(children.next()), (MinicFloatNode) replace(children.next()));
        } else if (node instanceof MinicDoubleRelationalNodeFactory.MinicDoubleLtNodeGen) {
            Iterator<Node> children = node.getChildren().iterator();
            replacement = MinicFloatRelationalNodeFactory.MinicFloatLtNodeGen.create((MinicFloatNode) replace(children.next()), (MinicFloatNode) replace(children.next()));
        } else if (node instanceof MinicDoubleRelationalNodeFactory.MinicDoubleLtENodeGen) {
            Iterator<Node> children = node.getChildren().iterator();
            replacement = MinicFloatRelationalNodeFactory.MinicFloatLtENodeGen.create((MinicFloatNode) replace(children.next()), (MinicFloatNode) replace(children.next()));
        } else if (node instanceof MinicDoubleRelationalNodeFactory.MinicDoubleNotEqualsNodeGen) {
            Iterator<Node> children = node.getChildren().iterator();
            replacement = MinicFloatRelationalNodeFactory.MinicFloatNotEqualsNodeGen.create((MinicFloatNode) replace(children.next()), (MinicFloatNode) replace(children.next()));
        } else if (node instanceof MinicDoubleLogicalNode.MinicDoubleOrNode) {
            Iterator<Node> children = node.getChildren().iterator();
            replacement = MinicFloatLogicalNodeFactory.MinicFloatOrNodeGen.create((MinicFloatNode) replace(children.next()), (MinicFloatNode) replace(children.next()));
        } else if (node instanceof MinicDoubleLogicalNode.MinicDoubleAndNode) {
            Iterator<Node> children = node.getChildren().iterator();
            replacement = MinicFloatLogicalNodeFactory.MinicFloatAndNodeGen.create((MinicFloatNode) replace(children.next()), (MinicFloatNode) replace(children.next()));
        } else if (node instanceof MinicDoubleUnaryNodeFactory.MinicDoubleLogicalNotNodeGen) {
            replacement = MinicFloatUnaryNodeFactory.MinicFloatLogicalNotNodeGen.create((MinicFloatNode) replace(node.getChildren().iterator().next()));
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