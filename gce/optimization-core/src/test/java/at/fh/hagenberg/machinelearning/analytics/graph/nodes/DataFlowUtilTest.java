/*
 * Copyright (c) 2022 the original author or authors.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

package at.fh.hagenberg.machinelearning.analytics.graph.nodes;

import at.fh.hagenberg.aist.gce.minic.language.MinicLanguage;
import at.fh.hagenberg.aist.gce.minic.nodes.impl.control.MinicBlockNode;
import at.fh.hagenberg.aist.gce.minic.nodes.impl.control.MinicWhileNode;
import at.fh.hagenberg.aist.gce.minic.nodes.impl.literals.MinicSimpleLiteralNode;
import at.fh.hagenberg.aist.gce.minic.nodes.impl.vars.*;
import at.fh.hagenberg.aist.gce.minic.types.integer.signed.MinicIntNode;
import at.fh.hagenberg.aist.gce.optimization.util.TruffleLanguageInformation;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.MaterializedFrame;
import com.oracle.truffle.api.nodes.Node;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.List;
import java.util.Map;

/**
 * @author Oliver Krauss on 17.06.2020
 */
public class DataFlowUtilTest {

    @Test
    public void testNoWrites() {
        // given
        Node n = new MinicWhileNode(new MinicSimpleLiteralNode.MinicIntLiteralNode(2),
            new MinicBlockNode(
                new MinicSimpleLiteralNode.MinicIntLiteralNode(5),
                new MinicSimpleLiteralNode.MinicCharLiteralNode('c'),
                new MinicSimpleLiteralNode.MinicDoubleLiteralNode(4.5)
            ));

        // when
        Map<Object, List<DataFlowNode>> availableDataItems = DataFlowUtil.findAvailableDataItems(TruffleLanguageInformation.getLanguageInformation(MinicLanguage.ID), n, null);

        // then
        Assert.assertTrue(availableDataItems.isEmpty());
    }

    @Test
    public void testWrites() {
        // given
        FrameDescriptor globalFrameDescriptor = new FrameDescriptor();
        MaterializedFrame globalFrame = Truffle.getRuntime().createMaterializedFrame(null, globalFrameDescriptor);
        FrameSlot slotA = globalFrameDescriptor.findOrAddFrameSlot("intVar");
        FrameDescriptor localFrameDescriptor = new FrameDescriptor();
        FrameSlot slotB = localFrameDescriptor.findOrAddFrameSlot("charVar");
        FrameSlot slotC = localFrameDescriptor.findOrAddFrameSlot("dblVar");

        Node n = new MinicWhileNode(new MinicSimpleLiteralNode.MinicIntLiteralNode(2),
            new MinicBlockNode(
                MinicWriteGlobalNodeFactory.MinicIntWriteGlobalNodeGen.create(new MinicSimpleLiteralNode.MinicIntLiteralNode(3),
                    slotA, globalFrame),
                MinicWriteNodeFactory.MinicCharWriteNodeGen.create(new MinicSimpleLiteralNode.MinicCharLiteralNode('c'), slotB),
                MinicWriteArrayNodeFactory.MinicDoubleArrayWriteNodeGen.create(new MinicIntNode[] {new MinicSimpleLiteralNode.MinicIntLiteralNode(3)}, new MinicSimpleLiteralNode.MinicDoubleLiteralNode(3.3), slotC)
            ));

        // when
        Map<Object, List<DataFlowNode>> availableDataItems = DataFlowUtil.findAvailableDataItems(TruffleLanguageInformation.getLanguageInformation(MinicLanguage.ID), n, null);

        // then
        Assert.assertFalse(availableDataItems.isEmpty());
        Assert.assertEquals(availableDataItems.size(), 2);
        Assert.assertNotNull(availableDataItems.get(null));
        Assert.assertNotNull(availableDataItems.get(globalFrame));
        Assert.assertEquals(availableDataItems.get(null).size(), 2);
        Assert.assertTrue(availableDataItems.get(null).stream().anyMatch(x -> slotB.equals(x.getSlot())));
        Assert.assertTrue(availableDataItems.get(null).stream().anyMatch(x -> slotC.equals(x.getSlot())));
        Assert.assertEquals(availableDataItems.get(globalFrame).size(), 1);
        Assert.assertEquals(availableDataItems.get(globalFrame).get(0).getSlot(), slotA);
        Assert.assertEquals(availableDataItems.get(globalFrame).get(0).getNode().getClass(), MinicWriteGlobalNodeFactory.MinicIntWriteGlobalNodeGen.class);
    }

