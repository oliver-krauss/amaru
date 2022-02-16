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
import at.fh.hagenberg.aist.gce.minic.nodes.base.MinicExpressionNode;
import at.fh.hagenberg.aist.gce.minic.nodes.base.MinicNode;
import at.fh.hagenberg.aist.gce.minic.nodes.impl.literals.MinicSimpleLiteralNode;
import at.fh.hagenberg.aist.gce.minic.types.integer.signed.MinicIntNode;
import at.fh.hagenberg.aist.gce.optimization.util.TruffleLanguageInformation;
import at.fh.hagenberg.aist.gce.pattern.algorithm.HierarchySupportingSubgraphIterator;
import at.fh.hagenberg.aist.gce.pattern.selection.TrufflePatternSearchSpace;
import at.fh.hagenberg.aist.gce.pattern.selection.TrufflePatternSearchSpaceDefinition;
import at.fh.hagenberg.machinelearning.analytics.graph.nodes.NodeWrapper;
import com.oracle.truffle.api.nodes.Node;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.*;

/**
 * @author Oliver Krauss on 18.11.2019
 */
public class BitwisePatternMetaTest extends TestRealNodesDbTest {


    @Test
    public void testCreateBitwiseMeta() {
        // given
        TruffleLanguageInformation information = TruffleLanguageInformation.getLanguageInformation(MinicLanguage.ID);

        // when
        BitwisePatternMeta meta = new BitwisePatternMeta(information, true);

        // then
        Assert.assertNotNull(meta);
        Assert.assertEquals((long) meta.mask("at.fh.hagenberg.aist.gce.minic.nodes.impl.function.MinicInvokeNode$MinicInvokeCharNode")
                        & meta.mask("at.fh.hagenberg.aist.gce.minic.types.integer.signed.MinicCharNode"),
                (long) meta.mask("at.fh.hagenberg.aist.gce.minic.types.integer.signed.MinicCharNode"));
        Assert.assertEquals(meta.mask("at.fh.hagenberg.aist.gce.minic.nodes.impl.function.MinicInvokeNode"), meta.mask("OTHER"));
    }

    @Test
    public void testCreateBitwiseMetaEnclosing() {
        // given
        TruffleLanguageInformation information = TruffleLanguageInformation.getLanguageInformation(MinicLanguage.ID);

        // when
        BitwisePatternMeta meta = new BitwisePatternMeta(information, false);

        // then
        Assert.assertNotNull(meta);
        Assert.assertEquals((long) meta.mask("at.fh.hagenberg.aist.gce.minic.nodes.impl.function.MinicInvokeNode$MinicInvokeCharNode")
                & meta.mask("at.fh.hagenberg.aist.gce.minic.nodes.impl.function.MinicInvokeNode"),
                (long) meta.mask("at.fh.hagenberg.aist.gce.minic.nodes.impl.function.MinicInvokeNode"));
        Assert.assertEquals(meta.mask("at.fh.hagenberg.aist.gce.minic.types.integer.signed.MinicCharNode"), meta.mask("OTHER"));
    }

    @Test
    public void testCreateDummyMeta() {
        // given
        List<String> classes = new ArrayList<>();
        classes.add("int");
        classes.add("double");
        classes.add("for");
        classes.add("if");

        // when
        BitwisePatternMeta meta = new BitwisePatternMeta(classes);

        // then
        Assert.assertNotNull(meta);
    }

    @Test
    public void testCreateCustomHierarchy() {
        // given
        List<String> dtypes = new ArrayList<>();
        dtypes.add("int");
        dtypes.add("double");

        List<String> intTypes = new ArrayList<>();
        intTypes.add("uint");
        intTypes.add("short");
        intTypes.add("long");

        List<String> ctrl = new ArrayList<>();
        ctrl.add("for");
        ctrl.add("if");

        List<String> root = new ArrayList<>();
        root.add("ctrl");
        root.add("dtypes");

        List<String> skippable = new ArrayList<>();
        skippable.add("skip");

        List<String> doubleSkippable = new ArrayList<>();
        doubleSkippable.add("skipTheSecond");

        List<String> doubleTypes = new ArrayList<>();
        doubleTypes.add("udouble");

        Map<String, List<String>> classes = new HashMap<>();
        classes.put("root", root);
        classes.put("ctrl", ctrl);
        classes.put("dtypes", dtypes);
        classes.put("int", intTypes);
        classes.put("double", skippable);
        classes.put("skip", doubleSkippable);
        classes.put("skipTheSecond", doubleTypes);

        // when
        BitwisePatternMeta meta = new BitwisePatternMeta(classes, "root");

        // then
        Assert.assertNotNull(meta);
        Assert.assertEquals(meta.mask("skip"), meta.mask("udouble")); // as skip was skipped we auto-add the same type
        Assert.assertEquals(meta.mask("double"), meta.mask("udouble")); // as skip was skipped we auto-add the same type
        Assert.assertEquals((long) meta.mask("int") & meta.mask("uint"), (long) meta.mask("int"));
    }

