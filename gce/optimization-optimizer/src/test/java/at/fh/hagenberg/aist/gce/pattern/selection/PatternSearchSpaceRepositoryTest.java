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

import at.fh.hagenberg.aist.gce.context.ApplicationContextProvider;
import at.fh.hagenberg.aist.gce.pattern.PatternNodeWrapper;
import at.fh.hagenberg.aist.gce.pattern.TrufflePattern;
import at.fh.hagenberg.aist.gce.pattern.constraint.CachetConstraint;
import at.fh.hagenberg.machinelearning.analytics.graph.nodes.NodeWrapper;
import at.fh.hagenberg.machinelearning.analytics.graph.nodes.OrderedRelationship;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * 2) Test that the Repository actually returns what we want
 * @author Oliver Krauss on 02.10.2019
 */
@Test
public class PatternSearchSpaceRepositoryTest extends TestDBTest {

    @Test
    public void testBuildFullSearchSpace() {
        // given
        // nothing

        // when
        TrufflePatternSearchSpace trees = repository.findTrees();

        // then
        Assert.assertNotNull(trees);
        Assert.assertEquals(trees.searchSpace.size(), 9);
    }

    @Test
    public void testBuildNothing() {
        // given
        CachetConstraint c = new CachetConstraint("noCachetExistsWeWontFindAnything");
        List<CachetConstraint> constraintList = new ArrayList<>();
        constraintList.add(c);
        TrufflePatternSearchSpaceDefinition definition = new TrufflePatternSearchSpaceDefinition();
        definition.setCachets(constraintList);

        // when
        TrufflePatternSearchSpace trees = repository.findTrees(definition);

        // then
        Assert.assertNotNull(trees);
        Assert.assertEquals(trees.searchSpace.size(), 0);
    }

    @Test
    public void testBuildExcludes() {
        // given
        TrufflePatternSearchSpaceDefinition definition = new TrufflePatternSearchSpaceDefinition();
        definition.setSolutionSpace(true);
        definition.excludeTree(t1); // tree wont be in searchspace
        definition.excludeNode(t2, true); // tree wont be in searchspace as all nodes are removed
        definition.excludeNode(t3, false); // tree will be headless in searchspace

        // when
        TrufflePatternSearchSpace trees = repository.findTrees(definition);

        // then
        Assert.assertNotNull(trees);
        Assert.assertEquals(trees.searchSpace.size(), 7);
    }

    @Test
    public void testBuildExcludesType() {
        // given
        TrufflePatternSearchSpaceDefinition definition = new TrufflePatternSearchSpaceDefinition();
        definition.setSolutionSpace(true);
        definition.excludeType("Const"); // tree t1 wont be in searchspace


        // when
        TrufflePatternSearchSpace trees = repository.findTrees(definition);

        // then
        Assert.assertNotNull(trees);
        Assert.assertEquals(trees.searchSpace.size(), 8);
    }

    @Test
    public void testBuildIncludeTypes() {
        // given
        TrufflePatternSearchSpaceDefinition definition = new TrufflePatternSearchSpaceDefinition();
        definition.setSolutionSpace(true);
        definition.excludeNode(t1, true);
        definition.excludeTree(t2); // t2 will be removed as the const will NOT apply to competely removed trees
        definition.includeType("Const"); // t1 WILL be in searchspace as includeType overrides excludeNode

        // when
        TrufflePatternSearchSpace trees = repository.findTrees(definition);

        // then
        Assert.assertNotNull(trees);
        Assert.assertEquals(trees.searchSpace.size(), 8);
    }

    @Test
    public void testBuildIncludes() {
        // given
        CachetConstraint c = new CachetConstraint("noCachetExistsWeWontFindAnything");
        List<CachetConstraint> constraintList = new ArrayList<>();
        constraintList.add(c);
        TrufflePatternSearchSpaceDefinition definition = new TrufflePatternSearchSpaceDefinition();
        definition.includeTree(t1);
        definition.excludeNode(t1, true); // T1 won't be in the search space since we included the tree but excluded all nodes
        definition.includeNode(t2, true); // does NOT do anything as we didn't include the tree
        definition.includeTree(t3); // will be completely IN the search space

        // when
        TrufflePatternSearchSpace trees = repository.findTrees(definition);

        // then
        Assert.assertNotNull(trees);
        Assert.assertEquals(trees.searchSpace.size(), 1);
    }

