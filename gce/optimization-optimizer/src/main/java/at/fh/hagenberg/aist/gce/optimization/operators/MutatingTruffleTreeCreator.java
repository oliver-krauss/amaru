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

import at.fh.hagenberg.aist.gce.optimization.ProfileKeeper;
import at.fh.hagenberg.aist.gce.optimization.TruffleOptimizationProblem;
import at.fh.hagenberg.aist.gce.optimization.TruffleOptimizationSolution;
import at.fh.hagenberg.machinelearning.analytics.TruffleGraphAnalytics;
import at.fh.hagenberg.machinelearning.analytics.graph.nodes.OperationNode;
import at.fh.hagenberg.machinelearning.core.ProblemGene;
import at.fh.hagenberg.machinelearning.core.options.Descriptor;
import org.springframework.beans.factory.annotation.Required;

import java.util.HashMap;
import java.util.Map;

/**
 * Instantiation method for the first generation in GI,
 * creates tree by mutating the original
 * Created by Oliver Krauss on 10.02.2017.
 */
public class MutatingTruffleTreeCreator implements ConfigurableGeneCreator<TruffleOptimizationSolution, TruffleOptimizationProblem> {

    private TruffleGraphAnalytics analyticsService;

    private ConfigurableMutator<TruffleOptimizationSolution, TruffleOptimizationProblem> mutator = new TruffleTreeMutator();

    @Override
    public TruffleOptimizationSolution createGene(TruffleOptimizationProblem gene) {
        long start = ProfileKeeper.profiler.start();
        // create node
        TruffleOptimizationSolution solution = new TruffleOptimizationSolution(gene.getNode(), gene, this);
        TruffleOptimizationSolution mutate = mutator.mutate(solution);
        start = ProfileKeeper.profiler.profile("MutatingTruffleTreeCreator.mutate", start);
        if (analyticsService != null) {
            OperationNode operation = new OperationNode("create");
            operation.addOutput(mutate.getTree());
            analyticsService.saveOperation(operation);
        }
        ProfileKeeper.profiler.profile("MutatingTruffleTreeCreator.mutateLOG", start);
        return mutate;
    }

    @Override
    public TruffleOptimizationSolution createGene(ProblemGene<TruffleOptimizationProblem> problemGene) {
        return createGene(problemGene.getGene());
    }

    @Override
    public void setAnalyticsService(TruffleGraphAnalytics analyticsService) {
        this.analyticsService = analyticsService;
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

    @Required
    public void setMutator(ConfigurableMutator<TruffleOptimizationSolution, TruffleOptimizationProblem> mutator) {
        this.mutator = mutator;
        if (mutator instanceof TruffleTreeMutator) {
            // mutator is not allowed to log
            ((TruffleTreeMutator) mutator).setAnalyticsService(null);
        }
    }

    public ConfigurableMutator<TruffleOptimizationSolution, TruffleOptimizationProblem> getMutator() {
        return mutator;
    }
}
