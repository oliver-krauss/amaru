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


import at.fh.hagenberg.aist.gce.minic.language.MinicContext;
import at.fh.hagenberg.aist.gce.minic.language.MinicLanguage;
import at.fh.hagenberg.aist.gce.minic.nodes.impl.control.MinicBlockNode;
import at.fh.hagenberg.aist.gce.minic.nodes.impl.literals.MinicSimpleLiteralNode;
import at.fh.hagenberg.aist.gce.minic.nodes.impl.op.arith.integer.signed.MinicIntArithmeticNode;
import at.fh.hagenberg.aist.gce.minic.nodes.impl.vars.AllocateArrayNode;
import at.fh.hagenberg.aist.gce.optimization.util.TruffleLanguageInformation;
import at.fh.hagenberg.aist.gce.optimization.util.TruffleLanguageSearchSpace;
import at.fh.hagenberg.aist.gce.optimization.util.strategy.other.DefaultFrameSlotStrategy;
import at.fh.hagenberg.aist.gce.optimization.util.strategy.other.StaticObjectStrategy;
import at.fh.hagenberg.aist.gce.optimization.util.strategy.values.*;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.FrameSlotTypeException;
import com.oracle.truffle.api.frame.MaterializedFrame;
import com.oracle.truffle.api.nodes.Node;
import org.graalvm.polyglot.Context;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;


/**
 * @author Oliver Krauss on 07.11.2018
 */

public class TruffleMasterStrategyTest extends AbstractTestNeedingMasterStrategyTest {

    @Test
    public void testInitializeMasterStrategy() {
        // given
        // all in create as this repeats for every test;

        // when
        TruffleMasterStrategy strategy = create();

        // then
        Assert.assertNotNull(strategy);
    }

    @Test
    public void testCanCreate() {
        // given
        TruffleMasterStrategy strategy = create();

        // when
        RequirementInformation canCreateObject = strategy.canCreate(new CreationInformation(null, null, new RequirementInformation(null), null, Object.class, 0, new CreationConfiguration(5, 5, Double.MAX_VALUE)));
        RequirementInformation canCreateNothing = strategy.canCreate(new CreationInformation(null, null, new RequirementInformation(null), null, Object.class, 0, new CreationConfiguration(0, 0, Double.MAX_VALUE)));
        RequirementInformation canCreateNode = strategy.canCreate(new CreationInformation(null, null, new RequirementInformation(null), null, Node.class, 0, new CreationConfiguration(5, 5, Double.MAX_VALUE)));
        RequirementInformation canCreateNonNode = strategy.canCreate(new CreationInformation(null, null, new RequirementInformation(null), null, this.getClass(), 0, new CreationConfiguration(5, 5, Double.MAX_VALUE)));
        RequirementInformation canCreateIntLiteral = strategy.canCreate(new CreationInformation(null, null, new RequirementInformation(null), null, MinicSimpleLiteralNode.MinicIntLiteralNode.class, 0, new CreationConfiguration(5, 5, Double.MAX_VALUE)));

        // then
        Assert.assertNotNull(canCreateObject);
        Assert.assertNotNull(canCreateNothing); // because we always allow creation of a terminal!
        Assert.assertNotNull(canCreateNode);
        Assert.assertNull(canCreateNonNode);
        Assert.assertNotNull(canCreateIntLiteral);
    }

    @Test(expectedExceptions = RuntimeException.class)
    public void testCreateTerminal() throws ClassNotFoundException, InstantiationException, IllegalAccessException, InvocationTargetException {
        // given
        TruffleMasterStrategy strategy = create();

        // when
        Node node = strategy.create(new CreationInformation(null, null, new RequirementInformation(null), null, Integer.class, 0, new CreationConfiguration(5, 5, Double.MAX_VALUE)));

        // then
        Assert.assertNull(node);
    }

    @Test
    public void testCreateNonterminal() throws ClassNotFoundException, InstantiationException, IllegalAccessException, InvocationTargetException {
        // given
        TruffleMasterStrategy strategy = create();

        // when
        Node node = strategy.create(new CreationInformation(null, null, new RequirementInformation(null), null, MinicIntArithmeticNode.MinicIntMulNode.class, 0, new CreationConfiguration(5, 5, Double.MAX_VALUE)));

        // then
        Assert.assertNotNull(node);
    }