    @Test
    public void testWritesSubset() {
        // given
        FrameDescriptor globalFrameDescriptor = new FrameDescriptor();
        MaterializedFrame globalFrame = Truffle.getRuntime().createMaterializedFrame(null, globalFrameDescriptor);
        FrameSlot slotA = globalFrameDescriptor.findOrAddFrameSlot("intVar");
        FrameDescriptor localFrameDescriptor = new FrameDescriptor();
        FrameSlot slotB = localFrameDescriptor.findOrAddFrameSlot("charVar");
        FrameSlot slotC = localFrameDescriptor.findOrAddFrameSlot("dblVar");
        MinicWriteNode.MinicCharWriteNode breakOffPoint = MinicWriteNodeFactory.MinicCharWriteNodeGen.create(new MinicSimpleLiteralNode.MinicCharLiteralNode('c'), slotB);

        Node n = new MinicWhileNode(new MinicSimpleLiteralNode.MinicIntLiteralNode(2),
            new MinicBlockNode(
                MinicWriteGlobalNodeFactory.MinicIntWriteGlobalNodeGen.create(new MinicSimpleLiteralNode.MinicIntLiteralNode(3),
                    slotA, globalFrame),
                breakOffPoint,
                MinicWriteArrayNodeFactory.MinicDoubleArrayWriteNodeGen.create(new MinicIntNode[] {new MinicSimpleLiteralNode.MinicIntLiteralNode(3)}, new MinicSimpleLiteralNode.MinicDoubleLiteralNode(3.3), slotC)
            ));

        // when
        Map<Object, List<DataFlowNode>> availableDataItems = DataFlowUtil.findAvailableDataItems(TruffleLanguageInformation.getLanguageInformation(MinicLanguage.ID), n, breakOffPoint);

        // then
        Assert.assertFalse(availableDataItems.isEmpty());
        Assert.assertEquals(availableDataItems.size(), 1);
        Assert.assertNotNull(availableDataItems.get(globalFrame));
        Assert.assertEquals(availableDataItems.get(globalFrame).size(), 1);
        Assert.assertEquals(availableDataItems.get(globalFrame).get(0).getSlot(), slotA);
        Assert.assertEquals(availableDataItems.get(globalFrame).get(0).getNode().getClass(), MinicWriteGlobalNodeFactory.MinicIntWriteGlobalNodeGen.class);
    }

    @Test
    public void testAllSatisfied() {
        // given
        FrameDescriptor globalFrameDescriptor = new FrameDescriptor();
        MaterializedFrame globalFrame = Truffle.getRuntime().createMaterializedFrame(null, globalFrameDescriptor);
        FrameSlot slotA = globalFrameDescriptor.findOrAddFrameSlot("intVar");
        FrameDescriptor localFrameDescriptor = new FrameDescriptor();
        FrameSlot slotB = localFrameDescriptor.findOrAddFrameSlot("charVar");
        FrameSlot slotC = localFrameDescriptor.findOrAddFrameSlot("dblVar");

        Node n = new MinicWhileNode(new MinicSimpleLiteralNode.MinicIntLiteralNode(2),
            new MinicBlockNode(
                MinicWriteGlobalNodeFactory.MinicIntWriteGlobalNodeGen.create(new MinicSimpleLiteralNode.MinicIntLiteralNode(3),
                    slotA, globalFrame),
                MinicWriteNodeFactory.MinicCharWriteNodeGen.create(new MinicSimpleLiteralNode.MinicCharLiteralNode('c'), slotB),
                MinicWriteArrayNodeFactory.MinicDoubleArrayWriteNodeGen.create(new MinicIntNode[] {new MinicSimpleLiteralNode.MinicIntLiteralNode(3)}, new MinicSimpleLiteralNode.MinicDoubleLiteralNode(3.3), slotC)
            ));

        // when
        Map<Object, List<DataFlowNode>> unsatisfiedDataItems = DataFlowUtil.findUnsatisfiedDataItems(TruffleLanguageInformation.getLanguageInformation(MinicLanguage.ID), n, null);

        // then
        Assert.assertTrue(unsatisfiedDataItems.isEmpty());
    }

