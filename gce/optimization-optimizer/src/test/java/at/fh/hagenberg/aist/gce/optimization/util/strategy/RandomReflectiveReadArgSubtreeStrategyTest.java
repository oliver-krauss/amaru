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

import at.fh.hagenberg.aist.gce.minic.language.MinicLanguage;
import at.fh.hagenberg.aist.gce.minic.nodes.base.MinicExpressionNode;
import at.fh.hagenberg.aist.gce.minic.nodes.base.MinicNode;
import at.fh.hagenberg.aist.gce.minic.nodes.impl.control.MinicBlockNode;
import at.fh.hagenberg.aist.gce.minic.nodes.impl.control.MinicWhileNode;
import at.fh.hagenberg.aist.gce.minic.nodes.impl.function.MinicReadFunctionArgumentNodeFactory;
import at.fh.hagenberg.aist.gce.minic.nodes.impl.literals.MinicSimpleLiteralNode;
import at.fh.hagenberg.aist.gce.minic.nodes.impl.op.rel.integer.signed.MinicIntRelationalNodeFactory;
import at.fh.hagenberg.aist.gce.minic.nodes.impl.op.unary.floating.MinicFloatUnaryNodeFactory;
import at.fh.hagenberg.aist.gce.minic.nodes.impl.vars.*;
import at.fh.hagenberg.aist.gce.minic.types.integer.signed.MinicIntNode;
import at.fh.hagenberg.aist.gce.optimization.util.TruffleFunctionSignature;
import at.fh.hagenberg.aist.gce.optimization.util.TruffleLanguageInformation;
import at.fh.hagenberg.aist.gce.optimization.util.TruffleLanguageLearner;
import at.fh.hagenberg.aist.gce.optimization.util.TruffleLanguageSearchSpace;
import at.fh.hagenberg.aist.gce.optimization.util.strategy.other.DefaultFrameSlotStrategy;
import at.fh.hagenberg.aist.gce.optimization.util.strategy.other.StaticObjectStrategy;
import at.fh.hagenberg.aist.gce.optimization.util.strategy.random.RandomReflectiveSubtreeStrategy;
import at.fh.hagenberg.aist.gce.optimization.util.strategy.values.*;
import at.fh.hagenberg.machinelearning.analytics.graph.nodes.DataFlowGraph;
import at.fh.hagenberg.machinelearning.analytics.graph.nodes.DataFlowUtil;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.MaterializedFrame;
import com.oracle.truffle.api.nodes.Node;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Oliver Krauss on 11.08.2020
 */

public class RandomReflectiveReadArgSubtreeStrategyTest {

    private TruffleLanguageSearchSpace searchSpace = null;

    private TruffleLanguageSearchSpace getSearchSpace() {
        if (searchSpace != null) {
            return searchSpace;
        }

        TruffleLanguageInformation information = TruffleLanguageInformation.getLanguageInformation(MinicLanguage.ID);
        if (information.getClass(MinicReadFunctionArgumentNodeFactory.MinicReadFunctionArgumentDoubleNodeGen.class).getArgumentReadClasses().size() == 0) {
            information.getClass(MinicReadFunctionArgumentNodeFactory.MinicReadFunctionArgumentDoubleNodeGen.class).getArgumentReadClasses().add("double");
            information.getClass(MinicReadFunctionArgumentNodeFactory.MinicReadFunctionArgumentFloatNodeGen.class).getArgumentReadClasses().add("float");
            information.getClass(MinicReadFunctionArgumentNodeFactory.MinicReadFunctionArgumentIntNodeGen.class).getArgumentReadClasses().add("int");
            information.getClass(MinicReadFunctionArgumentNodeFactory.MinicReadFunctionArgumentStringNodeGen.class).getArgumentReadClasses().add("java.lang.String");
            information.getClass(MinicReadFunctionArgumentNodeFactory.MinicReadFunctionArgumentCharNodeGen.class).getArgumentReadClasses().add("char");
            information.getClass(MinicReadFunctionArgumentNodeFactory.MinicReadGenericFunctionArgumentNodeGen.class).getArgumentReadClasses().addAll(Arrays.asList("double", "float", "int", "java.lang.String", "char"));
        }
        return searchSpace = new TruffleLanguageSearchSpace(information, null);
    }

