/*
 * Copyright (c) 2022 the original author or authors.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

package at.fh.hagenberg.aist.gce.optimization.util.strategy.random;

import at.fh.hagenberg.aist.gce.optimization.util.ClassLoadingHelper;
import at.fh.hagenberg.aist.gce.optimization.util.TruffleClassInitializer;
import at.fh.hagenberg.aist.gce.optimization.util.TruffleClassProperty;
import at.fh.hagenberg.aist.gce.optimization.util.TruffleParameterInformation;
import at.fh.hagenberg.aist.gce.optimization.util.strategy.*;
import at.fh.hagenberg.aist.gce.optimization.util.strategy.other.DefaultObservableAndObserverStrategy;
import at.fh.hagenberg.aist.gce.optimization.util.strategy.other.FrameSlotStrategy;
import at.fh.hagenberg.aist.gce.optimization.util.strategy.other.RandomArraySizeStrategy;
import at.fh.hagenberg.aist.gce.optimization.util.strategy.other.SelectorStrategy;
import at.fh.hagenberg.machinelearning.analytics.graph.nodes.NodeWrapper;
import at.fh.hagenberg.machinelearning.analytics.graph.nodes.NodeWrapperWeightUtil;
import at.fh.hagenberg.util.Pair;
import com.oracle.truffle.api.frame.MaterializedFrame;
import com.oracle.truffle.api.nodes.Node;

import java.lang.reflect.Array;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Strategy that uses given nodes as parameters, and only creates new ones if necessary.
 * Currently only used by {@link at.fh.hagenberg.aist.gce.optimization.operators.TruffleSingleNodeMutator}
 *
 * @author Oliver Krauss on 07.11.2018
 */
public class NodeInjectingSubtreeStrategy<T extends Node> extends DefaultObservableAndObserverStrategy implements TruffleHierarchicalStrategy<T> {

    /**
     * Strategy to access the patterns in the system
     */
    private TruffleMasterStrategy rootStrategy;

    /**
     * the child nodes that are to be injected into the initiation
     */
    private Collection<Node> nodes;
    /**
     * Initializer pre-selected by the Mutator
     */
    protected TruffleClassInitializer initializer;

    /**
     * Weight of the generated node + all its children (so the weight that create will always produce).
     */
    private double weight;

    /**
     * Strategies for terminal values
     */
    protected Map<String, TruffleVerifyingStrategy> terminalStrategies;

    /**
     * Strategy for nonterminals. Note: If you want to use more than one strategy refer to the
     * {@link SelectorStrategy}
     */
    protected TruffleHierarchicalStrategy nonTerminalStrategy;

    public NodeInjectingSubtreeStrategy(TruffleClassInitializer initializer, Map<String, TruffleVerifyingStrategy> terminalStrategies, TruffleHierarchicalStrategy nonTerminalStrategy, Collection<Node> nodes, NodeWrapperWeightUtil util) {
        this.initializer = initializer;
        this.nonTerminalStrategy = new RandomArraySizeStrategy(nonTerminalStrategy, util);
        this.terminalStrategies = terminalStrategies;
        this.nodes = nodes;

        if (util != null) {
            weight = nodes.stream().mapToDouble(util::weight).sum() + util.weight(new NodeWrapper(initializer.getClazz().getName()));
        }

        // prepare strategy to also work with globals
        if (initializer.getClassInfo().getProperties().contains(TruffleClassProperty.GLOBAL_STATE) && terminalStrategies.containsKey("com.oracle.truffle.api.frame.FrameSlot") && terminalStrategies.containsKey("com.oracle.truffle.api.frame.MaterializedFrame")) {
            MaterializedFrame globalFrame = (MaterializedFrame) terminalStrategies.get("com.oracle.truffle.api.frame.MaterializedFrame").create(null);
            if (globalFrame != null) {
                FrameSlotStrategy frameSlotStrategy = (FrameSlotStrategy) this.terminalStrategies.get("com.oracle.truffle.api.frame.FrameSlot");
                frameSlotStrategy.setDescriptor(globalFrame.getFrameDescriptor());
                frameSlotStrategy.setFrame(globalFrame);
            } else {
                // prevent unintentional access to a local frame
                this.terminalStrategies.remove("com.oracle.truffle.api.frame.FrameSlot");
            }
        }
    }

