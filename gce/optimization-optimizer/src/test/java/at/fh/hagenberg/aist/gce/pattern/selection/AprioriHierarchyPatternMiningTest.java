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
import at.fh.hagenberg.aist.gce.pattern.encoding.BitwisePatternMeta;
import at.fh.hagenberg.machinelearning.analytics.graph.nodes.NodeWrapper;
import at.fh.hagenberg.machinelearning.core.Problem;
import at.fh.hagenberg.machinelearning.core.ProblemGene;
import at.fh.hagenberg.machinelearning.core.Solution;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.*;

/**
 * 4a) This test ensures that the hierarchies are correct
 * The values below were hand-checked and should NEVER change,
 * unless you use a heuristic mining approach in which case they should neve be exceeded.
 *
 * @author Oliver Krauss on 02.10.2019
 */
@Test
public class AprioriHierarchyPatternMiningTest extends TestDBTest {

    @DataProvider(name = "provider")
    public static Object[][] algProvider() {
        AprioriPatternDetectorAlgorithm apriori = new AprioriPatternDetectorAlgorithm();
        return new Object[][]{{apriori}};
    }

    // Note: The following tests are direct copies from the String subgraph iterator, they exist only to ensure that the integration in the algorithm works
    @Test(dataProvider = "provider")
    public void testMineT1(AbstractPatternDetectorAlgorithm<TrufflePattern> detector) {
        //mine(detector, t1, 1, -1, 0, 0);
        mine(detector, t1, 2, -1, 1, Integer.MAX_VALUE);
        mine(detector, t1, 3, -1, 0, Integer.MAX_VALUE);
    }

    @Test(dataProvider = "provider")
    public void testMineT2(AbstractPatternDetectorAlgorithm<TrufflePattern> detector) {
        //mine(detector, t2, 6, -1, 0, 0);
        mine(detector, t2, 8, 1, 0, Integer.MAX_VALUE);
        mine(detector, t2, 8 + 20, 2, 0, Integer.MAX_VALUE);
        mine(detector, t2, 8 + 20 + 36, 3, 0, Integer.MAX_VALUE);
    }


    // when mining ONLY T3 we will get LESS patterns than permutations as Variable(x) and Const(..) occur multiple times
    @Test(dataProvider = "provider")
    public void testMineT3(AbstractPatternDetectorAlgorithm<TrufflePattern> detector) {
        mine(detector, t3, 11, 1, 0, Integer.MAX_VALUE);
        mine(detector, t3, 11 + 38, 2, 0, Integer.MAX_VALUE);
        mine(detector, t3, 11 + 38 + 190, 3, 0, Integer.MAX_VALUE);
        mine(detector, t3, 11 + 38 + 190 + 924, 4, 0, Integer.MAX_VALUE);
        mine(detector, t3, 11 + 38 + 190 + 924 + 3828, 5, 0, Integer.MAX_VALUE);
        mine(detector, t3, 11 + 38 + 190 + 924 + 3828 + 10512, 6, 0, Integer.MAX_VALUE);
        mine(detector, t3, 11 + 38 + 190 + 924 + 3828 + 10512 + 16848, 7, 0, Integer.MAX_VALUE);
        mine(detector, t3, 11 + 38 + 190 + 924 + 3828 + 10512 + 16848 + 11664, 8, 0, Integer.MAX_VALUE);
        mine(detector, t3, 44015, 9, 0, Integer.MAX_VALUE);
    }
    
    @Test(dataProvider = "provider")
    public void testMineT3H1(AbstractPatternDetectorAlgorithm<TrufflePattern> detector) {
        mine(detector, t3, 7, 1, 1, Integer.MAX_VALUE);
        mine(detector, t3, 7 + 23, 2, 1, Integer.MAX_VALUE);
        mine(detector, t3, 7 + 23 + 110, 3, 1, Integer.MAX_VALUE);
        mine(detector, t3, 7 + 23 + 110 + 468, 4, 1, Integer.MAX_VALUE);
        mine(detector, t3, 7 + 23 + 110 + 468 + 1440, 5, 1, Integer.MAX_VALUE);
        mine(detector, t3, 7 + 23 + 110 + 468 + 1440 + 2784, 6, 1, Integer.MAX_VALUE);
        mine(detector, t3, 7 + 23 + 110 + 468 + 1440 + 2784 + 3072, 7, 1, Integer.MAX_VALUE);
        mine(detector, t3, 7 + 23 + 110 + 468 + 1440 + 2784 + 3072 + 1536, 8, 1, Integer.MAX_VALUE);
    }

    // T1 is a subset of T2
    @Test(dataProvider = "provider")
    public void testMineT12(AbstractPatternDetectorAlgorithm<TrufflePattern> detector) {
        List<NodeWrapper> nodes = Arrays.asList(t1, t2);
        mine(detector, nodes, 8 + 20 + 36, -1, 0, Integer.MAX_VALUE);
    }

