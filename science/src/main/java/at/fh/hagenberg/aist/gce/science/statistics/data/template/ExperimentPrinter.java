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
import at.fh.hagenberg.machinelearning.analytics.graph.nodes.NodeWrapper;
import at.fh.hagenberg.util.Pair;

import java.util.*;
import java.util.function.IntToDoubleFunction;
import java.util.stream.Collectors;

/**
 * Printer for Pattern Analysis reports
 *
 * @author Oliver Krauss on 23.10.2019
 */
public class ExperimentPrinter extends FreemarkerPrinter {

    /**
     * Identifier of runtime profile template (single)
     */
    private static final String EXPERIMENT_TEMPLATE = "experiment";

    /**
     * Debug flag, adding additional information to the prints
     */
    private boolean debug = false;

    public ExperimentPrinter(String templateDirectory) {
        this(templateDirectory, false);
    }

    public ExperimentPrinter(String templateDirectory, boolean debug) {
        super(templateDirectory);
        this.setSupportedFormats(Arrays.asList("html"));
        this.setFormat("html");
        this.addTemplate(EXPERIMENT_TEMPLATE, EXPERIMENT_TEMPLATE);
        this.configuration.setAPIBuiltinEnabled(true);
        this.setDebug(debug);
    }

    /**
     * @param debug if debug output should be printed as well
     */
    public ExperimentPrinter(boolean debug) {
        this(null, debug);
    }

    public ExperimentPrinter() {
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
     * Prints the Experiment statistical data
     *
     * @param name        of experiment
     * @param parameters  the experiment was conducted with
     * @param steps       step node information
     * @param experiment  experiment individuals
     * @param originalAst original AST
     * @param bestAst     best found AST
     */
    public void printExperiment(String name, Map<String, Object> parameters, List<Map<String, Object>> steps, List<List<IndividualInformation>> experiment, NodeWrapper originalAst, NodeWrapper bestAst, double originalQuality) {
        this.addAdditionalTemplateValue("name", name);
        this.addAdditionalTemplateValue("parameters", parameters);
        this.addAdditionalTemplateValue("steps", steps);
        this.addAdditionalTemplateValue("originalAst", prepareNode(originalAst));
        this.addAdditionalTemplateValue("bestAst", prepareNode(bestAst));
        Map<String, Object> statistics = new HashMap<>();

        Set<NodeWrapper> uniqueASTs = new HashSet<>();
        List<Map<String, Integer>> stepsCount = new ArrayList<>();
        experiment.forEach(population -> {
            HashMap<String, Integer> stepAdd = new HashMap<>();
            stepAdd.put("fail", 0);
            stepAdd.put("type", 0);
            stepAdd.put("success", 0);
            stepAdd.put("solved", 0);
            population.forEach(individual -> {
                uniqueASTs.add(individual.getAst());
                if (individual.getQuality() <= originalQuality) {
                    stepAdd.put("solved", stepAdd.get("solved") + 1);
                } else if (individual.getQuality() <= 1.9999) {
                    stepAdd.put("success", stepAdd.get("success") + 1);
                } else if (individual.getQuality() <= 9.999) {
                    stepAdd.put("type", stepAdd.get("type") + 1);
                } else {
                    stepAdd.put("fail", stepAdd.get("fail") + 1);
                }
            });
            stepsCount.add(stepAdd);
        });
        List<Integer> sizes = uniqueASTs.stream().filter(Objects::nonNull).map(x -> NodeWrapper.size(x)).collect(Collectors.toList());
        statistics.put("initialAST", NodeWrapper.size(originalAst));
        statistics.put("bestAST", NodeWrapper.size(bestAst));
        statistics.put("maxAST", sizes.stream().max(Integer::compareTo).get());
        statistics.put("minAST", sizes.stream().min(Integer::compareTo).get());
        statistics.put("avgAST", sizes.stream().mapToDouble(x -> (double) x).average().orElse(-1.0));
        sizes.sort(Integer::compareTo);
        double median = sizes.get(sizes.size() / 2);
        if (sizes.size() % 2 == 0) median = (median + sizes.get(sizes.size() / 2 - 1)) / 2;
        statistics.put("medianAST", median);
        statistics.put("numberASTs", sizes.size());
        statistics.put("sumNodes", sizes.stream().mapToInt(x -> x).sum());


        this.addAdditionalTemplateValue("statistics", statistics);
        this.addAdditionalTemplateValue("stepsCount", stepsCount);
        super.transform(EXPERIMENT_TEMPLATE, experiment);
    }

    public boolean isDebug() {
        return debug;
    }

    public void setDebug(boolean debug) {
        this.debug = debug;
        this.addAdditionalTemplateValue("debug", debug);
    }

}
