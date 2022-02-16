/*
 * Copyright (c) 2022 the original author or authors.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

package at.fh.hagenberg.aist.gce.optimization.language.util;

import at.fh.hagenberg.machinelearning.analytics.graph.nodes.NodeWrapper;
import com.oracle.truffle.api.nodes.Node;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Default Interceptor for the JavassistWorker.
 * Calculates:
 * - the complexity measure of a test (the amount of nodes called, and the amount of nodes specialized)
 *
 * @author Oliver Krauss on 11.12.2019
 */
public class JavassistInterceptor implements JavassistInterceptCallback {

    /**
     * List of all nodes that have been executed during callbacks
     */
    private Map<String, Integer> executedNodes = new HashMap<>();

    /**
     * List of all nodes that have been specialized
     */
    private Map<String, Integer> specializedNodes = new HashMap<>();

    /**
     * Resets the execution counter to 0 (ex. for a re-run)
     */
    public void reset() {
        executedNodes.clear();
        specializedNodes.clear();
        previousSpecialization = null;
        previous = null;
    }

    /**
     * @return how many nodes were executed and intercepted by this class
     */
    public int getExecutedCount() {
        return executedNodes.size();
    }

    /**
     * Returns executed nodes only of one function instead of the entire program
     *
     * @param graph any node in hierarchy of execution
     * @return how many nodes were executed and intercepted by this class
     */
    public int getExecutedCount(Node graph) {
        String key = NodeWrapper.wrap(graph.getRootNode()).getHash();
        return (int) executedNodes.keySet().stream().filter(x -> x.startsWith(key)).count();
    }

    /**
     * @return how many nodes were specialized and intercepted by this class
     */
    public int getSpecializedCount() {
        return specializedNodes.size();
    }

    /**
     * Returns specialized nodes only of one function instead of the entire program
     *
     * @param graph any node in hierarchy of execution
     * @return how many nodes were specialized and intercepted by this class
     */
    public int getSpecializedCount(Node graph) {
        String key = NodeWrapper.wrap(graph.getRootNode()).getHash();
        return (int) specializedNodes.keySet().stream().filter(x -> x.startsWith(key)).count();
    }

    public Map<String, Integer> getNodeHashes() {
        return executedNodes;
    }

    /**
     * Returns the execution statistics only of a single function
     *
     * @param graph any node in hierarchy of execution
     * @return how many nodes were specialized and intercepted by this class
     */
    public Map<String, Integer> getNodeHashes(Node graph) {
        String key = NodeWrapper.wrap(graph.getRootNode()).getHash();
        Map<String, Integer> hashes = new HashMap<>();
        executedNodes.entrySet().stream().filter(x -> x.getKey().startsWith(key)).forEach(x -> {
            hashes.put(x.getKey().substring(key.length()), x.getValue());
        });
        return hashes;
    }

    private Object previous;

    private Object previousSpecialization;

    @Override
    public void beforeIntercept(Object target, String name, Object[] args) {
        String key = getNodePosition((Node) target);

        if (!name.contains("AndSpecialize")) {
            // Add to executions
            if (!executedNodes.containsKey(key)) {
                executedNodes.put(key, 0);
            }
            // prevent calls from node to itself to be a duplicate execution
            if (target != previous) {
                executedNodes.put(key, executedNodes.get(key) + 1);
            }
            previous = target;
        } else {
            // Add to specializations
            if (!specializedNodes.containsKey(key)) {
                specializedNodes.put(key, 0);
            }
            // prevent calls from node to itself to be a duplicate execution
            if (target != previousSpecialization) {
                specializedNodes.put(key, specializedNodes.get(key) + 1);
            }
            previousSpecialization = target;
            // execute and specialize sometimes calls back to itself, but never the other way around
            previous = target;
        }
    }

    protected String getNodePosition(Node target) {
        Node origin = target;
        String key = "";
        Node parent;
        while ((parent = target.getParent()) != null) {
            int i = 0;
            Iterator<Node> iterator = parent.getChildren().iterator();
            Node n = iterator.next();
            while (n != target) {
                n = iterator.next();
                i++;
            }
            target = parent;
            key = "." + i + key;
        }

        return NodeWrapper.wrap(origin.getRootNode()).getHash() + key;
    }
}
