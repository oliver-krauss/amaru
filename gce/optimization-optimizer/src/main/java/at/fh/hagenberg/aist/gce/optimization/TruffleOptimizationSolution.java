/*
 * Copyright (c) 2022 the original author or authors.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

package at.fh.hagenberg.aist.gce.optimization;

import at.fh.hagenberg.aist.gce.optimization.test.TruffleOptimizationTestResult;
import at.fh.hagenberg.machinelearning.analytics.graph.nodes.NodeWrapper;
import com.oracle.truffle.api.nodes.Node;
import org.neo4j.ogm.annotation.Id;
import org.neo4j.ogm.annotation.Relationship;
import org.neo4j.ogm.annotation.Transient;

import java.util.HashSet;
import java.util.Set;

/**
 * Created by Oliver Krauss on 10.02.2017.
 */
public class TruffleOptimizationSolution {

    /**
     * Id generated by database
     */
    @Id
    protected Long id;

    /**
     * The original problem, containing the execution context for running our node
     */
    @Transient // already given in this.genes
    protected TruffleOptimizationProblem problem;

    /**
     * Actual output of the solution if it is executed
     * Null if the program won't even compile
     */
    @Relationship(type = "TEST_RESULT", direction = Relationship.OUTGOING)
    protected Set<TruffleOptimizationTestResult> testResults = new HashSet<>();

    /**
     * Node that should replace the Problem-Node as new solution
     */
    @Transient
    protected Node node;

    /**
     * The node this solution addresses (== node)
     */
    @Relationship(type = "TREE", direction = "OUTGOING")
    protected NodeWrapper tree;

    public TruffleOptimizationSolution() {
    }

    protected TruffleOptimizationSolution(Node node, TruffleOptimizationProblem problem) {
        this.node = node;
        this.problem = problem;

        // connections for graph
        this.tree = NodeWrapper.wrap(node);
    }

    /**
     * Helper object that stores where this solution was created. Used exclusively for debugging.
     */
    @Transient
    private Object maker;

    public TruffleOptimizationSolution(Node node, TruffleOptimizationProblem problem, Object maker) {
        this(node, problem);
        this.maker = maker;
    }

    /**
     * The test results are being generated for EACH INDIVIDUAL test case given in the problem statement
     *
     * @return
     */
    public Set<TruffleOptimizationTestResult> getTestResults() {
        return testResults;
    }

    public Node getNode() {
        return node;
    }

    public TruffleOptimizationProblem getProblem() {
        return problem;
    }

    public NodeWrapper getTree() {
        return tree;
    }

    public void setTree(NodeWrapper tree) {
        this.tree = tree;
    }

    public Long getId() {
        return id;
    }
}