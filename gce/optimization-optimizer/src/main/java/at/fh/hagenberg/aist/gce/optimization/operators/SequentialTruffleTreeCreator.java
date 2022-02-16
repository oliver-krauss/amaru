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
import at.fh.hagenberg.machinelearning.analytics.TruffleGraphAnalytics;
import at.fh.hagenberg.machinelearning.analytics.graph.nodes.NodeWrapper;
import at.fh.hagenberg.machinelearning.analytics.graph.nodes.OperationNode;
import at.fh.hagenberg.machinelearning.core.ProblemGene;
import at.fh.hagenberg.machinelearning.core.mapping.SequentialGeneCreator;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.MaterializedFrame;
import com.oracle.truffle.api.nodes.Node;
import at.fh.hagenberg.aist.gce.optimization.TruffleOptimizationProblem;
import at.fh.hagenberg.aist.gce.optimization.TruffleOptimizationSolution;
import at.fh.hagenberg.aist.gce.optimization.util.strategy.sequential.TruffleNodeSequenceCreator;
import at.fh.hagenberg.aist.gce.optimization.util.strategy.sequential.TruffleSequentialStrategy;

/**
 * Implementation for Truffle creating every possible tree. It remembers which class was used first
 */
public class SequentialTruffleTreeCreator implements SequentialGeneCreator<TruffleOptimizationSolution, TruffleOptimizationProblem> {

    private TruffleGraphAnalytics analyticsService;

    private TruffleSequentialStrategy sequenceCreator;

    FrameDescriptor localFrame;

    MaterializedFrame globalFrame;

    Object context;

    String contextClass;

    private TruffleSequentialStrategy getSequenceCreator(TruffleOptimizationProblem problem) {
        if (sequenceCreator == null) {
            sequenceCreator = new TruffleNodeSequenceCreator(problem.getSearchSpace(),
                problem.getSearchSpace().getInstantiableNodes().keySet(),
                problem.getConfiguration().getMaxDepth(),
                1,
                problem.getConfiguration().getMaxWidth(),
                localFrame,
                globalFrame,
                context,
                contextClass);
        }
        return sequenceCreator;
    }

    @Override
    public TruffleOptimizationSolution createGene(ProblemGene<TruffleOptimizationProblem> problemGene) {
        long start = ProfileKeeper.profiler.start();
        if (getSequenceCreator(problemGene.getGene()).hasNext()) {
            Node node = (Node) sequenceCreator.next();
            TruffleOptimizationSolution solution = new TruffleOptimizationSolution(node, problemGene.getGene(), this);

            if (analyticsService != null) {
                OperationNode operation = new OperationNode("sequentialCreate");
                operation.addOutput(NodeWrapper.wrap(node));
                analyticsService.saveOperation(operation);
            }
            ProfileKeeper.profiler.profile("SequentialTruffleTreeCreator.mutate", start);
            return solution;
        }
        return null;
    }

    @Override
    public boolean hasNext() {
        return sequenceCreator == null ? false : sequenceCreator.hasNext();
    }

    public void setAnalyticsService(TruffleGraphAnalytics analyticsService) {
        this.analyticsService = analyticsService;
    }

    public void setLocalFrame(FrameDescriptor localFrame) {
        this.localFrame = localFrame;
    }

    public void setGlobalFrame(MaterializedFrame globalFrame) {
        this.globalFrame = globalFrame;
    }

    public void setContext(Object context) {
        this.context = context;
    }

    public void setContextClass(String contextClass) {
        this.contextClass = contextClass;
    }
}
