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
import at.fh.hagenberg.aist.gce.minic.nodes.impl.control.MinicIfNode;
import at.fh.hagenberg.aist.gce.minic.nodes.impl.function.MinicFunctionBodyNode;
import at.fh.hagenberg.aist.gce.minic.nodes.impl.literals.MinicSimpleLiteralNode;
import at.fh.hagenberg.aist.gce.minic.nodes.impl.op.rel.floating.MinicDoubleRelationalNodeFactory;
import at.fh.hagenberg.aist.gce.minic.nodes.impl.op.rel.integer.signed.MinicIntRelationalNodeFactory;
import at.fh.hagenberg.aist.gce.minic.nodes.impl.vars.MinicWriteNodeFactory;
import at.fh.hagenberg.aist.gce.optimization.util.ExtendedNodeUtil;
import at.fh.hagenberg.aist.gce.optimization.util.TruffleLanguageInformation;
import at.fh.hagenberg.machinelearning.analytics.graph.nodes.NodeWrapper;
import at.fh.hagenberg.machinelearning.analytics.graph.nodes.OrderedRelationship;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Pattern also containing info on the Nodes that it was mined from and the literal values of those nodes
 * <p>
 * Warning - the bitshift operations are finnicky:
 * Don't forget L when shifting otherwise Java shifts int https://www.lewuathe.com/bitshift-to-64-bit-in-java.html
 * Guide on bit operations: https://www.geeksforgeeks.org/bitwise-operators-in-java/
 *
 * @author Oliver Krauss on 10.07.2020
 */
public class TracableBitwisePatternTest extends TestRealNodesDbTest {

    TruffleLanguageInformation information = TruffleLanguageInformation.getLanguageInformation(MinicLanguage.ID);
    BitwisePatternMeta meta = new BitwisePatternMeta(information);

    @Test
    public void testCreateTracable() {
        // given
        NodeWrapper wrapper = t4;

        // when
        TracableBitwisePattern bitwisePattern = new TracableBitwisePattern(4, t4.getId(), wrapper, meta);

        // then
        Assert.assertNotNull(bitwisePattern);
        Assert.assertEquals(bitwisePattern.getOpenclosetags().length, 1);
        Assert.assertEquals(bitwisePattern.getOpenclosetags()[0], 5530420342410969088L);
        Assert.assertEquals(bitwisePattern.getPattern().length, 6);
        Assert.assertEquals(bitwisePattern.getPattern()[0], meta.mask(MinicIfNode.class).longValue());
        Assert.assertEquals(bitwisePattern.getPattern()[1], meta.mask(MinicSimpleLiteralNode.MinicIntLiteralNode.class).longValue());
        Assert.assertEquals(bitwisePattern.getPattern()[2], meta.mask(MinicWriteNodeFactory.MinicDoubleWriteNodeGen.class).longValue());
        Assert.assertEquals(bitwisePattern.getPattern()[3], meta.mask(MinicSimpleLiteralNode.MinicDoubleLiteralNode.class).longValue());
        Assert.assertEquals(bitwisePattern.getPattern()[4], meta.mask(MinicWriteNodeFactory.MinicDoubleWriteNodeGen.class).longValue());
        Assert.assertEquals(bitwisePattern.getPattern()[5], meta.mask(MinicSimpleLiteralNode.MinicDoubleLiteralNode.class).longValue());

        Assert.assertEquals(bitwisePattern.getCount(), 1);
        Assert.assertEquals(bitwisePattern.getTreeCount(), 1);
        Assert.assertEquals(bitwisePattern.getClusterCount(), 1);
        Assert.assertNotEquals(bitwisePattern.getNodes()[5], 0);
        Assert.assertEquals(bitwisePattern.getNodes()[6], 0);
        Assert.assertTrue(bitwisePattern.getNodeIds()[5][0] > 0);
        Assert.assertFalse(bitwisePattern.getNodeIds()[5][1] > 0);
        Assert.assertEquals(bitwisePattern.getNodeIds().length, 6);
        Assert.assertTrue(bitwisePattern.getTreeId()[0] > 0);
        Assert.assertFalse(bitwisePattern.getTreeId()[1] > 0);
    }

