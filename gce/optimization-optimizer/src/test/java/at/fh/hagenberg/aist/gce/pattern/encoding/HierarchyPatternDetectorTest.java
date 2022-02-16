/*
 * Copyright (c) 2022 the original author or authors.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

package at.fh.hagenberg.aist.gce.pattern.encoding;

import at.fh.hagenberg.aist.gce.minic.language.MinicLanguage;
import at.fh.hagenberg.aist.gce.pattern.*;
import at.fh.hagenberg.aist.gce.pattern.selection.DifferentialPatternMiningTest;
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
 * // TODO These all don't work until #251 is implemented again!!!!
 * @author Oliver Krauss on 18.11.2019
 */
@Test
public class HierarchyPatternDetectorTest extends TestRealNodesDbTest {

    TrufflePatternDetector detector = new TrufflePatternDetector();

//    @Test
//    public void testMineT34H1() {
//        // given
//        // we just check if our test setup is correct
//        detector.setHierarchyFloor(1);
//        detector.setMaxPatternSize(-1);
//
//        // when
//        Solution<TrufflePattern, TrufflePatternProblem> solution = significantMining(SignificanceType.ALL, 0, 1, Integer.MAX_VALUE);
//
//        // then
//        Assert.assertEquals(solution.getSolutionGenes().size(), 7954);
//    }
//
//    @Test
//    public void testMineT34H1FloorOnly() {
//        // given
//        // we just check if our test setup is correct
//        detector.setHierarchyFloor(1);
//        detector.setMaxPatternSize(-1);
//
//        // when
//        Solution<TrufflePattern, TrufflePatternProblem> solution = significantMining(SignificanceType.ALL, 0, 1, 1);
//
//        // then
//        Assert.assertEquals(solution.getSolutionGenes().size(), 32);
//    }
//
//    @Test
//    public void testFindSignificantPatternsMIN() {
//        // given
//        detector.setHierarchyFloor(0);
//
//        // when
//        Solution<TrufflePattern, TrufflePatternProblem> solution = significantMining(SignificanceType.MIN, 0, 1, Integer.MAX_VALUE);
//
//        // further then
//        Assert.assertEquals(solution.getSolutionGenes().size(), 13);
//    }
//
//    @Test
//    public void testFindSignificantPatternsMINH1() {
//        // given
//        detector.setHierarchyFloor(1);
//
//        // when
//        Solution<TrufflePattern, TrufflePatternProblem> solution = significantMining(SignificanceType.MIN, 0, 1, Integer.MAX_VALUE);
//
//        // further then
//        Assert.assertEquals(solution.getSolutionGenes().size(), 8);
//    }
//
//    @Test
//    public void testFindSignificantPatternsMAX() {
//        // given
//        detector.setHierarchyFloor(0);
//
//        // when
//        Solution<TrufflePattern, TrufflePatternProblem> solution = significantMining(SignificanceType.MAX, 0, 1, Integer.MAX_VALUE);
//
//        // further then
//        Assert.assertEquals(solution.getSolutionGenes().size(), 3);
//    }
//
//    @Test
//    public void testFindSignificantPatternsMAXH1() {
//        // given
//        detector.setHierarchyFloor(1);
//
//        // when
//        Solution<TrufflePattern, TrufflePatternProblem> solution = significantMining(SignificanceType.MAX, 0, 1, Integer.MAX_VALUE);
//
//        // further then
//        Assert.assertEquals(solution.getSolutionGenes().size(), 3);
//    }
//
//    @Test
//    public void testFindSignificantPatternsPercentageWithMin() {
//        // given
//        detector.setHierarchyFloor(1);
//
//        // when
//        Solution<TrufflePattern, TrufflePatternProblem> solution = significantMining(SignificanceType.MIN, 1, 1, Integer.MAX_VALUE);
//
//        // further then
//        Assert.assertEquals(solution.getSolutionGenes().size(), 5);
//    }
//
//    @Test
//    public void testFindSignificantPatternsPercentageWithMinFloorOnly() {
//        // given
//        detector.setHierarchyFloor(1);
//
//        // when
//        Solution<TrufflePattern, TrufflePatternProblem> solution = significantMining(SignificanceType.MIN, 1, 1, 1);
//
//        // further then
//        Assert.assertEquals(solution.getSolutionGenes().size(), 2);
//    }
//
//    @Test
//    public void testFindSignificantPatternsPercentageWithMax() {
//        // given
//        detector.setHierarchyFloor(1);
//
//        // when
//        Solution<TrufflePattern, TrufflePatternProblem> solution = significantMining(SignificanceType.MAX, 0, 0.5, 1);
//
//        // further then
//        Assert.assertEquals(solution.getSolutionGenes().size(), 2);
//    }
//
//    /**
//     * this test is just here to ENSURE that our implementation does not screw up the encapsulation of the DifferentialPatternMiningAlgorithm
//     * t is equivalent to {@link DifferentialPatternMiningTest ->testMineT34}
//     */
//    @Test
//    public void testDiffSignificantPatternsNoSignificance() {
//        // given
//        detector.setHierarchyFloor(0);
//
//        // when
//        TruffleDifferentialPatternSolution solution = differentialMining(SignificanceType.ALL, 0, 1, 0, 1, Integer.MAX_VALUE);
//
//        // further then
//        Assert.assertEquals(solution.getDifferential().size(), 21907);
//        // further then should be Equivalet to testMineT12 as the Const values are exactly the same!
//        TrufflePatternProblem t3Prob = getProblem(solution, "t3");
//        TrufflePatternProblem t4Prob = getProblem(solution, "t4");
//
//        // ensure patterns per problem are correct
//        Assert.assertEquals(solution.getPatternsPerProblem().get(t3Prob).size(), 11206);
//        Assert.assertEquals(solution.getPatternsPerProblem().get(t4Prob).size(), 11210);
//
//        // ensure the diff is correct
//        Assert.assertEquals(solution.getDifferential().values().stream().filter(x -> x.values().stream().anyMatch(y -> y == 0)).count(), 505);
//        Assert.assertEquals(solution.getDifferential().values().stream().filter(x -> x.values().stream().anyMatch(y -> y != 0)).count(), 21402);
//    }
//
//    /**
//     * this test is just here to ENSURE that our implementation does not screw up the encapsulation of the DifferentialPatternMiningAlgorithm
//     * t is equivalent to {@link DifferentialPatternMiningTest ->testMineT34}
//     */
//    @Test
//    public void testDiffSignificantPatternsNoSignificanceFloorOnly() {
//        // given
//        detector.setHierarchyFloor(0);
//
//        // when
//        TruffleDifferentialPatternSolution solution = differentialMining(SignificanceType.ALL, 0, 1, 0, 1, 1);
//
//        // further then
//        Assert.assertEquals(solution.getDifferential().size(), 290);
//        // further then should be Equivalet to testMineT12 as the Const values are exactly the same!
//        TrufflePatternProblem t3Prob = getProblem(solution, "t3");
//        TrufflePatternProblem t4Prob = getProblem(solution, "t4");
//
//        // ensure patterns per problem are correct
//        Assert.assertEquals(solution.getPatternsPerProblem().get(t3Prob).size(), 147);
//        Assert.assertEquals(solution.getPatternsPerProblem().get(t4Prob).size(), 148);
//
//        // ensure the diff is correct
//        Assert.assertEquals(solution.getDifferential().values().stream().filter(x -> x.values().stream().anyMatch(y -> y == 0)).count(), 4);
//        Assert.assertEquals(solution.getDifferential().values().stream().filter(x -> x.values().stream().anyMatch(y -> y != 0)).count(), 286);
//    }
//
//    @Test
//    public void testDiffSignificantPatternsMIN() {
//        // given
//        detector.setHierarchyFloor(0);
//
//        // when
//        TruffleDifferentialPatternSolution solution = differentialMining(SignificanceType.MIN, 0, 1, 0, 1, Integer.MAX_VALUE);
//
//        // further then
//        Assert.assertEquals(solution.getDifferential().size(), 315);
//        // further then should be Equivalet to testMineT12 as the Const values are exactly the same!
//        TrufflePatternProblem t3Prob = getProblem(solution, "t3");
//        TrufflePatternProblem t4Prob = getProblem(solution, "t4");
//
//        // ensure patterns per problem are correct
//        Assert.assertEquals(solution.getPatternsPerProblem().get(t3Prob).size(), 168);
//        Assert.assertEquals(solution.getPatternsPerProblem().get(t4Prob).size(), 164);
//
//        // ensure the diff is correct
//        Assert.assertEquals(solution.getDifferential().values().stream().filter(x -> x.values().stream().anyMatch(y -> y == 0)).count(), 15);
//        Assert.assertEquals(solution.getDifferential().values().stream().filter(x -> x.values().stream().anyMatch(y -> y != 0)).count(), 300);
//    }
//
//    @Test(invocationCount = 1)
//    public void testDiffSignificantPatternsMINH1() {
//        // given
//        detector.setHierarchyFloor(1);
//
//        // when
//        TruffleDifferentialPatternSolution solution = differentialMining(SignificanceType.MIN, 0, 1, 0, 1, Integer.MAX_VALUE);
//
//        // further then
//        Assert.assertEquals(solution.getDifferential().size(), 244);
//        // further then should be Equivalet to testMineT12 as the Const values are exactly the same!
//        TrufflePatternProblem t3Prob = getProblem(solution, "t3");
//        TrufflePatternProblem t4Prob = getProblem(solution, "t4");
//
//        // ensure patterns per problem are correct
//        Assert.assertEquals(solution.getPatternsPerProblem().get(t3Prob).size(), 132);
//        Assert.assertEquals(solution.getPatternsPerProblem().get(t4Prob).size(), 128);
//
//        // ensure the diff is correct
//        Assert.assertEquals(solution.getDifferential().values().stream().filter(x -> x.values().stream().anyMatch(y -> y == 0)).count(), 14);
//        Assert.assertEquals(solution.getDifferential().values().stream().filter(x -> x.values().stream().anyMatch(y -> y != 0)).count(), 230);
//    }
//
//    @Test(invocationCount = 1)
//    public void testDiffSignificantPatternsMINH1FloorOnly() {
//        // given
//        detector.setHierarchyFloor(1);
//
//        // when
//        TruffleDifferentialPatternSolution solution = differentialMining(SignificanceType.MIN, 0, 1, 0, 1, 1);
//
//        // further then
//        Assert.assertEquals(solution.getDifferential().size(), 5);
//        // further then should be Equivalet to testMineT12 as the Const values are exactly the same!
//        TrufflePatternProblem t3Prob = getProblem(solution, "t3");
//        TrufflePatternProblem t4Prob = getProblem(solution, "t4");
//
//        // ensure patterns per problem are correct
//        Assert.assertEquals(solution.getPatternsPerProblem().get(t3Prob).size(), 3);
//        Assert.assertEquals(solution.getPatternsPerProblem().get(t4Prob).size(), 4);
//
//        // ensure the diff is correct
//        Assert.assertEquals(solution.getDifferential().values().stream().filter(x -> x.values().stream().anyMatch(y -> y == 0)).count(), 1);
//        Assert.assertEquals(solution.getDifferential().values().stream().filter(x -> x.values().stream().anyMatch(y -> y != 0)).count(), 4);
//    }
//
//    @Test(enabled = false)
//    public void testDiffSignificantPatternsMAX() {
//        // given
//        detector.setHierarchyFloor(0);
//
//        // when
//        TruffleDifferentialPatternSolution solution = differentialMining(SignificanceType.MAX, 0, 1, 0, 1, Integer.MAX_VALUE);
//
//        // further then
//        Assert.assertEquals(solution.getDifferential().size(), 12340);
//        // further then should be Equivalet to testMineT12 as the Const values are exactly the same!
//        TrufflePatternProblem t3Prob = getProblem(solution, "t3");
//        TrufflePatternProblem t4Prob = getProblem(solution, "t4");
//
//        // ensure patterns per problem are correct
//        Assert.assertEquals(solution.getPatternsPerProblem().get(t3Prob).size(), 6250);
//        Assert.assertEquals(solution.getPatternsPerProblem().get(t4Prob).size(), 6250);
//
//        // ensure the diff is correct
//        Assert.assertEquals(solution.getDifferential().values().stream().filter(x -> x.values().stream().anyMatch(y -> y == 0)).count(), 160);
//        Assert.assertEquals(solution.getDifferential().values().stream().filter(x -> x.values().stream().anyMatch(y -> y != 0)).count(), 12180);
//    }
//
//    @Test
//    public void testDiffSignificantPatternsMAXH1() {
//        // given
//        detector.setHierarchyFloor(1);
//
//        // when
//        TruffleDifferentialPatternSolution solution = differentialMining(SignificanceType.MAX, 0, 1, 0, 1, Integer.MAX_VALUE);
//
//        // further then
//        Assert.assertEquals(solution.getDifferential().size(), 3968);
//        // further then should be Equivalet to testMineT12 as the Const values are exactly the same!
//        TrufflePatternProblem t3Prob = getProblem(solution, "t3");
//        TrufflePatternProblem t4Prob = getProblem(solution, "t4");
//
//        // ensure patterns per problem are correct
//        Assert.assertEquals(solution.getPatternsPerProblem().get(t3Prob).size(), 2048);
//        Assert.assertEquals(solution.getPatternsPerProblem().get(t4Prob).size(), 2048);
//
//        // ensure the diff is correct
//        Assert.assertEquals(solution.getDifferential().values().stream().filter(x -> x.values().stream().anyMatch(y -> y == 0)).count(), 128);
//        Assert.assertEquals(solution.getDifferential().values().stream().filter(x -> x.values().stream().anyMatch(y -> y != 0)).count(), 3840);
//    }
//
//    @Test
//    public void testDiffSignificantPatternsMAXH1FloorOnly() {
//        // given
//        detector.setHierarchyFloor(1);
//
//        // when
//        TruffleDifferentialPatternSolution solution = differentialMining(SignificanceType.MAX, 0, 1, 0, 1, 1);
//
//        // further then
//        Assert.assertEquals(solution.getDifferential().size(), 4);
//        // further then should be Equivalet to testMineT12 as the Const values are exactly the same!
//        TrufflePatternProblem t3Prob = getProblem(solution, "t3");
//        TrufflePatternProblem t4Prob = getProblem(solution, "t4");
//
//        // ensure patterns per problem are correct
//        Assert.assertEquals(solution.getPatternsPerProblem().get(t3Prob).size(), 3);
//        Assert.assertEquals(solution.getPatternsPerProblem().get(t4Prob).size(), 3);
//
//        // ensure the diff is correct
//        Assert.assertEquals(solution.getDifferential().values().stream().filter(x -> x.values().stream().anyMatch(y -> y == 0)).count(), 1);
//        Assert.assertEquals(solution.getDifferential().values().stream().filter(x -> x.values().stream().anyMatch(y -> y != 0)).count(), 3);
//    }
//
//    @Test
//    public void testDiffSignificantPatternsPercentageWithMax() {
//        // given
//        detector.setHierarchyFloor(0);
//
//        // when -> mine only those exactly equal
//        TruffleDifferentialPatternSolution solution = differentialMining(SignificanceType.MAX, 0, 1, 0, 0, 1);
//
//        // further then
//        Assert.assertEquals(solution.getDifferential().size(), 2);
//        // further then should be Equivalet to testMineT12 as the Const values are exactly the same!
//        TrufflePatternProblem t3Prob = getProblem(solution, "t3");
//        TrufflePatternProblem t4Prob = getProblem(solution, "t4");
//
//        // ensure patterns per problem are correct
//        Assert.assertEquals(solution.getPatternsPerProblem().get(t3Prob).size(), 2);
//        Assert.assertEquals(solution.getPatternsPerProblem().get(t4Prob).size(), 2);
//
//        // ensure the diff is correct
//        Assert.assertEquals(solution.getDifferential().values().stream().filter(x -> x.values().stream().anyMatch(y -> y == 0)).count(), 2);
//        Assert.assertEquals(solution.getDifferential().values().stream().filter(x -> x.values().stream().anyMatch(y -> y != 0)).count(), 0);
//    }
//
//    @Test
//    public void testDiffSignificantPatternsPercentageWithMin() {
//        // given
//        detector.setHierarchyFloor(0);
//
//        // when -> mine only those absolutely different
//        TruffleDifferentialPatternSolution solution = differentialMining(SignificanceType.MIN, 0, 1, 1, 1, 1);
//
//        // further then
//        Assert.assertEquals(solution.getDifferential().size(), 14);
//        // further then should be Equivalet to testMineT12 as the Const values are exactly the same!
//        TrufflePatternProblem t3Prob = getProblem(solution, "t3");
//        TrufflePatternProblem t4Prob = getProblem(solution, "t4");
//
//        // ensure patterns per problem are correct
//        Assert.assertEquals(solution.getPatternsPerProblem().get(t3Prob).size(), 7);
//        Assert.assertEquals(solution.getPatternsPerProblem().get(t4Prob).size(), 8);
//
//        // ensure the diff is correct
//        Assert.assertEquals(solution.getDifferential().values().stream().filter(x -> x.values().stream().anyMatch(y -> y == 0)).count(), 0);
//        Assert.assertEquals(solution.getDifferential().values().stream().filter(x -> x.values().stream().anyMatch(y -> y != 0)).count(), 14);
//    }

