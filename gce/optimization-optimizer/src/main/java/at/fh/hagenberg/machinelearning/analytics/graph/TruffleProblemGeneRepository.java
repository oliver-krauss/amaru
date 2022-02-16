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


import at.fh.hagenberg.aist.gce.optimization.TruffleOptimizationProblem;
import science.aist.neo4j.reflective.ReflectiveNeo4JNodeRepositoryImpl;
import science.aist.neo4j.transaction.TransactionManager;
import org.neo4j.driver.AccessMode;
import org.neo4j.driver.Record;
import org.neo4j.driver.Result;

import java.util.HashMap;
import java.util.Map;

public class TruffleProblemGeneRepository extends ReflectiveNeo4JNodeRepositoryImpl<TruffleOptimizationProblem> {

    public TruffleProblemGeneRepository(TransactionManager manager) throws NoSuchMethodException, ClassNotFoundException {
        super(manager, TruffleOptimizationProblem.class);
    }

    /**
     * Searches for a problem gene with specific tests and fitness function
     *
     * @param hash The hash of the problem gene
     * @return A problem gene if it esists
     */
    // WARNING: Do not add a cypher with "match (n:TruffleOptimizationProblem{hash:{0}}) return n"
    // the cypher generated by spring also loads the children, and we NEED THEM
    public TruffleOptimizationProblem findByHash(String hash) {
        return findBy("hash", hash);
    }

    public Map<String, Long> findExistingHashes() {
        return this.getTransactionManager().execute((transaction) -> {
            Result result = transaction.run("match (n:TruffleOptimizationProblem) return n.hash, id(n)");
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