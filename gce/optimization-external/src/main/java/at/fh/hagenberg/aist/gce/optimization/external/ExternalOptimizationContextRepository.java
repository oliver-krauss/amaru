/*
 * Copyright (c) 2022 the original author or authors.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

package at.fh.hagenberg.aist.gce.optimization.external;

import at.fh.hagenberg.aist.gce.optimization.TruffleOptimizationProblem;
import at.fh.hagenberg.aist.gce.optimization.util.TruffleLanguageSearchSpace;
import at.fh.hagenberg.aist.gce.optimization.util.strategy.DefaultStrategyUtil;
import at.fh.hagenberg.aist.gce.optimization.util.strategy.TruffleHierarchicalStrategy;
import at.fh.hagenberg.aist.gce.optimization.util.strategy.TruffleSimpleStrategy;
import at.fh.hagenberg.aist.gce.optimization.util.strategy.TruffleVerifyingStrategy;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Repository that provides the Truffle language context. Needs to be implemented for every language.
 *
 * @author Oliver Krauss on 17.04.2019
 */
public abstract class ExternalOptimizationContextRepository {

    /**
     * Map of all repositories. Each language must have its own
     */
    private static Map<Long, ExternalOptimizationContextRepository> repositories = new HashMap<>();

    public static void registerRepository(Long languageId, ExternalOptimizationContextRepository repository) {
        repositories.put(languageId, repository);
    }

    public static ExternalOptimizationContextRepository getRepository(Long languageId) {
        return repositories.get(languageId);
    }

    public static Map<Long, ExternalOptimizationContextRepository> getRepositories() {
        return repositories;
    }

    public static boolean hasRepository(Long languageId) {
        return repositories.containsKey(languageId);
    }

    /**
     * Creates a truffle problem out of a filename that needs to be available
     *
     * @param file     file to be optimized
     * @param function function IN FILE to be optimized
     * @param input    test case input
     * @param output   test case output
     * @return optimization problem
     */
    public abstract TruffleOptimizationProblem getProblem(TruffleLanguageSearchSpace space, String file, String function, String input, String output, String evaluationIdentity);

    /**
     * Provides the FULL Language Information
     *
     * @return truffle language information
     */
    public abstract String getLanguage();

    public List<TruffleHierarchicalStrategy> getStrategies(String file) {
        return new ArrayList<>();
    }

    public Map<String, TruffleVerifyingStrategy> getTerminalStrategies(String file) {
        return DefaultStrategyUtil.defaultStrategies();
    }
}
