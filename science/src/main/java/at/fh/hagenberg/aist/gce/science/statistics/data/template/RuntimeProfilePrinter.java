/*
 * Copyright (c) 2022 the original author or authors.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

package at.fh.hagenberg.aist.gce.science.statistics.data.template;

import at.fh.hagenberg.aist.gce.optimization.runtime.RuntimeProfile;
import at.fh.hagenberg.aist.gce.pattern.TruffleDifferentialPatternSolution;
import at.fh.hagenberg.aist.gce.pattern.TrufflePattern;
import at.fh.hagenberg.aist.gce.pattern.TrufflePatternProblem;
import at.fh.hagenberg.machinelearning.analytics.graph.nodes.NodeWrapper;
import at.fh.hagenberg.machinelearning.analytics.graph.nodes.OrderedRelationship;
import at.fh.hagenberg.machinelearning.core.Gene;
import at.fh.hagenberg.machinelearning.core.Solution;
import at.fh.hagenberg.util.Pair;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Printer for Pattern Analysis reports
 *
 * @author Oliver Krauss on 23.10.2019
 */
public class RuntimeProfilePrinter extends FreemarkerPrinter {

    /**
     * Identifier of runtime profile template (single)
     */
    private static final String RUNTIME_PROFILE_TEMPLATE = "runtimeProfile";

    /**
     * Identifier of runtime profile template (group) -> used to compare different profiles of same alg
     */
    private static final String RUNTIME_PROFILE_GROUP_TEMPLATE = "runtimeProfileGroup";

    /**
     * Identifier of the test group template
     */
    private static final String RUNTIME_TEST_PROFILE_TEMPLATE = "runtimeTestProfile";

    /**
     * Debug flag, adding additional information to the prints
     */
    private boolean debug = false;

    public RuntimeProfilePrinter(String templateDirectory) {
        this(templateDirectory, false);
    }

    public RuntimeProfilePrinter(String templateDirectory, boolean debug) {
        super(templateDirectory);
        this.setSupportedFormats(Arrays.asList("html"));
        this.setFormat("html");
        this.addTemplate(RUNTIME_PROFILE_TEMPLATE, RUNTIME_PROFILE_TEMPLATE);
        this.addTemplate(RUNTIME_PROFILE_GROUP_TEMPLATE, RUNTIME_PROFILE_GROUP_TEMPLATE);
        this.addTemplate(RUNTIME_TEST_PROFILE_TEMPLATE, RUNTIME_TEST_PROFILE_TEMPLATE);
        this.configuration.setAPIBuiltinEnabled(true);
        this.setDebug(debug);
    }

    /**
     * @param debug if debug output should be printed as well
     */
    public RuntimeProfilePrinter(boolean debug) {
        this(null, debug);
    }

    public RuntimeProfilePrinter() {
        this(null);
    }

    /**
     * Prints a runtime profile
     *
     * @param profile to be printed
     */
    public void printProfile(RuntimeProfile profile, String name, long[] perfMeasurements) {
        this.addAdditionalTemplateValue("name", name);
        this.addAdditionalTemplateValue("perfMeasurements", perfMeasurements);
        super.transform(RUNTIME_PROFILE_TEMPLATE, profile);
    }

    /**
     * Prints a profile for a group of tests
     *
     * @param tests to be printed
     */
    public void printProfile(String name, Map<String, String> testInfo, Map<String, Pair<RuntimeProfile, long[]>> tests) {
        this.addAdditionalTemplateValue("name", name);
        this.addAdditionalTemplateValue("testInfo", testInfo);
        List<String> sortedKeys = tests.keySet().stream().sorted((o1, o2) -> {
                                    long m1 = tests.get(o1).getKey().getMinimum();
                                    long m2 = tests.get(o2).getKey().getMinimum();
                                    return Long.compare(m1, m2);
                                }).collect(Collectors.toList());
        this.addAdditionalTemplateValue("sortedKeys", sortedKeys);
        super.transform(RUNTIME_TEST_PROFILE_TEMPLATE, tests);
    }

    public boolean isDebug() {
        return debug;
    }

    public void setDebug(boolean debug) {
        this.debug = debug;
        this.addAdditionalTemplateValue("debug", debug);
    }

    public void printProfileGroup(String name, Map<String, Pair<RuntimeProfile, long[]>> runtimes) {
        this.addAdditionalTemplateValue("name", name);
        List<String> sortedKeys = runtimes.keySet().stream().sorted().collect(Collectors.toList());
        this.addAdditionalTemplateValue("sortedKeys", sortedKeys);
        super.transform(RUNTIME_PROFILE_GROUP_TEMPLATE, runtimes);
    }
}
