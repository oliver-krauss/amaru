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

import at.fh.hagenberg.aist.gce.minic.language.MinicLanguage;
import at.fh.hagenberg.aist.gce.optimization.util.TruffleLanguageInformation;
import at.fh.hagenberg.aist.gce.pattern.algorithm.AprioriPatternDetectorAlgorithm;
import at.fh.hagenberg.aist.gce.pattern.algorithm.AprioriClusterPatternDetectorAlgorithm;
import at.fh.hagenberg.aist.gce.pattern.encoding.BitwisePatternMeta;
import at.fh.hagenberg.aist.gce.pattern.encoding.TestRealNodesDbTest;
import at.fh.hagenberg.aist.gce.pattern.selection.TrufflePatternSearchSpace;
import at.fh.hagenberg.aist.gce.pattern.selection.TrufflePatternSearchSpaceDefinition;
import at.fh.hagenberg.machinelearning.analytics.graph.nodes.NodeWrapper;
import at.fh.hagenberg.machinelearning.core.Problem;
import at.fh.hagenberg.machinelearning.core.ProblemGene;
import at.fh.hagenberg.machinelearning.core.Solution;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Oliver Krauss on 10.07.2020
 */

public class TrufflePatternMapperTest extends TestRealNodesDbTest {

    AprioriClusterPatternDetectorAlgorithm detector = new AprioriClusterPatternDetectorAlgorithm();

    AprioriPatternDetectorAlgorithm mineDetector = new AprioriPatternDetectorAlgorithm();

    TruffleLanguageInformation information = TruffleLanguageInformation.getLanguageInformation(MinicLanguage.ID);
    BitwisePatternMeta meta = new BitwisePatternMeta(information);
    private TrufflePatternMapper mapper = new TrufflePatternMapper(meta);

    @Test
    public void testMineT1() {
        // given
        Solution<TrufflePattern, TrufflePatternProblem> mine = mine(t1, -1, 1);

        // when
        Solution<TruffleRelatablePattern, TrufflePatternProblem> relatable = mapper.mapSolution(mine);

        // then
        TruffleRelatablePattern gene = relatable.getSolutionGenes().get(0).getGene();
        Assert.assertEquals(gene.getContainedIn().size(), 0);
        Assert.assertEquals(gene.getContains().size(), 0);
        Assert.assertEquals(gene.getGeneralizes().size(), 0);
        Assert.assertEquals(gene.getSpecializes().size(), 0);
    }

    @Test
    public void testMineT1H5() {
        // given
        Solution<TrufflePattern, TrufflePatternProblem> mine = mine(t1, -1, Integer.MAX_VALUE);

        // when
        Solution<TruffleRelatablePattern, TrufflePatternProblem> relatable = mapper.mapSolution(mine);

        // then
        // TODO #252 The relationship matching doesn't work
//        relatable.getSolutionGenes().stream().map(x -> x.getGene()).forEach(gene -> {
//            Assert.assertEquals(gene.getContainedIn().size(), 0);
//            Assert.assertEquals(gene.getContains().size(), 0);
//            Assert.assertEquals(gene.getGeneralizes().size() + gene.getSpecializes().size(), 3);
//        });
    }


    @Test
    public void testMineT2() {
        // given
        Solution<TrufflePattern, TrufflePatternProblem> mine = mine(t2, -1, 1);

        // when
        Solution<TruffleRelatablePattern, TrufflePatternProblem> relatable = mapper.mapSolution(mine);

        // then
        AtomicInteger totalcount = new AtomicInteger();
        relatable.getSolutionGenes().stream().map(x -> x.getGene()).forEach(gene -> {
            int count = gene.getContainedIn().size() + gene.getContains().size();
            totalcount.addAndGet(count);
            Assert.assertTrue((count) > 0);
            Assert.assertEquals(gene.getGeneralizes().size() + gene.getSpecializes().size(), 0);
        });
        Assert.assertEquals(totalcount.get(), 4);
    }


    // when mining ONLY T3 we will get LESS patterns than permutations as Variable(x) and Const(..) occur multiple times
    @Test
    public void testMineT3() {
        // given
        Solution<TrufflePattern, TrufflePatternProblem> mine = mine(t3, -1, 1);

        // when
        Solution<TruffleRelatablePattern, TrufflePatternProblem> relatable = mapper.mapSolution(mine);

        // then
        AtomicInteger totalcount = new AtomicInteger();
        relatable.getSolutionGenes().stream().map(x -> x.getGene()).forEach(gene -> {
            int count = gene.getContainedIn().size() + gene.getContains().size();
            totalcount.addAndGet(count);
            Assert.assertTrue((count) > 0);
            Assert.assertEquals(gene.getGeneralizes().size() + gene.getSpecializes().size(), 0);
        });
        Assert.assertEquals(totalcount.get(), 194);
    }

