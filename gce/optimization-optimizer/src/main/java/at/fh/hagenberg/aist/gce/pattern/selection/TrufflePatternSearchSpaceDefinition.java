/*
 * Copyright (c) 2022 the original author or authors.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

package at.fh.hagenberg.aist.gce.pattern.selection;

import at.fh.hagenberg.aist.gce.optimization.test.TruffleOptimizationTestResult;
import at.fh.hagenberg.aist.gce.pattern.TrufflePattern;
import at.fh.hagenberg.aist.gce.pattern.TrufflePatternProblem;
import at.fh.hagenberg.aist.gce.pattern.constraint.*;
import at.fh.hagenberg.machinelearning.analytics.graph.nodes.NodeWrapper;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/**
 * This class is a definitions class for the search space of a TrufflePatternProblem
 * It is used by the {@link PatternSearchSpaceRepository} to generate a TrufflePatternSearchSpace for the {@link TrufflePatternProblem}
 * Please note that all INCLUDES supercede EXCLUDES. Also if an include is already defined the exclude cannot be done.
 * If an exclude exists that is incompatible with an added include, the exclude will automatically be removed.
 */
public class TrufflePatternSearchSpaceDefinition {

    /**
     * Constraint on patterns that should be only searched in specific cachet.
     * If null, cachets won't be restricted at all
     */
    List<CachetConstraint> cachets;

    /**
     * Constraint on the solution in which patterns should be found
     */
    private SolutionConstraint solution;

    /**
     * Problem(s) that the solutions must have tried to solve.
     */
    private List<ProblemConstraint> problems;

    /**
     * Experiment(s) that the solutions must have occured in.
     */
    private List<ExperimentConstraint> experiments;

    /**
     * Test Values (or types) that the problem must have had
     */
    private List<TestValueConstraint> testValues;

    /**
     * Test Results that the problem must have. Only used to search for specific exceptions (or lack thereof)
     */
    private List<TestResultConstraint> testResult;

    /**
     * Patterns that a tree must contain to be included in the search space
     * The outer list is an OR constraint, The inner list an AND
     * ex. (patterns.0.0 AND patterns.0.1) OR (patterns.1.0)
     */
    private List<List<TrufflePattern>>  patterns;

    /**
     * Trees that will be skipped ENTIRELY in the mining (except if the IncludedTreeIds forces them back in again)
     */
    private Set<Long> excludedTreeIds;

    /**
     * Nodes that will be skipped IF they occur in the search space.
     */
    private Set<Long> excludedNodeIds;

    /**
     * Nodes that will be skipped if they are of that type (does NOT consider the hierarchy just that explicit type)
     */
    private Set<String> excludedTypes;

    /**
     * trees that will be ADDED in the mining, even if they do NOT occur in the search space.
     * This will not add the ENTIRE tree, just add it to the searchspace. All other Excludes and Includes will still be applied to the nodes.
     */
    private Set<Long> includedTreeIds;

    /**
     * nodes that will be ADDED in the mining (only if the node is in a tree in the search space)
     */
    private Set<Long> includedNodeIds;

    /**
     * Types that WILL be included even if any exclude would have prevented it
     */
    private Set<String> includedTypes;


    public TrufflePatternSearchSpaceDefinition() {

    }

    public TrufflePatternSearchSpaceDefinition(List<CachetConstraint> cachets, SolutionConstraint solution, List<ProblemConstraint> problems, List<ExperimentConstraint> experiments, List<TestValueConstraint> testValues, boolean solutionSpace) {
        this.cachets = cachets;
        this.solution = solution;
        this.problems = problems;
        this.experiments = experiments;
        this.testValues = testValues;
        this.solutionSpace = solutionSpace;
    }

    public TrufflePatternSearchSpaceDefinition(Set<Long> excludedTreeIds, Set<Long> excludedNodeIds, Set<String> excludedTypes, Set<Long> includedTreeIds, Set<Long> includedNodeIds, Set<String> includedTypes) {
        this.excludedTreeIds = excludedTreeIds;
        this.excludedNodeIds = excludedNodeIds;
        this.excludedTypes = excludedTypes;
        this.includedTreeIds = includedTreeIds;
        this.includedNodeIds = includedNodeIds;
        this.includedTypes = includedTypes;
    }

