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


import at.fh.hagenberg.aist.gce.pattern.encoding.BitwisePattern;
import at.fh.hagenberg.aist.gce.pattern.encoding.BitwisePatternMeta;
import at.fh.hagenberg.machinelearning.analytics.graph.nodes.NodeWrapper;
import org.neo4j.driver.AccessMode;
import org.neo4j.driver.Record;
import org.neo4j.driver.Result;
import org.neo4j.driver.Values;
import science.aist.neo4j.reflective.ReflectiveNeo4JNodeRepositoryImpl;
import science.aist.neo4j.transaction.TransactionManager;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Repository for patterns and anti-patterns
 *
 * TODO #258 move this into a database and create an interface that allows context loading
 *
 * @author Oliver Krauss on 3.6.2021
 */
public abstract class PatternRepository {

    private static Map<String, PatternRepository> languageRepositories = new HashMap<>();

    public static PatternRepository loadForLanguage(String language) {
        return languageRepositories.getOrDefault(language, null);
    }

    public static void register(String language, PatternRepository repository) {
        languageRepositories.put(language, repository);
    }

    public abstract HashMap<NodeWrapper, BitwisePatternMeta> loadAntipatterns();

    public abstract HashMap<NodeWrapper, BitwisePatternMeta>  loadPatterns();
}
