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

import at.fh.hagenberg.aist.gce.pattern.TrufflePattern;
import at.fh.hagenberg.aist.gce.pattern.TrufflePatternProblem;
import at.fh.hagenberg.aist.gce.pattern.algorithm.AbstractPatternDetectorAlgorithm;
import at.fh.hagenberg.aist.gce.pattern.algorithm.AprioriPatternDetectorAlgorithm;
import at.fh.hagenberg.aist.gce.pattern.algorithm.PatternGrowthPatternDetector;
import at.fh.hagenberg.aist.gce.pattern.algorithm.labeller.ManualLabeller;
import at.fh.hagenberg.machinelearning.analytics.graph.nodes.NodeWrapper;
import at.fh.hagenberg.machinelearning.core.Problem;
import at.fh.hagenberg.machinelearning.core.ProblemGene;
import at.fh.hagenberg.machinelearning.core.Solution;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.*;

/**
 * 4a) This test ensures the validity of mining the * wildcard patterns *
 * As this is only supported by IGOR only the PatternGrowthPatternDetector is tested.
 *
 * @author Oliver Krauss on 22.02.2021
 */
@Test
public class EmbeddedPatternMiningTest extends TestDBTest {

    @DataProvider(name = "provider")
    public static Object[][] algProvider() {
        PatternGrowthPatternDetector detector = new PatternGrowthPatternDetector();
        detector.setEmbedded(true);
        Map<String, String> labelMap = new HashMap<>();
        labelMap.put("Variable", "value");
        ManualLabeller manualLabeller = new ManualLabeller(labelMap);
        detector.setVariableLabeller(manualLabeller);
        return new Object[][]{{detector}};
    }

    // Note: The following tests are direct copies from the String subgraph iterator, they exist only to ensure that the integration in the algorithm works
    @Test(dataProvider = "provider")
    public void testMineT1(AbstractPatternDetectorAlgorithm<TrufflePattern> detector) {
        mine(detector, t1, 2, -1, true);
    }

    // Similarity pruning test. This should return 1, 2 and 3 patterns respectively
    @Test(dataProvider = "provider")
    public void testMineT5(AbstractPatternDetectorAlgorithm<TrufflePattern> detector) {
        List<NodeWrapper> nodes = Collections.singletonList(t5);
        mine(detector, nodes, 2, 1, false);
        mine(detector, nodes, 4, 2, false);
        mine(detector, nodes, 6, 3, false);
    }

    @Test(dataProvider = "provider")
    public void testMineT2(AbstractPatternDetectorAlgorithm<TrufflePattern> detector) {
        Solution<TrufflePattern, TrufflePatternProblem> mine = mine(detector, t2, 10, -1, true);

        // further then
        mine.getSolutionGenes().forEach(x -> {
            // trees
            Assert.assertEquals(x.getGene().getTreeIds().size(), 1);
            Assert.assertTrue(x.getGene().getTreeIds().contains(t2.getId()));
            Assert.assertEquals(x.getGene().getTreeCount(), 1);

            // nodes
            Assert.assertEquals(x.getGene().getNodeIds().size(), x.getGene().getSize());
            Assert.assertEquals(x.getGene().getCount(), 1);
            Assert.assertNotNull(x.getGene().getPatternNode());
        });
    }


    // when mining ONLY T3 we will get LESS patterns than permutations as Variable(x) and Const(..) occur multiple times
    @Test(dataProvider = "provider")
    public void testMineT3(AbstractPatternDetectorAlgorithm<TrufflePattern> detector) {
        mine(detector, t3, 7, 1, true);
        mine(detector, t3, 7 + (5 + 8), 2, true);
        mine(detector, t3, 7 + 13 + (7 + 29), 3, true);
        mine(detector, t3, 7 + 8 + 36 + (10 + 61), 4, true);
        mine(detector, t3, 7 + 8 + 36 + 71 + (12 + 58), 5, true);
        mine(detector, t3, 7 + 8 + 36 + 71 + 70 + (10 + 32), 6, true);
        mine(detector, t3, 7 + 8 + 36 + 71 + 70 + 42 + (5 + 9), 7, true);
        mine(detector, t3, 7 + 8 + 36 + 71 + 70 + 42 + 14 + 2, 8, true);
        mine(detector, t3, 7 + 8 + 36 + 71 + 70 + 42 + 14 + 2, 9, true);
    }


    @Test(dataProvider = "provider")
    public void testMineT3H1(AbstractPatternDetectorAlgorithm<TrufflePattern> detector) {
        mine(detector, t3, 5, 1, false);
        mine(detector, t3, 5 + (4 + 4), 2, false);
        mine(detector, t3, 5 + 8 + (5 + 18), 3, false);
        mine(detector, t3, 5 + 8 + 23 + (8 + 46), 4, false);
        mine(detector, t3, 5 + 8 + 23 + 54 + (11 + 55), 5, false);
        mine(detector, t3, 5 + 8 + 23 + 54 + 66 + (10 + 32), 6, false);
        mine(detector, t3, 5 + 8 + 23 + 54 + 66 + 42 + (5 + 9), 7, false);
        mine(detector, t3, 5 + 8 + 23 + 54 + 66 + 42 + 14 + 2, 8, false);
        mine(detector, t3, 5 + 8 + 23 + 54 + 66 + 42 + 14 + 2, 9, false);
    }


