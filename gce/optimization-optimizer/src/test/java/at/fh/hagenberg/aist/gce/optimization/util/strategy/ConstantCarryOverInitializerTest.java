/*
 * Copyright (c) 2022 the original author or authors.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

package at.fh.hagenberg.aist.gce.optimization.util.strategy;

import com.oracle.truffle.api.nodes.Node;
import at.fh.hagenberg.aist.gce.minic.nodes.impl.control.MinicBlockNode;
import at.fh.hagenberg.aist.gce.minic.nodes.impl.function.MinicReadFunctionArgumentNodeFactory;
import at.fh.hagenberg.aist.gce.minic.nodes.impl.literals.MinicSimpleLiteralNode;
import at.fh.hagenberg.aist.gce.minic.nodes.impl.op.arith.integer.signed.MinicIntArithmeticNodeFactory;
import at.fh.hagenberg.aist.gce.optimization.util.strategy.initializer.ConstantCarryOverInitializer;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.LinkedList;
import java.util.List;

/**
 * @author Oliver Krauss on 07.11.2018
 */

public class ConstantCarryOverInitializerTest {

    private static KnownValueStrategy<MinicSimpleLiteralNode.MinicIntLiteralNode> intStrategy = new KnownValueStrategy<>(new LinkedList<>());

    private static ConstantCarryOverInitializer<MinicSimpleLiteralNode.MinicIntLiteralNode> initializer = new ConstantCarryOverInitializer<>(MinicSimpleLiteralNode.MinicIntLiteralNode.class);

    @BeforeClass
    public static void setUp() {
        intStrategy.addValue(new MinicSimpleLiteralNode.MinicIntLiteralNode(55));
        initializer.setStrategy(intStrategy);
    }

    @Test
    public void testConstantCarryOverInitializer() {
        // given
        // nothing

        // when
        TruffleSimpleStrategy<MinicSimpleLiteralNode.MinicIntLiteralNode> strategy = initializer.createStrategy();

        // then
        Assert.assertEquals(strategy.next().executeInt(null), 55);
        Assert.assertEquals(((KnownValueStrategy) strategy).values.size(), 1);
    }

    @Test
    public void testConstantCarryOverInitializerNode() {
        // given
        MinicBlockNode node = new MinicBlockNode(
            new MinicSimpleLiteralNode.MinicIntLiteralNode(3),
            MinicIntArithmeticNodeFactory.MinicIntMulNodeGen.create(
                new MinicSimpleLiteralNode.MinicIntLiteralNode(8),
                MinicReadFunctionArgumentNodeFactory.MinicReadFunctionArgumentIntNodeGen.create(3)
            )
        );

        // when
        TruffleSimpleStrategy<MinicSimpleLiteralNode.MinicIntLiteralNode> strategy = initializer.createStrategy(node);

        // then
        Assert.assertEquals(((KnownValueStrategy) strategy).values.size(), 3);
    }

    @Test
    public void testConstantCarryOverInitializerNodeList() {
        // given
        MinicBlockNode node = new MinicBlockNode(
            new MinicSimpleLiteralNode.MinicIntLiteralNode(3),
            MinicIntArithmeticNodeFactory.MinicIntMulNodeGen.create(
                new MinicSimpleLiteralNode.MinicIntLiteralNode(8),
                MinicReadFunctionArgumentNodeFactory.MinicReadFunctionArgumentIntNodeGen.create(3)
            )
        );
        MinicBlockNode node2 = new MinicBlockNode(
            new MinicSimpleLiteralNode.MinicIntLiteralNode(3),
            MinicIntArithmeticNodeFactory.MinicIntMulNodeGen.create(
                new MinicSimpleLiteralNode.MinicIntLiteralNode(55),
                MinicReadFunctionArgumentNodeFactory.MinicReadFunctionArgumentIntNodeGen.create(3)
            )
        );
        MinicBlockNode node3 = new MinicBlockNode(
            new MinicSimpleLiteralNode.MinicIntLiteralNode(4),
            MinicIntArithmeticNodeFactory.MinicIntMulNodeGen.create(
                new MinicSimpleLiteralNode.MinicIntLiteralNode(5),
                MinicReadFunctionArgumentNodeFactory.MinicReadFunctionArgumentIntNodeGen.create(3)
            )
        );
        List<Node> nodes = new LinkedList<>();
        nodes.add(node);
        nodes.add(node2);
        nodes.add(node3);

        // when
        TruffleSimpleStrategy<MinicSimpleLiteralNode.MinicIntLiteralNode> strategy = initializer.createStrategy(nodes);

        // then
        Assert.assertEquals(((KnownValueStrategy) strategy).values.size(), 5);
    }

}
