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
import at.fh.hagenberg.aist.gce.minic.nodes.impl.op.rel.floating.MinicDoubleRelationalNode;
import at.fh.hagenberg.aist.gce.minic.nodes.impl.op.rel.floating.MinicDoubleRelationalNodeFactory;
import at.fh.hagenberg.aist.gce.minic.nodes.impl.op.rel.integer.signed.MinicIntRelationalNodeFactory;
import at.fh.hagenberg.aist.gce.optimization.util.TruffleLanguageInformation;
import at.fh.hagenberg.machinelearning.analytics.graph.nodes.NodeWrapper;
import com.oracle.truffle.api.frame.VirtualFrame;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * @author Oliver Krauss on 10.07.2020
 */

public class BitwisePatternTest extends TestRealNodesDbTest {

    TruffleLanguageInformation information = TruffleLanguageInformation.getLanguageInformation(MinicLanguage.ID);
    BitwisePatternMeta meta = new BitwisePatternMeta(information);


    @Test
    public void testCreateBitwisePattern() {
        // given
        NodeWrapper node = NodeWrapper.wrap(new MinicSimpleLiteralNode.MinicIntLiteralNode(3));

        // when
        BitwisePattern bitwisePattern = new BitwisePattern(node, meta);

        // then
        Assert.assertNotNull(bitwisePattern);
        Assert.assertEquals(bitwisePattern.getOpenclosetags().length, 0);
        Assert.assertEquals(bitwisePattern.getPattern().length, 1);
        Assert.assertEquals(bitwisePattern.getPattern()[0], meta.mask(MinicSimpleLiteralNode.MinicIntLiteralNode.class).longValue());
    }

    @Test
    public void testCreateBitwisePatternLong() {
        // given
        NodeWrapper node = NodeWrapper.wrap(new MinicFunctionBodyNode(new MinicIfNode(
            new MinicSimpleLiteralNode.MinicIntLiteralNode(2),
            MinicIntRelationalNodeFactory.MinicIntEqualsNodeGen.create(new MinicSimpleLiteralNode.MinicIntLiteralNode(2), new MinicSimpleLiteralNode.MinicIntLiteralNode(3)),
            MinicIntRelationalNodeFactory.MinicIntEqualsNodeGen.create(new MinicSimpleLiteralNode.MinicIntLiteralNode(4), new MinicSimpleLiteralNode.MinicIntLiteralNode(5))
        )));

        // when
        BitwisePattern bitwisePattern = new BitwisePattern(node, meta);

        // then
        Assert.assertNotNull(bitwisePattern);
        Assert.assertEquals(bitwisePattern.getOpenclosetags().length, 1);
        Assert.assertEquals(bitwisePattern.getOpenclosetags()[0], 2708633700886642688L);
        Assert.assertEquals(bitwisePattern.getPattern().length, 9);
        Assert.assertEquals(bitwisePattern.getPattern()[0], meta.mask(MinicFunctionBodyNode.class).longValue());
        Assert.assertEquals(bitwisePattern.getPattern()[1], meta.mask(MinicIfNode.class).longValue());
        Assert.assertEquals(bitwisePattern.getPattern()[2], meta.mask(MinicSimpleLiteralNode.MinicIntLiteralNode.class).longValue());
        Assert.assertEquals(bitwisePattern.getPattern()[3], meta.mask(MinicIntRelationalNodeFactory.MinicIntEqualsNodeGen.class).longValue());
        Assert.assertEquals(bitwisePattern.getPattern()[4], meta.mask(MinicSimpleLiteralNode.MinicIntLiteralNode.class).longValue());
        Assert.assertEquals(bitwisePattern.getPattern()[5], meta.mask(MinicSimpleLiteralNode.MinicIntLiteralNode.class).longValue());
        Assert.assertEquals(bitwisePattern.getPattern()[6], meta.mask(MinicIntRelationalNodeFactory.MinicIntEqualsNodeGen.class).longValue());
        Assert.assertEquals(bitwisePattern.getPattern()[7], meta.mask(MinicSimpleLiteralNode.MinicIntLiteralNode.class).longValue());
        Assert.assertEquals(bitwisePattern.getPattern()[8], meta.mask(MinicSimpleLiteralNode.MinicIntLiteralNode.class).longValue());
    }

    @Test
    public void testGeneralizesNot() {
        // given
        BitwisePattern nodeA = new BitwisePattern(NodeWrapper.wrap(new MinicSimpleLiteralNode.MinicIntLiteralNode(3)), meta);
        BitwisePattern nodeB = new BitwisePattern(NodeWrapper.wrap(new MinicSimpleLiteralNode.MinicDoubleLiteralNode(3.0)), meta);

        // when
        boolean generalizes = nodeA.generalizes(nodeB);
        boolean generalizesInverse = nodeB.generalizes(nodeA);

        // then
        Assert.assertFalse(generalizes);
        Assert.assertFalse(generalizesInverse);
    }

    @Test
    public void testGeneralizes() {
        // given
        BitwisePattern nodeA = new BitwisePattern(NodeWrapper.wrap(new MinicSimpleLiteralNode.MinicIntLiteralNode(3)), meta);
        BitwisePattern nodeB = new BitwisePattern(new NodeWrapper(MinicExpressionNode.class.getName()), meta);

        // when
        boolean generalizes = nodeB.generalizes(nodeA);
        boolean generalizesInverse = nodeA.generalizes(nodeB);

        // then
        Assert.assertTrue(generalizes);
        Assert.assertFalse(generalizesInverse);
    }