    // when mining ONLY T3 we will get LESS patterns than permutations as Variable(x) and Const(..) occur multiple times
    @Test(dataProvider = "provider")
    public void testMineT6(AbstractPatternDetectorAlgorithm<TrufflePattern> detector) {
        if (detector instanceof AprioriPatternDetectorAlgorithm) {
            return;
        }
        mine(detector, t6, 4, 1, true);
        mine(detector, t6, 4 + (3 + 2), 2, true);
        mine(detector, t6, 4 + 5 + (4 + 9), 3, true);
        mine(detector, t6, 4 + 5 + 13 + (6 + 26), 4, true);
        mine(detector, t6, 4 + 5 + 13 + 32 + (9 + 43), 5, true);
        mine(detector, t6, 4 + 5 + 13 + 32 + 52 + (9 + 31), 6, true);
        mine(detector, t6, 4 + 5 + 13 + 32 + 52 + 40 + (5 + 9), 7, true);
        mine(detector, t6, 4 + 5 + 13 + 32 + 52 + 40 + 14 + 2, 8, true);
        mine(detector, t6, 4 + 5 + 13 + 32 + 52 + 40 + 14 + 2, 9, true);
    }

    // when mining ONLY T3 we will get LESS patterns than permutations as Variable(x) and Const(..) occur multiple times
    @Test(dataProvider = "provider")
    public void testMineT7(AbstractPatternDetectorAlgorithm<TrufflePattern> detector) {
        mine(detector, t7, 4, 1, true);
        mine(detector, t7, 4 + (3 + 2), 2, true);
        mine(detector, t7, 4 + 5 + (4 + 7), 3, true);
        mine(detector, t7, 4 + 5 + 11 + (5 + 15), 4, true);
        mine(detector, t7, 4 + 5 + 11 + 20 + (6 + 20), 5, true);
        mine(detector, t7, 4 + 5 + 11 + 20 + 26 + (5 + 17), 6, true);
        mine(detector, t7, 4 + 5 + 11 + 20 + 26 + 22 + (3 + 7), 7, true);
        mine(detector, t7, 4 + 5 + 11 + 20 + 26 + 22 + 10 + 2, 8, true);
        mine(detector, t7, 4 + 5 + 11 + 20 + 26 + 22 + 10 + 2, 9, true);
    }

    // checking out the variable labelling. With 3 and 3a the patterns should be equal as to just mining t3
    @Test(dataProvider = "provider")
    public void testMineTVarLabelSame(AbstractPatternDetectorAlgorithm<TrufflePattern> detector) {
        List<NodeWrapper> nodes = Arrays.asList(t3, t3a);
        mine(detector, nodes, 7, 1, true);
        mine(detector, nodes, 7 + (5 + 8), 2, true);
        mine(detector, nodes, 7 + 13 + (7 + 29), 3, true);
        mine(detector, nodes, 7 + 13 + 36 + (10 + 56), 4, true);
        mine(detector, nodes, 7 + 13 + 36 + 66 + (12 + 58), 5, true);
        mine(detector, nodes, 7 + 13 + 36 + 66 + 70 + (10 + 32), 6, true);
        mine(detector, nodes, 7 + 13 + 36 + 66 + 70 + 42 + (5 + 9), 7, true);
        mine(detector, nodes, 7 + 13 + 36 + 66 + 70 + 42 + 14 + 2, 8, true);
        mine(detector, nodes, 7 + 13 + 36 + 66 + 70 + 42 + 14 + 2, 9, true);
    }

    // checking out the variable labelling. With 3 and 3b the patterns should be MORE than from mining just t3
    @Test(dataProvider = "provider")
    public void testMineTVarLabelDifferent(AbstractPatternDetectorAlgorithm<TrufflePattern> detector) {
        List<NodeWrapper> nodes = Arrays.asList(t3, t3b);
        mine(detector, nodes, 7, 1, true);
        mine(detector, nodes, 7 + (5 + 8), 2, true);
        mine(detector, nodes, 7 + 13 + (7 + 31), 3, true);
        mine(detector, nodes, 7 + 13 + 38 + (10 + 66), 4, true);
        mine(detector, nodes, 7 + 13 + 38 + 76 + (13 + 77), 5, true);
        mine(detector, nodes, 7 + 13 + 38 + 76 + 90 + (13 + 49), 6, true);
        mine(detector, nodes, 7 + 13 + 38 + 76 + 90 + 62 + (8 + 16), 7, true);
        mine(detector, nodes, 7 + 13 + 38 + 76 + 90 + 62 + 24 + 4, 8, true);
        mine(detector, nodes, 7 + 13 + 38 + 76 + 90 + 62 + 24 + 4, 9, true);
    }

