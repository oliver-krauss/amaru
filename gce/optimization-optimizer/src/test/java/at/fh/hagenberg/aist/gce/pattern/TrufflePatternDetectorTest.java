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

import at.fh.hagenberg.aist.gce.pattern.*;
import at.fh.hagenberg.aist.gce.pattern.selection.DifferentialPatternMiningTest;
import at.fh.hagenberg.aist.gce.pattern.selection.TestDBTest;
import at.fh.hagenberg.aist.gce.pattern.selection.TrufflePatternSearchSpaceDefinition;
import at.fh.hagenberg.machinelearning.analytics.graph.nodes.NodeWrapper;
import at.fh.hagenberg.machinelearning.core.Solution;
import at.fh.hagenberg.util.Pair;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 5) Test the "service" class used in the pattern mining
 *
 * @author Oliver Krauss on 07.10.2019
 */
public class TrufflePatternDetectorTest extends TestDBTest {

    TrufflePatternDetector detector = new TrufflePatternDetector();

    @Test
    public void testMineT34H1() {
        // given
        // we just check if our test setup is correct
        detector.setHierarchyFloor(1);
        detector.setMaxPatternSize(-1);

        // when
        Solution<TrufflePattern, TrufflePatternProblem> solution = significantMining(SignificanceType.ALL, 0, 1);

        // then
        Assert.assertEquals(solution.getSolutionGenes().size(), 5 + 7 + 10 + 16 + 22 + 20 + 10 + 2);
    }

    @Test
    public void testFindSignificantPatternsMIN() {
        // given
        detector.setHierarchyFloor(0);

        // when
        Solution<TrufflePattern, TrufflePatternProblem> solution = significantMining(SignificanceType.MIN, 0, 1);

        // further then
        Assert.assertEquals(solution.getSolutionGenes().size(), 5);
    }

    @Test
    public void testFindSignificantPatternsMINH1() {
        // given
        detector.setHierarchyFloor(1);

        // when
        Solution<TrufflePattern, TrufflePatternProblem> solution = significantMining(SignificanceType.MIN, 0, 1);

        // further then
        Assert.assertEquals(solution.getSolutionGenes().size(), 2 + 3);
    }

    @Test
    public void testFindSignificantPatternsMAX() {
        // given
        detector.setHierarchyFloor(0);

        // when
        Solution<TrufflePattern, TrufflePatternProblem> solution = significantMining(SignificanceType.MAX, 0, 1);

        // further then
        Assert.assertEquals(solution.getSolutionGenes().size(), 5);
    }

    @Test
    public void testFindSignificantPatternsMAXH1() {
        // given
        detector.setHierarchyFloor(1);

        // when
        Solution<TrufflePattern, TrufflePatternProblem> solution = significantMining(SignificanceType.MAX, 0, 1);

        // further then
        Assert.assertEquals(solution.getSolutionGenes().size(), 2 + 3);
    }

    @Test(enabled = false)  // TODO #252 MIN MAX is broken right now
    public void testFindSignificantPatternsPercentageWithMin() {
        // given
        detector.setHierarchyFloor(1);

        // when
        Solution<TrufflePattern, TrufflePatternProblem> solution = significantMining(SignificanceType.MIN, 1, 1);

        // further then
        Assert.assertEquals(solution.getSolutionGenes().size(), 3);
    }

    @Test(enabled = false)  // TODO #252 MIN MAX is broken right now
    public void testFindSignificantPatternsPercentageWithMax() {
        // given
        detector.setHierarchyFloor(1);

        // when
        Solution<TrufflePattern, TrufflePatternProblem> solution = significantMining(SignificanceType.MAX, 0, 0.5);

        // further then
        Assert.assertEquals(solution.getSolutionGenes().size(), 2);
    }

