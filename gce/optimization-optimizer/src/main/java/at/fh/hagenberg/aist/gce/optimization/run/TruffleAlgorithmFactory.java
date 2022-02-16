/*
 * Copyright (c) 2022 the original author or authors.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

package at.fh.hagenberg.aist.gce.optimization.run;

import at.fh.hagenberg.aist.gce.context.ApplicationContextProvider;
import at.fh.hagenberg.aist.gce.optimization.TruffleEvaluatorImpl;
import at.fh.hagenberg.aist.gce.optimization.TruffleOptimizationProblem;
import at.fh.hagenberg.aist.gce.optimization.TruffleOptimizationSolution;
import at.fh.hagenberg.aist.gce.optimization.cachet.*;
import at.fh.hagenberg.aist.gce.optimization.operators.*;
import at.fh.hagenberg.aist.gce.optimization.operators.selection.*;
import at.fh.hagenberg.aist.gce.optimization.util.strategy.TruffleMasterStrategy;
import at.fh.hagenberg.aist.gce.optimization.util.strategy.initializer.TruffleEntryPointStrategy;
import at.fh.hagenberg.aist.gce.optimization.util.strategy.selection.BiasedChooser;
import at.fh.hagenberg.aist.gce.optimization.util.strategy.selection.BiasedPatternMiningChooser;
import at.fh.hagenberg.aist.gce.optimization.util.strategy.selection.ChooseOption;
import at.fh.hagenberg.aist.gce.optimization.util.strategy.selection.RandomChooser;
import at.fh.hagenberg.machinelearning.algorithm.ga.*;
import at.fh.hagenberg.machinelearning.algorithm.ga.selector.TournamentSelector;
import at.fh.hagenberg.machinelearning.analytics.TruffleGraphAnalytics;
import at.fh.hagenberg.machinelearning.core.Algorithm;
import at.fh.hagenberg.machinelearning.core.fitness.CachetEvaluator;
import at.fh.hagenberg.machinelearning.core.fitness.Evaluator;
import at.fh.hagenberg.machinelearning.core.mapping.GeneCreator;
import at.fh.hagenberg.machinelearning.core.mapping.OneToOneSolutionCreator;
import at.fh.hagenberg.machinelearning.core.mapping.SolutionCreator;
import at.fh.hagenberg.util.Pair;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Oliver Krauss on 11.07.2020
 */

public class TruffleAlgorithmFactory {

    private static int timeout = 3000;

    private int populationSize = 100;

    private int elites = 1;

    private int maximumGenerations = 20;

    private double mutationProbability = 0.13;

    private int sequences = 3;

    private int generationalElites = 1;

    private int startingGroups = 3;

    private int combinationRate = 2;

    private boolean groupSimilar = true;

    private final String language;

    private final TruffleMasterStrategy masterStrategy;

    private final TruffleEntryPointStrategy entryPointStrategy;

    public TruffleAlgorithmFactory(String language, TruffleMasterStrategy masterStrategy, TruffleEntryPointStrategy entryPointStrategy) {
        this.language = language;
        this.masterStrategy = masterStrategy;
        this.entryPointStrategy = entryPointStrategy;

        // add default choosers to the strategies:
        entryPointStrategy.setChooser(this.createChooser());
        // master strategy has the DOF chooser per default
    }

    public static TruffleGraphAnalytics getAnalytics() {
        ClassPathXmlApplicationContext ctx = ApplicationContextProvider.getCtx();
        return ctx.getBean(TruffleGraphAnalytics.class);
    }

    public Algorithm<TruffleOptimizationSolution, TruffleOptimizationProblem> createAlgorithm() {
        return createGeneticAlgorithm();
    }

    public static Evaluator<TruffleOptimizationSolution, TruffleOptimizationProblem> createEvaluator() {
        return createTruffleEvaluator();
    }

    public static TruffleEvaluatorImpl createTruffleEvaluator() {
        TruffleEvaluatorImpl evaluator = new TruffleEvaluatorImpl();
        evaluator.setTimeout(timeout);
        evaluator.setAnalyticsService(getAnalytics());
        evaluator.setCachetEvaluators(createDefaultCachets());
        return evaluator;
    }

    public static Map<CachetEvaluator<TruffleOptimizationSolution, TruffleOptimizationProblem>, Double> createDefaultCachets() {
        Map<CachetEvaluator<TruffleOptimizationSolution, TruffleOptimizationProblem>, Double> cachetMap = new HashMap<>();
        cachetMap.put(new AccuracyCachetEvaluator(), 1.0);
        return cachetMap;
    }

    public static Map<CachetEvaluator<TruffleOptimizationSolution, TruffleOptimizationProblem>, Double> createAllCachets() {
        Map<CachetEvaluator<TruffleOptimizationSolution, TruffleOptimizationProblem>, Double> cachetMap = new HashMap<>();
        cachetMap.put(new AccuracyCachetEvaluator(), 1.0);
        cachetMap.put(new ApproximatingPerformanceCachetEvaluator(), 1.0);
        cachetMap.put(new CodeComplexityCachetEvaluator(), 1.0);
        cachetMap.put(new PerformanceCachetEvaluator(), 1.0);
        cachetMap.put(new SelfAdjustingApproximatingPerformanceCachetEvaluator(), 1.0);
        cachetMap.put(new SelfAdjustingPerformanceCachetEvaluator(), 1.0);
        return cachetMap;
    }