    private Object selectParam(TruffleParameterInformation parameter, CreationInformation information) throws ClassNotFoundException {
        return terminalStrategies.containsKey(parameter.getType().getName()) ?
            terminalStrategies.get(parameter.getType().getName()).create(information) :
            nonTerminalStrategy.create(information.copy().setClazz(Class.forName(parameter.getType().getName())).incCurrentDepth());
    }

    @Override
    public T create(CreationInformation information) {
        Object[] params = new Object[initializer.getParameters().length];
        for (int i = 0; i < params.length; i++) {
            int finalI = i;
            List<Node> possibleInjectNodes = nodes.stream().filter(n -> initializer.getParameters()[finalI].getClazz().isAssignableFrom(n.getClass())).collect(Collectors.toList());
            if (!possibleInjectNodes.isEmpty()) {
                if (initializer.getParameters()[i].isArray()) {
                    Object[] resultArray = (Object[]) Array.newInstance(initializer.getParameters()[i].getClazz(), possibleInjectNodes.size());
                    for (int r = 0; r < resultArray.length; r++) {
                        resultArray[r] = possibleInjectNodes.get(r);
                    }
                    params[i] = resultArray;
                    nodes.removeAll(possibleInjectNodes);
                } else {
                    params[i] = possibleInjectNodes.get(0);
                    nodes.remove(possibleInjectNodes.get(0));
                }
            } else {
                try {
                    // TODO #229 we must switch to a DOF approach similar to the RandomReflectiveSubtreeStrategy
                    params[i] = selectParam(initializer.getParameters()[i], information);
                } catch (ClassNotFoundException e) {
                    throw new RuntimeException(e);
                }
            }
        }
        if (!nodes.isEmpty()) {
            int i = 1;
        }
        return (T) initializer.instantiate(params);
    }

    @Override
    public RequirementInformation canCreate(CreationInformation information) {
        return information.getClazz().getName().equals(initializer.getClazz()) &&
            weight < information.getConfiguration().getMaxWeight() - information.getCurrentWeight() ?
            information.getRequirements() :
            null;
    }

    @Override
    public RequirementInformation canCreateVerbose(CreationInformation information) {
        RequirementInformation combinedRequirements = new RequirementInformation(null);
        return canCreate(information) != null &&
            Arrays.stream(this.initializer.getParameters()).allMatch(x ->
                (terminalStrategies.containsKey(x.getClazz().getName()) && null != combine(combinedRequirements, terminalStrategies.get(x.getClazz().getName()).canCreate(information.copy().setInformation(this.initializer.getClassInfo())))) // parameter is a terminal with available strategy
                    || null != combine(combinedRequirements, nonTerminalStrategy.canCreateVerbose(information.copy().setClazz(x.getType()).incCurrentDepth())) // parameter is a nonterminal with available strategy
            ) ?
            combinedRequirements : null;
    }

    @Override
    public void addCreateRequirement(Requirement requirement) {
        // TODO #229 this is exactly the same impl as the randomreflective Strat
    }

    @Override
    public void removeCreateRequirement(Requirement requirement) {
        // TODO #229 this is exactly the same impl as the randomreflective Strat
    }

    @Override
    public void attach(TruffleHierarchicalStrategy strategy) {
        if (this.nonTerminalStrategy == null) {
            this.nonTerminalStrategy = strategy;
        }
    }

    @Override
    public TruffleHierarchicalStrategy<T> injectRootStrategy(TruffleMasterStrategy rootStrategy) {
        this.rootStrategy = rootStrategy;
        return this;
    }

    @Override
    public Collection<Class> getManagedClasses() {
        return Collections.singleton(initializer.getClazz());
    }

    @Override
    public double minWeight(CreationInformation information) {
        return weight;
    }

    @Override
    public Map<Node, LoadedRequirementInformation> loadRequirements(Node ast, RequirementInformation parentInformation, Map<Node, LoadedRequirementInformation> requirementMap) {
        // TODO #229 IMPLEMENT by simulating create
        return requirementMap;
    }
}