    @Test
    public void testRestrictsCorrectlySimple() {
        // given
        CreationConfiguration cc = new CreationConfiguration(4, 4, Double.MAX_VALUE);
        FrameDescriptor localFrameDescriptor = new FrameDescriptor();
        TruffleMasterStrategy strategy = createDataFlowMasterStrategy(localFrameDescriptor);
        DataFlowGraph dfg = new DataFlowGraph(null, null, null, new TruffleFunctionSignature(null, new String[]{"int"}));

        // when
        strategy.setDataFlowGraph(dfg);

        // then
        // check if useless ones were removed
        Assert.assertTrue(strategy.getStrategies().stream().noneMatch(x -> x.getManagedClasses().contains(MinicReadFunctionArgumentNodeFactory.MinicReadFunctionArgumentCharNodeGen.class)));
        Assert.assertTrue(strategy.getStrategies().stream().noneMatch(x -> x.getManagedClasses().contains(MinicReadFunctionArgumentNodeFactory.MinicReadFunctionArgumentStringNodeGen.class)));
        Assert.assertTrue(strategy.getStrategies().stream().noneMatch(x -> x.getManagedClasses().contains(MinicReadFunctionArgumentNodeFactory.MinicReadFunctionArgumentDoubleNodeGen.class)));
        Assert.assertTrue(strategy.getStrategies().stream().noneMatch(x -> x.getManagedClasses().contains(MinicReadFunctionArgumentNodeFactory.MinicReadFunctionArgumentFloatNodeGen.class)));

        // check if they are still there. Unfortunately we can't check the KnownValue indices
        Assert.assertTrue(strategy.getStrategies().stream().anyMatch(x -> x.getManagedClasses().contains(MinicReadFunctionArgumentNodeFactory.MinicReadFunctionArgumentIntNodeGen.class)));
        Assert.assertTrue(strategy.getStrategies().stream().anyMatch(x -> x.getManagedClasses().contains(MinicReadFunctionArgumentNodeFactory.MinicReadGenericFunctionArgumentNodeGen.class)));
    }

    @Test
    public void testRestrictsCorrectlyComplex() {
        // given
        CreationConfiguration cc = new CreationConfiguration(4, 4, Double.MAX_VALUE);
        FrameDescriptor localFrameDescriptor = new FrameDescriptor();
        TruffleMasterStrategy strategy = createDataFlowMasterStrategy(localFrameDescriptor);
        DataFlowGraph dfg = new DataFlowGraph(null, null, null, new TruffleFunctionSignature(null, new String[]{"int", "float", "int", "double"}));

        // when
        strategy.setDataFlowGraph(dfg);

        // then
        // check if useless ones were removed
        Assert.assertTrue(strategy.getStrategies().stream().noneMatch(x -> x.getManagedClasses().contains(MinicReadFunctionArgumentNodeFactory.MinicReadFunctionArgumentCharNodeGen.class)));
        Assert.assertTrue(strategy.getStrategies().stream().noneMatch(x -> x.getManagedClasses().contains(MinicReadFunctionArgumentNodeFactory.MinicReadFunctionArgumentStringNodeGen.class)));

        // check if they are still there. Unfortunately we can't check the KnownValue indices
        Assert.assertTrue(strategy.getStrategies().stream().anyMatch(x -> x.getManagedClasses().contains(MinicReadFunctionArgumentNodeFactory.MinicReadFunctionArgumentDoubleNodeGen.class)));
        Assert.assertTrue(strategy.getStrategies().stream().anyMatch(x -> x.getManagedClasses().contains(MinicReadFunctionArgumentNodeFactory.MinicReadFunctionArgumentFloatNodeGen.class)));
        Assert.assertTrue(strategy.getStrategies().stream().anyMatch(x -> x.getManagedClasses().contains(MinicReadFunctionArgumentNodeFactory.MinicReadFunctionArgumentIntNodeGen.class)));
        Assert.assertTrue(strategy.getStrategies().stream().anyMatch(x -> x.getManagedClasses().contains(MinicReadFunctionArgumentNodeFactory.MinicReadGenericFunctionArgumentNodeGen.class)));
    }