    public TrufflePatternSearchSpaceDefinition(List<CachetConstraint> cachets, SolutionConstraint solution, List<ProblemConstraint> problems, List<ExperimentConstraint> experiments, List<TestValueConstraint> testValues, Set<Long> excludedTreeIds, Set<Long> excludedNodeIds, Set<String> excludedTypes, Set<Long> includedTreeIds, Set<Long> includedNodeIds, Set<String> includedTypes, boolean solutionSpace) {
        this.cachets = cachets;
        this.solution = solution;
        this.problems = problems;
        this.experiments = experiments;
        this.testValues = testValues;
        this.excludedTreeIds = excludedTreeIds;
        this.excludedNodeIds = excludedNodeIds;
        this.excludedTypes = excludedTypes;
        this.includedTreeIds = includedTreeIds;
        this.includedNodeIds = includedNodeIds;
        this.includedTypes = includedTypes;
        this.solutionSpace = solutionSpace;
    }

    public TrufflePatternSearchSpaceDefinition(List<CachetConstraint> cachets, SolutionConstraint solution, List<ProblemConstraint> problems, List<ExperimentConstraint> experiments, List<TestValueConstraint> testValues, List<TestResultConstraint> testResult, List<List<TrufflePattern>> patterns, Set<Long> excludedTreeIds, Set<Long> excludedNodeIds, Set<String> excludedTypes, Set<Long> includedTreeIds, Set<Long> includedNodeIds, Set<String> includedTypes, boolean solutionSpace) {
        this.cachets = cachets;
        this.solution = solution;
        this.problems = problems;
        this.experiments = experiments;
        this.testValues = testValues;
        this.testResult = testResult;
        this.patterns = patterns;
        this.excludedTreeIds = excludedTreeIds;
        this.excludedNodeIds = excludedNodeIds;
        this.excludedTypes = excludedTypes;
        this.includedTreeIds = includedTreeIds;
        this.includedNodeIds = includedNodeIds;
        this.includedTypes = includedTypes;
        this.solutionSpace = solutionSpace;
    }

    /**
     * Includes the given tree from the search space
     *
     * @param tree to be excluded
     */
    public void includeTree(NodeWrapper tree) {
        includeTree(tree.getId());
    }

    /**
     * Includes all trees from the given pattern
     *
     * @param pattern to be included
     */
    public void includeTree(TrufflePattern pattern) {
        pattern.getTreeIds().forEach(x -> includeTree(x));
    }

    /**
     * Includes the entire tree from the run
     * If tree is excluded it will
     *
     * @param id of tree to be included
     */
    public void includeTree(Long id) {
        if (includedTreeIds == null) {
            includedTreeIds = new HashSet<>();
        }
        if (excludedTreeIds == null) {
            excludedTreeIds = new HashSet<>();
        }
        includedTreeIds.add(id);
        excludedTreeIds.remove(id);
    }

    /**
     * Excludes the given tree from the search space
     *
     * @param tree to be excluded
     */
    public void excludeTree(NodeWrapper tree) {
        excludeTree(tree.getId());
    }

    /**
     * Excludes all trees from the given pattern
     *
     * @param pattern to be included
     */
    public void excludeTree(TrufflePattern pattern) {
        pattern.getTreeIds().forEach(x -> excludeTree(x));
    }

    /**
     * Excludes the entire tree from the run
     *
     * @param id of tree to be excluded
     */
    public void excludeTree(Long id) {
        if (includedTreeIds == null) {
            includedTreeIds = new HashSet<>();
        }
        if (excludedTreeIds == null) {
            excludedTreeIds = new HashSet<>();
        }
        if (!includedTreeIds.contains(id)) {
            excludedTreeIds.add(id);
        }
    }


    /**
     * Excludes node from search space ONLY IF the node is not forced to be included
     *
     * @param node      to be excluded
     * @param recursive if the exclusion is recursive
     */
    public void excludeNode(NodeWrapper node, boolean recursive) {
        excludeNode(node.getId());
        if (recursive) {
            node.getChildren().forEach(x -> excludeNode(x.getChild(), true));
        }
    }

    /**
     * Excludes all nodes that are contained in this pattern
     *
     * @param pattern to be excluded
     */
    public void excludeNode(TrufflePattern pattern) {
        pattern.getNodeIds().forEach(x -> excludeNode(x));
    }

    /**
     * Excludes node from search space ONLY IF the node is not forced to be included
     *
     * @param id id of node to be excluded
     */
    public void excludeNode(Long id) {
        if (includedNodeIds == null) {
            includedNodeIds = new HashSet<>();
        }
        if (excludedNodeIds == null) {
            excludedNodeIds = new HashSet<>();
        }
        if (!includedNodeIds.contains(id)) {
            excludedNodeIds.add(id);
        }
    }