    @Test
    public void testGeneralizesNotLong() {
        // given
        BitwisePattern nodeA = new BitwisePattern(NodeWrapper.wrap(new MinicFunctionBodyNode(new MinicIfNode(
            new MinicSimpleLiteralNode.MinicIntLiteralNode(2),
            MinicIntRelationalNodeFactory.MinicIntEqualsNodeGen.create(new MinicSimpleLiteralNode.MinicIntLiteralNode(2), new MinicSimpleLiteralNode.MinicIntLiteralNode(3)),
            MinicIntRelationalNodeFactory.MinicIntEqualsNodeGen.create(new MinicSimpleLiteralNode.MinicIntLiteralNode(4), new MinicSimpleLiteralNode.MinicIntLiteralNode(5))
        ))), meta);
        BitwisePattern nodeB = new BitwisePattern(NodeWrapper.wrap(new MinicFunctionBodyNode(new MinicIfNode(
            new MinicSimpleLiteralNode.MinicIntLiteralNode(2),
            MinicDoubleRelationalNodeFactory.MinicDoubleEqualsNodeGen.create(new MinicSimpleLiteralNode.MinicDoubleLiteralNode(2.0), new MinicSimpleLiteralNode.MinicDoubleLiteralNode(3.0)),
            MinicDoubleRelationalNodeFactory.MinicDoubleEqualsNodeGen.create(new MinicSimpleLiteralNode.MinicDoubleLiteralNode(4.0), new MinicSimpleLiteralNode.MinicDoubleLiteralNode(5.0))
        ))), meta);


        // when
        boolean generalizes = nodeA.generalizes(nodeB);
        boolean generalizesInverse = nodeB.generalizes(nodeA);

        // then
        Assert.assertFalse(generalizes);
        Assert.assertFalse(generalizesInverse);
    }

    @Test
    public void testGeneralizesLong() {
        // given
        NodeWrapper wrappedA = NodeWrapper.wrap(new MinicFunctionBodyNode(new MinicIfNode(
            new MinicSimpleLiteralNode.MinicIntLiteralNode(2),
            MinicIntRelationalNodeFactory.MinicIntEqualsNodeGen.create(new MinicSimpleLiteralNode.MinicIntLiteralNode(2), new MinicSimpleLiteralNode.MinicIntLiteralNode(3)),
            MinicIntRelationalNodeFactory.MinicIntEqualsNodeGen.create(new MinicSimpleLiteralNode.MinicIntLiteralNode(4), new MinicSimpleLiteralNode.MinicIntLiteralNode(5))
        )));
        NodeWrapper.flatten(wrappedA).filter(x -> x.getType().contains("MinicIntLiteralNode")).forEach(x -> x.setType(MinicExpressionNode.class.getName()));
        BitwisePattern nodeA = new BitwisePattern(wrappedA, meta);
        BitwisePattern nodeB = new BitwisePattern(NodeWrapper.wrap(new MinicFunctionBodyNode(new MinicIfNode(
            new MinicSimpleLiteralNode.MinicIntLiteralNode(2),
            MinicIntRelationalNodeFactory.MinicIntEqualsNodeGen.create(new MinicSimpleLiteralNode.MinicIntLiteralNode(2), new MinicSimpleLiteralNode.MinicIntLiteralNode(3)),
            MinicIntRelationalNodeFactory.MinicIntEqualsNodeGen.create(new MinicSimpleLiteralNode.MinicIntLiteralNode(4), new MinicSimpleLiteralNode.MinicIntLiteralNode(5))
        ))), meta);



        // when
        boolean generalizes = nodeA.generalizes(nodeB);
        boolean generalizesInverse = nodeB.generalizes(nodeA);

        // then
        Assert.assertTrue(generalizes);
        Assert.assertFalse(generalizesInverse);
    }

    @Test
    public void testGeneralizesWrongStructure() {
        // given
        BitwisePattern nodeA = new BitwisePattern(NodeWrapper.wrap(new MinicSimpleLiteralNode.MinicIntLiteralNode(2)), meta);
        BitwisePattern nodeB = new BitwisePattern(NodeWrapper.wrap(new MinicFunctionBodyNode(new MinicIfNode(
            new MinicSimpleLiteralNode.MinicIntLiteralNode(2),
            MinicDoubleRelationalNodeFactory.MinicDoubleEqualsNodeGen.create(new MinicSimpleLiteralNode.MinicDoubleLiteralNode(2.0), new MinicSimpleLiteralNode.MinicDoubleLiteralNode(3.0)),
            MinicDoubleRelationalNodeFactory.MinicDoubleEqualsNodeGen.create(new MinicSimpleLiteralNode.MinicDoubleLiteralNode(4.0), new MinicSimpleLiteralNode.MinicDoubleLiteralNode(5.0))
        ))), meta);


        // when
        boolean generalizes = nodeA.generalizes(nodeB);
        boolean generalizesInverse = nodeB.generalizes(nodeA);

        // then
        Assert.assertFalse(generalizes);
        Assert.assertFalse(generalizesInverse);
    }

}
