/*
 * Copyright (c) 2022 the original author or authors.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

package at.fh.hagenberg.aist.gce.optimization.test;

import at.fh.hagenberg.aist.gce.optimization.TruffleOptimizationProblem;
import org.neo4j.ogm.annotation.*;

import java.util.List;

@RelationshipEntity(type = "TEST_CASE")
public class TruffleOptimizationTestComplexity implements Comparable<TruffleOptimizationTestComplexity> {

    /**
     * Id generated by database
     */
    @Id
    private Long id;

    /**
     * Amount of nodeCount visited with original AST (problem.getNode) during this test
     */
    private int nodeCount;

    /**
     * Amout of spezialized nodeCount observed during this test (changes in the AST)
     */
    private int spezialisations;

    /**
     * Node hashes of the nodes that were executed.
     * NOTE: This SHOULD be represented by relationships to the nodes, but this would make our database
     * so incredibly complex (for something with a small impact on the big piture) that we intentionally omit
     * designing this as relationships
     */
    private List<String> nodes;

    @StartNode
    private TruffleOptimizationProblem problem;

    @EndNode
    private TruffleOptimizationTest test;

    public TruffleOptimizationTestComplexity() {
    }

    public TruffleOptimizationTestComplexity(TruffleOptimizationProblem problem, TruffleOptimizationTest test) {
        this.problem = problem;
        this.test = test;
    }

    public TruffleOptimizationTestComplexity(int nodeCount, int spezialisations, List<String> nodes, TruffleOptimizationProblem problem, TruffleOptimizationTest test) {
        this.nodeCount = nodeCount;
        this.spezialisations = spezialisations;
        this.nodes = nodes;
        this.problem = problem;
        this.test = test;
    }

    public int getNodeCount() {
        return nodeCount;
    }

    public int getSpezialisations() {
        return spezialisations;
    }

    public void setNodeCount(int nodeCount) {
        this.nodeCount = nodeCount;
    }

    public void setSpezialisations(int spezialisations) {
        this.spezialisations = spezialisations;
    }

    public TruffleOptimizationProblem getProblem() {
        return problem;
    }

    public TruffleOptimizationTest getTest() {
        return test;
    }

    public void setTest(TruffleOptimizationTest test) {
        this.test = test;
    }

    public List<String> getNodes() {
        return nodes;
    }

    public void setNodes(List<String> nodes) {
        this.nodes = nodes;
    }

    /**
     * Calculates a complexity value referring as to how "hard" this test is in the context of the problem description
     *
     * @return test complexity
     */
    public double getComplexity() {
        return nodeCount + spezialisations;
    }

    @Override
    public int compareTo(TruffleOptimizationTestComplexity o) {
        return test.compareTo(o.getTest());
    }

    /**
     * Calculates the overlap with another complexity metric
     * 0 being -> no node between these tests was the same
     * 1 being -> all nodes between these tests were the same
     * <p>
     * note that the overlap will be calculated from the "bigger" test. So a test that is a subset of another test
     * will an have an overlap of 1
     * <p>
     * If any of the complexities does not have any nodes information (nodes == null;) the overlap will be returned as 0
     * as it can't be determined what the overlap is
     *
     * @param complexity another test to be compared
     * @return overlap between 0 and 1
     */
    public double overlap(TruffleOptimizationTestComplexity complexity) {
        if (complexity == null) {
            return 0;
        }
        if (complexity.getNodes() == null || this.getNodes() == null) {
            return 0;
        }

        double overlap = 0;

        List<String> a = this.nodeCount >= complexity.nodeCount ? this.getNodes() : complexity.getNodes();
        List<String> b = a.equals(this.nodes) ? complexity.getNodes() : this.getNodes();

        for (String source : a) {
            for (String target : b) {
                if (source.equals(target)) {
                    overlap++;
                }
            }
        }

        return overlap / nodeCount;
    }
}