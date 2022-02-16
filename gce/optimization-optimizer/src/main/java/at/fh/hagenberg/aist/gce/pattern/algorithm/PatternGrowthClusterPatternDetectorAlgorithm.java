/*
 * Copyright (c) 2022 the original author or authors.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

package at.fh.hagenberg.aist.gce.pattern.algorithm;

import at.fh.hagenberg.aist.gce.optimization.util.TruffleLanguageInformation;
import at.fh.hagenberg.aist.gce.pattern.PatternNodeWrapper;
import at.fh.hagenberg.aist.gce.pattern.TruffleDifferentialPatternSolution;
import at.fh.hagenberg.aist.gce.pattern.TrufflePattern;
import at.fh.hagenberg.aist.gce.pattern.TrufflePatternProblem;
import at.fh.hagenberg.aist.gce.pattern.algorithm.editor.NodeEditor;
import at.fh.hagenberg.aist.gce.pattern.algorithm.editor.ValueAbstractingNodeEditor;
import at.fh.hagenberg.aist.gce.pattern.algorithm.labeller.VariableLabeller;
import at.fh.hagenberg.aist.gce.pattern.algorithm.metric.Metric;
import at.fh.hagenberg.aist.gce.pattern.encoding.BitwisePatternMeta;
import at.fh.hagenberg.aist.gce.pattern.encoding.TracableBitwisePattern;
import at.fh.hagenberg.machinelearning.analytics.graph.nodes.NodeWrapper;
import at.fh.hagenberg.machinelearning.analytics.graph.nodes.OrderedRelationship;
import at.fh.hagenberg.machinelearning.core.Problem;
import at.fh.hagenberg.machinelearning.core.Solution;
import at.fh.hagenberg.machinelearning.core.SolutionGene;
import at.fh.hagenberg.util.Pair;
import org.apache.commons.lang3.ArrayUtils;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Implements a rightmost pattern growth algorithm with clustering
 *
 * @author Oliver Krauss on 02.12.2020
 */
public class PatternGrowthClusterPatternDetectorAlgorithm extends AbstractPatternDetectorAlgorithm<TruffleDifferentialPatternSolution> {

    /**
     * Solution produced by the algorithm
     */
    protected Solution<TruffleDifferentialPatternSolution, TrufflePatternProblem> solution;

    private PatternGrowthPatternDetector detector = new PatternGrowthPatternDetector();

    @Override
    public Solution<TruffleDifferentialPatternSolution, TrufflePatternProblem> solve(Problem<TrufflePatternProblem> problem, Solution<TruffleDifferentialPatternSolution, TrufflePatternProblem> solution) {
        // ensure safe reuse
        logInitialized = false;
        logFinalized = false;
        this.solution = null;
        initializeLog(problem);
        this.solution = solution;

        // extract clusters
        List<TrufflePatternProblem> clusters = problem.getProblemGenes().stream().map(x -> x.getGene()).collect(Collectors.toList());

        // call IGOR
        Map<Long, NodeWrapper> wrapperMap = new HashMap<>();
        Map<Long, List<OrderedRelationship>> relationshipMap = new HashMap<>();
        Map<Integer, TrufflePatternProblem> clusterMap = new HashMap<>();
        Map<Long, String> variableMap = new HashMap<>();
        Map<Long, Map<String, Object>> nodeContentMap = new HashMap<>();
        List<TracableBitwisePattern> diffPatterns = detector.minePatterns(clusters, wrapperMap, relationshipMap, clusterMap, variableMap, nodeContentMap);

        // transform results
        Map<TrufflePattern, Map<Pair<TrufflePatternProblem, TrufflePatternProblem>, Long>> differential = new HashMap<>();
        Map<TrufflePatternProblem, List<TrufflePattern>> patternsPerProblem = new HashMap<>();

        // init patterns per problem
        clusters.forEach(x -> {
            patternsPerProblem.put(x, new ArrayList<>());
        });

        TruffleDifferentialPatternSolution tdpSolution = new TruffleDifferentialPatternSolution(patternsPerProblem, differential);
        solution.addGene(new SolutionGene<>(tdpSolution, problem.getProblemGenes()));

        diffPatterns.forEach(x -> {
            if (x == null) {
                return;
            }
            PatternNodeWrapper transform = detector.transform(x, wrapperMap, relationshipMap, detector.loadMeta(clusters), variableMap, nodeContentMap);
            NodeWrapper.reHash(transform);
            TrufflePattern pattern = new TrufflePattern(x, transform);
            pattern.setBitRepresentation(x);

            Map<Pair<TrufflePatternProblem, TrufflePatternProblem>, Long> diff = new HashMap<>();
            Collection<Integer> diffTo = new LinkedList<>(clusterMap.keySet());

            Collection<Integer> clusterIds = x.getClusterIds();
            clusterIds.forEach(c -> {
                // calculate diff for existing clusters
                diffTo.remove(c);
                diffTo.parallelStream().forEach(d -> {
                    long diffValue = x.getClusterCount(c) - x.getClusterCount(d);
                    diff.put(new Pair<>(clusterMap.get(c), clusterMap.get(d)), diffValue);
                });

                if (x.getClusterCount(c) > 0) {
                    patternsPerProblem.get(clusterMap.get(c)).add(new TrufflePattern(x, transform, c));
                }
            });
            differential.put(pattern, diff);
        });


        // return solution
        finalizeLog(problem);
        return solution;
    }

    @Override
    protected Solution<TruffleDifferentialPatternSolution, TrufflePatternProblem> getSolution() {
        return solution;
    }

    public NodeEditor<NodeWrapper> getEditor() {
        return detector.getEditor();
    }

    public void setEditor(NodeEditor<NodeWrapper> editor) {
        detector.setEditor(editor);
    }

    public VariableLabeller getVariableLabeller() {
        return detector.getVariableLabeller();
    }

    public void setVariableLabeller(VariableLabeller variableLabeller) {
        detector.setVariableLabeller(variableLabeller);
    }

    public boolean isEmbedded() {
        return detector.isEmbedded();
    }

    public void setEmbedded(boolean embedded) {
        detector.setEmbedded(embedded);
    }

    @Override
    public void setHierarchyFloor(int hierarchyFloor) {
        super.setHierarchyFloor(hierarchyFloor);
        detector.setHierarchyFloor(hierarchyFloor);
    }

    @Override
    public void setMaxPatternSize(int maxPatternSize) {
        super.setMaxPatternSize(maxPatternSize);
        detector.setMaxPatternSize(maxPatternSize);

    }

    @Override
    public void setHierarchyCeil(int hierarchyCeil) {
        super.setHierarchyCeil(hierarchyCeil);
        detector.setHierarchyCeil(hierarchyCeil);
    }

    public List<Metric> getMetrics() {
        return detector.getMetrics();
    }

    public void setMetrics(List<Metric> metrics) {
        detector.setMetrics(metrics);
    }
}
