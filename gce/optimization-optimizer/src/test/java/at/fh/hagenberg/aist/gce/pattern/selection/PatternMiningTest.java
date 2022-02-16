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
import at.fh.hagenberg.aist.gce.pattern.TrufflePatternDetector;
import at.fh.hagenberg.aist.gce.pattern.TrufflePatternProblem;
import at.fh.hagenberg.aist.gce.pattern.algorithm.AbstractPatternDetectorAlgorithm;
import at.fh.hagenberg.aist.gce.pattern.algorithm.AprioriPatternDetectorAlgorithm;
import at.fh.hagenberg.aist.gce.pattern.algorithm.PatternGrowthPatternDetector;
import at.fh.hagenberg.aist.gce.pattern.algorithm.editor.NodeEditor;
import at.fh.hagenberg.aist.gce.pattern.algorithm.editor.ValueAbstractingNodeEditor;
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
 * 4a) This test ensures the validity of the pattern mining.
 * The values below were hand-checked and should NEVER change,
 * unless you use a heuristic mining approach in which case they should neve be exceeded.
 *
 * @author Oliver Krauss on 02.10.2019
 */
@Test
public class PatternMiningTest extends TestDBTest {

    @DataProvider(name = "provider")
    public static Object[][] algProvider() {
        AprioriPatternDetectorAlgorithm apriori = new AprioriPatternDetectorAlgorithm();
        PatternGrowthPatternDetector detector = new PatternGrowthPatternDetector();
        Map<String, String> labelMap = new HashMap<>();
        labelMap.put("Variable", "value");
        ManualLabeller manualLabeller = new ManualLabeller(labelMap);
        detector.setVariableLabeller(manualLabeller);
        return new Object[][]{{apriori}, {detector}};
    }

    // Note: The following tests are direct copies from the String subgraph iterator, they exist only to ensure that the integration in the algorithm works
    @Test(dataProvider = "provider")
    public void testMineT1(AbstractPatternDetectorAlgorithm<TrufflePattern> detector) {
        mine(detector, t1, 1, -1, true);
    }

    // Similarity pruning test. This should return 1, 2 and 3 patterns respectively
    @Test(dataProvider = "provider")
    public void testMineT5(AbstractPatternDetectorAlgorithm<TrufflePattern> detector) {
        List<NodeWrapper> nodes = Collections.singletonList(t5);
        mine(detector, nodes, 1, 1, false);
        mine(detector, nodes, 2, 2, false);
        mine(detector, nodes, 3, 3, false);
    }

    @Test(dataProvider = "provider")
    public void testMineT2(AbstractPatternDetectorAlgorithm<TrufflePattern> detector) {
        Solution<TrufflePattern, TrufflePatternProblem> mine = mine(detector, t2, 6, -1, true);

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
        mine(detector, t3, 6, 1, true);
        mine(detector, t3, 6 + 5, 2, true);
        mine(detector, t3, 6 + 5 + 7, 3, true);
        mine(detector, t3, 6 + 5 + 7 + 10, 4, true);
        mine(detector, t3, 6 + 5 + 7 + 10 + 12, 5, true);
        mine(detector, t3, 6 + 5 + 7 + 10 + 12 + 10, 6, true);
        mine(detector, t3, 6 + 5 + 7 + 10 + 12 + 10 + 5, 7, true);
        mine(detector, t3, 6 + 5 + 7 + 10 + 12 + 10 + 5 + 1, 8, true);
        mine(detector, t3, 6 + 5 + 7 + 10 + 12 + 10 + 5 + 1, 9, true);
    }


    @Test(dataProvider = "provider")
    public void testMineT3H1(AbstractPatternDetectorAlgorithm<TrufflePattern> detector) {
        mine(detector, t3, 4, 1, false);
        mine(detector, t3, 4 + 4, 2, false);
        mine(detector, t3, 4 + 4 + 5, 3, false);
        mine(detector, t3, 4 + 4 + 5 + 8, 4, false);
        mine(detector, t3, 4 + 4 + 5 + 8 + 11, 5, false);
        mine(detector, t3, 4 + 4 + 5 + 8 + 11 + 10, 6, false);
        mine(detector, t3, 4 + 4 + 5 + 8 + 11 + 10 + 5, 7, false);
        mine(detector, t3, 4 + 4 + 5 + 8 + 11 + 10 + 5 + 1, 8, false);
        mine(detector, t3, 4 + 4 + 5 + 8 + 11 + 10 + 5 + 1, 9, false);
    }