    @Test
    public void testGrow() {
        // given
        NodeWrapper wrapper = t4.deepCopy();
        TracableBitwisePattern comparison = new TracableBitwisePattern(4, t4.getId(), wrapper, meta);
        List<NodeWrapper> children = NodeWrapper.flatten(wrapper).collect(Collectors.toList());
        children.forEach(x -> x.getChildren().clear());
        children.remove(wrapper);
        Map<Long, long[]> growthList = new HashMap<>();
        long[] growthPoints = new long[3];
        growthPoints[0] = children.get(0).getId();
        growthPoints[1] = children.get(1).getId();
        growthPoints[2] = children.get(3).getId();
        growthList.put(wrapper.getId(), growthPoints);
        TracableBitwisePattern bitwisePattern = new TracableBitwisePattern(4, t4.getId(), wrapper, meta, growthList, null, 0L);

        // we sneak a second location in to test out the location reduction
        NodeWrapper copy = wrapper.copy();
        copy.setId(234234234L);
        bitwisePattern.addLocation(new TracableBitwisePattern(4, copy.getId(), copy, meta, new HashMap<>(), null, 0L));

        // when
        // adding the int condition back
        long[][] ext_point = new long[2][2];
        ext_point[0][0] = 0;
        ext_point[0][1] = children.get(0).getId();
        ext_point[1] = null;
        bitwisePattern = bitwisePattern.grow(0, ext_point, new TracableBitwisePattern(4, t4.getId(), children.get(0), meta, new HashMap<>(), null, 0L), false).get(0);
        children.remove(0);

        // adding the double write back
        ext_point = new long[2][2];
        ext_point[1] = null;
        ext_point[0][0] = 0;
        long dbl_write = children.get(0).getId();
        ext_point[0][1] = children.get(0).getId();
        Map<Long, long[]> dbl_write_growthList = new HashMap<>();
        long[] dbl_write_growthPoints = new long[1];
        dbl_write_growthList.put(dbl_write, dbl_write_growthPoints);
        dbl_write_growthPoints[0] = children.get(1).getId();
        bitwisePattern = bitwisePattern.grow(0, ext_point, new TracableBitwisePattern(4, t4.getId(), children.get(0), meta, dbl_write_growthList, null, 0L), false).get(0);
        children.remove(0);

        // adding the other double write back
        ext_point = new long[2][2];
        ext_point[1] = null;
        ext_point[0][0] = 0;
        ext_point[0][1] = children.get(0).getId();
        bitwisePattern = bitwisePattern.grow(2, ext_point, new TracableBitwisePattern(4, t4.getId(), children.get(0), meta, new HashMap<>(), null, 0L), false).get(0);
        children.remove(0);

        // adding the other double write back (so we can test out not-at-end additions)
        ext_point = new long[2][2];
        ext_point[1] = null;
        ext_point[0][0] = 0;
        long dbl_write2 = children.get(0).getId();
        ext_point[0][1] = children.get(0).getId();
        Map<Long, long[]> dbl_write2_growthList = new HashMap<>();
        long[] dbl_write2_growthPoints = new long[1];
        dbl_write2_growthList.put(dbl_write2, dbl_write2_growthPoints);
        dbl_write2_growthPoints[0] = children.get(1).getId();
        bitwisePattern = bitwisePattern.grow(0, ext_point, new TracableBitwisePattern(4, t4.getId(), children.get(0), meta, dbl_write2_growthList, null, 0L), false).get(0);
        children.remove(0);

        // adding the other double write back
        ext_point = new long[2][2];
        ext_point[1] = null;
        ext_point[0][0] = 0;
        ext_point[0][1] = children.get(0).getId();
        bitwisePattern = bitwisePattern.grow(4, ext_point, new TracableBitwisePattern(4, t4.getId(), children.get(0), meta, new HashMap<>(), null, 1L), false).get(0);
        children.remove(0);

        // then
        Assert.assertNotNull(bitwisePattern);
        Assert.assertEquals(bitwisePattern.getOpenclosetags().length, 1);
        Assert.assertEquals(bitwisePattern.getOpenclosetags()[0], 5530420342410969088L);
        Assert.assertEquals(bitwisePattern.getPattern().length, 6);
        Assert.assertEquals(bitwisePattern.getPattern()[0], meta.mask(MinicIfNode.class).longValue());
        Assert.assertEquals(bitwisePattern.getPattern()[1], meta.mask(MinicSimpleLiteralNode.MinicIntLiteralNode.class).longValue());
        Assert.assertEquals(bitwisePattern.getPattern()[2], meta.mask(MinicWriteNodeFactory.MinicDoubleWriteNodeGen.class).longValue());
        Assert.assertEquals(bitwisePattern.getPattern()[3], meta.mask(MinicSimpleLiteralNode.MinicDoubleLiteralNode.class).longValue());
        Assert.assertEquals(bitwisePattern.getPattern()[4], meta.mask(MinicWriteNodeFactory.MinicDoubleWriteNodeGen.class).longValue());
        Assert.assertEquals(bitwisePattern.getPattern()[5], meta.mask(MinicSimpleLiteralNode.MinicDoubleLiteralNode.class).longValue());

        Assert.assertEquals(bitwisePattern.getCount(), 1);
        Assert.assertEquals(bitwisePattern.getTreeCount(), 1);
        Assert.assertEquals(bitwisePattern.getClusterCount(), 1);
        Assert.assertNotEquals(bitwisePattern.getNodes()[5], 0);
        Assert.assertTrue(bitwisePattern.getNodeIds()[5][0] > 0);
        Assert.assertEquals(bitwisePattern.getNodeIds()[0].length, 1);
        Assert.assertEquals(bitwisePattern.getNodeIds().length, 6);
        Assert.assertTrue(bitwisePattern.getTreeId()[0] > 0);
        Assert.assertEquals(bitwisePattern.getTreeId().length, 1);

        Assert.assertEquals(bitwisePattern.getTreeId()[0], comparison.getTreeId()[0]);
        for (int i = 0; i < 6; i++) {
            Assert.assertEquals(bitwisePattern.getNodes()[i], comparison.getNodes()[i]);
            Assert.assertEquals(bitwisePattern.getNodeIds()[i][0], comparison.getNodeIds()[i][0]);

        }
    }


}
