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

import at.fh.hagenberg.aist.gce.context.ApplicationContextProvider;
import at.fh.hagenberg.aist.gce.optimization.TruffleEvaluatorImpl;
import at.fh.hagenberg.aist.gce.optimization.TruffleOptimizationProblem;
import at.fh.hagenberg.aist.gce.optimization.TruffleOptimizationSolution;
import at.fh.hagenberg.aist.gce.optimization.TruffleOptimizationSolutionRepository;
import at.fh.hagenberg.aist.gce.optimization.operators.*;
import at.fh.hagenberg.aist.gce.optimization.util.TruffleLanguageInformation;
import at.fh.hagenberg.aist.gce.optimization.util.strategy.TruffleHierarchicalStrategy;
import at.fh.hagenberg.aist.gce.optimization.util.strategy.TruffleMasterStrategy;
import at.fh.hagenberg.aist.gce.optimization.util.strategy.TruffleSimpleStrategy;
import at.fh.hagenberg.aist.gce.optimization.util.strategy.TruffleVerifyingStrategy;
import at.fh.hagenberg.aist.gce.optimization.util.strategy.initializer.TruffleEntryPointStrategy;
import at.fh.hagenberg.machinelearning.analytics.TruffleGraphAnalytics;
import at.fh.hagenberg.machinelearning.core.Problem;
import at.fh.hagenberg.machinelearning.core.ProblemGene;
import at.fh.hagenberg.machinelearning.core.Solution;
import at.fh.hagenberg.machinelearning.core.SolutionGene;
import at.fh.hagenberg.machinelearning.core.fitness.Evaluator;
import com.oracle.truffle.api.CallTarget;
import org.ehcache.Cache;
import org.ehcache.CacheManager;
import org.ehcache.config.builders.CacheConfigurationBuilder;
import org.ehcache.config.builders.CacheManagerBuilder;
import org.ehcache.config.builders.ResourcePoolsBuilder;
import org.neo4j.ogm.annotation.Transient;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author Oliver Krauss on 26.10.2019
 */

public class GceWorkContext {

    /**
     * Unique Identifier of the experiment IN HL that the worker works for.
     * This is NOT THE DATABASE ID
     */
    private String id;

    /**
     * Stream redirect of truffle output
     */
    private ByteArrayOutputStream out;

    /**
     * The thing that is actually being executed
     */
    private CallTarget main;

    /**
     * Problem that actually needs to be handled
     */
    private TruffleOptimizationProblem problem;

    /**
     * Database Logging
     */
    private TruffleGraphAnalytics analytics;

    /**
     * Repository for storing and loading solutions
     */
    private TruffleOptimizationSolutionRepository truffleOptimizationSolutionRepository;

    /**
     * evaluator for solutions
     */
    private TruffleEvaluatorImpl evaluator;

    /**
     * Creator opreator
     */
    private ConfigurableGeneCreator<TruffleOptimizationSolution, TruffleOptimizationProblem> geneCreator;

    /**
     * crossover for solutions
     */
    private ConfigurableCrossover<TruffleOptimizationSolution, TruffleOptimizationProblem> crossover;

    /**
     * mutator for solutions
     */
    private ConfigurableMutator<TruffleOptimizationSolution, TruffleOptimizationProblem> mutator;

    private TruffleOptimizationSolutionRepository repository = ApplicationContextProvider.getCtx().getBean("truffleOptimizationSolutionRepository", TruffleOptimizationSolutionRepository.class);


    public GceWorkContext(String id, TruffleOptimizationProblem problem,
                          ConfigurableGeneCreator<TruffleOptimizationSolution, TruffleOptimizationProblem> geneCreator,
                          Evaluator<TruffleOptimizationSolution, TruffleOptimizationProblem> evaluator,
                          ConfigurableCrossover<TruffleOptimizationSolution, TruffleOptimizationProblem> crossover,
                          ConfigurableMutator<TruffleOptimizationSolution,
                              TruffleOptimizationProblem> mutator,
                          List<TruffleHierarchicalStrategy> strategies,
                          Map<String, TruffleVerifyingStrategy> terminalStrategies) {
        this.id = id;
        this.problem = problem;
        this.geneCreator = geneCreator;
        this.evaluator = (TruffleEvaluatorImpl) evaluator; // we cast here as only one evaluator exists. Might become a problem in the far future.
        this.crossover = crossover;
        this.mutator = mutator;

        // load the analytics
        ClassPathXmlApplicationContext ctx = ApplicationContextProvider.getCtx();
        analytics = ctx.getBean(TruffleGraphAnalytics.class);
        truffleOptimizationSolutionRepository = ctx.getBean(TruffleOptimizationSolutionRepository.class);


        if (this.analytics != null) {
            this.analytics.startAnalytics();
            this.analytics.logParam("creator", this.geneCreator.getClass().getName());
            List<ProblemGene<TruffleOptimizationProblem>> genes = new ArrayList<>();
            genes.add(new ProblemGene<>(problem));
            this.analytics.logProblem(new Problem<>(genes));
            this.analytics.logAlgorithmStepHeaders(new ArrayList<>());

            // set it to the operators
            this.geneCreator.setAnalyticsService(this.analytics);
            this.evaluator.setAnalyticsService(this.analytics);
            this.crossover.setAnalyticsService(this.analytics);
            this.mutator.setAnalyticsService(this.analytics);
        }

        // load strats
        TruffleMasterStrategy masterStrategy = TruffleMasterStrategy.createFromTLI(problem.getConfiguration(), problem.getSearchSpace(), strategies, terminalStrategies);
        masterStrategy.autoLoadPatterns();
        TruffleEntryPointStrategy entryPointStrategy = new TruffleEntryPointStrategy(problem.getSearchSpace(), problem.getNode(), problem.getNode(), masterStrategy, problem.getConfiguration());

        // special handling of special creators
        if (this.geneCreator instanceof MutatingTruffleTreeCreator) {
            processMutator(((MutatingTruffleTreeCreator) this.geneCreator).getMutator(), masterStrategy, entryPointStrategy, terminalStrategies);
        }

        // handle mutator
        processMutator(this.mutator, masterStrategy, entryPointStrategy, terminalStrategies);

        // special handling of special crossovers
        if (this.crossover instanceof TruffleTreeCrossover) {
            ((TruffleTreeCrossover) this.crossover).setSafelyAssignableToplevelClasses(entryPointStrategy.getManagedClasses());
        } else if (this.crossover instanceof TrufflePatternAdheringTreeCrossover) {
            ((TrufflePatternAdheringTreeCrossover) this.crossover).setSafelyAssignableToplevelClasses(entryPointStrategy.getManagedClasses());
            ((TrufflePatternAdheringTreeCrossover) this.crossover).setMasterStrategy(masterStrategy);
        }
    }

