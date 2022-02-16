/*
 * Copyright (c) 2022 the original author or authors.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

package at.fh.hagenberg.machinelearning.analytics.graph.nodes;

import at.fh.hagenberg.aist.gce.optimization.runtime.SystemInformation;
import at.fh.hagenberg.aist.gce.optimization.util.ExtendedNodeUtil;
import at.fh.hagenberg.aist.gce.optimization.util.TruffleClassInformation;
import at.fh.hagenberg.aist.gce.optimization.util.TruffleClassProperty;
import at.fh.hagenberg.aist.gce.optimization.util.TruffleLanguageInformation;
import com.oracle.truffle.api.nodes.Node;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.stream.StreamSupport;

public class NodeWrapperWeightUtil {

    /**
     * Assertion how often a loop is "usually" called.
     */
    private static final int LOOP_ASSERTION = 10;

    /**
     * Assertion how "expensive" the condition is compared to the branches in a branching node
     */
    private static final double CONDITION_ASSERTION = 1.1;

    /**
     * System information that the weighting will be based on.
     */
    private SystemInformation information = SystemInformation.getCurrentSystem();

    /**
     * Language information used for weighting
     */
    private TruffleLanguageInformation tli;

    /**
     * Weight map for easier acccess out of tli
     */
    private Map<String, Double> weights;

    /**
     * Average value to approximate uknown nodes
     */
    private double average;

    private void init() {
        weights = new HashMap<>();
        tli.getInstantiableNodes().forEach((key, value) -> {
            if (value.getWeight().containsKey(information)) {
                weights.put(key.getName(), value.getWeight().get(information));
            }
        });

        // safety feature - add averages to all missing nodes
        average = tli.getInstantiableNodes().values().stream().filter(x -> x.getSystemWeight() > 0).mapToDouble(TruffleClassInformation::getSystemWeight).average().orElse(1.0);
        tli.getInstantiableNodes().values().stream().filter(x -> !x.getWeight().containsKey(SystemInformation.getCurrentSystem()))
            .forEach(x -> weights.put(x.getClazz().getName(), average));
    }

    public NodeWrapperWeightUtil(String languageId) {
        this.tli = TruffleLanguageInformation.getLanguageInformationMinimal(languageId);
        init();
    }

    public NodeWrapperWeightUtil(TruffleLanguageInformation tli) {
        this.tli = tli;
        init();
    }

    private static boolean warned = false;

    /**
     * This function attempts to predict the run-time performance weight of a node
     * As no runtime information is provided some assertions are taken for branches and loops
     * @param node to be weighted
     * @return     appoximation of the run-time performance (loop, branch assertions)
     */
    public double weight(NodeWrapper node) {
        if (node == null) {
            return 0;
        }
        if (ExtendedNodeUtil.isAPINode(node)) {
            // skip api nodes
            return node.getChildren().stream().mapToDouble(x -> weight(x.getChild())).sum();
        }
        if (!weights.containsKey(node.getType())) {
            if (!warned) {
                warned = true;
                System.out.println("Warning: Weight is missing for type: " + node.getType());
            }
            return average;
        }

        // calculate child weights
        double childWeights = node.getChildren().stream().mapToDouble(x -> weight(x.getChild())).sum();
        TruffleClassInformation tci = tli.getTci(node.getType());
        if (tci.hasProperty(TruffleClassProperty.LOOP)) {
            // for loops we assume an average call amount (other option woudld be to do childWeight^2 or childweight + log(childWeight)...
            childWeights *= LOOP_ASSERTION;
        } else if (tci.hasProperty(TruffleClassProperty.BRANCH)) {
            // for branches we assert that all branches are taken equally often (we assume 1 condition node)
            childWeights /= node.getChildren().size() - 1;
            // we also don't know which node is the condition. As that one was "folded" into the branches we must increase our assumption
            childWeights *= CONDITION_ASSERTION;
        }

        return weights.get(node.getType()) + childWeights;
    }

    public double weight(Node node) {
        if (node == null) {
            return 0;
        }
        if (ExtendedNodeUtil.isAPINode(node)) {
            // skip api nodes
            return StreamSupport.stream(node.getChildren().spliterator(), true).mapToDouble(this::weight).sum();
        }
        if (!weights.containsKey(node.getClass().getName())) {
            if (!warned) {
                warned = true;
                System.out.println("Warning: Weight is missing for type: " + node.getClass().getName());
            }
            return average;
        }

        // calculate child weights
        double childWeights = StreamSupport.stream(node.getChildren().spliterator(), true).mapToDouble(this::weight).sum();
        TruffleClassInformation tci = tli.getTci(node.getClass());
        if (tci.hasProperty(TruffleClassProperty.LOOP)) {
            // for loops we assume an average call amount (other option woudld be to do childWeight^2 or childweight + log(childWeight)...
            childWeights *= LOOP_ASSERTION;
        } else if (tci.hasProperty(TruffleClassProperty.BRANCH)) {
            // for branches we assert that all branches are taken equally often (we assume 1 condition node)
            childWeights /= StreamSupport.stream(node.getChildren().spliterator(), true).count() - 1;
            // we also don't know which node is the condition. As that one was "folded" into the branches we must increase our assumption
            childWeights *= CONDITION_ASSERTION;
        }

        return weights.get(node.getClass().getName()) + childWeights;
    }

    /**
     * This function attempts to predict the run-time performance weight of a node.
     * It uses the runtime traces to do this more accuratly, due to the knowledge which branch was taken and how often loops were executed
     * @param node   to be weighted
     * @param traces of run for weighing Map<Key, ExecutionCount> Key -> "position"."position" where position is the position the iterator returns on Node.children() (root is "0")
     * @return       approximation of the run-time performance (no assertions)
     */
    public double weight(NodeWrapper node, Collection<Map<String, Integer>> traces) {
        return traces.stream().mapToDouble(x -> weight(node, x)).average().orElse(Double.MAX_VALUE);
    }

    /**
     * This function attempts to predict the run-time performance weight of a node.
     * It uses the runtime traces to do this more accuratly, due to the knowledge which branch was taken and how often loops were executed
     * @param node   to be weighted
     * @param traces of run for weighing Map<Key, ExecutionCount> Key -> "position"."position" where position is the position the iterator returns on Node.children() (root is "0")
     * @return       approximation of the run-time performance (no assertions)
     */
    public double weight(NodeWrapper node, Map<String, Integer> traces) {
        // Note: trace key is .0.0 as we are skipping the "rootNode" and the "functionBodyNode" directly to the function body itself
        return weight(node, traces, ".0.0");
    }

    /**
     * This function attempts to predict the run-time performance weight of a node.
     * It uses the runtime traces to do this more accuratly, due to the knowledge which branch was taken and how often loops were executed
     * @param node     to be weighted
     * @param traces   of run for weighing Map<Key, ExecutionCount> Key -> "position"."position" where position is the position the iterator returns on Node.children() (root is "0")
     * @param traceKey key where trace starts, in case you want to only determine the weight of a child node in a trace execution
     * @return       approximation of the run-time performance (no assertions)
     */
    public double weight(NodeWrapper node, Map<String, Integer> traces, String traceKey) {
        return weight(node, traces, traceKey, true);
    }

    /**
     * This function attempts to predict the run-time performance weight of a node.
     * It uses the runtime traces to do this more accuratly, due to the knowledge which branch was taken and how often loops were executed
     * @param node     to be weighted
     * @param traces   of run for weighing Map<Key, ExecutionCount> Key -> "position"."position" where position is the position the iterator returns on Node.children() (root is "0")
     * @param traceKey key where trace starts, in case you want to only determine the weight of a child node in a trace execution
     * @param optimized if true -> runtime after graal optimization, false -> runtime before graal optimization
     * @return       approximation of the run-time performance (no assertions)
     */
    public double weight(NodeWrapper node, Map<String, Integer> traces, String traceKey, boolean optimized) {
        if (ExtendedNodeUtil.isAPINode(node)) {
            // skip over api nodes
            int i = 0;
            double weight = 0;
            Iterator<OrderedRelationship> iterator = node.getChildren().iterator();
            while (iterator.hasNext()) {
                NodeWrapper next = iterator.next().getChild();
                weight += weight(next, traces, traceKey + "." + i++);
            }
            return weight;
        }
        if (!traces.containsKey(traceKey)) {
            // not contained -> no execution -> no weight
            return 0;
        }
        // find tli
        TruffleClassInformation tci = tli.getTci(node);

        // weight = amount of executions * assumed node weight
        double weight = traces.get(traceKey) * (optimized ? tci.getWeight() : tci.getWeightUnoptimized()).get(this.information);

        // do the same for all child-nodes
        int i = 0;
        Iterator<OrderedRelationship> iterator = node.getChildren().iterator();
        while (iterator.hasNext()) {
            NodeWrapper next = iterator.next().getChild();
            weight += weight(next, traces, traceKey + "." + i++);
        }
        return weight;
    }

    public void setInformation(SystemInformation information) {
        this.information = information;
        init();
    }

    public SystemInformation getInformation() {
        return information;
    }

    public TruffleLanguageInformation getTli() {
        return tli;
    }

    public double getAverage() {
        return average;
    }
}
