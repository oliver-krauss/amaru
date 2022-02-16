/*
 * Copyright (c) 2022 the original author or authors.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

package at.fh.hagenberg.machinelearning.analytics;


import at.fh.hagenberg.aist.gce.optimization.TruffleOptimizationProblem;
import at.fh.hagenberg.aist.gce.optimization.TruffleOptimizationSolution;
import at.fh.hagenberg.aist.gce.optimization.test.TruffleOptimizationTest;
import at.fh.hagenberg.aist.gce.optimization.test.TruffleOptimizationTestComplexity;
import at.fh.hagenberg.aist.gce.optimization.test.TruffleTestValue;
import at.fh.hagenberg.aist.gce.optimization.util.JavaAssistUtil;
import at.fh.hagenberg.aist.gce.optimization.util.NanoProfiler;
import science.aist.neo4j.Neo4jRepository;
import at.fh.hagenberg.machinelearning.analytics.graph.*;
import at.fh.hagenberg.machinelearning.analytics.graph.nodes.NodeWrapper;
import at.fh.hagenberg.machinelearning.analytics.graph.nodes.OperationNode;
import at.fh.hagenberg.machinelearning.analytics.graph.nodes.PopulationRelationship;
import at.fh.hagenberg.machinelearning.core.Problem;
import at.fh.hagenberg.machinelearning.core.ProblemGene;
import at.fh.hagenberg.machinelearning.core.Solution;
import at.fh.hagenberg.util.Pair;
import com.oracle.truffle.api.nodes.Node;
import org.springframework.beans.factory.annotation.Required;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Note: This class IS Threadsafe, but NOT safe when running multiple experiments in parallel (sequential is fine!).
 * If you run Experiments in parallel make sure each one has their own analytics instance!
 */
public class TruffleGraphAnalytics extends GraphAnalytics {

    public NanoProfiler profiler = new NanoProfiler("Analytics", -1);

    /**
     * Node Repository for storing Truffle Nodes
     */
    private NodeRepository nodeRepository;

    /**
     * Repository for storing Operations of the Algorithm
     */
    private Neo4jRepository<OperationNode, Long> operationRepository;

    /**
     * Repository for creating relationships between steps and evaluations
     */
    private PopulationRelationshipRepository populationRelationshipRepository;

    /**
     * Repository for test cases
     */
    private TruffleTestCaseRepository truffleTestCaseRepository;

    /**
     * Repository for truffle problems
     */
    private TruffleProblemGeneRepository truffleProblemGeneRepository;

    /**
     * Repository for test values
     */
    private TruffleTestValueRepository truffleTestValueRepository;

    /**
     * Cache for Root Nodes of Trees
     * Caching strategy of loading all hashes from the DB on init.
     * Represents a Factor 20 SPEEDUP
     */
    private Map<String, Long> treeCache;

    /**
     * Cache for Test Values (which are unique in DB)
     * Caching strategy of loading all hashes from the DB on init.
     */
    private Map<String, Long> truffleTestValueCache;

    /**
     * Cache for Solutions (depend on the problem being processed!)
     */
    private Map<String, Long> solutionCache;

    /**
     * Cache for Problems
     */
    private Map<String, Long> problemCache;


    /**
     * Cache for Tests to connect to solutions, etc.
     */
    private Map<String, Long> optimizationTestCache;


    public TruffleGraphAnalytics() {
    }

    /**
     * Stores a new operation node in the database
     *
     * @param node the node to store
     * @return the stored node
     */
    public OperationNode saveOperation(OperationNode node) {
        node.setInput(findTrees(node.getInput()));
        node.setOutput(findTrees(node.getOutput()));

        long start = profiler.start();
        node = operationRepository.save(node);
        profiler.profile("saveOperation.save", start);

        // Connect operations to steps
        if (postfixStepLogging) {
            // delay because steps logged after operations
            operations.add(node);
        } else {
            if (currentStep == null) {
                // switch to postfix as operations are logged before step
                postfixStepLogging = true;
                operations.add(node);
            } else {
                // log to step directly
                node.setStep(currentStep);
                node = operationRepository.save(node);
            }
        }

        return node;
    }