    private void processMutator(ConfigurableMutator<TruffleOptimizationSolution, TruffleOptimizationProblem> mutator, TruffleMasterStrategy masterStrategy, TruffleEntryPointStrategy entryPointStrategy, Map<String, TruffleVerifyingStrategy> strategies) {
        // all mutators need this
        mutator.setSubtreeStrategy(masterStrategy);
        mutator.setFullTreeStrategy(entryPointStrategy);
        // special handling of special mutators
        if (mutator instanceof TruffleSingleNodeMutator) {
            ((TruffleSingleNodeMutator) this.mutator).setStrategies(strategies);
        }
    }


    /**
     * contains anything that shall happen after a run is finished.
     */
    public void finishRun() {
        // shutdown the analytics
        if (this.analytics != null) {
            this.analytics.logAlgorithmStep(new ArrayList<>());
            this.analytics.finishAnalytics();
        }
    }

    public TruffleOptimizationSolution create() {
        return geneCreator.createGene(problem);
    }

    public Solution evaluate(long solutionId) {
        return evaluate(this.loadSolution(solutionId));
    }

    public TruffleOptimizationSolution crossover(long leftId, long rightId) {
        // load trees to be crossed
        TruffleOptimizationSolution left = this.loadSolution(leftId);
        TruffleOptimizationSolution right = this.loadSolution(rightId);
        return crossover.breed(left, right);
    }

    public TruffleOptimizationSolution mutate(long solutionId) {
        // load tree to mutate
        TruffleOptimizationSolution mutagen = this.loadSolution(solutionId);
        return mutator.mutate(mutagen);
    }

    public Solution evaluate(TruffleOptimizationSolution solution) {
        // Evaluate the tree
        Solution<TruffleOptimizationSolution, TruffleOptimizationProblem> s = new Solution<>();
        ArrayList<ProblemGene<TruffleOptimizationProblem>> pgenes = new ArrayList<>();
        pgenes.add(new ProblemGene<>(this.problem));
        s.addGene(new SolutionGene<>(solution, pgenes));
        evaluator.evaluateQuality(s);

        // Sync the solution with the database
        if (solution.getId() == null) {
            solution = repository.syncWithDb(solution, s.getId());
        }
        storeSolution(solution.getId(), solution);
        return s;
    }

    @Transient
    private static Cache<Long, TruffleOptimizationSolution> solutionCache = init();

    private static Cache<Long, TruffleOptimizationSolution> init() {
        CacheManager cacheManager = CacheManagerBuilder.newCacheManagerBuilder()
            .withCache("SolutionStoreCache",
                CacheConfigurationBuilder.newCacheConfigurationBuilder(Long.class, TruffleOptimizationSolution.class, ResourcePoolsBuilder.heap(50)))
            .build();
        cacheManager.init();
        return cacheManager.getCache("SolutionStoreCache", Long.class, TruffleOptimizationSolution.class);
    }

    /**
     * Helper function storing the solution id.
     *
     * @param solution
     */
    private void storeSolution(long id, TruffleOptimizationSolution solution) {
        solutionCache.put(id, solution);
    }


    /**
     * Helper function loading the solution id.
     *
     * @param solutionId
     * @return
     */
    private TruffleOptimizationSolution loadSolution(long solutionId) {
        if (solutionCache.containsKey(solutionId)) {
            return solutionCache.get(solutionId);
        } else {
            TruffleOptimizationSolution solution =
                truffleOptimizationSolutionRepository.loadAndUnpackWithTree(solutionId, problem.getNode().getRootNode().getFrameDescriptor(), null, getLanguage().getName());
            // also add in the accompanying problem
            return new TruffleOptimizationSolution(solution.getNode(), problem, solution);
        }
    }

    public TruffleLanguageInformation getLanguage() {
        return problem.getSearchSpace().getInformation();
    }
}
