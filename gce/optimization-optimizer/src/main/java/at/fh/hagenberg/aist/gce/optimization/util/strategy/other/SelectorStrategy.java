/*
 * Copyright (c) 2022 the original author or authors.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

package at.fh.hagenberg.aist.gce.optimization.util.strategy.other;

import at.fh.hagenberg.aist.gce.optimization.util.strategy.*;
import at.fh.hagenberg.aist.gce.optimization.util.strategy.selection.DegreeOfFreedomChooser;
import at.fh.hagenberg.util.Pair;
import com.oracle.truffle.api.nodes.Node;
import at.fh.hagenberg.aist.gce.optimization.util.strategy.selection.ChooseOption;
import at.fh.hagenberg.aist.gce.optimization.util.strategy.selection.RandomChooser;

import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Selection strategy that simply forward between different strategies, the selection mechansism is defined on degrees of freedom.
 *
 * @author Oliver Krauss on 07.11.2018
 */
public class SelectorStrategy<T extends Node> extends DefaultObservableAndObserverStrategy implements TruffleHierarchicalStrategy<T> {

    /**
     * Strategy to access the patterns in the system
     */
    private TruffleMasterStrategy rootStrategy;

    private List<TruffleHierarchicalStrategy> strategies;

    /**
     * Strategy for selecting one of the values
     */
    protected ChooseOption<Pair<TruffleHierarchicalStrategy, RequirementInformation>> chooser = new DegreeOfFreedomChooser();

    public SelectorStrategy(List<TruffleHierarchicalStrategy> strategies) {
        this.strategies = strategies;
        strategies.forEach(x -> {
            if (x instanceof TruffleObservableStrategy) {
                subscribe((TruffleObservableStrategy) x);
            }
        });
        if (observing.stream().allMatch(x -> x.isDisabled())) {
            this.disabled = true;
        }
    }

    @Override
    public T create(CreationInformation information) {
        return (T) chooser.choose(strategies.stream().map(s -> new Pair<>(s, s.canCreateVerbose(information))).filter(s -> s.getValue() != null && s.getValue().fullfillsAll()).collect(Collectors.toList())).getKey().create(information);
    }

    @Override
    public RequirementInformation canCreate(CreationInformation information) {
        return !isDisabled() && strategies.stream().anyMatch(s -> null != s.canCreate(information)) ? information.getRequirements() : null;
    }

    @Override
    public RequirementInformation canCreateVerbose(CreationInformation information) {
        return !isDisabled() && strategies.stream().anyMatch(s -> null != s.canCreateVerbose(information)) ? information.getRequirements() : null;
    }

    @Override
    public void addCreateRequirement(Requirement requirement) {
        // NOTHING we don't deal with create requirements here
    }

    @Override
    public void removeCreateRequirement(Requirement requirement) {
        // NOTHING we don't deal with create requirements here
    }

    @Override
    public void attach(TruffleHierarchicalStrategy strategy) {
        // delegate attachment to children
        for (TruffleHierarchicalStrategy truffleHierarchicalStrategy : this.strategies) {
            strategy.attach(strategy);
        }
    }

    @Override
    public TruffleHierarchicalStrategy<T> injectRootStrategy(TruffleMasterStrategy rootStrategy) {
        this.rootStrategy = rootStrategy;
        return this;
    }

    public void setChooser(ChooseOption<Pair<TruffleHierarchicalStrategy, RequirementInformation>> chooser) {
        this.chooser = chooser;
    }

    @Override
    public void disabled(TruffleObservableStrategy strategy) {
        // when no choice is left, disable all, when not all are observable,
        if (observing.size() == strategies.size() && observing.stream().allMatch(x -> x.isDisabled())) {
            notifyDisable();
        }
    }

    @Override
    public void enabled(TruffleObservableStrategy strategy) {
        // one single valid strategy is enough to re-enable the selector
        notifyEnable();
    }

    @Override
    public Map<Node, LoadedRequirementInformation> loadRequirements(Node ast, RequirementInformation parentInformation, Map<Node, LoadedRequirementInformation> requirementMap) {
        // forward to real strat -> same impl as MasterStrategy
        List<TruffleHierarchicalStrategy> collect = strategies.stream().filter(x -> x.getManagedClasses().contains(ast.getClass())).collect(Collectors.toList());
        if (!collect.isEmpty()) {
            // alwayys work with strat 0, there should only be one anyways
            DefaultObservableAndObserverStrategy strategy = (DefaultObservableAndObserverStrategy) collect.get(0);
            return strategy.loadRequirements(ast, parentInformation, requirementMap);
        }

        throw new RuntimeException("Can't load requirements for class " + ast.getClass());
    }

    @Override
    public Collection<Class> getManagedClasses() {
        return (Collection<Class>) strategies.stream().flatMap(x -> x.getManagedClasses().stream()).distinct().collect(Collectors.toList());
    }

    @Override
    public double minWeight(CreationInformation information) {
        return strategies.stream().filter(x -> x.canCreate(information) != null).mapToDouble(x -> x.minWeight(information)).min().orElse(Double.MAX_VALUE);
    }
}