    @Test
    public void testApplyBitwiseMeta() {
        // given
        TruffleLanguageInformation information = TruffleLanguageInformation.getLanguageInformation(MinicLanguage.ID);
        BitwisePatternMeta meta = new BitwisePatternMeta(information);

        // when
        Long mask = meta.mask(MinicSimpleLiteralNode.MinicIntLiteralNode.class);

        // then
        Assert.assertEquals((long) mask, 4780289529477070848L);
    }

    @Test
    public void testgetHierarchy() {
        // given
        TruffleLanguageInformation information = TruffleLanguageInformation.getLanguageInformation(MinicLanguage.ID);
        BitwisePatternMeta meta = new BitwisePatternMeta(information);

        // when
        String[] mask = meta.hierarchy(MinicSimpleLiteralNode.MinicIntLiteralNode.class);

        // then
        Assert.assertNotNull(mask);
        Assert.assertEquals(mask.length, 5);
        Assert.assertEquals(mask[0], Node.class.getName());
        Assert.assertEquals(mask[1], MinicNode.class.getName());
        Assert.assertEquals(mask[2], MinicExpressionNode.class.getName());
        Assert.assertEquals(mask[3], MinicIntNode.class.getName());
        Assert.assertEquals(mask[4], MinicSimpleLiteralNode.MinicIntLiteralNode.class.getName());
    }

    @Test
    public void testProduceHierarchicalPermutationsT1F0C7() {
        // given

        // when (5 permutations, 1 explicit, and 4 for the hierarchy)
        List<NodeWrapper> permute = permute(t1, 6, -1, 0, 7);

        // then
        Assert.assertTrue(permute.get(0).getValues().size() > 0);
        Assert.assertEquals(permute.get(0).getType(), MinicSimpleLiteralNode.MinicIntLiteralNode.class.getName());
        Assert.assertFalse(permute.get(1).getValues().size() > 0);
        Assert.assertEquals(permute.get(1).getType(), MinicSimpleLiteralNode.MinicIntLiteralNode.class.getName());
        Assert.assertEquals(permute.get(2).getType(), MinicIntNode.class.getName());
        Assert.assertEquals(permute.get(3).getType(), MinicExpressionNode.class.getName());
        Assert.assertEquals(permute.get(4).getType(), MinicNode.class.getName());
    }

    @Test
    public void testProduceHierarchicalPermutationsT2F0T7() {
        // given

        // when (5 permutations, 1 explicit, and 4 for the hierarchy)
        permute(t2, 6 + 6 + 29, -1, 0, 7);

        // then
    }

    @Test
    public void testProduceHierarchicalPermutationsT2F7T7() {
        // given

        // when (5 permutations, 1 explicit, and 4 for the hierarchy)
        permute(t2, 3, -1, 7, 7);

        // then
    }

    @Test
    public void testProduceHierarchicalPermutationsT3F0T7() {
        // given

        // when (5 permutations, 1 explicit, and 5 for the hierarchy)
        permute(t3, 36376, -1, 0, 7);

        // then
    }

    @Test
    public void testProduceHierarchicalPermutationsT4F0T7() {
        // given

        // when (5 permutations, 1 explicit, and 4 for the hierarchy)
        permute(t4, 36376, -1, 0, 7);

        // then
    }

    private List<NodeWrapper> permute(NodeWrapper node, int size, int patternSize, int floor, int ceil) {
        // given
        TruffleLanguageInformation information = TruffleLanguageInformation.getLanguageInformation(MinicLanguage.ID);
        BitwisePatternMeta meta = new BitwisePatternMeta(information);
        TrufflePatternSearchSpaceDefinition definition = new TrufflePatternSearchSpaceDefinition();
        definition.includeTree(node);
        TrufflePatternSearchSpace trees = repository.findTrees(definition);
        HierarchySupportingSubgraphIterator iterator = new HierarchySupportingSubgraphIterator(trees, patternSize, floor, ceil);
        iterator.setMeta(meta);


        // when
        List<NodeWrapper> permutations = new LinkedList<>();
        int i = 0;
        while (iterator.hasNext()) {
            NodeWrapper next = iterator.next();
            if (next != null) {
                permutations.add(next);
                i++;
            }
        }

        // then
        Assert.assertNotNull(iterator);
        Assert.assertEquals(i, size);

        return permutations;
    }
}
