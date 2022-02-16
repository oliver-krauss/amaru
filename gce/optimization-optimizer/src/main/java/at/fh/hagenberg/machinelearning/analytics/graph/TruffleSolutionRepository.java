/*
 * Copyright (c) 2022 the original author or authors.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

package at.fh.hagenberg.machinelearning.analytics.graph;


import science.aist.neo4j.reflective.ReflectiveNeo4JNodeRepositoryImpl;
import science.aist.neo4j.transaction.TransactionManager;
import at.fh.hagenberg.machinelearning.core.Solution;
import org.neo4j.driver.AccessMode;
import org.neo4j.driver.Record;
import org.neo4j.driver.Result;
import org.neo4j.driver.Values;

import java.util.HashMap;
import java.util.Map;

public class TruffleSolutionRepository extends ReflectiveNeo4JNodeRepositoryImpl<Solution> {

    public TruffleSolutionRepository(TransactionManager manager) throws NoSuchMethodException, ClassNotFoundException {
        super(manager, Solution.class);
    }

    /**
     * Searches for a solution (evaluation) for a specific truffle-tree
     *
     * @param hash   The hash of the tree the solution is for
     * @param utHash The hash of the unittests of the problem gene
     * @return A solution if it was ever evaluated, or null
     */
    public Solution findSolutionNodeByTreeHash(String hash, String utHash) {
        return queryTyped("match (s:Solution)-[]->(sg:SolutionGene)-[]->(:TruffleOptimizationSolution)-[:TREE]->({hash: $hash}) where " +
            "(sg)-[:SOLVES]->()-[:RWGENE]->(:TruffleOptimizationProblem{hash: $utHash}) return s", Values.parameters("hash", hash, "utHash", utHash));
    }

    /**
     * Searches for the solutions corresponding to a given utHash identifying the Fitness Evaluation of a problem definition
     * @return hashes for the given problem definition
     */
    public Map<String, Long> findExistingHashes(String utHash) {
        return this.getTransactionManager().execute((transaction) -> {
            Result result = transaction.run("match (s:Solution)-[]->(sg:SolutionGene)-[]->(:TruffleOptimizationSolution)-[:TREE]->(t) where (sg)-[:SOLVES]->()-[:RWGENE]->(:TruffleOptimizationProblem{hash: $hash}) return t.hash, id(s)", Values.parameters("hash", utHash));
            if (result.hasNext()) {
                HashMap<String, Long> values = new HashMap<>();
                while (result.hasNext()) {
                    Record val = result.next();
                    values.put(val.get(0).asString(), val.get(1).asLong());
                }
                return values;
            } else {
                return new HashMap<>();
            }
        }, AccessMode.READ);
    }
}
