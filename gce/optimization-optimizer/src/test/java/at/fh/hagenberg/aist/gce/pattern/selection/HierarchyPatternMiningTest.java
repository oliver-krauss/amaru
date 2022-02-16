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
import at.fh.hagenberg.aist.gce.pattern.encoding.BitwisePatternMeta;
import at.fh.hagenberg.machinelearning.analytics.graph.nodes.NodeWrapper;
import at.fh.hagenberg.machinelearning.core.Problem;
import at.fh.hagenberg.machinelearning.core.ProblemGene;
import at.fh.hagenberg.machinelearning.core.Solution;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 4a) This test ensures that the hierarchies are correct
 * The values below were hand-checked and should NEVER change,
 * unless you use a heuristic mining approach in which case they should neve be exceeded.
 *
 * NOTE: This test set is SPLIT from the Apriori miner, as the PatternGrowthDetector skips irrelevant generalizations.
 *
 * @author Oliver Krauss on 02.10.2019
 */
@Test
public class HierarchyPatternMiningTest extends TestDBTest {

    @DataProvider(name = "provider")
    public static Object[][] algProvider() {
        PatternGrowthPatternDetector detector = new PatternGrowthPatternDetector();
        return new Object[][]{{detector}};
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
        //mine(detector, t2, 3, 1, 0, 1);
        mine(detector, t2, 7, 1, 0, Integer.MAX_VALUE);
        mine(detector, t2, 7 + 15, 2, 0, Integer.MAX_VALUE);
        mine(detector, t2, 7 + 15 + 27, 3, 0, Integer.MAX_VALUE);
    }


    // when mining ONLY T3 we will get LESS patterns than permutations as Variable(x) and Const(..) occur multiple times
    @Test(dataProvider = "provider")
    public void testMineT3(AbstractPatternDetectorAlgorithm<TrufflePattern> detector) {
        mine(detector, t3, 10, 1, 0, Integer.MAX_VALUE);
        mine(detector, t3, 10 + 29, 2, 0, Integer.MAX_VALUE);
        mine(detector, t3, 10 + 29 + 133, 3, 0, Integer.MAX_VALUE);
        mine(detector, t3, 10 + 29 + 133 + 576, 4, 0, Integer.MAX_VALUE);
        mine(detector, t3, 10 + 29 + 133 + 576 + 2214, 5, 0, Integer.MAX_VALUE);
        mine(detector, t3, 10 + 29 + 133 + 576 + 2214 + 5913, 6, 0, Integer.MAX_VALUE);
        mine(detector, t3, 10 + 29 + 133 + 576 + 2214 + 5913 + 9477, 7, 0, Integer.MAX_VALUE);
        mine(detector, t3, 10 + 29 + 133 + 576 + 2214 + 5913 + 9477 + 6561, 8, 0, Integer.MAX_VALUE);
        mine(detector, t3, 24913, 9, 0, Integer.MAX_VALUE);
    }

    @Test(dataProvider = "provider")
    public void testMineT3H1(AbstractPatternDetectorAlgorithm<TrufflePattern> detector) {
        mine(detector, t3, 6, 1, 1, Integer.MAX_VALUE);
        mine(detector, t3, 6 + 17, 2, 1, Integer.MAX_VALUE);
        mine(detector, t3, 6 + 17 + 73, 3, 1, Integer.MAX_VALUE);
        mine(detector, t3, 6 + 17 + 73 + 279, 4, 1, Integer.MAX_VALUE);
        mine(detector, t3, 6 + 17 + 73 + 279 + 819, 5, 1, Integer.MAX_VALUE);
        mine(detector, t3, 6 + 17 + 73 + 279 + 819 + 1566, 6, 1, Integer.MAX_VALUE);
        mine(detector, t3, 6 + 17 + 73 + 279 + 819 + 1566 + 1728, 7, 1, Integer.MAX_VALUE);
        mine(detector, t3, 6 + 17 + 73 + 279 + 819 + 1566 + 1728 + 864, 8, 1, Integer.MAX_VALUE);
    }

    // T1 is a subset of T2
    @Test(dataProvider = "provider")
    public void testMineT12(AbstractPatternDetectorAlgorithm<TrufflePattern> detector) {
        List<NodeWrapper> nodes = Arrays.asList(t1, t2);
        mine(detector, nodes, 7 + 15 + 27, -1, 0, Integer.MAX_VALUE);
    }

    // T1 is a subset of T2  is a subset of T3. Thus the patterns mined from T3 should be 100% EQUIVALENT to the ones JUST from T3
    @Test(dataProvider = "provider")
    public void testMineT123(AbstractPatternDetectorAlgorithm<TrufflePattern> detector) {
        List<NodeWrapper> nodes = Arrays.asList(t1, t2, t3);
        // TODO #252 MIN MAX is broken right now
        mine(detector, nodes, 10 + 29 + 133 + 576 + 2214 + 5913 + 9477 + 6561, -1, 0, Integer.MAX_VALUE);
        mine(detector, nodes, 6 + 17 + 73 + 279 + 819 + 1566 + 1728 + 864, -1, 1, Integer.MAX_VALUE);
    }

    // T3 and T4 are DIFFERENT in exactly 2 spots the "!=" instead of the "="
    @Test(dataProvider = "provider")
    public void testMineT34H0(AbstractPatternDetectorAlgorithm<TrufflePattern> detector) {
        List<NodeWrapper> nodes = Arrays.asList(t3, t4);
        mine(detector, nodes, 12, 1, 0, Integer.MAX_VALUE);
        mine(detector, nodes, 12 + 47, 2, 0, Integer.MAX_VALUE);
        mine(detector, nodes, 12 + 47 + 241, 3, 0, Integer.MAX_VALUE);
        mine(detector, nodes, 12 + 47 + 241 + 1194, 4, 0, Integer.MAX_VALUE);
        mine(detector, nodes, 12 + 47 + 241 + 1194 + 5004, 5, 0, Integer.MAX_VALUE);
        mine(detector, nodes, 12 + 47 + 241 + 1194 + 5004 + 13797, 6, 0, Integer.MAX_VALUE);
        mine(detector, nodes, 12 + 47 + 241 + 1194 + 5004 + 13797 + 22113, 7, 0, Integer.MAX_VALUE);
        mine(detector, nodes, 12 + 47 + 241 + 1194 + 5004 + 13797 + 22113 + 15309, 8, 0, Integer.MAX_VALUE);
    }

    @Test(dataProvider = "provider")
    public void testMineT34H1(AbstractPatternDetectorAlgorithm<TrufflePattern> detector) {
        List<NodeWrapper> nodes = Arrays.asList(t3, t4);
        mine(detector, nodes, 8, 1, 1, Integer.MAX_VALUE);
        mine(detector, nodes, 8 + 29, 2, 1, Integer.MAX_VALUE);
        mine(detector, nodes, 8 + 29 + 141, 3, 1, Integer.MAX_VALUE);
        mine(detector, nodes, 8 + 29 + 141 + 609, 4, 1, Integer.MAX_VALUE);
        mine(detector, nodes, 8 + 29 + 141 + 609 + 1887, 5, 1, Integer.MAX_VALUE);
        mine(detector, nodes, 8 + 29 + 141 + 609 + 1887 + 3654, 6, 1, Integer.MAX_VALUE);
        mine(detector, nodes, 8 + 29 + 141 + 609 + 1887 + 3654 + 4032, 7, 1, Integer.MAX_VALUE);
        mine(detector, nodes, 8 + 29 + 141 + 609 + 1887 + 3654 + 4032 + 2016, 8, 1, Integer.MAX_VALUE);
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