    /**
     * Excludes the data type from the search. Does NOT apply to the types that descend from this one!
     *
     * @param type
     */
    public void excludeType(String type) {
        if (includedTypes == null) {
            includedTypes = new HashSet<>();
        }
        if (excludedTypes == null) {
            excludedTypes = new HashSet<>();
        }
        if (!includedTypes.contains(type)) {
            excludedTypes.add(type);
        }
    }

    /**
     * Includes a node in the search space no matter what the exclusions say..
     *
     * @param node      to be included
     * @param recursive if the exclusion is recursive
     */
    public void includeNode(NodeWrapper node, boolean recursive) {
        includeNode(node.getId());
        if (recursive) {
            node.getChildren().forEach(x -> includeNode(x.getChild(), true));
        }
    }

    /**
     * Excludes all nodes that are contained in this pattern
     *
     * @param pattern to be excluded
     */
    public void includeNode(TrufflePattern pattern) {
        pattern.getNodeIds().forEach(this::includeNode);
    }

    /**
     * Includes a node in the search space no matter what the exclusions say.
     *
     * @param id
     */
    public void includeNode(Long id) {
        if (includedNodeIds == null) {
            includedNodeIds = new HashSet<>();
        }
        if (excludedNodeIds == null) {
            excludedNodeIds = new HashSet<>();
        }
        includedNodeIds.add(id);
        excludedNodeIds.remove(id);
    }

    /**
     * Forces include the data type from the search. Does NOT apply to the types that descend from this one!
     *
     * @param type
     */
    public void includeType(String type) {
        if (includedTypes == null) {
            includedTypes = new HashSet<>();
        }
        if (excludedTypes == null) {
            excludedTypes = new HashSet<>();
        }
        includedTypes.add(type);
        excludedTypes.remove(type);
    }

    /**
     * Searching patterns in the solution space or in the target
     */
    private boolean solutionSpace;

    public List<CachetConstraint> getCachets() {
        return cachets;
    }

    public void setCachets(List<CachetConstraint> cachets) {
        this.cachets = cachets;
    }

    public SolutionConstraint getSolution() {
        return solution;
    }

    public void setSolution(SolutionConstraint solution) {
        this.solution = solution;
    }

    public List<ProblemConstraint> getProblems() {
        return problems;
    }

    public void setProblems(List<ProblemConstraint> problems) {
        this.problems = problems;
    }

    public List<ExperimentConstraint> getExperiments() {
        return experiments;
    }

    public void setExperiments(List<ExperimentConstraint> experiments) {
        this.experiments = experiments;
    }

    public List<TestValueConstraint> getTestValues() {
        return testValues;
    }

    public void setTestValues(List<TestValueConstraint> testValues) {
        this.testValues = testValues;
    }

    public boolean isSolutionSpace() {
        return solutionSpace;
    }

    public void setSolutionSpace(boolean solutionSpace) {
        this.solutionSpace = solutionSpace;
    }

    public Set<Long> getExcludedTreeIds() {
        return excludedTreeIds;
    }

    public void setExcludedTreeIds(Set<Long> excludedTreeIds) {
        this.excludedTreeIds = excludedTreeIds;
    }

    public Set<Long> getExcludedNodeIds() {
        return excludedNodeIds;
    }

    public void setExcludedNodeIds(Set<Long> excludedNodeIds) {
        this.excludedNodeIds = excludedNodeIds;
    }

    public Set<String> getExcludedTypes() {
        return excludedTypes;
    }

    public void setExcludedTypes(Set<String> excludedTypes) {
        this.excludedTypes = excludedTypes;
    }

    public Set<Long> getIncludedTreeIds() {
        return includedTreeIds;
    }

    public void setIncludedTreeIds(Set<Long> includedTreeIds) {
        this.includedTreeIds = includedTreeIds;
    }

    public Set<Long> getIncludedNodeIds() {
        return includedNodeIds;
    }

    public void setIncludedNodeIds(Set<Long> includedNodeIds) {
        this.includedNodeIds = includedNodeIds;
    }

    public Set<String> getIncludedTypes() {
        return includedTypes;
    }

    public void setIncludedTypes(Set<String> includedTypes) {
        this.includedTypes = includedTypes;
    }

    public List<TestResultConstraint> getTestResult() {
        return testResult;
    }

    public void setTestResult(List<TestResultConstraint> testResult) {
        this.testResult = testResult;
    }

    public List<List<TrufflePattern>> getPatterns() {
        return patterns;
    }

    public void setPatterns(List<List<TrufflePattern>> patterns) {
        this.patterns = patterns;
    }
}