    // when mining ONLY T3 we will get LESS patterns than permutations as Variable(x) and Const(..) occur multiple times
    public void testMineT3SpecialValue() {
        PatternGrowthPatternDetector detector = new PatternGrowthPatternDetector();
        ValueAbstractingNodeEditor editor = new ValueAbstractingNodeEditor(true);
        editor.addSpecialValue("123");
        detector.setEditor(editor);
        mine(detector, Collections.singletonList(t3), 5, 1, true, editor);
        mine(detector, Collections.singletonList(t3), 5 + 5, 2, true, editor);
        mine(detector, Collections.singletonList(t3), 5 + 5 + 7, 3, true, editor);
        mine(detector, Collections.singletonList(t3), 5 + 5 + 7 + 10, 4, true, editor);
        mine(detector, Collections.singletonList(t3), 5 + 5 + 7 + 10 + 12, 5, true, editor);
        mine(detector, Collections.singletonList(t3), 5 + 5 + 7 + 10 + 12 + 10, 6, true, editor);
        mine(detector, Collections.singletonList(t3), 5 + 5 + 7 + 10 + 12 + 10 + 5, 7, true, editor);
        mine(detector, Collections.singletonList(t3), 5 + 5 + 7 + 10 + 12 + 10 + 5 + 1, 8, true, editor);
        mine(detector, Collections.singletonList(t3), 5 + 5 + 7 + 10 + 12 + 10 + 5 + 1, 9, true, editor);
    }

    // when mining ONLY T3 we will get LESS patterns than permutations as Variable(x) and Const(..) occur multiple times
    @Test(dataProvider = "provider")
    public void testMineT6(AbstractPatternDetectorAlgorithm<TrufflePattern> detector) {
        if (detector instanceof AprioriPatternDetectorAlgorithm) {
            return;
        }
        mine(detector, t6, 3, 1, true);
        mine(detector, t6, 3 + 3, 2, true);
        mine(detector, t6, 3 + 3 + 4, 3, true);
        mine(detector, t6, 3 + 3 + 4 + 6, 4, true);
        mine(detector, t6, 3 + 3 + 4 + 6 + 9, 5, true);
        mine(detector, t6, 3 + 3 + 4 + 6 + 9 + 9, 6, true);
        mine(detector, t6, 3 + 3 + 4 + 6 + 9 + 9 + 5, 7, true);
        mine(detector, t6, 3 + 3 + 4 + 6 + 9 + 9 + 5 + 1, 8, true);
        mine(detector, t6, 3 + 3 + 4 + 6 + 9 + 9 + 5 + 1, 9, true);
    }

    // when mining ONLY T3 we will get LESS patterns than permutations as Variable(x) and Const(..) occur multiple times
    @Test(dataProvider = "provider")
    public void testMineT7(AbstractPatternDetectorAlgorithm<TrufflePattern> detector) {
        mine(detector, t7, 3, 1, true);
        mine(detector, t7, 3 + 3, 2, true);
        mine(detector, t7, 3 + 3 + 4, 3, true);
        mine(detector, t7, 3 + 3 + 4 + 5, 4, true);
        mine(detector, t7, 3 + 3 + 4 + 5 + 6, 5, true);
        mine(detector, t7, 3 + 3 + 4 + 5 + 6 + 5, 6, true);
        mine(detector, t7, 3 + 3 + 4 + 5 + 6 + 5 + 3, 7, true);
        mine(detector, t7, 3 + 3 + 4 + 5 + 6 + 5 + 3 + 1, 8, true);
        mine(detector, t7, 3 + 3 + 4 + 5 + 6 + 5 + 3 + 1, 9, true);
    }

    // checking out the variable labelling. With 3 and 3a the patterns should be equal as to just mining t3
    @Test(dataProvider = "provider")
    public void testMineTVarLabelSame(AbstractPatternDetectorAlgorithm<TrufflePattern> detector) {
        if (detector instanceof AprioriPatternDetectorAlgorithm) {
            return;
        }
        List<NodeWrapper> nodes = Arrays.asList(t3, t3a);
        mine(detector, nodes, 6, 1, true);
        mine(detector, nodes, 6 + 5, 2, true);
        mine(detector, nodes, 6 + 5 + 7, 3, true);
        mine(detector, nodes, 6 + 5 + 7 + 10, 4, true);
        mine(detector, nodes, 6 + 5 + 7 + 10 + 12, 5, true);
        mine(detector, nodes, 6 + 5 + 7 + 10 + 12 + 10, 6, true);
        mine(detector, nodes, 6 + 5 + 7 + 10 + 12 + 10 + 5, 7, true);
        mine(detector, nodes, 6 + 5 + 7 + 10 + 12 + 10 + 5 + 1, 8, true);
        mine(detector, nodes, 6 + 5 + 7 + 10 + 12 + 10 + 5 + 1, 9, true);
    }

    // checking out the variable labelling. With 3 and 3b the patterns should be MORE than from mining just t3
    @Test(dataProvider = "provider")
    public void testMineTVarLabelDifferent(AbstractPatternDetectorAlgorithm<TrufflePattern> detector) {
        if (detector instanceof AprioriPatternDetectorAlgorithm) {
            return;
        }
        List<NodeWrapper> nodes = Arrays.asList(t3, t3b);
        mine(detector, nodes, 6, 1, true);
        mine(detector, nodes, 6 + 5, 2, true);
        mine(detector, nodes, 6 + 5 + 7, 3, true);
        mine(detector, nodes, 6 + 5 + 7 + 10, 4, true);
        mine(detector, nodes, 6 + 5 + 7 + 10 + 13, 5, true);
        mine(detector, nodes, 6 + 5 + 7 + 10 + 13 + 13, 6, true);
        mine(detector, nodes, 6 + 5 + 7 + 10 + 13 + 13 + 8, 7, true);
        mine(detector, nodes, 6 + 5 + 7 + 10 + 13 + 13 + 8 + 2, 8, true);
        mine(detector, nodes, 6 + 5 + 7 + 10 + 13 + 13 + 8 + 2, 9, true);
    }

