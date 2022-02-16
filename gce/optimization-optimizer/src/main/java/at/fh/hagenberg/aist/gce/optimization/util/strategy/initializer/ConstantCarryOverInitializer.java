/*
 * Copyright (c) 2022 the original author or authors.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

package at.fh.hagenberg.aist.gce.optimization.util.strategy.initializer;

import at.fh.hagenberg.aist.gce.optimization.util.strategy.TruffleSimpleStrategy;
import at.fh.hagenberg.machinelearning.analytics.graph.nodes.NodeWrapper;
import com.oracle.truffle.api.nodes.Node;
import at.fh.hagenberg.aist.gce.optimization.util.strategy.KnownValueStrategy;
import at.fh.hagenberg.aist.gce.optimization.util.strategy.TruffleStrategyInitializer;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

/**
 * The Constant Carry Over selects all Nodes of type <T> and inserts them into a truffle strategy.
 * Please note that a strategy may have NO values, if T doesn't occur in the given AST
 *
 * @param <T> the object to be created by the strategy
 * @author Oliver Krauss on 07.11.2018
 */
public class ConstantCarryOverInitializer<T> implements TruffleStrategyInitializer<T> {

    /**
     * The strategy that will be filled with constants that should be carried over.
     */
    private KnownValueStrategy<T> strategy = new KnownValueStrategy<>(new LinkedList<>());

    /**
     * List of hashes that are already in a strategy given by the user
     */
    private List<String> preIncludedHashes = new LinkedList<>();

    /**
     * List of hashes of already contained values in the strategy to prevent duplicates
     */
    private List<String> hashes = new LinkedList<>();

    private void resetHashes() {
        hashes = new LinkedList<>(preIncludedHashes);
    }

    /**
     * Class we are supposed to extract
     */
    private Class clazz;

    public ConstantCarryOverInitializer(Class clazz) {
        this.clazz = clazz;
    }

    /**
     * Adds all instances of T that can be found in the tree to the strategy
     *
     * @param strategy to be added to
     * @param node     to be searched (recursively)
     */
    private void addToStrategy(KnownValueStrategy<T> strategy, Node node) {
        String hash = NodeWrapper.hash(NodeWrapper.wrap(node)).getHash();
        if (node.getClass().isAssignableFrom(clazz) && !hashes.contains(hash)) {
            hashes.add(hash);
            strategy.addValue((T) node);
        }
        node.getChildren().forEach(x -> addToStrategy(strategy, x));
    }

    @Override
    public TruffleSimpleStrategy<T> createStrategy() {
        return strategy.clone();
    }

    @Override
    public TruffleSimpleStrategy<T> createStrategy(Node node) {
        KnownValueStrategy<T> result = strategy.clone();
        addToStrategy(result, node);
        resetHashes();
        return result;
    }

    @Override
    public TruffleSimpleStrategy<T> createStrategy(Collection<Node> nodes) {
        KnownValueStrategy<T> result = strategy.clone();
        nodes.forEach(x -> addToStrategy(result, x));
        resetHashes();
        return result;
    }

    /**
     * Setter for the strategy, can be replaced with any Known Value Strategy that allows adding and removing values
     *
     * @param strategy
     */
    public void setStrategy(KnownValueStrategy<T> strategy) {
        this.strategy = strategy;
        this.strategy.getValues().forEach(x -> preIncludedHashes.add(NodeWrapper.hash(NodeWrapper.wrap((Node) x)).getHash()));
        resetHashes();
    }
}
