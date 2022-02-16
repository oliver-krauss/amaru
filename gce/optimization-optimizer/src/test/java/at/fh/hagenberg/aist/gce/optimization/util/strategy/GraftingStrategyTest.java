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


import at.fh.hagenberg.aist.gce.optimization.runtime.SystemInformation;
import at.fh.hagenberg.aist.gce.optimization.util.TruffleLanguageInformation;
import at.fh.hagenberg.machinelearning.analytics.graph.nodes.NodeWrapperWeightUtil;
import com.oracle.truffle.api.nodes.Node;
import at.fh.hagenberg.aist.gce.minic.language.MinicLanguage;
import at.fh.hagenberg.aist.gce.minic.nodes.impl.control.MinicBlockNode;
import at.fh.hagenberg.aist.gce.minic.nodes.impl.literals.MinicSimpleLiteralNode;
import at.fh.hagenberg.aist.gce.minic.nodes.impl.op.arith.integer.signed.MinicIntArithmeticNodeFactory;
import at.fh.hagenberg.aist.gce.optimization.util.strategy.other.GraftingStrategy;
import at.fh.hagenberg.aist.gce.minic.types.integer.signed.MinicCharNode;
import at.fh.hagenberg.aist.gce.minic.types.integer.signed.MinicIntNode;
import org.graalvm.polyglot.Context;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.lang.reflect.InvocationTargetException;
import java.util.LinkedList;
import java.util.List;

/**
 * @author Oliver Krauss on 07.11.2018
 */

public class GraftingStrategyTest {

    @BeforeClass
    public void setUp() {
        TruffleLanguageInformation.getLanguageInformation(MinicLanguage.ID).getInstantiableNodes().values().forEach(x -> {
            if (!x.getWeight().containsKey(SystemInformation.getCurrentSystem())) {
                x.getWeight().put(SystemInformation.getCurrentSystem(), 2.2);
            }
        });
    }

    private GraftingStrategy create() {
        List<Node> values = new LinkedList<Node>();

        // depth 1
        values.add(new MinicSimpleLiteralNode.MinicIntLiteralNode(3));

        // depth 2
        values.add(MinicIntArithmeticNodeFactory.MinicIntMulNodeGen.create(new MinicSimpleLiteralNode.MinicIntLiteralNode(2), new MinicSimpleLiteralNode.MinicIntLiteralNode(5)));

        // depth 2
        values.add(MinicIntArithmeticNodeFactory.MinicIntMulNodeGen.create(new MinicSimpleLiteralNode.MinicIntLiteralNode(3), new MinicSimpleLiteralNode.MinicIntLiteralNode(588)));

        // depth 4
        values.add(new MinicBlockNode(new MinicBlockNode(MinicIntArithmeticNodeFactory.MinicIntMulNodeGen.create(new MinicSimpleLiteralNode.MinicIntLiteralNode(2), new MinicSimpleLiteralNode.MinicIntLiteralNode(5)))));

        NodeWrapperWeightUtil weightUtil = new NodeWrapperWeightUtil(MinicLanguage.ID);

        return new GraftingStrategy(values, weightUtil);
    }

    @Test
    public void testAdd() {
        // given
        GraftingStrategy strategy = create();

        // when
        strategy.addValue(new MinicBlockNode(new MinicBlockNode(MinicIntArithmeticNodeFactory.MinicIntMulNodeGen.create(new MinicSimpleLiteralNode.MinicIntLiteralNode(45), new MinicSimpleLiteralNode.MinicIntLiteralNode(898)))));

        // then
        Assert.assertEquals(strategy.getValues().size(), 5);
    }

    @Test()
    public void testRemove() {
        // given
        GraftingStrategy strategy = create();

        // when
        strategy.removeValue(new MinicBlockNode(new MinicBlockNode(MinicIntArithmeticNodeFactory.MinicIntMulNodeGen.create(new MinicSimpleLiteralNode.MinicIntLiteralNode(45), new MinicSimpleLiteralNode.MinicIntLiteralNode(898)))));

        // then
        Assert.assertEquals(strategy.getValues().size(), 4);
    }