    @Test
    public void testDiffSignificantPatternsRestrictToEmpty() {
        // given
        detector.setHierarchyFloor(0);

        // when -> We want trees to be AT LEAST diff of 1 but allow only diffs with 0 trees which is impossible
        TruffleDifferentialPatternSolution solution = differentialMining(SignificanceType.MIN, 0, 0, 1, 1, 1);

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


    private Solution<TrufflePattern, TrufflePatternProblem> significantMining(SignificanceType grouping, double min, double max, int ceiling) {
        // given
        detector.setGrouping(grouping);
        detector.setMinSimilarityRating(min);
        detector.setMaxSimilarityRating(max);
        detector.setHierarchyCeil(ceiling);

        List<NodeWrapper> nodes = Arrays.asList(t3, t4);
        TrufflePatternSearchSpaceDefinition definition = new TrufflePatternSearchSpaceDefinition();
        nodes.forEach(definition::includeTree);

        Solution<TrufflePattern, TrufflePatternProblem> solution = detector.findPatterns(MinicLanguage.ID, definition, "test");

        // when
        solution = detector.findSignificantPatterns(solution);

        // debug code in case something goes horribly wrong
        // solution.getSolutionGenes().forEach(x -> System.out.println(x.getGene().getPatternNode().humanReadable()));

        // then
        Assert.assertNotNull(solution);
        return solution;
    }

    private TruffleDifferentialPatternSolution differentialMining(SignificanceType grouping, double min, double max, double minDiff, double maxDiff, int ceil) {
        // given
        detector.setGrouping(grouping);
        detector.setMinSimilarityRating(min);
        detector.setMaxSimilarityRating(max);
        detector.setMinDifferential(minDiff);
        detector.setMaxDifferential(maxDiff);
        detector.setHierarchyCeil(ceil);

        List<NodeWrapper> nodesA = Arrays.asList(t3);
        TrufflePatternSearchSpaceDefinition definitionA = new TrufflePatternSearchSpaceDefinition();
        nodesA.forEach(definitionA::includeTree);

        List<NodeWrapper> nodesB = Arrays.asList(t4);
        TrufflePatternSearchSpaceDefinition definitionB = new TrufflePatternSearchSpaceDefinition();
        nodesB.forEach(definitionB::includeTree);

        List<Pair<TrufflePatternSearchSpaceDefinition, String>> ssD = new ArrayList<>();
        ssD.add(new Pair<>(definitionA, "t3"));
        ssD.add(new Pair<>(definitionB, "t4"));

        Solution<TruffleDifferentialPatternSolution, TrufflePatternProblem> solution = detector.comparePatternsBySearchSpaceDefinition(MinicLanguage.ID, ssD);

        // when
        solution = detector.compareSignificantPatterns(solution);

        // debug code in case something goes horribly wrong
//        solution.getSolutionGenes().get(0).getGene().getDifferential().forEach((key, value) -> {
//            System.out.println(key.getPatternNode().humanReadable() + " " + key.getCount());
//        });

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