    /**
     * this test is just here to ENSURE that our implementation does not screw up the encapsulation of the DifferentialPatternMiningAlgorithm
     * t is equivalent to {@link DifferentialPatternMiningTest ->testMineT34}
     */
    @Test(enabled = false)  // TODO #252 MIN MAX is broken right now
    public void testDiffSignificantPatternsNoSignificance() {
        // given
        detector.setHierarchyFloor(0);

        // when
        TruffleDifferentialPatternSolution solution = differentialMining(SignificanceType.ALL, 0, 1, 0, 1);

        // further then
        Assert.assertEquals(solution.getDifferential().size(), 7 + 9 + 14 + 20 + 24 + 20 + 10 + 2);
        // further then should be Equivalet to testMineT12 as the Const values are exactly the same!
        TrufflePatternProblem t3Prob = getProblem(solution, "t3");
        TrufflePatternProblem t4Prob = getProblem(solution, "t4");

        // ensure patterns per problem are correct
        Assert.assertEquals(solution.getPatternsPerProblem().get(t3Prob).size(), 6 + 5 + 7 + 10 + 12 + 10 + 5 + 1);
        Assert.assertEquals(solution.getPatternsPerProblem().get(t4Prob).size(), 6 + 5 + 7 + 10 + 12 + 10 + 5 + 1);

        // ensure the diff is correct
        Assert.assertEquals(solution.getDifferential().values().stream().filter(x -> x.values().stream().anyMatch(y -> y == 0)).count(), 6);
        Assert.assertEquals(solution.getDifferential().values().stream().filter(x -> x.values().stream().anyMatch(y -> y != 0)).count(), 7 + 9 + 14 + 20 + 24 + 20 + 10 + 2 - 6);
    }

    @Test(enabled = false) // TODO #252 MIN MAX is broken right now
    public void testDiffSignificantPatternsMIN() {
        // given
        detector.setHierarchyFloor(0);

        // when
        TruffleDifferentialPatternSolution solution = differentialMining(SignificanceType.MIN, 0, 1, 0, 1);

        // further then
        Assert.assertEquals(solution.getDifferential().size(), 6 + 5);
        // further then should be Equivalet to testMineT12 as the Const values are exactly the same!
        TrufflePatternProblem t3Prob = getProblem(solution, "t3");
        TrufflePatternProblem t4Prob = getProblem(solution, "t4");

        // ensure patterns per problem are correct
        Assert.assertEquals(solution.getPatternsPerProblem().get(t3Prob).size(), 8);
        Assert.assertEquals(solution.getPatternsPerProblem().get(t4Prob).size(), 8);

        // ensure the diff is correct
        Assert.assertEquals(solution.getDifferential().values().stream().filter(x -> x.values().stream().anyMatch(y -> y == 0)).count(), 5);
        Assert.assertEquals(solution.getDifferential().values().stream().filter(x -> x.values().stream().anyMatch(y -> y != 0)).count(), 6);
    }

    @Test
    public void testDiffSignificantPatternsMINH1() {
        // given
        detector.setHierarchyFloor(1);

        // when
        TruffleDifferentialPatternSolution solution = differentialMining(SignificanceType.MIN, 0, 1, 0, 1);

        // further then
        Assert.assertEquals(solution.getDifferential().size(), 2 + 3);
        // further then should be Equivalet to testMineT12 as the Const values are exactly the same!
        TrufflePatternProblem t3Prob = getProblem(solution, "t3");
        TrufflePatternProblem t4Prob = getProblem(solution, "t4");

        // ensure patterns per problem are correct
        Assert.assertEquals(solution.getPatternsPerProblem().get(t3Prob).size(), 4);
        Assert.assertEquals(solution.getPatternsPerProblem().get(t4Prob).size(), 4);

        // ensure the diff is correct
        Assert.assertEquals(solution.getDifferential().values().stream().filter(x -> x.values().stream().anyMatch(y -> y == 0)).count(), 3);
        Assert.assertEquals(solution.getDifferential().values().stream().filter(x -> x.values().stream().anyMatch(y -> y != 0)).count(), 2);
    }