    @Test
    public void testAllUnsatisified() {
        // given
        FrameDescriptor globalFrameDescriptor = new FrameDescriptor();
        MaterializedFrame globalFrame = Truffle.getRuntime().createMaterializedFrame(null, globalFrameDescriptor);
        FrameSlot slotA = globalFrameDescriptor.findOrAddFrameSlot("intVar");
        FrameDescriptor localFrameDescriptor = new FrameDescriptor();
        FrameSlot slotB = localFrameDescriptor.findOrAddFrameSlot("charVar");
        FrameSlot slotC = localFrameDescriptor.findOrAddFrameSlot("dblVar");

        Node n = new MinicWhileNode(new MinicSimpleLiteralNode.MinicIntLiteralNode(2),
            new MinicBlockNode(
                MinicReadGlobalNodeFactory.MinicIntReadGlobalNodeGen.create(slotA, globalFrame),
                MinicReadNodeFactory.MinicCharReadNodeGen.create(slotB),
                MinicReadArrayNodeFactory.MinicDoubleArrayReadNodeGen.create(new MinicIntNode[] {new MinicSimpleLiteralNode.MinicIntLiteralNode(3)}, slotC)
            ));

        // when
        Map<Object, List<DataFlowNode>> unsatisfiedDataItems = DataFlowUtil.findUnsatisfiedDataItems(TruffleLanguageInformation.getLanguageInformation(MinicLanguage.ID), n, null);

        // then
        Assert.assertFalse(unsatisfiedDataItems.isEmpty());
        Assert.assertEquals(unsatisfiedDataItems.size(), 2);
        Assert.assertNotNull(unsatisfiedDataItems.get(null));
        Assert.assertNotNull(unsatisfiedDataItems.get(globalFrame));
        Assert.assertEquals(unsatisfiedDataItems.get(null).size(), 2);
        Assert.assertTrue(unsatisfiedDataItems.get(null).stream().anyMatch(x -> slotB.equals(x.getSlot())));
        Assert.assertTrue(unsatisfiedDataItems.get(null).stream().anyMatch(x -> slotC.equals(x.getSlot())));
        Assert.assertEquals(unsatisfiedDataItems.get(globalFrame).size(), 1);
        Assert.assertEquals(unsatisfiedDataItems.get(globalFrame).get(0).getSlot(), slotA);
        Assert.assertEquals(unsatisfiedDataItems.get(globalFrame).get(0).getNode().getClass(), MinicReadGlobalNodeFactory.MinicIntReadGlobalNodeGen.class);
    }