    private TruffleMasterStrategy createDataFlowMasterStrategy(FrameDescriptor f) {
        CreationConfiguration configuration = new CreationConfiguration(4, 4, Double.MAX_VALUE);
        Map<String, TruffleVerifyingStrategy> strategies = new HashMap<>();
        strategies.put("int", new KnownValueStrategy<Integer>(new IntDefault().getValues()));
        strategies.put("char", new KnownValueStrategy<Character>(new CharDefault().getValues()));
        strategies.put("double", new KnownValueStrategy<Double>(new DoubleDefault().getValues()));
        strategies.put("float", new KnownValueStrategy<Float>(new FloatDefault().getValues()));
        strategies.put("java.lang.String", new KnownValueStrategy<String>(new StringDefault().getValues()));
        strategies.put("com.oracle.truffle.api.frame.MaterializedFrame", new StaticObjectStrategy<MaterializedFrame>(null));
        strategies.put("com.oracle.truffle.api.frame.FrameSlot", new DefaultFrameSlotStrategy(f));
        strategies.put("at.fh.hagenberg.aist.gce.minic.language.MinicContext", new StaticObjectStrategy<>(MinicLanguage.getCurrentContext()));
        return TruffleMasterStrategy.createFromTLI(configuration, getSearchSpace(), new ArrayList<>(), strategies);
    }

    @Test
    public void testCanCreateExistingParameter() {
        // given
        CreationConfiguration cc = new CreationConfiguration(4, 4, Double.MAX_VALUE);
        FrameDescriptor localFrameDescriptor = new FrameDescriptor();
        TruffleMasterStrategy strategy = createDataFlowMasterStrategy(localFrameDescriptor);
        DataFlowGraph dfg = new DataFlowGraph(null, null, null, new TruffleFunctionSignature(null, new String[]{"int", "float", "int", "double"}));
        strategy.setDataFlowGraph(dfg);

        // when
        RequirementInformation requirementInformation = strategy.canCreate(new CreationInformation(null, null, new RequirementInformation(null), dfg, MinicReadFunctionArgumentNodeFactory.MinicReadFunctionArgumentIntNodeGen.class, 0, cc));
        RequirementInformation requirementInformation2 = strategy.canCreate(new CreationInformation(null, null, new RequirementInformation(null), dfg, MinicReadFunctionArgumentNodeFactory.MinicReadFunctionArgumentFloatNodeGen.class, 0, cc));
        RequirementInformation requirementInformation3 = strategy.canCreate(new CreationInformation(null, null, new RequirementInformation(null), dfg, MinicReadFunctionArgumentNodeFactory.MinicReadGenericFunctionArgumentNodeGen.class, 0, cc));

        Assert.assertNotNull(requirementInformation);
        Assert.assertTrue(requirementInformation.fullfillsAll());
        Assert.assertNotNull(requirementInformation2);
        Assert.assertTrue(requirementInformation2.fullfillsAll());
        Assert.assertNotNull(requirementInformation3);
        Assert.assertTrue(requirementInformation3.fullfillsAll());
    }

    @Test
    public void testCanCreateNotExistingParameter() {
        // given
        CreationConfiguration cc = new CreationConfiguration(4, 4, Double.MAX_VALUE);
        FrameDescriptor localFrameDescriptor = new FrameDescriptor();
        TruffleMasterStrategy strategy = createDataFlowMasterStrategy(localFrameDescriptor);
        DataFlowGraph dfg = new DataFlowGraph(null, null, null, new TruffleFunctionSignature(null, new String[]{"int", "float", "int", "double"}));
        strategy.setDataFlowGraph(dfg);

        // when
        RequirementInformation requirementInformation = strategy.canCreate(new CreationInformation(null, null, new RequirementInformation(null), dfg, MinicReadFunctionArgumentNodeFactory.MinicReadFunctionArgumentStringNodeGen.class, 0, cc));
        RequirementInformation requirementInformation2 = strategy.canCreate(new CreationInformation(null, null, new RequirementInformation(null), dfg, MinicReadFunctionArgumentNodeFactory.MinicReadFunctionArgumentCharNodeGen.class, 0, cc));

        Assert.assertNull(requirementInformation);
        Assert.assertNull(requirementInformation2);
    }

