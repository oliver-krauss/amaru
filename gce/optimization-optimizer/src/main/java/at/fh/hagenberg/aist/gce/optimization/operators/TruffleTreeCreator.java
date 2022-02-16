/*
 * Copyright (c) 2022 the original author or authors.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

package at.fh.hagenberg.aist.gce.optimization.operators;

import at.fh.hagenberg.aist.gce.optimization.TruffleOptimizationProblem;
import at.fh.hagenberg.aist.gce.optimization.TruffleOptimizationSolution;
import at.fh.hagenberg.aist.gce.optimization.util.strategy.TruffleSimpleStrategy;
import at.fh.hagenberg.machinelearning.analytics.TruffleGraphAnalytics;
import at.fh.hagenberg.machinelearning.analytics.graph.nodes.OperationNode;
import at.fh.hagenberg.machinelearning.core.ProblemGene;
import at.fh.hagenberg.machinelearning.core.options.Descriptor;
import com.oracle.truffle.api.nodes.Node;
import org.springframework.beans.factory.annotation.Required;

import java.util.HashMap;
import java.util.Map;

/**
 * Instantiation method for the first generation in GA,
 * creates Random trees.
 * Created by Oliver Krauss on 10.02.2017.
 */
public class TruffleTreeCreator implements ConfigurableGeneCreator<TruffleOptimizationSolution, TruffleOptimizationProblem> {

    private TruffleGraphAnalytics analyticsService;

    private TruffleSimpleStrategy<Node> strategy;

    @Override
    public TruffleOptimizationSolution createGene(TruffleOptimizationProblem gene) {
        // create node
        Node node = null;

        // create node
        // TODO #230 The data flow graph is not set. BUG for global variables
        node = strategy.next();

        TruffleOptimizationSolution solution = new TruffleOptimizationSolution(node, gene, this);
        if (analyticsService != null) {
            OperationNode operation = new OperationNode("create");
            operation.addOutput(solution.getTree());
            analyticsService.saveOperation(operation);
        }

        return solution;
    }

    @Override
    public TruffleOptimizationSolution createGene(ProblemGene<TruffleOptimizationProblem> problemGene) {
        return createGene(problemGene.getGene());
    }

    @Override
    public void setAnalyticsService(TruffleGraphAnalytics analyticsService) {
        this.analyticsService = analyticsService;
    }

    @Required
    public void setStrategy(TruffleSimpleStrategy<Node> strategy) {
        this.strategy = strategy;
    }


    @Override
    public Map<String, Descriptor> getOptions() {
        Map<String, Descriptor> options = new HashMap<>();
        return options;
    }

    @Override
    public boolean setOption(String name, Descriptor descriptor) {
        return false;
    }
}