    @Test
    public void testSomeUnsatisfied() {
        // given
        FrameDescriptor globalFrameDescriptor = new FrameDescriptor();
        MaterializedFrame globalFrame = Truffle.getRuntime().createMaterializedFrame(null, globalFrameDescriptor);
        FrameSlot slotA = globalFrameDescriptor.findOrAddFrameSlot("intVar");
        FrameDescriptor localFrameDescriptor = new FrameDescriptor();
        FrameSlot slotB = localFrameDescriptor.findOrAddFrameSlot("charVar");
        FrameSlot slotC = localFrameDescriptor.findOrAddFrameSlot("dblVar");
        MinicWriteNode.MinicCharWriteNode breakoff = MinicWriteNodeFactory.MinicCharWriteNodeGen.create(new MinicSimpleLiteralNode.MinicCharLiteralNode('c'), slotB);

        Node n = new MinicWhileNode(new MinicSimpleLiteralNode.MinicIntLiteralNode(2),
            new MinicBlockNode(
                MinicWriteGlobalNodeFactory.MinicIntWriteGlobalNodeGen.create(new MinicSimpleLiteralNode.MinicIntLiteralNode(3),
                    slotA, globalFrame),
                breakoff,
                MinicWriteArrayNodeFactory.MinicDoubleArrayWriteNodeGen.create(new MinicIntNode[] {new MinicSimpleLiteralNode.MinicIntLiteralNode(3)}, new MinicSimpleLiteralNode.MinicDoubleLiteralNode(3.3), slotC),
                MinicReadGlobalNodeFactory.MinicIntReadGlobalNodeGen.create(slotA, globalFrame),
                MinicReadNodeFactory.MinicCharReadNodeGen.create(slotB),
                MinicReadArrayNodeFactory.MinicDoubleArrayReadNodeGen.create(new MinicIntNode[] {new MinicSimpleLiteralNode.MinicIntLiteralNode(3)}, slotC)
            ));

        // when
        Map<Object, List<DataFlowNode>> unsatisfiedDataItems = DataFlowUtil.findUnsatisfiedDataItems(TruffleLanguageInformation.getLanguageInformation(MinicLanguage.ID), n, breakoff);

        // then
        Assert.assertFalse(unsatisfiedDataItems.isEmpty());
        Assert.assertEquals(unsatisfiedDataItems.size(), 1);
        Assert.assertNotNull(unsatisfiedDataItems.get(null));
        Assert.assertEquals(unsatisfiedDataItems.get(null).size(), 1);
        Assert.assertTrue(unsatisfiedDataItems.get(null).stream().anyMatch(x -> slotB.equals(x.getSlot())));
    }

    @Test
    public void testReads() {
        // given
        FrameDescriptor globalFrameDescriptor = new FrameDescriptor();
        MaterializedFrame globalFrame = Truffle.getRuntime().createMaterializedFrame(null, globalFrameDescriptor);
        FrameSlot slotA = globalFrameDescriptor.findOrAddFrameSlot("intVar");
        FrameDescriptor localFrameDescriptor = new FrameDescriptor();
        FrameSlot slotB = localFrameDescriptor.findOrAddFrameSlot("charVar");
        FrameSlot slotC = localFrameDescriptor.findOrAddFrameSlot("dblVar");

        Node n = new MinicWhileNode(new MinicSimpleLiteralNode.MinicIntLiteralNode(2),
            new MinicBlockNode(
                MinicReadGlobalNodeFactory.MinicIntReadGlobalNodeGen.create(slotA, globalFrame),
                MinicReadNodeFactory.MinicCharReadNodeGen.create(slotB),
                MinicReadArrayNodeFactory.MinicDoubleArrayReadNodeGen.create(new MinicIntNode[] {new MinicSimpleLiteralNode.MinicIntLiteralNode(3)}, slotC)
            ));

        // when
        Map<Object, List<DataFlowNode>> availableDataItems = DataFlowUtil.findRequiredDataItems(TruffleLanguageInformation.getLanguageInformation(MinicLanguage.ID), n, null);

        // then
        Assert.assertFalse(availableDataItems.isEmpty());
        Assert.assertEquals(availableDataItems.size(), 2);
        Assert.assertNotNull(availableDataItems.get(null));
        Assert.assertNotNull(availableDataItems.get(globalFrame));
        Assert.assertEquals(availableDataItems.get(null).size(), 2);
        Assert.assertTrue(availableDataItems.get(null).stream().anyMatch(x -> slotB.equals(x.getSlot())));
        Assert.assertTrue(availableDataItems.get(null).stream().anyMatch(x -> slotC.equals(x.getSlot())));
        Assert.assertEquals(availableDataItems.get(globalFrame).size(), 1);
        Assert.assertEquals(availableDataItems.get(globalFrame).get(0).getSlot(), slotA);
        Assert.assertEquals(availableDataItems.get(globalFrame).get(0).getNode().getClass(), MinicReadGlobalNodeFactory.MinicIntReadGlobalNodeGen.class);
    }