    private GeneticAlgorithm<TruffleOptimizationSolution, TruffleOptimizationProblem> configureGA(GeneticAlgorithm<TruffleOptimizationSolution, TruffleOptimizationProblem> geneticAlgorithm) {
        // set the general values
        geneticAlgorithm.setElites(elites);
        geneticAlgorithm.setPopulationSize(populationSize);
        geneticAlgorithm.setMaximumGenerations(maximumGenerations);
        geneticAlgorithm.setMutationProbability(mutationProbability);
        geneticAlgorithm.setAnalytics(getAnalytics());

        // set evaluator
        geneticAlgorithm.setEvaluator(createEvaluator());

        // set operators
        geneticAlgorithm.setSelector(createSelector());
        geneticAlgorithm.setSolutionCreator(createCreator());
        geneticAlgorithm.setCrossover(createPatternAdheringCrossover());
        geneticAlgorithm.setGenMutator(createMutator());

        return geneticAlgorithm;
    }

    public GeneticAlgorithm<TruffleOptimizationSolution, TruffleOptimizationProblem> createGeneticAlgorithm() {
        GeneticAlgorithm<TruffleOptimizationSolution, TruffleOptimizationProblem> geneticAlgorithm = new GeneticAlgorithm<>();
        configureGA(geneticAlgorithm);
        return geneticAlgorithm;
    }

    public GeneticAlgorithm<TruffleOptimizationSolution, TruffleOptimizationProblem> createSequentialComplexityGeneticAlgorithm() {
        SequentialComplexityGeneticAlgorithm geneticAlgorithm = new SequentialComplexityGeneticAlgorithm();

        configureGA(geneticAlgorithm);
        geneticAlgorithm.setSequences(sequences);
        geneticAlgorithm.setGenerationalElites(generationalElites);

        return geneticAlgorithm;
    }

    public GeneticAlgorithm<TruffleOptimizationSolution, TruffleOptimizationProblem> createRepeatingGeneticAlgorithm() {
        RepeatingGeneticAlgorithm geneticAlgorithm = new RepeatingGeneticAlgorithm();

        configureGA(geneticAlgorithm);
        geneticAlgorithm.setSequences(sequences);
        geneticAlgorithm.setGenerationalElites(generationalElites);

        return geneticAlgorithm;
    }

    public GeneticAlgorithm<TruffleOptimizationSolution, TruffleOptimizationProblem> createParallelComplexityGeneticAlgorithm() {
        ParallelComplexityGeneticAlgorithm geneticAlgorithm = new ParallelComplexityGeneticAlgorithm();

        configureGA(geneticAlgorithm);
        geneticAlgorithm.setStartingGroups(startingGroups);
        geneticAlgorithm.setCombinationRate(combinationRate);
        geneticAlgorithm.setGenerationalElites(generationalElites);
        geneticAlgorithm.setGroupSimilar(groupSimilar);

        return geneticAlgorithm;
    }

    public Crossover<TruffleOptimizationSolution, TruffleOptimizationProblem> createCrossover() {
        TruffleTreeCrossover crossover = new TruffleTreeCrossover();
        crossover.setAnalyticsService(getAnalytics());
        crossover.setSelector(getTreeSelector());
        crossover.setSafelyAssignableToplevelClasses(entryPointStrategy.getManagedClasses());
        return crossover;
    }

    public Crossover<TruffleOptimizationSolution, TruffleOptimizationProblem> createPatternAdheringCrossover() {
        TrufflePatternAdheringTreeCrossover crossover = new TrufflePatternAdheringTreeCrossover();
        crossover.setAnalyticsService(getAnalytics());
        crossover.setSelector(getTreeSelector());
        crossover.setSafelyAssignableToplevelClasses(entryPointStrategy.getManagedClasses());
        crossover.setMasterStrategy(masterStrategy);
        return crossover;
    }

    public ContractualTruffleTreeSelector getTreeSelector() {
        return getRandomTruffleTreeSelector();
    }

    public ContractualTruffleTreeSelector getCodeComplexityTruffleTreeSelector() {
        CodeComplexityTruffleTreeSelector complexitySelector = new CodeComplexityTruffleTreeSelector();
        complexitySelector.setLanguage(language);
        complexitySelector.setMaxComplexity(3);
        return complexitySelector;
    }

    public ContractualTruffleTreeSelector getDescendingTruffleTreeSelector() {
        DescendingTruffleTreeSelector selector = new DescendingTruffleTreeSelector();
        selector.setDescentProbability(0.5);
        selector.setMaxDescent(20);
        return selector;
    }

