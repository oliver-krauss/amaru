/*
 * Copyright (c) 2022 the original author or authors.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

package at.fh.hagenberg.aist.gce.optimization.operators.selection;

import at.fh.hagenberg.aist.gce.minic.language.MinicLanguage;
import at.fh.hagenberg.aist.gce.minic.nodes.base.MinicExpressionNode;
import at.fh.hagenberg.aist.gce.minic.nodes.builtin.PrintNodeFactory;
import at.fh.hagenberg.aist.gce.minic.nodes.impl.control.MinicBlockNode;
import at.fh.hagenberg.aist.gce.minic.nodes.impl.control.MinicWhileNode;
import at.fh.hagenberg.aist.gce.minic.nodes.impl.literals.MinicSimpleLiteralNode;
import at.fh.hagenberg.machinelearning.analytics.graph.nodes.NodeWrapper;
import com.oracle.truffle.api.nodes.LoopNode;
import com.oracle.truffle.api.nodes.Node;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * @author Oliver Krauss on 06.12.2019
 */

public class TruffleTreeSelectorTest {

    @Test
    public void testRandom() {
        // given
        MinicWhileNode node = new MinicWhileNode(new MinicSimpleLiteralNode.MinicIntLiteralNode(0), new MinicBlockNode(PrintNodeFactory.create(new MinicExpressionNode[]{new MinicSimpleLiteralNode.MinicStringLiteralNode("test")}, null)));
        RandomTruffleTreeSelector selector = new RandomTruffleTreeSelector();

        // when
        Node selected = selector.selectSubtree(node);

        // then
        Assert.assertNotNull(selected);
        Assert.assertFalse(LoopNode.class.isAssignableFrom(selected.getClass()));
    }


    @Test
    public void testSizeRestricted() {
        // given
        MinicWhileNode node = new MinicWhileNode(new MinicSimpleLiteralNode.MinicIntLiteralNode(0), new MinicBlockNode(PrintNodeFactory.create(new MinicExpressionNode[]{new MinicSimpleLiteralNode.MinicStringLiteralNode("test")}, null)));
        SizeRestrictedTruffleTreeSelector selector = new SizeRestrictedTruffleTreeSelector();

        // when
        Node selected = selector.selectSubtree(node);

        // then
        Assert.assertNotNull(selected);
        Assert.assertFalse(LoopNode.class.isAssignableFrom(selected.getClass()));
        Assert.assertFalse(selected.getChildren().iterator().hasNext());
    }

    @Test
    public void testDWRestricted() {
        // given
        MinicWhileNode node = new MinicWhileNode(new MinicSimpleLiteralNode.MinicIntLiteralNode(0), new MinicBlockNode(PrintNodeFactory.create(new MinicExpressionNode[]{new MinicSimpleLiteralNode.MinicStringLiteralNode("test")}, null)));
        DepthWidthRestrictedTruffleTreeSelector selector = new DepthWidthRestrictedTruffleTreeSelector();

        // when
        Node selected = selector.selectSubtree(node);

        // then
        Assert.assertNotNull(selected);
        Assert.assertFalse(LoopNode.class.isAssignableFrom(selected.getClass()));
        Assert.assertFalse(selected.getChildren().iterator().hasNext());
    }

    @Test
    public void testComplexityRestricted() {
        // given
        MinicWhileNode node = new MinicWhileNode(new MinicSimpleLiteralNode.MinicIntLiteralNode(0), new MinicBlockNode(PrintNodeFactory.create(new MinicExpressionNode[]{new MinicSimpleLiteralNode.MinicStringLiteralNode("test")}, null)));
        CodeComplexityTruffleTreeSelector selector = new CodeComplexityTruffleTreeSelector();
        selector.setMaxComplexity(0);
        selector.setLanguage(MinicLanguage.ID);

        // when
        Node selected = selector.selectSubtree(node);

        // then
        Assert.assertNotNull(selected);
        Assert.assertFalse(LoopNode.class.isAssignableFrom(selected.getClass()));
        Assert.assertEquals(NodeWrapper.cyclomaticComplexity(selected, MinicLanguage.ID), 0.0);
    }
}
