/*
 * Copyright (c) 2022 the original author or authors.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

package at.fh.hagenberg.machinelearning.algorithm.ga;

import at.fh.hagenberg.machinelearning.core.Problem;
import at.fh.hagenberg.machinelearning.core.ProblemGene;
import at.fh.hagenberg.machinelearning.core.Solution;
import at.fh.hagenberg.machinelearning.core.SolutionGene;
import com.oracle.truffle.api.nodes.Node;
import at.fh.hagenberg.aist.gce.optimization.TruffleOptimizationProblem;
import at.fh.hagenberg.aist.gce.optimization.TruffleOptimizationSolution;
import at.fh.hagenberg.aist.gce.optimization.test.TruffleOptimizationTestComplexity;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

public class RepackagingHelper {

    public static Problem<TruffleOptimizationProblem> createProblem(Problem<TruffleOptimizationProblem> problem, TruffleOptimizationTestComplexity test) {
        HashSet<TruffleOptimizationTestComplexity> truffleOptimizationTestComplexities = new HashSet<>();
        truffleOptimizationTestComplexities.add(test);
        return createProblem(problem, truffleOptimizationTestComplexities);
    }

    public static Problem<TruffleOptimizationProblem> createProblem(Problem<TruffleOptimizationProblem> problem, Collection<TruffleOptimizationTestComplexity> test) {
        TruffleOptimizationProblem newProblemGene = TruffleOptimizationProblem.copy(problem.getProblemGenes().get(0).getGene());
        newProblemGene.getTests().removeIf(x -> test.stream().noneMatch(keep -> keep.getTest().getHash().equals(x.getTest().getHash())));
        List<ProblemGene<TruffleOptimizationProblem>> problemGenes = new ArrayList<>();
        problemGenes.add(new ProblemGene<>(newProblemGene));
        return new Problem<>(problemGenes);
    }

    public static Solution<TruffleOptimizationSolution, TruffleOptimizationProblem> createSolution(TruffleOptimizationProblem problem, Node node) {
        TruffleOptimizationSolution tos = new TruffleOptimizationSolution(node, problem, RepackagingHelper.class);
        Solution<TruffleOptimizationSolution, TruffleOptimizationProblem> s = new Solution();
        List<ProblemGene<TruffleOptimizationProblem>> problemGenes = new ArrayList<>();
        problemGenes.add(new ProblemGene<TruffleOptimizationProblem>(problem));
        SolutionGene<TruffleOptimizationSolution, TruffleOptimizationProblem> sg = new SolutionGene<TruffleOptimizationSolution, TruffleOptimizationProblem>(tos, problemGenes);
        s.addGene(sg);
        return s;
    }
}
