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

import at.fh.hagenberg.aist.gce.pattern.TruffleDifferentialPatternSolution;
import at.fh.hagenberg.aist.gce.pattern.TrufflePatternProblem;
import at.fh.hagenberg.aist.gce.pattern.algorithm.AbstractPatternDetectorAlgorithm;
import at.fh.hagenberg.aist.gce.pattern.algorithm.AprioriClusterPatternDetectorAlgorithm;
import at.fh.hagenberg.aist.gce.pattern.algorithm.PatternGrowthClusterPatternDetectorAlgorithm;
import at.fh.hagenberg.machinelearning.analytics.graph.nodes.NodeWrapper;
import at.fh.hagenberg.machinelearning.core.Problem;
import at.fh.hagenberg.machinelearning.core.ProblemGene;
import at.fh.hagenberg.machinelearning.core.Solution;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.*;

/**
 * 4b) test ensures the validity of the pattern mining. The values below were hand-checked and should NEVER change,
 * unless you use a heuristic mining approach in which case they should neve be exceeded.
 *
 * @author Oliver Krauss on 02.10.2019
 */
@Test
public class DifferentialPatternMiningTest extends TestDBTest {

    @DataProvider(name = "provider")
    public static Object[][] algProvider() {
        AprioriClusterPatternDetectorAlgorithm apriori = new AprioriClusterPatternDetectorAlgorithm();
        PatternGrowthClusterPatternDetectorAlgorithm detector = new PatternGrowthClusterPatternDetectorAlgorithm();
        return new Object[][] {{apriori}, {detector}};
    }

    private TrufflePatternProblem getProblem(TruffleDifferentialPatternSolution solution, NodeWrapper n) {
        return solution.getPatternsPerProblem().keySet().stream().filter(x -> x.getName().equals(String.valueOf(n.getId()))).findFirst().orElse(null);
    }

    // Note: t1 is a subset of t2 the diff should be reflected in the nodes t1 does NOT have
    @Test(dataProvider = "provider")
    public void testMineT12(AbstractPatternDetectorAlgorithm<TruffleDifferentialPatternSolution> algorithm) {
        TruffleDifferentialPatternSolution solution = diff(algorithm, Arrays.asList(t1, t2), 6, -1, true);

        // further then
        TrufflePatternProblem t1Prob = getProblem(solution, t1);
        TrufflePatternProblem t2Prob = getProblem(solution, t2);

        // ensure patterns per problem are correct
        Assert.assertEquals(solution.getPatternsPerProblem().get(t1Prob).size(), 1);
        Assert.assertEquals(solution.getPatternsPerProblem().get(t2Prob).size(), 6);

        // ensure the diff is correct
        Assert.assertEquals(solution.getDifferential().values().stream().filter(x -> x.values().stream().anyMatch(y -> y == 0)).count(), 1);
        Assert.assertEquals(solution.getDifferential().values().stream().filter(x -> x.values().stream().anyMatch(y -> y == -1 || y == 1)).count(), 5);
    }

    @Test(dataProvider = "provider")
    public void testMineT12H1(AbstractPatternDetectorAlgorithm<TruffleDifferentialPatternSolution> algorithm) {
        TruffleDifferentialPatternSolution solution = diff(algorithm, Arrays.asList(t1, t2), 6, -1, false);

        // further then should be equivalent to testMineT12 as the Const values are exactly the same!
        TrufflePatternProblem t1Prob = getProblem(solution, t1);
        TrufflePatternProblem t2Prob = getProblem(solution, t2);

        // ensure patterns per problem are correct
        Assert.assertEquals(solution.getPatternsPerProblem().get(t1Prob).size(), 1);
        Assert.assertEquals(solution.getPatternsPerProblem().get(t2Prob).size(), 6);

        // ensure the diff is correct
        Assert.assertEquals(solution.getDifferential().values().stream().filter(x -> x.values().stream().anyMatch(y -> y == 0)).count(), 1);
        Assert.assertEquals(solution.getDifferential().values().stream().filter(x -> x.values().stream().anyMatch(y -> y == -1 || y == 1)).count(), 5);
    }