    public <GT, PT> Solution logEvaluation(Solution<GT, PT> solution) {
        solution = storeSolutionNode(solution);
        connectSolutionToStep(solution);
        return solution;
    }

    private void connectSolutionToStep(Solution evaluation) {
        Long key = evaluation.getId();
        if (postfixStepLogging) {
            // store them for later connection
            if (evaluations.containsKey(key)) {
                evaluations.put(key, new Pair<>(evaluation, 1 + evaluations.get(key).getValue()));
            } else {
                evaluations.put(key, new Pair<>(evaluation, 1L));
            }
        } else {
            if (currentStep == null) {
                // switch to postfix as operations are logged before step
                postfixStepLogging = true;
                evaluations.put(key, new Pair<>(evaluation, 1L)); // definitely no evaluations present, as this can only happen on the first eval
            } else {
                PopulationRelationship relationship = populationRelationshipRepository.findByStepAndSolution(currentStep.getId(), evaluation.getId());
                if (relationship != null) {
                    relationship.setCount(relationship.getCount() + 1);
                } else {
                    relationship = new PopulationRelationship(1L, currentStep, evaluation);
                }
                populationRelationshipRepository.save(relationship);
            }
        }
    }


    // is true if the algorithm used logs it's executed steps AFTER the executed
    private boolean postfixStepLogging = false;

    // Operations that need to be logged after a step was logged
    private List<OperationNode> operations = new ArrayList<>();

    private Map<Long, Pair<Solution, Long>> evaluations = new HashMap<>();

    @Override
    public void logAlgorithmStep(List<String> values) {
        long start = profiler.start();
        super.logAlgorithmStep(values);
        start = profiler.profile("logAlgorithmStep.super", start);

        // TODO check if we can improve via a saveall
        // connect with operations
        operations.forEach(x -> {
            x.setStep(currentStep);
            operationRepository.save(x);
        });
        operations.clear();
        start = profiler.profile("logAlgorithmStep.operations", start);

        // connect with solutions
        populationRelationshipRepository.saveAll(evaluations.keySet().stream().map(x -> {
            Pair<Solution, Long> pair = evaluations.get(x);
            return new PopulationRelationship(pair.getValue(), currentStep, pair.getKey());
        }).collect(Collectors.toList()));
        evaluations.clear();
        profiler.profile("logAlgorithmStep.solutions", start);
    }

    /**
     * Finds a solution. If the solution exists then it is also logged as an evaluation (no need to call logEvaluation)
     *
     * @param node Tree that solution solves
     * @param gene Problem gene this solution is responsible for
     * @return Solution for that tree
     */
    public Solution findSolution(Node node, ProblemGene<TruffleOptimizationProblem> gene) {
        NodeWrapper tree = findTree(NodeWrapper.wrap(node));
        if (solutionCache.containsKey(tree.getHash())) {
            long start = profiler.start();
            // WARNING: As we use a custom cypher in the first "findBy" the children aren't loaded. We re-load by Id as this also finds all direct relationships
            Solution solution = ((TruffleSolutionRepository) solutionRepository).findSolutionNodeByTreeHash(tree.getHash(), gene.getGene().getHash());
            if (solution == null) {
                return null;
            }
            profiler.profile("findSolution.findById", start);
            connectSolutionToStep(solution);
            return solution;
        }
        return null;
    }

