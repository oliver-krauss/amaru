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
import at.fh.hagenberg.aist.gce.pattern.encoding.BitwisePatternMeta;
import at.fh.hagenberg.machinelearning.core.*;
import at.fh.hagenberg.aist.gce.pattern.TrufflePatternProblem;
import at.fh.hagenberg.aist.gce.pattern.TrufflePattern;

import java.util.*;

/**
 * This algorithm implements an Apriori-Approach of pattern mining.
 * It selects all trees and produces all patterns of size n for them iteratively until the entire tree is returned,
 * or the maximum requested pattern size is returned
 * @author Oliver Krauss on 28.11.2018
 */
public class AprioriPatternDetectorAlgorithm extends AbstractPatternDetectorAlgorithm<TrufflePattern> {

    /**
     * Solution produced by the algorithm
     */
    private Solution<TrufflePattern, TrufflePatternProblem> solution;

    @Override
    public Solution<TrufflePattern, TrufflePatternProblem> solve(Problem<TrufflePatternProblem> problem, Solution<TrufflePattern, TrufflePatternProblem> solution) {
        // ensure safe reuse
        Map<String, TrufflePattern> patterns = new HashMap<>();
        logInitialized = false;
        logFinalized = false;
        this.solution = null;

        initializeLog(problem);
        this.solution = solution;

        // get actual problem description
        TrufflePatternProblem gene = problem.getProblemGenes().get(0).getGene();

        // set hierarchy
        SubgraphIterator iterator;
        BitwisePatternMeta meta = gene.getHierarchy();
        if (meta == null) {
            TruffleLanguageInformation tli = TruffleLanguageInformation.getLanguageInformation(gene.getLanguage());
            if (tli != null) {
                meta = new BitwisePatternMeta(tli);
            }
        }
        if (meta != null) {
            iterator = new HierarchySupportingSubgraphIterator(gene.getSearchSpace(), maxPatternSize, hierarchyFloor, hierarchyCeil);
            ((HierarchySupportingSubgraphIterator) iterator).setMeta(meta);
        } else {
            iterator = new StringSubgraphIterator(gene.getSearchSpace(), maxPatternSize, hierarchyFloor == 0);
        }

        // work
        iterator.forEachRemaining(x -> {
            if (x != null) {
                if (patterns.containsKey(x.getHash())) {
                    patterns.get(x.getHash()).addTree(iterator.getTreeId(), x);
                } else {
                    patterns.put(x.getHash(), new TrufflePattern(iterator.getTreeId(), x));
                }
            }
        });

        // encapsulate in solution
        List<ProblemGene<TrufflePatternProblem>> list = new ArrayList<>(problem.getProblemGenes());
        patterns.values().stream().sorted((o1, o2) -> (int) (o2.getCount() - o1.getCount())).forEach(x -> solution.addGene(new SolutionGene<>(x, list)));

        finalizeLog(problem);
        return solution;
    }


    @Override
    protected Solution<TrufflePattern, TrufflePatternProblem> getSolution() {
        return solution;
    }
}