    // T1 is a subset of T2  is a subset of T3. Thus the patterns mined from T3 should be 100% EQUIVALENT to the ones JUST from T3
    @Test(dataProvider = "provider")
    public void testMineT123(AbstractPatternDetectorAlgorithm<TrufflePattern> detector) {
        List<NodeWrapper> nodes = Arrays.asList(t1, t2, t3);
        mine(detector, nodes, 11 + 38 + 190 + 924 + 3828 + 10512 + 16848 + 11664, -1, 0, Integer.MAX_VALUE);
        mine(detector, nodes, 7 + 23 + 110 + 468 + 1440 + 2784 + 3072 + 1536, -1, 1, Integer.MAX_VALUE);
    }

    // T3 and T4 are DIFFERENT in exactly 2 spots the "!=" instead of the "="
    @Test(dataProvider = "provider")
    public void testMineT34H0(AbstractPatternDetectorAlgorithm<TrufflePattern> detector) {
        List<NodeWrapper> nodes = Arrays.asList(t3, t4);
        mine(detector, nodes, 12, 1, 0, Integer.MAX_VALUE);
        mine(detector, nodes, 12 + 47, 2, 0, Integer.MAX_VALUE);
        mine(detector, nodes, 12 + 47 + 247, 3, 0, Integer.MAX_VALUE);
        mine(detector, nodes, 12 + 47 + 247 + 1272, 4, 0, Integer.MAX_VALUE);
        mine(detector, nodes, 12 + 47 + 247 + 1272 + 5442, 5, 0, Integer.MAX_VALUE);
        mine(detector, nodes, 12 + 47 + 247 + 1272 + 5442 + 15111, 6, 0, Integer.MAX_VALUE);
        mine(detector, nodes, 12 + 47 + 247 + 1272 + 5442 + 15111 + 24219, 7, 0, Integer.MAX_VALUE);
        mine(detector, nodes, 12 + 47 + 247 + 1272 + 5442 + 15111 + 24219 + 16767, 8, 0, Integer.MAX_VALUE);
    }

    @Test(dataProvider = "provider")
    public void testMineT34H1(AbstractPatternDetectorAlgorithm<TrufflePattern> detector) {
        List<NodeWrapper> nodes = Arrays.asList(t3, t4);
        mine(detector, nodes, 8, 1, 1, Integer.MAX_VALUE);
        mine(detector, nodes, 8 + 29, 2, 1, Integer.MAX_VALUE);
        mine(detector, nodes, 8 + 29 + 147, 3, 1, Integer.MAX_VALUE);
        mine(detector, nodes, 8 + 29 + 147 + 657, 4, 1, Integer.MAX_VALUE);
        mine(detector, nodes, 8 + 29 + 147 + 657 + 2061, 5, 1, Integer.MAX_VALUE);
        mine(detector, nodes, 8 + 29 + 147 + 657 + 2061 + 4002, 6, 1, Integer.MAX_VALUE);
        mine(detector, nodes, 8 + 29 + 147 + 657 + 2061 + 4002 + 4416, 7, 1, Integer.MAX_VALUE);
        mine(detector, nodes, 8 + 29 + 147 + 657 + 2061 + 4002 + 4416 + 2208, 8, 1, Integer.MAX_VALUE);
    }

    private Solution<TrufflePattern, TrufflePatternProblem> mine(AbstractPatternDetectorAlgorithm<TrufflePattern> detector, NodeWrapper node, int amountOfPatterns, int patternSize, int floor, int ceil) {
        ArrayList<NodeWrapper> nodes = new ArrayList<>();
        nodes.add(node);
        return mine(detector, nodes, amountOfPatterns, patternSize, floor, ceil);
    }

    private Solution<TrufflePattern, TrufflePatternProblem> mine(AbstractPatternDetectorAlgorithm<TrufflePattern> detector, List<NodeWrapper> nodes, int amountOfPatterns, int patternSize, int floor, int ceil) {
        // given
        detector.setHierarchyFloor(floor);
        detector.setHierarchyCeil(ceil);
        detector.setMaxPatternSize(patternSize);

        TrufflePatternSearchSpaceDefinition definition = new TrufflePatternSearchSpaceDefinition();
        nodes.forEach(definition::includeTree);
        TrufflePatternSearchSpace trees = repository.findTrees(definition);

        TrufflePatternProblem tpp = new TrufflePatternProblem(null, trees, "TEST", getMeta());
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

    private BitwisePatternMeta getMeta() {
        List<String> root = new ArrayList<>();
        root.add("Const");
        root.add("Variable");
        root.add("ctrl");
        root.add("compare");

        List<String> ctrl = new ArrayList<>();
        ctrl.add("For");
        ctrl.add("If");

        List<String> compare = new ArrayList<>();
        ctrl.add("int_cmp");
        ctrl.add("dbl_cmp");

        List<String> intCmp = new ArrayList<>();
        intCmp.add("=");
        intCmp.add("!=");

        Map<String, List<String>> classes = new HashMap<>();
        classes.put("root", root);
        classes.put("ctrl", ctrl);
        classes.put("compare", compare);
        classes.put("int_cmp", intCmp);

        return new BitwisePatternMeta(classes, "root");
    }
}