    private <GT, PT> Solution storeSolutionNode(Solution<GT, PT> solution) {
        if (!(solution.getSolutionGenes().size() == 1 && solution.getSolutionGenes().get(0).getGene() instanceof TruffleOptimizationSolution)) {
            throw new RuntimeException("Multi tree optimizations not yet supported");
            // TODO: #106 Support multi tree optimizations way later in the future
        }


        synchronized (this) {
            TruffleOptimizationSolution solutionGene = (TruffleOptimizationSolution) solution.getSolutionGenes().get(0).getGene();
            NodeWrapper tree = findTree(solutionGene.getTree());

            if (solutionCache.containsKey(tree.getHash())) {
                // load from cache and inject ID
                JavaAssistUtil.safeFieldWrite("id", solution, solutionCache.get(tree.getHash()));
                return solution;
            }
            long start = profiler.start();

            // create new solution
            solutionGene.setTree(tree);
            solutionGene.getTestResults().forEach(x -> x.setOutput(findOrCreateTruffleTestValue(x.getOutput())));
            profiler.profile("storeSolutionNode.connect", start);
            Solution<GT, PT> savedSolution = this.solutionRepository.save(solution);
            solutionCache.put(tree.getHash(), solution.getId());

            profiler.profile("storeSolutionNode.save", start);
            return savedSolution;
        }
    }

    private TruffleOptimizationTestComplexity findOrCreateTruffleOptimizationTestComplexity(TruffleOptimizationTestComplexity complexity) {
        complexity.setTest(findOrCreateTruffleOptimizationTest(complexity.getTest()));
        return complexity;
    }

    private TruffleOptimizationTest findOrCreateTruffleOptimizationTest(TruffleOptimizationTest test) {
        long start = profiler.start();
        if (optimizationTestCache.containsKey(test.getHash())) {
            test.setId(optimizationTestCache.get(test.getHash()));
            // also sync the in / out values
            Set<TruffleTestValue> inValues = test.getInput().stream().map(this::findOrCreateTruffleTestValue).collect(Collectors.toSet());
            test.getInput().clear();
            test.getInput().addAll(inValues);

            test.setOutput(findOrCreateTruffleTestValue(test.getOutput()));
            return test;
        }

        synchronized (this) {
            Set<TruffleTestValue> inValues = test.getInput().stream().map(this::findOrCreateTruffleTestValue).collect(Collectors.toSet());
            test.getInput().clear();
            test.getInput().addAll(inValues);

            test.setOutput(findOrCreateTruffleTestValue(test.getOutput()));
            test = truffleTestCaseRepository.save(test);
            optimizationTestCache.put(test.getHash(), test.getId());
        }

        profiler.profile("findOrCreateTruffleOptimizationTest", start);
        return test;
    }

    private TruffleTestValue findOrCreateTruffleTestValue(TruffleTestValue value) {
        long start = profiler.start();
        if (truffleTestValueCache.containsKey(value.getHash())) {
            value.setId(truffleTestValueCache.get(value.getHash()));
            return value;
        }

        synchronized (this) {
            value = truffleTestValueRepository.save(value);
            truffleTestValueCache.put(value.getHash(), value.getId());
        }

        profiler.profile("findOrCreateTruffleTestValue", start);
        return value;
    }

    /**
     * Replaces all trees in the given set with trees from the db (with ID!)
     *
     * @param hashedTrees un-synced NodeWrappers
     * @return synced NodeWrappers
     */
    private Set<NodeWrapper> findTrees(Set<NodeWrapper> hashedTrees) {
        Set<NodeWrapper> foundTrees = new HashSet<>();
        hashedTrees.forEach(x -> {
            foundTrees.add(findTree(x));
        });
        return foundTrees;
    }

    /**
     * Replaces a given tree with one from the db (with ID!)
     * If the tree is not in the db it will be added!
     *
     * @param hashedTree un-synced NodeWrapper
     * @return synced NodeWrapper
     */
    private NodeWrapper findTree(NodeWrapper hashedTree) {
        if (treeCache.containsKey(hashedTree.getHash())) {
            hashedTree.setId(treeCache.get(hashedTree.getHash()));
            return hashedTree;
        }

        NodeWrapper node = null;
        synchronized (this) {
            // TODO #41 This strategy allows race conditions if MULTIPLE clients access the same DB, however it is factor 20 speedup
            long start = profiler.start();
            node = nodeRepository.save(hashedTree);
            profiler.profile("findTree.save", start);
            treeCache.put(node.getHash(), node.getId());
        }
        return node;
    }