    @Test
    public void testCreateExistingParameter() {
        // given
        CreationConfiguration cc = new CreationConfiguration(4, 4, Double.MAX_VALUE);
        FrameDescriptor localFrameDescriptor = new FrameDescriptor();
        TruffleMasterStrategy strategy = createDataFlowMasterStrategy(localFrameDescriptor);
        DataFlowGraph dfg = new DataFlowGraph(null, null, null, new TruffleFunctionSignature(null, new String[]{"int", "float", "int", "double"}));
        strategy.setDataFlowGraph(dfg);

        // when
        Node intNode = strategy.create(new CreationInformation(null, null, new RequirementInformation(null), dfg, MinicReadFunctionArgumentNodeFactory.MinicReadFunctionArgumentIntNodeGen.class, 0, cc));
        Node floatNode = strategy.create(new CreationInformation(null, null, new RequirementInformation(null), dfg, MinicReadFunctionArgumentNodeFactory.MinicReadFunctionArgumentFloatNodeGen.class, 0, cc));
        Node genericNode = strategy.create(new CreationInformation(null, null, new RequirementInformation(null), dfg, MinicReadFunctionArgumentNodeFactory.MinicReadGenericFunctionArgumentNodeGen.class, 0, cc));

        Assert.assertNotNull(intNode);
        Assert.assertEquals(intNode.getClass(), MinicReadFunctionArgumentNodeFactory.MinicReadFunctionArgumentIntNodeGen.class);
        MinicReadFunctionArgumentNodeFactory.MinicReadFunctionArgumentIntNodeGen castIntNode = (MinicReadFunctionArgumentNodeFactory.MinicReadFunctionArgumentIntNodeGen) intNode;
        Assert.assertTrue(castIntNode.getIndex() == 0 || castIntNode.getIndex() == 2);
        Assert.assertNotNull(floatNode);
        Assert.assertEquals(floatNode.getClass(), MinicReadFunctionArgumentNodeFactory.MinicReadFunctionArgumentFloatNodeGen.class);
        MinicReadFunctionArgumentNodeFactory.MinicReadFunctionArgumentFloatNodeGen castFloatNode = (MinicReadFunctionArgumentNodeFactory.MinicReadFunctionArgumentFloatNodeGen) floatNode;
        Assert.assertEquals(((MinicReadFunctionArgumentNodeFactory.MinicReadFunctionArgumentFloatNodeGen) floatNode).getIndex(), 1);
        Assert.assertNotNull(genericNode);
        Assert.assertEquals(genericNode.getClass(), MinicReadFunctionArgumentNodeFactory.MinicReadGenericFunctionArgumentNodeGen.class);
        MinicReadFunctionArgumentNodeFactory.MinicReadGenericFunctionArgumentNodeGen castGenericNode = (MinicReadFunctionArgumentNodeFactory.MinicReadGenericFunctionArgumentNodeGen) genericNode;
        Assert.assertTrue(castGenericNode.getIndex() >= 0 && castGenericNode.getIndex() <= 3);
    }

    @Test
    public void testCreateNotExistingParameter() {
        // given
        CreationConfiguration cc = new CreationConfiguration(4, 4, Double.MAX_VALUE);
        FrameDescriptor localFrameDescriptor = new FrameDescriptor();
        TruffleMasterStrategy strategy = createDataFlowMasterStrategy(localFrameDescriptor);
        DataFlowGraph dfg = new DataFlowGraph(null, null, null, new TruffleFunctionSignature(null, new String[]{"int", "float", "int", "double"}));
        strategy.setDataFlowGraph(dfg);

        // when
        Exception eString = null, eChar = null;
        try {
            Node stringNode = strategy.create(new CreationInformation(null, null, new RequirementInformation(null), dfg, MinicReadFunctionArgumentNodeFactory.MinicReadFunctionArgumentStringNodeGen.class, 0, cc));
        } catch (Exception e) {
            eString = e;
        }
        try {
            Node charNode = strategy.create(new CreationInformation(null, null, new RequirementInformation(null), dfg, MinicReadFunctionArgumentNodeFactory.MinicReadFunctionArgumentCharNodeGen.class, 0, cc));
        } catch (Exception e) {
            eChar = e;
        }

        Assert.assertNotNull(eString);
        Assert.assertNotNull(eChar);
    }

}
