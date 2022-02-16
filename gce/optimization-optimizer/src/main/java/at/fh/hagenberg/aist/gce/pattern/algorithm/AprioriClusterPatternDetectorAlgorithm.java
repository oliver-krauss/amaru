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
import at.fh.hagenberg.aist.gce.pattern.encoding.BitwisePatternMeta;
import at.fh.hagenberg.machinelearning.core.Problem;
import at.fh.hagenberg.machinelearning.core.ProblemGene;
import at.fh.hagenberg.machinelearning.core.Solution;
import at.fh.hagenberg.machinelearning.core.SolutionGene;
import at.fh.hagenberg.util.Pair;

import java.util.*;

/**
 * @author Oliver Krauss on 28.11.2018
 */

public class AprioriClusterPatternDetectorAlgorithm extends AbstractPatternDetectorAlgorithm<TruffleDifferentialPatternSolution> {

    /**
     * Solution produced by the algorithm
     */
    private Solution<TruffleDifferentialPatternSolution, TrufflePatternProblem> solution;

    Map<String, TrufflePatternProblem> patterns = new HashMap();

    @Override
    public Solution<TruffleDifferentialPatternSolution, TrufflePatternProblem> solve(Problem<TrufflePatternProblem> problem, Solution<TruffleDifferentialPatternSolution, TrufflePatternProblem> solution) {
        // ensure safe reuse
        patterns = new HashMap<>();
        logInitialized = false;
        logFinalized = false;
        this.solution = null;

        // init log
        initializeLog(problem);
        this.solution = solution;

        // contains the differential we want to calculate
        Map<TrufflePattern, Map<Pair<TrufflePatternProblem, TrufflePatternProblem>, Long>> differential = new HashMap<>();

        // contains the problems we previously analyzed
        Map<TrufflePatternProblem, List<TrufflePattern>> patternsPerProblem = new HashMap<>();

        // contains overall pattern info
        Map<String, TrufflePattern> patternSuperMap = new HashMap<>();

        // contains the mapping from the super map to the local patterns
        Map<TrufflePatternProblem, Map<TrufflePattern, TrufflePattern>> patternSuperMapToMapSuper = new HashMap<>();

        for (ProblemGene<TrufflePatternProblem> pGene : problem.getProblemGenes()) {
            TrufflePatternProblem gene = pGene.getGene();
            // contains patterns found for current problem
            Map<String, TrufflePattern> patternMap = new HashMap<>();

            // contains a mapping from the super map to the local patterns
            Map<TrufflePattern, TrufflePattern> patternSuperMapToMap = new HashMap<>();

            // set hierarchy
            BitwisePatternMeta meta = gene.getHierarchy();
            if (meta == null) {
                TruffleLanguageInformation tli = TruffleLanguageInformation.getLanguageInformation(pGene.getGene().getLanguage());
                if (tli != null) {
                    meta = new BitwisePatternMeta(tli);
                }
            }
            SubgraphIterator allPossibleSubgraphPermutations;
            if (meta != null) {
                allPossibleSubgraphPermutations = new HierarchySupportingSubgraphIterator(gene.getSearchSpace(), maxPatternSize, hierarchyFloor, hierarchyCeil);
                ((HierarchySupportingSubgraphIterator)allPossibleSubgraphPermutations).setMeta(meta);
            } else {
                allPossibleSubgraphPermutations = new StringSubgraphIterator(gene.getSearchSpace(), maxPatternSize, hierarchyFloor == 0);
            }

            // work
            allPossibleSubgraphPermutations.forEachRemaining(x -> {
                if (x != null) {

                    TrufflePattern trufflePattern = patternMap.get(x.getHash());
                    TrufflePattern truffleSuperPattern = patternSuperMap.get(x.getHash());
                    if (patternMap.containsKey(x.getHash())) {
                        trufflePattern.addTree(allPossibleSubgraphPermutations.getTreeId(), x);
                    } else {
                        trufflePattern = new TrufflePattern(allPossibleSubgraphPermutations.getTreeId(), x);
                        patternMap.put(x.getHash(), trufflePattern);
                    }
                    if (patternSuperMap.containsKey(x.getHash())) {
                        truffleSuperPattern.addTree(allPossibleSubgraphPermutations.getTreeId(), x);
                    } else {
                        truffleSuperPattern = new TrufflePattern(allPossibleSubgraphPermutations.getTreeId(), (PatternNodeWrapper) x.copy());
                        patternSuperMap.put(x.getHash(), truffleSuperPattern);
                    }
                    patternSuperMapToMap.put(truffleSuperPattern, trufflePattern);
                }
            });

            // create diff to all previously analyzed problems
            for (TrufflePatternProblem previousGene : patternSuperMapToMapSuper.keySet()) {
                Map<TrufflePattern, TrufflePattern> oldSuperMapToMap = patternSuperMapToMapSuper.get(previousGene);

                // collect all patterns that need to generate a diff
                Set<TrufflePattern> patterns = new HashSet<>();
                patterns.addAll(oldSuperMapToMap.keySet());
                patterns.addAll(patternSuperMapToMap.keySet());

                patterns.forEach(x -> {
                    if (!differential.containsKey(x)) {
                        differential.put(x, new HashMap<>());
                    }
                    Map<Pair<TrufflePatternProblem, TrufflePatternProblem>, Long> differentialSet = differential.get(x);

                    long diff = 0;
                    if (oldSuperMapToMap.containsKey(x)) {
                        diff += oldSuperMapToMap.get(x).count;
                    }
                    if (patternSuperMapToMap.containsKey(x)) {
                        diff -= patternSuperMapToMap.get(x).count;
                    }

                    differentialSet.put(new Pair<>(previousGene, gene), diff);
                });
            }


            // move to map
            patternsPerProblem.put(gene, new ArrayList<>(patternMap.values()));
            // move related map out of the way
            patternSuperMapToMapSuper.put(gene, patternSuperMapToMap);
        }

        TruffleDifferentialPatternSolution tdpSolution = new TruffleDifferentialPatternSolution(patternsPerProblem, differential);
        solution.addGene(new SolutionGene<>(tdpSolution, problem.getProblemGenes()));

        finalizeLog(problem);
        return solution;
    }

    @Override
    protected Solution<TruffleDifferentialPatternSolution, TrufflePatternProblem> getSolution() {
        return solution;
    }
}
