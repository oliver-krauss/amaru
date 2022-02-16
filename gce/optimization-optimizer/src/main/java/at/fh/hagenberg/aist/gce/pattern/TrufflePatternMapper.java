/*
 * Copyright (c) 2022 the original author or authors.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

package at.fh.hagenberg.aist.gce.pattern;

import at.fh.hagenberg.aist.gce.pattern.encoding.BitwisePattern;
import at.fh.hagenberg.aist.gce.pattern.encoding.BitwisePatternMeta;
import at.fh.hagenberg.machinelearning.core.Gene;
import at.fh.hagenberg.machinelearning.core.Solution;
import at.fh.hagenberg.machinelearning.core.SolutionGene;
import at.fh.hagenberg.util.Pair;
import com.oracle.truffle.api.Truffle;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Class that Maps out the relationships between patterns.
 * See {@link TruffleRelatablePattern}
 * @author Oliver Krauss on 08.07.2020
 */
public class TrufflePatternMapper {

    /**
     * Meta info for generalizations
     */
    private final BitwisePatternMeta meta;

    public TrufflePatternMapper(BitwisePatternMeta meta) {
        this.meta = meta;
    }

    /**
     * Maps out a Solution containing Truffle Patterns
     * @param solution to be mapped
     * @return copy of solution with TruffleRelatablePattern
     */
    public Solution<TruffleRelatablePattern, TrufflePatternProblem> mapSolution(Solution<TrufflePattern, TrufflePatternProblem> solution) {
        Solution<TruffleRelatablePattern, TrufflePatternProblem> result = new Solution<>();
        result.getCachets().addAll(solution.getCachets());
        result.setQuality(solution.getQuality());

        List<TruffleRelatablePattern> allPatterns = mapPatterns(solution.getSolutionGenes().stream().map(Gene::getGene).collect(Collectors.toList()));

        // map out all the patterns in the solution
        solution.getSolutionGenes().forEach(x -> {
            SolutionGene<TruffleRelatablePattern, TrufflePatternProblem> gene = new SolutionGene<>();
            gene.getProblemGenes().addAll(x.getProblemGenes());
            gene.setGene(allPatterns.stream().filter(y -> y.getPatternNode().getHash().equals(x.getGene().getPatternNode().getHash())).findFirst().orElse(null));
            result.addGene(gene);
        });
        return result;
    }

    /**
     * Maps out a group of Truffle patterns among each other
     * @param patterns to be mapped to
     * @return one completely mapped pattern
     */
    public List<TruffleRelatablePattern> mapPatterns(Collection<TrufflePattern> patterns) {
        List<TruffleRelatablePattern> result = new LinkedList<>();
        for (TrufflePattern pattern : patterns) {
            TruffleRelatablePattern relatable = TruffleRelatablePattern.copy(pattern);

            LinkedList<TruffleRelatablePattern> contained = new LinkedList<>(result);
            while (!contained.isEmpty()) {
                TruffleRelatablePattern x = contained.pop();
                if (x.getPatternNode().contains(relatable.getPatternNode())) {
                    // this is contained in x
                    x.getContains().add(relatable);
                    x.getContainedIn().forEach(y -> y.getContains().add(relatable));
                    relatable.getContainedIn().add(x);
                    relatable.getContainedIn().addAll(x.getContainedIn());
                    contained.removeAll(x.getContainedIn());
                } else if (relatable.getPatternNode().contains(x.getPatternNode())) {
                    // x is contained in this
                    relatable.getContains().add(x);
                    relatable.getContains().addAll(x.getContains());
                    x.getContainedIn().add(relatable);
                    x.getContains().forEach(y -> y.getContainedIn().add(relatable));
                    contained.removeAll(x.getContains());
                }
            }

            LinkedList<TruffleRelatablePattern> generalized = new LinkedList<>(result);
            while (!generalized.isEmpty()) {
                TruffleRelatablePattern x = generalized.pop();
                BitwisePattern xBitwise = new BitwisePattern(x.getPatternNode(), meta);
                BitwisePattern relatableBitwise = new BitwisePattern(relatable.getPatternNode(), meta);
                if (xBitwise.generalizes(relatableBitwise)) {
                    // this is a specialization of x
                    x.getGeneralizes().add(relatable);
                    x.getSpecializes().forEach(y -> y.getGeneralizes().add(relatable));
                    relatable.getSpecializes().add(x);
                    relatable.getSpecializes().addAll(x.getSpecializes());
                    generalized.removeAll(x.getSpecializes());
                } else if (relatableBitwise.generalizes(xBitwise)) {
                    // x is a specialization this
                    relatable.getGeneralizes().add(x);
                    relatable.getGeneralizes().addAll(x.getGeneralizes());
                    x.getSpecializes().add(relatable);
                    x.getGeneralizes().forEach(y -> y.getSpecializes().add(relatable));
                    generalized.removeAll(x.getGeneralizes());
                }
            }
            result.add(relatable);
        }

        return result;
    }

    /**
     * Maps out a Truffle Differential Pattern Solution. The Solution will then contain TruffleRelatablePattern instead of TrufflePattern
     * @param solution to be mapped
     * @return copy of solution with TruffleRelatablePattern
     */
    public Solution<TruffleDifferentialPatternSolution, TrufflePatternProblem> mapDifferential(Solution<TruffleDifferentialPatternSolution, TrufflePatternProblem> solution) {
        Solution<TruffleDifferentialPatternSolution, TrufflePatternProblem> result = new Solution<>();
        result.getCachets().addAll(solution.getCachets());
        result.setQuality(solution.getQuality());
        solution.getSolutionGenes().forEach(x -> {
            // map out each differential individually
            SolutionGene<TruffleDifferentialPatternSolution, TrufflePatternProblem> gene = new SolutionGene<>();
            gene.getProblemGenes().addAll(x.getProblemGenes());
            gene.setGene(mapDifferential(x.getGene()));
            result.addGene(gene);
        });
        return result;
    }

    /**
     * Maps out a Truffle Differential Pattern Solution. The Solution will then contain TruffleRelatablePattern instead of TrufflePattern
     * Note that the mapping occurs only within the Problem space:
     *   patternsPerProblem will only map within that specific problem
     *   differential will map to all patterns but only within differential.keys
     * @param solution to be mapped
     * @return copy of solution with TruffleRelatablePattern
     */
    public TruffleDifferentialPatternSolution mapDifferential(TruffleDifferentialPatternSolution solution) {
        java.util.Map<TrufflePatternProblem, List<TrufflePattern>> patternsPerProblem = new HashMap<>();
        // map the individual problem spaces
        solution.getPatternsPerProblem().forEach((k, v) -> {
            patternsPerProblem.put(k, new LinkedList<>(mapPatterns(v)));
        });

        // map out the differential
        Map<TrufflePattern, Map<Pair<TrufflePatternProblem, TrufflePatternProblem>, Long>> differential = new HashMap<>();
        List<TruffleRelatablePattern> patternKeys = mapPatterns(solution.getDifferential().keySet());
        solution.getDifferential().forEach((k, v) -> differential.put(patternKeys.stream().filter(y -> y.getPatternNode().getHash().equals(k.getPatternNode().getHash())).findFirst().orElse(null), v));

        return new TruffleDifferentialPatternSolution(patternsPerProblem, differential);
    }



}