    @Test
    public void testCanCreateGeneralized() {
        // given
        GraftingStrategy strategy = create();
        CreationInformation information = new CreationInformation(null, null, null, null, Object.class, 0, new CreationConfiguration(10, 10, Double.MAX_VALUE));

        // when
        RequirementInformation can = strategy.canCreate(information);

        // then
        Assert.assertNotNull(can);
    }

    @Test
    public void testCanCreateSpezialized() {
        // given
        GraftingStrategy strategy = create();
        CreationInformation information = new CreationInformation(null, null, null,null, MinicIntNode.class, 0, new CreationConfiguration(10, 10, Double.MAX_VALUE));

        // when
        RequirementInformation can = strategy.canCreate(information);

        // then
        Assert.assertNotNull(can);
    }

    @Test
    public void testCantCreateMissing() {
        // given
        GraftingStrategy strategy = create();
        CreationInformation information = new CreationInformation(null, null, null,null, MinicCharNode.class, 0, new CreationConfiguration(10, 10, Double.MAX_VALUE));

        // when
        RequirementInformation can = strategy.canCreate(information);

        // then
        Assert.assertNull(can);
    }


    @Test
    public void testCanCreateDepth() {
        // given
        GraftingStrategy strategy = create();
        CreationInformation information = new CreationInformation(null, null, null,null, MinicBlockNode.class, 5, new CreationConfiguration(10, 10, Double.MAX_VALUE));

        // when
        RequirementInformation can = strategy.canCreate(information);

        // then
        Assert.assertNotNull(can);
    }

    @Test
    public void testCantCreateDepth() {
        // given
        GraftingStrategy strategy = create();
        CreationInformation information = new CreationInformation(null, null, null,null, MinicBlockNode.class, 6, new CreationConfiguration(10, 10, Double.MAX_VALUE));

        // when
        RequirementInformation can = strategy.canCreate(information);

        // then
        Assert.assertNull(can);
    }

    @Test
    public void testCanCreateWeight() {
        // given
        GraftingStrategy strategy = create();
        CreationInformation information = new CreationInformation(null, null, null,null, MinicIntNode.class, 1, new CreationConfiguration(10, 10, 2.2));

        // when
        RequirementInformation can = strategy.canCreate(information);

        // then
        Assert.assertNotNull(can);
    }

    @Test
    public void testCantCreateWeight() {
        // given
        GraftingStrategy strategy = create();
        CreationInformation information = new CreationInformation(null, null, null,null, MinicIntNode.class, 1, new CreationConfiguration(10, 10, 2.1));

        // when
        RequirementInformation can = strategy.canCreate(information);

        // then
        Assert.assertNull(can);
    }

    @Test
    public void testCreate() throws InvocationTargetException, ClassNotFoundException, InstantiationException, IllegalAccessException {
        // given
        GraftingStrategy strategy = create();
        CreationInformation information = new CreationInformation(null, null, null,null, MinicBlockNode.class, 5, new CreationConfiguration(10, 10, Double.MAX_VALUE));

        // when
        Node n = strategy.create(information);

        // then
        Assert.assertNotNull(n);
    }

    @Test
    public void testCreateWeight() throws InvocationTargetException, ClassNotFoundException, InstantiationException, IllegalAccessException {
        // given
        GraftingStrategy strategy = create();
        CreationInformation information = new CreationInformation(null, null, null,null, MinicIntNode.class, 5, new CreationConfiguration(10, 10, 2.3));

        // when
        Node n = strategy.create(information);

        // then
        Assert.assertNotNull(n);
        Assert.assertEquals(n.getClass(), MinicSimpleLiteralNode.MinicIntLiteralNode.class);
    }

}
