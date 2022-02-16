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

import at.fh.hagenberg.aist.gce.optimization.test.TruffleOptimizationTest;
import science.aist.neo4j.reflective.ReflectiveNeo4JNodeRepositoryImpl;
import science.aist.neo4j.transaction.TransactionManager;
import org.neo4j.driver.AccessMode;
import org.neo4j.driver.Record;
import org.neo4j.driver.Result;

import java.util.HashMap;
import java.util.Map;

public class TruffleTestCaseRepository extends ReflectiveNeo4JNodeRepositoryImpl<TruffleOptimizationTest> {


    public TruffleTestCaseRepository(TransactionManager manager) throws NoSuchMethodException, ClassNotFoundException {
        super(manager, TruffleOptimizationTest.class);
    }

    public TruffleOptimizationTest findByHash(String hash) {
        return findBy("hash", hash);
    }

    public Map<String, Long> findExistingHashes() {
        return this.getTransactionManager().execute((transaction) -> {
            Result result = transaction.run("match (n:TruffleOptimizationTest) return n.hash, id(n)");
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