    @Test(enabled = false) // TODO #252 MIN MAX is broken right now
    public void testDiffSignificantPatternsMAX() {
        // given
        detector.setHierarchyFloor(0);

        // when
        TruffleDifferentialPatternSolution solution = differentialMining(SignificanceType.MAX, 0, 1, 0, 1);

        // further then
        Assert.assertEquals(solution.getDifferential().size(), 2 + 4);
        // further then should be Equivalet to testMineT12 as the Const values are exactly the same!
        TrufflePatternProblem t3Prob = getProblem(solution, "t3");
        TrufflePatternProblem t4Prob = getProblem(solution, "t4");

        // ensure patterns per problem are correct
        Assert.assertEquals(solution.getPatternsPerProblem().get(t3Prob).size(), 5);
        Assert.assertEquals(solution.getPatternsPerProblem().get(t4Prob).size(), 5);

        // ensure the diff is correct
        Assert.assertEquals(solution.getDifferential().values().stream().filter(x -> x.values().stream().anyMatch(y -> y == 0)).count(), 4);
        Assert.assertEquals(solution.getDifferential().values().stream().filter(x -> x.values().stream().anyMatch(y -> y != 0)).count(), 2);
    }

    @Test
    public void testDiffSignificantPatternsMAXH1() {
        // given
        detector.setHierarchyFloor(1);

        // when
        TruffleDifferentialPatternSolution solution = differentialMining(SignificanceType.MAX, 0, 1, 0, 1);

        // further then
        Assert.assertEquals(solution.getDifferential().size(), 2 + 3);
        // further then should be Equivalet to testMineT12 as the Const values are exactly the same!
        TrufflePatternProblem t3Prob = getProblem(solution, "t3");
        TrufflePatternProblem t4Prob = getProblem(solution, "t4");

        // ensure patterns per problem are correct
        Assert.assertEquals(solution.getPatternsPerProblem().get(t3Prob).size(), 4);
        Assert.assertEquals(solution.getPatternsPerProblem().get(t4Prob).size(), 4);

        // ensure the diff is correct
        Assert.assertEquals(solution.getDifferential().values().stream().filter(x -> x.values().stream().anyMatch(y -> y == 0)).count(), 3);
        Assert.assertEquals(solution.getDifferential().values().stream().filter(x -> x.values().stream().anyMatch(y -> y != 0)).count(), 2);
    }

    @Test(enabled = false)  // TODO #252 MIN MAX is broken right now
    public void testDiffSignificantPatternsPercentageWithMax() {
        // given
        detector.setHierarchyFloor(0);

        // when -> mine only those exactly equal
        TruffleDifferentialPatternSolution solution = differentialMining(SignificanceType.MAX, 0, 1, 0, 0);

        // further then
        Assert.assertEquals(solution.getDifferential().size(), 4);
        // further then should be Equivalet to testMineT12 as the Const values are exactly the same!
        TrufflePatternProblem t3Prob = getProblem(solution, "t3");
        TrufflePatternProblem t4Prob = getProblem(solution, "t4");

        // ensure patterns per problem are correct
        Assert.assertEquals(solution.getPatternsPerProblem().get(t3Prob).size(), 4);
        Assert.assertEquals(solution.getPatternsPerProblem().get(t4Prob).size(), 4);

        // ensure the diff is correct
        Assert.assertEquals(solution.getDifferential().values().stream().filter(x -> x.values().stream().anyMatch(y -> y == 0)).count(), 4);
        Assert.assertEquals(solution.getDifferential().values().stream().filter(x -> x.values().stream().anyMatch(y -> y != 0)).count(), 0);
    }

    @Test(enabled = false)  // TODO #252 MIN MAX is broken right now
    public void testDiffSignificantPatternsPercentageWithMin() {
        // given
        detector.setHierarchyFloor(0);

        // when -> mine only those absolutely different
        TruffleDifferentialPatternSolution solution = differentialMining(SignificanceType.MIN, 0, 1, 1, 1);

        // further then
        Assert.assertEquals(solution.getDifferential().size(), 6);
        // further then should be Equivalet to testMineT12 as the Const values are exactly the same!
        TrufflePatternProblem t3Prob = getProblem(solution, "t3");
        TrufflePatternProblem t4Prob = getProblem(solution, "t4");

        // ensure patterns per problem are correct
        Assert.assertEquals(solution.getPatternsPerProblem().get(t3Prob).size(), 3);
        Assert.assertEquals(solution.getPatternsPerProblem().get(t4Prob).size(), 3);

        // ensure the diff is correct
        Assert.assertEquals(solution.getDifferential().values().stream().filter(x -> x.values().stream().anyMatch(y -> y == 0)).count(), 0);
        Assert.assertEquals(solution.getDifferential().values().stream().filter(x -> x.values().stream().anyMatch(y -> y != 0)).count(), 6);
    }

