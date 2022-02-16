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

import science.aist.neo4j.reflective.ReflectiveNeo4JNodeRepositoryImpl;
import science.aist.neo4j.transaction.TransactionManager;
import at.fh.hagenberg.machinelearning.analytics.graph.nodes.NodeWrapper;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.MaterializedFrame;
import org.neo4j.driver.AccessMode;
import org.neo4j.driver.Values;

/**
 * @author Oliver Krauss on 07.05.2019
 */

public class TruffleOptimizationSolutionRepository extends ReflectiveNeo4JNodeRepositoryImpl<TruffleOptimizationSolution> {

    public TruffleOptimizationSolutionRepository(TransactionManager manager, Class<TruffleOptimizationSolution> clazz) throws NoSuchMethodException, ClassNotFoundException {
        super(manager, clazz);
    }

    /**
     * Synchronizes the ID from the DB
     *
     * @param solution         to get an ID assigned
     * @param parentSolutionId to assign the ID in relation
     * @return solution with id
     */
    public TruffleOptimizationSolution syncWithDb(TruffleOptimizationSolution solution, Long parentSolutionId) {
        solution.id = queryTyped("match (n:TruffleOptimizationSolution)<-[:RWGENE]-()<-[:GENE]-(s) where id(s) = $parentSolutionId return n limit 1", Values.parameters("parentSolutionId", parentSolutionId)).getId();
        return solution;
    }


    /**
     * Synchronizes the solution and also returns the entire subgraph (tree, test-results)
     * @param solutionId to be returned
     * @return solution with tree (if Id exists)
     */
    public TruffleOptimizationSolution loadWithTree(Long solutionId) {
        TruffleOptimizationSolution truffleOptimizationSolution = execute("MATCH (n:TruffleOptimizationSolution) WHERE ID(n) = $solutionId OPTIONAL MATCH (n)-[r*..]->(c) UNWIND r as row RETURN {root: n, relationships: collect(distinct row), nodes: collect(distinct c)}", Values.parameters("solutionId", solutionId), AccessMode.READ);
        return truffleOptimizationSolution;
    }

    /**
     * Loads the solution and also returns the entire subgraph (tree, test-results) but not the problem it is matched to
     * @param solutionId to be returned
     * @return solution with tree (if Id exists)
     */
    public TruffleOptimizationSolution loadAndUnpackWithTree(Long solutionId, FrameDescriptor localDescriptor, MaterializedFrame globalFrame, String language) {
        TruffleOptimizationSolution truffleOptimizationSolution = execute("MATCH (n:TruffleOptimizationSolution) WHERE ID(n) = $solutionId OPTIONAL MATCH (n)-[r*..]->(c) UNWIND r as row RETURN {root: n, relationships: collect(distinct row), nodes: collect(distinct c)}", Values.parameters("solutionId", solutionId), AccessMode.READ);
        truffleOptimizationSolution.node = NodeWrapper.unwrap(truffleOptimizationSolution.getTree(), localDescriptor, globalFrame, language);
        return truffleOptimizationSolution;
    }
}