    @Test
    public void testCreatePseudoTerminalNoDepthLeft() throws ClassNotFoundException, InstantiationException, IllegalAccessException, InvocationTargetException {
        // given
        TruffleMasterStrategy strategy = create();

        // when
        Node node = strategy.create(new CreationInformation(null, null, new RequirementInformation(null), null, MinicSimpleLiteralNode.MinicIntLiteralNode.class, 0, new CreationConfiguration(5, 5, Double.MAX_VALUE)));

        // then
        Assert.assertNotNull(node);
    }

    @Test(expectedExceptions = RuntimeException.class)
    public void testCreateNonterminalNoDepthLeft() throws ClassNotFoundException, InstantiationException, IllegalAccessException, InvocationTargetException {
        // given
        TruffleMasterStrategy strategy = create();

        // when
        Node node = strategy.create(new CreationInformation(null, null, new RequirementInformation(null), null, MinicIntArithmeticNode.MinicIntMulNode.class, 0, new CreationConfiguration(0, 0, Double.MAX_VALUE)));

        // then
        // nothing. this is supposed to fail
    }

    @Test
    public void testCreateArray() throws ClassNotFoundException, InstantiationException, IllegalAccessException, InvocationTargetException {
        // given
        TruffleMasterStrategy strategy = create();

        // when
        Node node = strategy.create(new CreationInformation(null, null, new RequirementInformation(null), null, MinicBlockNode.class, 0, new CreationConfiguration(5, 5, Double.MAX_VALUE)));

        // then
        Assert.assertNotNull(node);
    }

    @Test
    public void testCreateTerminalArray() throws ClassNotFoundException, InstantiationException, IllegalAccessException, InvocationTargetException {
        // given
        TruffleMasterStrategy strategy = create();

        // when
        Node node = strategy.create(new CreationInformation(null, null, new RequirementInformation(null), null, AllocateArrayNode.MinicAllocateIntArrayNode.class, 0, new CreationConfiguration(5, 5, Double.MAX_VALUE)));

        // then
        Assert.assertNotNull(node);
    }

    @Test
    public void testCreateObject() throws ClassNotFoundException, InstantiationException, IllegalAccessException, InvocationTargetException {
        // given
        TruffleMasterStrategy strategy = create();

        // when
        Node node = strategy.create(new CreationInformation(null, null, new RequirementInformation(null), null, Object.class, 0, new CreationConfiguration(5, 5, Double.MAX_VALUE)));

        // then
        Assert.assertNotNull(node);
    }

    @Test
    public void testCreateRandom() {
        // given
        TruffleMasterStrategy strategy = create();

        // when
        Node n = strategy.next();

        // then
        Assert.assertNotNull(n);
    }

    @Test
    public void testCorrectStateFromStart() {
        // given
        KnownValueStrategy<Integer> intStrat = new KnownValueStrategy<Integer>(new IntDefault().getValues());
        CreationConfiguration configuration = new CreationConfiguration(10, 10, Double.MAX_VALUE);
        TruffleLanguageInformation information = TruffleLanguageInformation.getLanguageInformation(MinicLanguage.ID);
        Map<String, TruffleVerifyingStrategy> strategies = new HashMap<>();
        strategies.put("char", new KnownValueStrategy<Character>(new CharDefault().getValues()));
        strategies.put("double", new KnownValueStrategy<Double>(new DoubleDefault().getValues()));
        strategies.put("float", new KnownValueStrategy<Float>(new FloatDefault().getValues()));
        strategies.put("java.lang.String", new KnownValueStrategy<String>(new StringDefault().getValues()));
        strategies.put("com.oracle.truffle.api.frame.MaterializedFrame", new StaticObjectStrategy<MaterializedFrame>(null));
        FrameDescriptor f = new FrameDescriptor();
        f.addFrameSlot("asdf");
        strategies.put("com.oracle.truffle.api.frame.FrameSlot", new DefaultFrameSlotStrategy(f));
        strategies.put("at.fh.hagenberg.aist.gce.minic.language.MinicContext", new StaticObjectStrategy<MinicContext>(MinicLanguage.getCurrentContext()));
        TruffleMasterStrategy master = TruffleMasterStrategy.createFromTLI(configuration, new TruffleLanguageSearchSpace(information, null), new ArrayList<>(), strategies);

        // when
        intStrat.getValues().forEach(x -> intStrat.removeValue(x));
        master.invalidateCache();

        // then
        Assert.assertNull(master.canCreate(new CreationInformation(null, null, new RequirementInformation(null), null, MinicSimpleLiteralNode.MinicIntLiteralNode.class, 0, new CreationConfiguration(5, 5, Double.MAX_VALUE))));
    }