    @Test
    public void testDiffSignificantPatternsRestrictToEmpty() {
        // given
        detector.setHierarchyFloor(0);

        // when -> We want trees to be AT LEAST diff of 1 but allow only diffs with 0 trees which is impossible
        TruffleDifferentialPatternSolution solution = differentialMining(SignificanceType.MIN, 0, 0, 1, 1);

        // further then
        Assert.assertEquals(solution.getDifferential().size(), 0);
        // further then should be Equivalet to testMineT12 as the Const values are exactly the same!
        TrufflePatternProblem t3Prob = getProblem(solution, "t3");
        TrufflePatternProblem t4Prob = getProblem(solution, "t4");

        // ensure patterns per problem are correct
        Assert.assertEquals(solution.getPatternsPerProblem().get(t3Prob).size(), 0);
        Assert.assertEquals(solution.getPatternsPerProblem().get(t4Prob).size(), 0);

        // ensure the diff is correct
        Assert.assertEquals(solution.getDifferential().values().stream().filter(x -> x.values().stream().anyMatch(y -> y == 0)).count(), 0);
        Assert.assertEquals(solution.getDifferential().values().stream().filter(x -> x.values().stream().anyMatch(y -> y != 0)).count(), 0);
    }


    private Solution<TrufflePattern, TrufflePatternProblem> significantMining(SignificanceType grouping, double min, double max) {
        // given
        detector.setGrouping(grouping);
        detector.setMinSimilarityRating(min);
        detector.setMaxSimilarityRating(max);
        detector.setHierarchyFloor(1);
        detector.setHierarchyCeil(1);

        List<NodeWrapper> nodes = Arrays.asList(t3, t4);
        TrufflePatternSearchSpaceDefinition definition = new TrufflePatternSearchSpaceDefinition();
        nodes.forEach(definition::includeTree);

        Solution<TrufflePattern, TrufflePatternProblem> solution = detector.findPatterns(null,definition, "test");

        // when
        solution = detector.findSignificantPatterns(solution);

        // then
        Assert.assertNotNull(solution);

        return solution;
    }

    private TruffleDifferentialPatternSolution differentialMining(SignificanceType grouping, double min, double max, double minDiff, double maxDiff) {
        // given
        detector.setGrouping(grouping);
        detector.setMinSimilarityRating(min);
        detector.setMaxSimilarityRating(max);
        detector.setMinDifferential(minDiff);
        detector.setMaxDifferential(maxDiff);
        detector.setHierarchyCeil(1);

        List<NodeWrapper> nodesA = Arrays.asList(t3);
        TrufflePatternSearchSpaceDefinition definitionA = new TrufflePatternSearchSpaceDefinition();
        nodesA.forEach(definitionA::includeTree);

        List<NodeWrapper> nodesB = Arrays.asList(t4);
        TrufflePatternSearchSpaceDefinition definitionB = new TrufflePatternSearchSpaceDefinition();
        nodesB.forEach(definitionB::includeTree);

        List<Pair<TrufflePatternSearchSpaceDefinition, String>> ssD = new ArrayList<>();
        ssD.add(new Pair<>(definitionA, "t3"));
        ssD.add(new Pair<>(definitionB, "t4"));

        Solution<TruffleDifferentialPatternSolution, TrufflePatternProblem> solution = detector.comparePatternsBySearchSpaceDefinition(null,ssD);

        // when
        solution = detector.compareSignificantPatterns(solution);

        // then
        Assert.assertNotNull(solution);
        Assert.assertEquals(solution.getSolutionGenes().size(), 1);
        Assert.assertEquals(solution.getSolutionGenes().get(0).getGene().getPatternsPerProblem().size(), 2);

        return solution.getSolutionGenes().get(0).getGene();
    }

    private TrufflePatternProblem getProblem(TruffleDifferentialPatternSolution solution, String name) {
        return solution.getPatternsPerProblem().keySet().stream().filter(x -> x.getName().equals(String.valueOf(name))).findFirst().orElse(null);
    }
}
