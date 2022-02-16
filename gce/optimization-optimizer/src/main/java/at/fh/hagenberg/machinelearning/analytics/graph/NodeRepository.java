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
import at.fh.hagenberg.machinelearning.analytics.graph.nodes.NodeWrapper;
import org.neo4j.driver.*;

import java.util.HashMap;
import java.util.Map;

public class NodeRepository extends ReflectiveNeo4JNodeRepositoryImpl<NodeWrapper> {

    public NodeRepository(TransactionManager manager) throws NoSuchMethodException, ClassNotFoundException {
        super(manager, NodeWrapper.class);
    }

    /**
     * Finds a node by it's hash value
     * This includes both sub-trees as well as parents
     *
     * @param hash
     * @return the found node wrapper
     */
    public Iterable<NodeWrapper> findByHash(String hash) {
        return findAllBy("hash", hash);
    }

    public NodeWrapper findParentByHash(String hash) {
        return queryTyped("match (n:Node{hash: $hash}) where not ()-[:CHILD]->(n) return n", Values.parameters("hash", hash));
    }

    public Map<String, Long> findExistingHashes() {
        return this.getTransactionManager().execute((transaction) -> {
            Result result = transaction.run("match (n:Node) where not ()-[:CHILD]->(n) return n.hash, id(n)");
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