    // T1 is a subset of T2
    @Test(dataProvider = "provider")
    public void testMineT12(AbstractPatternDetectorAlgorithm<TrufflePattern> detector) {
        List<NodeWrapper> nodes = Arrays.asList(t1, t2);
        mine(detector, nodes, 6, -1, true);
    }

    // T1 is a subset of T2  is a subset of T3. Thus the patterns mined from T3 should be 100% EQUIVALENT to the ones JUST from T3
    @Test(dataProvider = "provider")
    public void testMineT123(AbstractPatternDetectorAlgorithm<TrufflePattern> detector) {
        List<NodeWrapper> nodes = Arrays.asList(t1, t2, t3);
        mine(detector, nodes, 6 + 5 + 7 + 10 + 12 + 10 + 5 + 1, -1, true);
        mine(detector, nodes, 4 + 4 + 5 + 8 + 11 + 10 + 5 + 1, -1, false);
    }

    // T3 and T4 are DIFFERENT in exactly 2 spots the "!=" instead of the "="
    @Test(dataProvider = "provider")
    public void testMineT34H0(AbstractPatternDetectorAlgorithm<TrufflePattern> detector) {
        List<NodeWrapper> nodes = Arrays.asList(t3, t4);
        mine(detector, nodes, 7, 1, true);
        mine(detector, nodes, 7 + 9, 2, true);
        mine(detector, nodes, 7 + 9 + 14, 3, true);
        mine(detector, nodes, 7 + 9 + 14 + 20, 4, true);
        mine(detector, nodes, 7 + 9 + 14 + 20 + 24, 5, true);
        mine(detector, nodes, 7 + 9 + 14 + 20 + 24 + 20, 6, true);
        mine(detector, nodes, 7 + 9 + 14 + 20 + 24 + 20 + 10, 7, true);
        mine(detector, nodes, 7 + 9 + 14 + 20 + 24 + 20 + 10 + 2, 8, true);
        mine(detector, nodes, 7 + 9 + 14 + 20 + 24 + 20 + 10 + 2, 9, true);
    }

    @Test(dataProvider = "provider")
    public void testMineT34H1(AbstractPatternDetectorAlgorithm<TrufflePattern> detector) {
        List<NodeWrapper> nodes = Arrays.asList(t3, t4);
        mine(detector, nodes, 5, 1, false);
        mine(detector, nodes, 5 + 7, 2, false);
        mine(detector, nodes, 5 + 7 + 10, 3, false);
        mine(detector, nodes, 5 + 7 + 10 + 16, 4, false);
        mine(detector, nodes, 5 + 7 + 10 + 16 + 22, 5, false);
        mine(detector, nodes, 5 + 7 + 10 + 16 + 22 + 20, 6, false);
        mine(detector, nodes, 5 + 7 + 10 + 16 + 22 + 20 + 10, 7, false);
        mine(detector, nodes, 5 + 7 + 10 + 16 + 22 + 20 + 10 + 2, 8, false);
        mine(detector, nodes, 5 + 7 + 10 + 16 + 22 + 20 + 10 + 2, 9, false);
    }



    private Solution<TrufflePattern, TrufflePatternProblem> mine(AbstractPatternDetectorAlgorithm<TrufflePattern> detector, NodeWrapper node, int amountOfPatterns, int patternSize, boolean explicit) {
        ArrayList<NodeWrapper> nodes = new ArrayList<>();
        nodes.add(node);
        return mine(detector, nodes, amountOfPatterns, patternSize, explicit);
    }

    private Solution<TrufflePattern, TrufflePatternProblem> mine(AbstractPatternDetectorAlgorithm<TrufflePattern> detector, List<NodeWrapper> nodes, int amountOfPatterns, int patternSize, boolean explicit) {
        return mine(detector, nodes, amountOfPatterns, patternSize, explicit, null);
    }

        private Solution<TrufflePattern, TrufflePatternProblem> mine(AbstractPatternDetectorAlgorithm<TrufflePattern> detector, List<NodeWrapper> nodes, int amountOfPatterns, int patternSize, boolean explicit, NodeEditor<NodeWrapper> editor) {
        // given
        if (editor == null) {
            detector.setHierarchyFloor(explicit ? 0 : 1);
            detector.setHierarchyCeil(explicit ? 0 : 1);
        } else {
            ((PatternGrowthPatternDetector)detector).setEditor(editor);
        }
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