    // T1 is a subset of T2
    @Test(dataProvider = "provider")
    public void testMineT12(AbstractPatternDetectorAlgorithm<TrufflePattern> detector) {
        List<NodeWrapper> nodes = Arrays.asList(t1, t2);
        mine(detector, nodes, 10, -1, true);
    }

    // T1 is a subset of T2  is a subset of T3. Thus the patterns mined from T3 should be 100% EQUIVALENT to the ones JUST from T3
    @Test(dataProvider = "provider")
    public void testMineT123(AbstractPatternDetectorAlgorithm<TrufflePattern> detector) {
        List<NodeWrapper> nodes = Arrays.asList(t1, t2, t3);
        mine(detector, nodes, 7 + 8 + 36 + 71 + 70 + 42 + 14 + 2, -1, true);
        mine(detector, nodes, 5 + 8 + 23 + 54 + 66 + 42 + 14 + 2, -1, false);
    }

    // T3 and T4 are DIFFERENT in exactly 2 spots the "!=" instead of the "="
    @Test(dataProvider = "provider")
    public void testMineT34H0(AbstractPatternDetectorAlgorithm<TrufflePattern> detector) {
        List<NodeWrapper> nodes = Arrays.asList(t3, t4);
        mine(detector, nodes, 8, 1, true);
        mine(detector, nodes, 8 + (9 + 9), 2, true);
        mine(detector, nodes, 8 + 18 + (14 + 42), 3, true);
        mine(detector, nodes, 8 + 18 + 56 + (20 + 94), 4, true);
        mine(detector, nodes, 8 + 18 + 56 + 114 + (24 + 106), 5, true);
        mine(detector, nodes, 8 + 18 + 56 + 114 + 130 + (20 + 62), 6, true);
        mine(detector, nodes, 8 + 18 + 56 + 114 + 130 + (82 + 18) + 10, 7, true);
        mine(detector, nodes, 8 + 18 + 56 + 114 + 130 + 100 + 10 + 4, 8, true);
        mine(detector, nodes, 8 + 18 + 56 + 114 + 130 + 100 + 10 + 4, 9, true);
    }

    @Test(dataProvider = "provider")
    public void testMineT34H1(AbstractPatternDetectorAlgorithm<TrufflePattern> detector) {
        List<NodeWrapper> nodes = Arrays.asList(t3, t4);
        mine(detector, nodes, 6, 1, false);
        mine(detector, nodes, 6 + (7 + 5), 2, false);
        mine(detector, nodes, 6 + 12 + (10 + 28), 3, false);
        mine(detector, nodes, 6 + 12 + 38 + (16 + 78), 4, false);
        mine(detector, nodes, 6 + 12 + 38 + 94 + (22 + 100), 5, false);
        mine(detector, nodes, 6 + 12 + 38 + 94 + 122 + (20 + 62), 6, false);
        mine(detector, nodes, 6 + 12 + 38 + 94 + 122 + 82 + (10 + 18), 7, false);
        mine(detector, nodes, 6 + 12 + 38 + 94 + 122 + 82 + 28 + 4, 8, false);
        mine(detector, nodes, 6 + 12 + 38 + 94 + 122 + 82 + 28 + 4, 9, false);
    }


    private Solution<TrufflePattern, TrufflePatternProblem> mine(AbstractPatternDetectorAlgorithm<TrufflePattern> detector, NodeWrapper node, int amountOfPatterns, int patternSize, boolean explicit) {
        ArrayList<NodeWrapper> nodes = new ArrayList<>();
        nodes.add(node);
        return mine(detector, nodes, amountOfPatterns, patternSize, explicit);
    }

    private Solution<TrufflePattern, TrufflePatternProblem> mine(AbstractPatternDetectorAlgorithm<TrufflePattern> detector, List<NodeWrapper> nodes, int amountOfPatterns, int patternSize, boolean explicit) {
        // given
        detector.setHierarchyFloor(explicit ? 0 : 1);
        detector.setHierarchyCeil(explicit ? 0 : 1);
        detector.setMaxPatternSize(patternSize);

        TrufflePatternSearchSpaceDefinition definition = new TrufflePatternSearchSpaceDefinition();
        nodes.forEach(definition::includeTree);
        TrufflePatternSearchSpace trees = repository.findTrees(definition);

        TrufflePatternProblem tpp = new TrufflePatternProblem(null, trees, "TEST");
        List<ProblemGene<TrufflePatternProblem>> list = new ArrayList<>();
        list.add(new ProblemGene<>(tpp));
        Problem<TrufflePatternProblem> problem = new Problem<>(list);

        // when
        Solution<TrufflePattern, TrufflePatternProblem> solution = detector.solve(problem);

        // then
        Assert.assertNotNull(solution);
        Assert.assertEquals(solution.getSolutionGenes().size(), amountOfPatterns);

        return solution;
    }
}