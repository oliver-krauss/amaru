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
import at.fh.hagenberg.aist.gce.minic.nodes.impl.control.MinicIfNode;
import at.fh.hagenberg.aist.gce.minic.nodes.impl.control.MinicReturnNode;
import at.fh.hagenberg.aist.gce.minic.nodes.impl.control.MinicWhileNode;
import at.fh.hagenberg.aist.gce.minic.nodes.impl.literals.MinicSimpleLiteralNode;
import at.fh.hagenberg.aist.gce.optimization.util.TruffleLanguageInformation;
import com.oracle.truffle.api.nodes.Node;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * @author Oliver Krauss on 01.11.2019
 */

public class NodeWrapperTest {

    @BeforeClass
    public void setUp() {
        TruffleLanguageInformation.getLanguageInformation(MinicLanguage.ID);
    }

    @Test
    public void testWrap() {
        // given
        MinicSimpleLiteralNode.MinicIntLiteralNode n = new MinicSimpleLiteralNode.MinicIntLiteralNode(1);

        // when
        NodeWrapper wrap = NodeWrapper.wrap(n);

        // then
        Assert.assertNotNull(wrap);
        Assert.assertNotNull(wrap.getHash());
        Assert.assertEquals(wrap.getType(), "at.fh.hagenberg.aist.gce.minic.nodes.impl.literals.MinicSimpleLiteralNode$MinicIntLiteralNode");
        Assert.assertEquals(wrap.getValues().get("value:int"), 1);
    }

    @Test
    public void testUnwrap() {
        // given
        NodeWrapper wrap = NodeWrapper.wrap(new MinicSimpleLiteralNode.MinicIntLiteralNode(1));

        // when
        Node n = NodeWrapper.unwrap(wrap, null, null, "c");

        // then
        Assert.assertNotNull(n);
    }

    @Test
    public void testUnwrapFromDb() {
        // given
        NodeWrapper wrap = NodeWrapper.wrap(new MinicSimpleLiteralNode.MinicIntLiteralNode(1));
        long l = 1L;
        wrap.getValues().put("value:int", l);
        // neo4j returns ints as long check if this is still ok

        // when
        Node n = NodeWrapper.unwrap(wrap, null, null, "c");

        // then
        Assert.assertNotNull(n);
    }

    @Test
    public void testUnwrapComplex() {
        // given
        NodeWrapper wrap = NodeWrapper.wrap(new MinicBlockNode(
            new MinicIfNode(
                new MinicSimpleLiteralNode.MinicIntLiteralNode(1),
                new MinicReturnNode(new MinicSimpleLiteralNode.MinicIntLiteralNode(5)),
                new MinicReturnNode(new MinicSimpleLiteralNode.MinicIntLiteralNode(3)))));

        // when
        Node n = NodeWrapper.unwrap(wrap, null, null, "c");

        // then
        Assert.assertNotNull(n);
        Assert.assertNotNull(n.getChildren());
        Assert.assertNotNull(n.getChildren().iterator().next());
    }

    @Test
    public void testUnwrapLoop() {
        // given
        NodeWrapper wrap = NodeWrapper.wrap(new MinicWhileNode(new MinicSimpleLiteralNode.MinicIntLiteralNode(2), new MinicBlockNode(
            new MinicSimpleLiteralNode.MinicIntLiteralNode(3)
        )));

        // when
        Node n = NodeWrapper.unwrap(wrap, null, null, "c");

        // then
        Assert.assertNotNull(n);
        Assert.assertNotNull(n.getChildren());
        Assert.assertNotNull(n.getChildren().iterator().next());
        Assert.assertNotNull(n.getChildren().iterator().next().getChildren().iterator().next());
        Assert.assertNotNull(n.getChildren().iterator().next().getChildren().iterator().next().getChildren().iterator().next());
    }

    @Test
    public void testSerialize() {
        // given
        NodeWrapper wrap = NodeWrapper.wrap(new MinicBlockNode(
            new MinicIfNode(
                new MinicSimpleLiteralNode.MinicIntLiteralNode(1),
                new MinicReturnNode(new MinicSimpleLiteralNode.MinicIntLiteralNode(5)),
                new MinicReturnNode(new MinicSimpleLiteralNode.MinicIntLiteralNode(3)))));

        // when
        String serialize = NodeWrapper.serialize(wrap);
        NodeWrapper deserialize = NodeWrapper.deserialize(serialize);

        // then
        Assert.assertNotNull(serialize);
        Assert.assertNotNull(deserialize);
        Assert.assertEquals(deserialize.getHash(), wrap.getHash());
    }

    @Test
    public void testCyclomaticComplexity() {
        // given
        NodeWrapper ifreturn = NodeWrapper.wrap(new MinicBlockNode(
            new MinicIfNode(
                new MinicSimpleLiteralNode.MinicIntLiteralNode(1),
                new MinicReturnNode(new MinicSimpleLiteralNode.MinicIntLiteralNode(5)),
                new MinicReturnNode(new MinicSimpleLiteralNode.MinicIntLiteralNode(3)))));
        NodeWrapper literal = NodeWrapper.wrap(new MinicSimpleLiteralNode.MinicIntLiteralNode(1));

        // when
        double ifreturnC = NodeWrapper.cyclomaticComplexity(ifreturn, MinicLanguage.ID);
        double literalC = NodeWrapper.cyclomaticComplexity(literal, MinicLanguage.ID);

        // then
        Assert.assertEquals(ifreturnC, 3.0);
        Assert.assertEquals(literalC, 0.0);
    }

    @Test
    public void testContains() {
        // given
        NodeWrapper in = NodeWrapper.wrap(new MinicBlockNode(
            new MinicIfNode(
                new MinicSimpleLiteralNode.MinicIntLiteralNode(1),
                new MinicReturnNode(new MinicSimpleLiteralNode.MinicIntLiteralNode(5)),
                new MinicReturnNode(new MinicSimpleLiteralNode.MinicIntLiteralNode(3)))));

        NodeWrapper find = NodeWrapper.wrap(new MinicSimpleLiteralNode.MinicIntLiteralNode(1));
        NodeWrapper notFind = NodeWrapper.wrap(new MinicSimpleLiteralNode.MinicIntLiteralNode(7));
        NodeWrapper find2 = NodeWrapper.wrap(new MinicReturnNode(new MinicSimpleLiteralNode.MinicIntLiteralNode(5)));
        NodeWrapper notFind2 = NodeWrapper.wrap(new MinicReturnNode(new MinicSimpleLiteralNode.MinicIntLiteralNode(6)));
        NodeWrapper find3 = NodeWrapper.wrap(new MinicIfNode(null, null, null));
        NodeWrapper notFind3 = NodeWrapper.wrap(new MinicIfNode(null, new MinicSimpleLiteralNode.MinicIntLiteralNode(5), null));


        // when
        boolean found = in.contains(find);
        boolean notFound = in.contains(notFind);
        boolean found2 = in.contains(find2);
        boolean notFound2 = in.contains(notFind2);
        boolean found3 = in.contains(find3);
        boolean notFound3 = in.contains(notFind3);

        // then
        Assert.assertTrue(found);
        Assert.assertFalse(notFound);
        Assert.assertTrue(found2);
        Assert.assertFalse(notFound2);
        Assert.assertTrue(found3);
        Assert.assertFalse(notFound3);
    }

}