    @Test
    public void testBuildPatternContainmentAnd() {
        // given
        TrufflePatternSearchSpaceDefinition definition = new TrufflePatternSearchSpaceDefinition();
        definition.setSolutionSpace(true);
        List<List<TrufflePattern>> patterns = new LinkedList<>();
        List<TrufflePattern> andList = new LinkedList<>();

        NodeWrapper t43 = new NodeWrapper("!=");
        NodeWrapper t431 = new NodeWrapper("Variable");
        t431.getValues().put("value", "x");
        andList.add(new TrufflePattern(0L, new PatternNodeWrapper(t43, 0L)));
        andList.add(new TrufflePattern(0L, new PatternNodeWrapper(t431, 0L)));
        patterns.add(andList);
        definition.setPatterns(patterns);

        // when
        TrufflePatternSearchSpace trees = repository.findTrees(definition);

        // then
        Assert.assertNotNull(trees);
        Assert.assertEquals(trees.searchSpace.size(), 1);
    }

    @Test
    public void testBuildPatternContainmentOr() {
        // given
        TrufflePatternSearchSpaceDefinition definition = new TrufflePatternSearchSpaceDefinition();
        definition.setSolutionSpace(true);
        List<List<TrufflePattern>> patterns = new LinkedList<>();
        List<TrufflePattern> orList = new LinkedList<>();
        List<TrufflePattern> orList2 = new LinkedList<>();

        NodeWrapper t43 = new NodeWrapper("!=");
        NodeWrapper t431 = new NodeWrapper("Variable");
        t431.getValues().put("value", "x");
        orList.add(new TrufflePattern(0L, new PatternNodeWrapper(t43, 0L)));
        orList2.add(new TrufflePattern(0L, new PatternNodeWrapper(t431, 0L)));
        patterns.add(orList);
        patterns.add(orList2);
        definition.setPatterns(patterns);

        // when
        TrufflePatternSearchSpace trees = repository.findTrees(definition);

        // then
        Assert.assertNotNull(trees);
        Assert.assertEquals(trees.searchSpace.size(), 6);
    }

    // Note: We repeat this test so often to ENSURE that there are no bugs that stem from DB-modification
    @Test(invocationCount = 20)
    public void testFixedOrder() {
        // given
        setUp();
        TrufflePatternSearchSpaceDefinition definition = new TrufflePatternSearchSpaceDefinition();
        definition.includeTree(t3);

        // when
        TrufflePatternSearchSpace trees = repository.findTrees(definition);

        // then
        Assert.assertNotNull(trees);
        Assert.assertEquals(trees.searchSpace.size(), 1);
        Assert.assertTrue(trees.searchSpace.get(0).getKey().length == 8);
        Assert.assertTrue(trees.searchSpace.get(0).getValue().length == 7);

        // verify node order
        NodeWrapper[] nodes = trees.searchSpace.get(0).getKey();
        Assert.assertEquals(nodes[0].getType(), "If");
        Assert.assertEquals(nodes[1].getType(), "Const");
        Assert.assertEquals(nodes[2].getType(), "=");
        Assert.assertEquals(nodes[3].getType(), "=");
        Assert.assertEquals(nodes[4].getType(), "Variable");
        Assert.assertEquals(nodes[5].getType(), "Const");
        Assert.assertEquals(nodes[5].getValues().get("value").toString(), "123");
        Assert.assertEquals(nodes[6].getType(), "Variable");
        Assert.assertEquals(nodes[7].getType(), "Const");
        Assert.assertEquals(nodes[7].getValues().get("value").toString(), "456");

        // verify relationship order
        OrderedRelationship[] r = trees.searchSpace.get(0).getValue();
        Assert.assertEquals(r[0].getParent().getId(), nodes[0].getId());
        Assert.assertEquals(r[0].getChild().getId(), nodes[1].getId());
        Assert.assertEquals(r[1].getParent().getId(), nodes[0].getId());
        Assert.assertEquals(r[1].getChild().getId(), nodes[2].getId());
        Assert.assertEquals(r[2].getParent().getId(), nodes[0].getId());
        Assert.assertEquals(r[2].getChild().getId(), nodes[3].getId());
        Assert.assertEquals(r[3].getParent().getId(), nodes[2].getId());
        Assert.assertEquals(r[3].getChild().getId(), nodes[4].getId());
        Assert.assertEquals(r[4].getParent().getId(), nodes[2].getId());
        Assert.assertEquals(r[4].getChild().getId(), nodes[5].getId());
        Assert.assertEquals(r[5].getParent().getId(), nodes[3].getId());
        Assert.assertEquals(r[5].getChild().getId(), nodes[6].getId());
        Assert.assertEquals(r[6].getParent().getId(), nodes[3].getId());
        Assert.assertEquals(r[6].getChild().getId(), nodes[7].getId());
    }


}