    @Test
    public void testSelfRepairDisable() {
        // given
        KnownValueStrategy<Integer> intStrat = new KnownValueStrategy<Integer>(new IntDefault().getValues());
        CreationConfiguration configuration = new CreationConfiguration(10, 10, Double.MAX_VALUE);
        TruffleLanguageInformation information = TruffleLanguageInformation.getLanguageInformation(MinicLanguage.ID);
        Map<String, TruffleVerifyingStrategy> strategies = new HashMap<>();
        strategies.put("int", intStrat);
        strategies.put("char", new KnownValueStrategy<Character>(new CharDefault().getValues()));
        strategies.put("double", new KnownValueStrategy<Double>(new DoubleDefault().getValues()));
        strategies.put("float", new KnownValueStrategy<Float>(new FloatDefault().getValues()));
        strategies.put("java.lang.String", new KnownValueStrategy<String>(new StringDefault().getValues()));
        strategies.put("com.oracle.truffle.api.frame.MaterializedFrame", new StaticObjectStrategy<MaterializedFrame>(null));
        FrameDescriptor f = new FrameDescriptor();
        f.addFrameSlot("asdf");
        strategies.put("com.oracle.truffle.api.frame.FrameSlot", new DefaultFrameSlotStrategy(f));
        strategies.put("at.fh.hagenberg.aist.gce.minic.language.MinicContext", new StaticObjectStrategy<MinicContext>(MinicLanguage.getCurrentContext()));
        TruffleMasterStrategy master = TruffleMasterStrategy.createFromTLI(configuration, new TruffleLanguageSearchSpace(information, null), new ArrayList<>(), strategies);

        // when
        intStrat.getValues().forEach(x -> intStrat.removeValue(x));
        master.invalidateCache();

        // then
        Assert.assertNull(master.canCreate(new CreationInformation(null, null, new RequirementInformation(null), null, MinicSimpleLiteralNode.MinicIntLiteralNode.class, 0, new CreationConfiguration(5, 5, Double.MAX_VALUE))));
    }

    @Test
    public void testSelfRepairReEnable() {
        // given
        KnownValueStrategy<Integer> intStrat = new KnownValueStrategy<Integer>(new IntDefault().getValues());
        CreationConfiguration configuration = new CreationConfiguration(10, 10, Double.MAX_VALUE);
        TruffleLanguageInformation information = TruffleLanguageInformation.getLanguageInformation(MinicLanguage.ID);
        Map<String, TruffleVerifyingStrategy> strategies = new HashMap<>();
        strategies.put("int", intStrat);
        strategies.put("char", new KnownValueStrategy<Character>(new CharDefault().getValues()));
        strategies.put("double", new KnownValueStrategy<Double>(new DoubleDefault().getValues()));
        strategies.put("float", new KnownValueStrategy<Float>(new FloatDefault().getValues()));
        strategies.put("java.lang.String", new KnownValueStrategy<String>(new StringDefault().getValues()));
        strategies.put("com.oracle.truffle.api.frame.MaterializedFrame", new StaticObjectStrategy<MaterializedFrame>(null));
        FrameDescriptor f = new FrameDescriptor();
        f.addFrameSlot("asdf");
        strategies.put("com.oracle.truffle.api.frame.FrameSlot", new DefaultFrameSlotStrategy(f));
        strategies.put("at.fh.hagenberg.aist.gce.minic.language.MinicContext", new StaticObjectStrategy<MinicContext>(MinicLanguage.getCurrentContext()));
        TruffleMasterStrategy master = TruffleMasterStrategy.createFromTLI(configuration, new TruffleLanguageSearchSpace(information, null), new ArrayList<>(), strategies);

        // when
        intStrat.getValues().forEach(x -> intStrat.removeValue(x));
        master.invalidateCache();
        Assert.assertNull(master.canCreate(new CreationInformation(null, null, new RequirementInformation(null), null, MinicSimpleLiteralNode.MinicIntLiteralNode.class, 0, new CreationConfiguration(5, 5, Double.MAX_VALUE))));
        intStrat.addValue(234);
        master.invalidateCache();

        // then
        Assert.assertNotNull(master.canCreate(new CreationInformation(null, null, new RequirementInformation(null), null, MinicSimpleLiteralNode.MinicIntLiteralNode.class, 0, new CreationConfiguration(5, 5, Double.MAX_VALUE))));
    }

}