    @Test
    public void testMapT12() {
        // given
        Solution<TruffleDifferentialPatternSolution, TrufflePatternProblem> solution = diff(Arrays.asList(t1, t2), 6, false);

        // when
        solution = mapper.mapDifferential(solution);

        // then
        TruffleDifferentialPatternSolution gene = solution.getSolutionGenes().get(0).getGene();
        gene.getPatternsPerProblem().values().stream().forEach(x -> x.stream().forEach(y -> {
            Assert.assertTrue(y instanceof TruffleRelatablePattern);
            TruffleRelatablePattern yR = (TruffleRelatablePattern) y;
            Assert.assertEquals(yR.getGeneralizes().size() + yR.getSpecializes().size(), 0);
        }));
        gene.getDifferential().keySet().stream().forEach(y -> {
            Assert.assertTrue(y instanceof TruffleRelatablePattern);
            TruffleRelatablePattern yR = (TruffleRelatablePattern) y;
        });
    }

    @Test
    public void testMapT12Diff() {
        // given
        Solution<TruffleDifferentialPatternSolution, TrufflePatternProblem> solution = diff(Arrays.asList(t1, t2), 6, false);

        // when
        TruffleDifferentialPatternSolution gene = solution.getSolutionGenes().get(0).getGene();
        gene = mapper.mapDifferential(gene);

        // then
        gene.getPatternsPerProblem().values().stream().forEach(x -> x.stream().forEach(y -> {
            Assert.assertTrue(y instanceof TruffleRelatablePattern);
            TruffleRelatablePattern yR = (TruffleRelatablePattern) y;
            Assert.assertEquals(yR.getGeneralizes().size() + yR.getSpecializes().size(), 0);
        }));
        gene.getDifferential().keySet().stream().forEach(y -> {
            Assert.assertTrue(y instanceof TruffleRelatablePattern);
            TruffleRelatablePattern yR = (TruffleRelatablePattern) y;
        });
    }


    private TrufflePatternProblem createProblem(NodeWrapper node, int patternSize, boolean explicit, String name) {
        TrufflePatternSearchSpaceDefinition definition = new TrufflePatternSearchSpaceDefinition();
        definition.includeTree(node);
        TrufflePatternSearchSpace trees = repository.findTrees(definition);
        return new TrufflePatternProblem(null, trees, name);
    }

    private Solution<TruffleDifferentialPatternSolution, TrufflePatternProblem> diff(List<NodeWrapper> nodes, int patternSize, boolean explicit) {
        this.detector.setMaxPatternSize(patternSize);
        this.detector.setHierarchyFloor(explicit ? 0 : 1);

        // given
        List<ProblemGene<TrufflePatternProblem>> list = new ArrayList<>();
        nodes.forEach(x -> list.add(new ProblemGene<>(createProblem(x, patternSize, explicit, String.valueOf(x.getId())))));
        Problem<TrufflePatternProblem> problem = new Problem<>(list);

        // when
        Solution<TruffleDifferentialPatternSolution, TrufflePatternProblem> solution = detector.solve(problem);

        return solution;
    }

    private Solution<TrufflePattern, TrufflePatternProblem> mine(NodeWrapper node, int patternSize, int ceil) {
        ArrayList<NodeWrapper> nodes = new ArrayList<>();
        nodes.add(node);
        return mine(nodes, patternSize, ceil);
    }

    private Solution<TrufflePattern, TrufflePatternProblem> mine(List<NodeWrapper> nodes, int patternSize, int ceil) {
        // given
        mineDetector.setHierarchyFloor(1);
        mineDetector.setHierarchyCeil(ceil);
        mineDetector.setMaxPatternSize(patternSize);

        TrufflePatternSearchSpaceDefinition definition = new TrufflePatternSearchSpaceDefinition();
        nodes.forEach(definition::includeTree);
        TrufflePatternSearchSpace trees = repository.findTrees(definition);

        TrufflePatternProblem tpp = new TrufflePatternProblem(MinicLanguage.ID, trees, "TEST");
        List<ProblemGene<TrufflePatternProblem>> list = new ArrayList<>();
        list.add(new ProblemGene<>(tpp));
        Problem<TrufflePatternProblem> problem = new Problem<>(list);

        Solution<TrufflePattern, TrufflePatternProblem> solution = mineDetector.solve(problem);

        return solution;
    }
}