    @Override
    public <PT> void logProblem(Problem<PT> problem) {
        long start = profiler.start();
        // ensure that the problem is actually synced with the db
        problem.getProblemGenes().forEach(x -> x.setGene(logTruffleProblem(x.getGene())));
        super.logProblem(problem);
        profiler.profile("logProblem", start);
    }

    private <PT> PT logTruffleProblem(PT gene) {
        long start = profiler.start();

        TruffleOptimizationProblem problem = (TruffleOptimizationProblem) gene;
        // init the solution cache which are dependent on the problem
        solutionCache = ((TruffleSolutionRepository) solutionRepository).findExistingHashes(problem.getHash());

        // problem has too many transient fields, just set the ID
        if (problemCache.containsKey(problem.getHash())) {
            problem.setId(problemCache.get(problem.getHash()));
        }
        // also sync the tree
        problem.setWrappedNode(findTree(problem.getWrappedNode()));
        // sync the test data
        problem.getTests().forEach(x -> x = findOrCreateTruffleOptimizationTestComplexity(x));
        // also sync origin
        if (problem.getOriginalSolution() != null && problem.getOriginalSolution().getId() == null) {
            problem.setOriginalSolution(storeSolutionNode(problem.getOriginalSolution()));
        }
        // also sync best solution
        if (problem.getBestKnownSolution() != null && problem.getBestKnownSolution().getId() == null) {
            problem.setBestKnownSolution(storeSolutionNode(problem.getBestKnownSolution()));
        }

        profiler.profile("logTruffleProblem", start);
        return (PT) problem;
    }

    @Override
    public <GT, PT> void logSolution(Solution<GT, PT> solution) {
        // ensure correct syncing of solution
        this.storeSolutionNode(solution);
        super.logSolution(solution);
    }

    @Required
    public void setNodeRepository(NodeRepository nodeRepository) {
        this.nodeRepository = nodeRepository;
    }

    @Required
    public void setOperationRepository(Neo4jRepository<OperationNode, Long> operationRepository) {
        this.operationRepository = operationRepository;
    }

    @Override
    public void setSolutionRepository(Neo4jRepository<Solution, Long> solutionRepository) {
        if (!(solutionRepository instanceof TruffleSolutionRepository)) {
            throw new RuntimeException("TruffleGraphAnalytics must use a TruffleSolutionRepository");
        }
        super.setSolutionRepository(solutionRepository);
    }

    @Required
    public void setPopulationRelationshipRepository(PopulationRelationshipRepository populationRelationshipRepository) {
        this.populationRelationshipRepository = populationRelationshipRepository;
    }

    @Required
    public void setTruffleTestCaseRepository(TruffleTestCaseRepository truffleTestCaseRepository) {
        this.truffleTestCaseRepository = truffleTestCaseRepository;
    }

    @Required
    public void setTruffleTestValueRepository(TruffleTestValueRepository truffleTestValueRepository) {
        this.truffleTestValueRepository = truffleTestValueRepository;
    }

    @Required
    public void setTruffleOptimizationProblemRepository(TruffleProblemGeneRepository truffleProblemGeneRepository) {
        this.truffleProblemGeneRepository = truffleProblemGeneRepository;
    }

    @Override
    public void startAnalytics() {
        super.startAnalytics();
        init();
    }

    boolean initialized = false;

    public void init() {
        if (initialized) {
            return;
        }
        initialized = true;
        if (treeCache == null) {
            treeCache = nodeRepository.findExistingHashes();
        }
        if (truffleTestValueCache == null) {
            truffleTestValueCache = truffleTestValueRepository.findExistingHashes();
        }
        if (problemCache == null) {
            problemCache = truffleProblemGeneRepository.findExistingHashes();
        }
        if (optimizationTestCache == null) {
            optimizationTestCache = truffleTestCaseRepository.findExistingHashes();
        }
    }

    @Override
    public void finishAnalytics() {
        super.finishAnalytics();
        System.out.println("CLOSING STATEMENT OF OUR PROFILER: ");
        profiler.report();
        profiler.reset();
    }
}