    public ContractualTruffleTreeSelector getDepthWidthRestrictedTruffleTreeSelector() {
        DepthWidthRestrictedTruffleTreeSelector selector = new DepthWidthRestrictedTruffleTreeSelector();
        selector.setMaxDepth(3);
        selector.setMaxWidth(3);
        return selector;
    }

    public SizeRestrictedTruffleTreeSelector getSizeRestrictedTruffleTreeSelector() {
        SizeRestrictedTruffleTreeSelector selector = new SizeRestrictedTruffleTreeSelector();
        selector.setMaxSize(5);
        return selector;
    }

    public ContractualTruffleTreeSelector getRandomTruffleTreeSelector() {
        return new RandomTruffleTreeSelector();
    }

    public ConfigurableMutator<TruffleOptimizationSolution, TruffleOptimizationProblem> createMutator() {
        TruffleTreeMutator truffleTreeMutator = new TruffleTreeMutator();
        truffleTreeMutator.setSelector(getDepthWidthRestrictedTruffleTreeSelector());
        truffleTreeMutator.setSubtreeStrategy(masterStrategy);
        truffleTreeMutator.setFullTreeStrategy(entryPointStrategy);
        truffleTreeMutator.setMutationChoice(createChooser());
        truffleTreeMutator.setAnalyticsService(getAnalytics());
        return truffleTreeMutator;
    }

    public <T> ChooseOption<T> createChooser() {
        return createRandomChooser();
    }

    public <T> RandomChooser<T> createRandomChooser() {
        return new RandomChooser<>();
    }

    public <T> BiasedChooser<T> createBiasedChooser(Collection<Pair<Double, T>> options) {
        BiasedChooser<T> chooser = new BiasedChooser<>();
        chooser.setOptionBias(options);
        return chooser;
    }

    public <T> BiasedPatternMiningChooser createBiasedPatternMiningChooser(Collection<Class> options) {
        return new BiasedPatternMiningChooser(options);
    }

    public SolutionCreator<TruffleOptimizationSolution, TruffleOptimizationProblem> createCreator() {
        SolutionCreator<TruffleOptimizationSolution, TruffleOptimizationProblem> creator = new OneToOneSolutionCreator<TruffleOptimizationSolution, TruffleOptimizationProblem>();
        creator.setGeneCreator(getGeneCreator());
        return creator;
    }

    private GeneCreator<TruffleOptimizationSolution, TruffleOptimizationProblem> getGeneCreator() {
        return getMutatingTreeCreator();
    }

    private GeneCreator<TruffleOptimizationSolution, TruffleOptimizationProblem> getMutatingTreeCreator() {
        MutatingTruffleTreeCreator mutatingTruffleTreeCreator = new MutatingTruffleTreeCreator();
        mutatingTruffleTreeCreator.setAnalyticsService(getAnalytics());
        mutatingTruffleTreeCreator.setMutator(createMutator());
        return mutatingTruffleTreeCreator;
    }

    private GeneCreator<TruffleOptimizationSolution, TruffleOptimizationProblem> getTreeCreator() {
        TruffleTreeCreator truffleTreeCreator = new TruffleTreeCreator();
        truffleTreeCreator.setStrategy(entryPointStrategy);
        truffleTreeCreator.setAnalyticsService(getAnalytics());
        return truffleTreeCreator;
    }

    private Selector<TruffleOptimizationSolution, TruffleOptimizationProblem> createSelector() {
        TournamentSelector<TruffleOptimizationSolution, TruffleOptimizationProblem> tournamentSelector = new LivingOnlyTournamentSelector<>();
        tournamentSelector.setTournamentSize(populationSize > 20 ? 10 : populationSize / 4);
        return tournamentSelector;
    }

    public int getPopulationSize() {
        return populationSize;
    }

    public void setPopulationSize(int populationSize) {
        this.populationSize = populationSize;
    }

    public int getElites() {
        return elites;
    }

    public void setElites(int elites) {
        this.elites = elites;
    }

    public int getMaximumGenerations() {
        return maximumGenerations;
    }

    public void setMaximumGenerations(int maximumGenerations) {
        this.maximumGenerations = maximumGenerations;
    }

    public double getMutationProbability() {
        return mutationProbability;
    }

    public void setMutationProbability(double mutationProbability) {
        this.mutationProbability = mutationProbability;
    }

    public int getSequences() {
        return sequences;
    }

    public void setSequences(int sequences) {
        this.sequences = sequences;
    }

    public int getGenerationalElites() {
        return generationalElites;
    }

    public void setGenerationalElites(int generationalElites) {
        this.generationalElites = generationalElites;
    }

    public int getStartingGroups() {
        return startingGroups;
    }

    public void setStartingGroups(int startingGroups) {
        this.startingGroups = startingGroups;
    }

    public int getCombinationRate() {
        return combinationRate;
    }

    public void setCombinationRate(int combinationRate) {
        this.combinationRate = combinationRate;
    }

    public boolean isGroupSimilar() {
        return groupSimilar;
    }

    public void setGroupSimilar(boolean groupSimilar) {
        this.groupSimilar = groupSimilar;
    }

    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }
}