    // Note: t3 is nearly equivalent to t4 the diff should be reflected in the two "!=" and "=" nodes
    @Test(dataProvider = "provider")
    public void testMineT34(AbstractPatternDetectorAlgorithm<TruffleDifferentialPatternSolution> algorithm) {
        TruffleDifferentialPatternSolution solution = diff(algorithm, Arrays.asList(t3, t4), 7 + 9 + 14 + 20 + 24 + 20 + 10 + 2, -1, true);

        // further then should be Equivalet to testMineT12 as the Const values are exactly the same!
        TrufflePatternProblem t3Prob = getProblem(solution, t3);
        TrufflePatternProblem t4Prob = getProblem(solution, t4);

        // ensure patterns per problem are correct
        Assert.assertEquals(solution.getPatternsPerProblem().get(t3Prob).size(), 6 + 5 + 7 + 10 + 12 + 10 + 5 + 1);
        Assert.assertEquals(solution.getPatternsPerProblem().get(t4Prob).size(), 6 + 5 + 7 + 10 + 12 + 10 + 5 + 1);

        // ensure the diff is correct
        Assert.assertEquals(solution.getDifferential().values().stream().filter(x -> x.values().stream().anyMatch(y -> y == 0)).count(), 6);
        Assert.assertEquals(solution.getDifferential().values().stream().filter(x -> x.values().stream().anyMatch(y -> y != 0)).count(), 7 + 9 + 14 + 20 + 24 + 20 + 10 + 2 - 6);
    }

    @Test(dataProvider = "provider")
    public void testMineT34H1(AbstractPatternDetectorAlgorithm<TruffleDifferentialPatternSolution> algorithm) {
        TruffleDifferentialPatternSolution solution = diff(algorithm, Arrays.asList(t3, t4), 5 + 7 + 10 + 16 + 22 + 20 + 10 + 2, -1, false);

        // further then should be Equivalet to testMineT12 as the Const values are exactly the same!
        TrufflePatternProblem t3Prob = getProblem(solution, t3);
        TrufflePatternProblem t4Prob = getProblem(solution, t4);

        // ensure patterns per problem are correct
        Assert.assertEquals(solution.getPatternsPerProblem().get(t3Prob).size(), 4 + 4 + 5 + 8 + 11 + 10 + 5 + 1);
        Assert.assertEquals(solution.getPatternsPerProblem().get(t4Prob).size(), 4 + 4 + 5 + 8 + 11 + 10 + 5 + 1);

        // ensure the diff is correct
        Assert.assertEquals(solution.getDifferential().values().stream().filter(x -> x.values().stream().anyMatch(y -> y == 0)).count(), 4);
        Assert.assertEquals(solution.getDifferential().values().stream().filter(x -> x.values().stream().anyMatch(y -> y != 0)).count(), 5 + 7 + 10 + 16 + 22 + 20 + 10 + 2 - 4);
    }


    private TrufflePatternProblem createProblem(NodeWrapper node, int patternSize, boolean explicit, String name) {
        TrufflePatternSearchSpaceDefinition definition = new TrufflePatternSearchSpaceDefinition();
        definition.includeTree(node);
        TrufflePatternSearchSpace trees = repository.findTrees(definition);
        return new TrufflePatternProblem(null,trees, name);
    }

    private TruffleDifferentialPatternSolution diff(AbstractPatternDetectorAlgorithm<TruffleDifferentialPatternSolution> algorithm, List<NodeWrapper> nodes, int amountOfPatterns, int patternSize, boolean explicit) {
        algorithm.setMaxPatternSize(patternSize);
        algorithm.setHierarchyFloor(explicit ? 0 : 1);
        algorithm.setHierarchyCeil(explicit ? 0 : 1);

        // given
        List<ProblemGene<TrufflePatternProblem>> list = new ArrayList<>();
        nodes.forEach(x -> list.add(new ProblemGene<>(createProblem(x, patternSize, explicit, String.valueOf(x.getId())))));
        Problem<TrufflePatternProblem> problem = new Problem<>(list);

        // when
        Solution<TruffleDifferentialPatternSolution, TrufflePatternProblem> solution = algorithm.solve(problem);

        // then
        Assert.assertNotNull(solution);
        Assert.assertEquals(solution.getSolutionGenes().get(0).getGene().getDifferential().size(), amountOfPatterns);

        return solution.getSolutionGenes().get(0).getGene();
    }


}