    @Test
    public void testReadSubset() {
        // given
        FrameDescriptor globalFrameDescriptor = new FrameDescriptor();
        MaterializedFrame globalFrame = Truffle.getRuntime().createMaterializedFrame(null, globalFrameDescriptor);
        FrameSlot slotA = globalFrameDescriptor.findOrAddFrameSlot("intVar");
        FrameDescriptor localFrameDescriptor = new FrameDescriptor();
        FrameSlot slotB = localFrameDescriptor.findOrAddFrameSlot("charVar");
        FrameSlot slotC = localFrameDescriptor.findOrAddFrameSlot("dblVar");
        MinicReadNode.MinicCharReadNode breakoff = MinicReadNodeFactory.MinicCharReadNodeGen.create(slotB);

        Node n = new MinicWhileNode(new MinicSimpleLiteralNode.MinicIntLiteralNode(2),
            new MinicBlockNode(
                MinicReadGlobalNodeFactory.MinicIntReadGlobalNodeGen.create(slotA, globalFrame),
                breakoff,
                MinicReadArrayNodeFactory.MinicDoubleArrayReadNodeGen.create(new MinicIntNode[] {new MinicSimpleLiteralNode.MinicIntLiteralNode(3)}, slotC)
            ));

        // when
        Map<Object, List<DataFlowNode>> availableDataItems = DataFlowUtil.findRequiredDataItems(TruffleLanguageInformation.getLanguageInformation(MinicLanguage.ID), n, breakoff);

        // then
        Assert.assertFalse(availableDataItems.isEmpty());
        Assert.assertEquals(availableDataItems.size(), 1);
        Assert.assertNotNull(availableDataItems.get(globalFrame));
        Assert.assertEquals(availableDataItems.get(globalFrame).size(), 1);
        Assert.assertEquals(availableDataItems.get(globalFrame).get(0).getSlot(), slotA);
        Assert.assertEquals(availableDataItems.get(globalFrame).get(0).getNode().getClass(), MinicReadGlobalNodeFactory.MinicIntReadGlobalNodeGen.class);
    }

    @Test
    public void testParentWrite() {
        // given
        FrameDescriptor globalFrameDescriptor = new FrameDescriptor();
        MaterializedFrame globalFrame = Truffle.getRuntime().createMaterializedFrame(null, globalFrameDescriptor);
        FrameSlot slotA = globalFrameDescriptor.findOrAddFrameSlot("intVar");
        FrameDescriptor localFrameDescriptor = new FrameDescriptor();
        FrameSlot slotB = localFrameDescriptor.findOrAddFrameSlot("charVar");
        MinicSimpleLiteralNode.MinicCharLiteralNode breakoff = new MinicSimpleLiteralNode.MinicCharLiteralNode('c');

        Node n = new MinicWhileNode(new MinicSimpleLiteralNode.MinicIntLiteralNode(2),
            new MinicBlockNode(
                MinicWriteGlobalNodeFactory.MinicIntWriteGlobalNodeGen.create(new MinicSimpleLiteralNode.MinicIntLiteralNode(3),
                    slotA, globalFrame),
                MinicWriteNodeFactory.MinicCharWriteNodeGen.create(breakoff, slotB)
                ));
        n.adoptChildren();

        // when
        Map<Object, List<DataFlowNode>> availableDataItems = DataFlowUtil.findAvailableDataItems(TruffleLanguageInformation.getLanguageInformation(MinicLanguage.ID), n, breakoff);

        // then
        Assert.assertFalse(availableDataItems.isEmpty());
        Assert.assertEquals(availableDataItems.size(), 1);
        Assert.assertNull(availableDataItems.get(null));
        Assert.assertNotNull(availableDataItems.get(globalFrame));
        Assert.assertEquals(availableDataItems.get(globalFrame).size(), 1);
        Assert.assertEquals(availableDataItems.get(globalFrame).get(0).getSlot(), slotA);
        Assert.assertEquals(availableDataItems.get(globalFrame).get(0).getNode().getClass(), MinicWriteGlobalNodeFactory.MinicIntWriteGlobalNodeGen.class);
    }
}
