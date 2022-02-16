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

import at.fh.hagenberg.machinelearning.analytics.graph.nodes.NodeWrapper;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Printer for Pattern Analysis reports
 *
 * @author Oliver Krauss on 23.10.2019
 */
public class PatternConfidencePrinter extends FreemarkerPrinter {

    /**
     * Identifier of runtime profile template (single)
     */
    private static final String PATTERNCONFIDENCE_TEMPLATE = "patternConfidence";

    /**
     * Debug flag, adding additional information to the prints
     */
    private boolean debug = false;

    public PatternConfidencePrinter(String templateDirectory) {
        this(templateDirectory, false);
    }

    public PatternConfidencePrinter(String templateDirectory, boolean debug) {
        super(templateDirectory);
        this.setSupportedFormats(Arrays.asList("html"));
        this.setFormat("html");
        this.addTemplate(PATTERNCONFIDENCE_TEMPLATE, PATTERNCONFIDENCE_TEMPLATE);
        this.configuration.setAPIBuiltinEnabled(true);
        this.setDebug(debug);
    }

    /**
     * @param debug if debug output should be printed as well
     */
    public PatternConfidencePrinter(boolean debug) {
        this(null, debug);
    }

    public PatternConfidencePrinter() {
        this(null);
    }

    /**
     * Temporary Id counter
     */
    long tempId = 0;

    /**
     * Helper to write temporary ids
     *
     * @param node with temporary ids
     */
    private void tempId(NodeWrapper node) {
        node.setId(tempId++);
        node.getChildren().forEach(x -> tempId(x.getChild()));
    }

    /**
     * Modify nodes for printing
     *
     * @param node
     */
    private NodeWrapper prepareNode(NodeWrapper node) {
        node = node.deepCopy();
        // Nodes MUST have Ids. If they don't we have to create them
        if (node.getId() == null) {
            tempId = 0;
            tempId(node);
        }
        return node;
    }

    /**
     * Prints the confidence data for a given pattern
     * @param name           Human readable name
     * @param patterns       Pattern under test
     * @param antiPatterns   Anti-Patterns under test
     * @param exceptions     Exceptions that occured during the test
     * @param confidence     Confidence Score (hard -> only EXACT)
     * @param softConfidence Soft Confidence Score (any exception)
     */
    public void printConfidence(String name, List<NodeWrapper> patterns, List<NodeWrapper> antiPatterns, HashMap<String, Integer> exceptions, double confidence, double softConfidence) {
        this.addAdditionalTemplateValue("name", name);
        this.addAdditionalTemplateValue("exceptions", exceptions);
        this.addAdditionalTemplateValue("confidence", confidence);
        this.addAdditionalTemplateValue("softConfidence", softConfidence);

        List<NodeWrapper> preppedPatterns = new ArrayList<>();
        patterns.forEach(x -> preppedPatterns.add(prepareNode(x)));
        List<NodeWrapper> preppedAntiPatterns = new ArrayList<>();
        antiPatterns.forEach(x -> preppedAntiPatterns.add(prepareNode(x)));
        this.addAdditionalTemplateValue("antiPatterns", preppedAntiPatterns);
        super.transform(PATTERNCONFIDENCE_TEMPLATE, preppedPatterns);
    }

    public boolean isDebug() {
        return debug;
    }

    public void setDebug(boolean debug) {
        this.debug = debug;
        this.addAdditionalTemplateValue("debug", debug);
    }

}
