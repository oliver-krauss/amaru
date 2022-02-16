/*
 * Copyright (c) 2022 the original author or authors.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

package at.fh.hagenberg.aist.gce.optimization.util.strategy.initializer;

import at.fh.hagenberg.aist.gce.minic.language.MinicContext;
import at.fh.hagenberg.aist.gce.minic.language.MinicLanguage;
import at.fh.hagenberg.aist.gce.minic.nodes.base.MinicExpressionNode;
import at.fh.hagenberg.aist.gce.minic.nodes.base.MinicNode;
import at.fh.hagenberg.aist.gce.minic.nodes.base.MinicRootNode;
import at.fh.hagenberg.aist.gce.minic.nodes.builtin.LengthNode;
import at.fh.hagenberg.aist.gce.minic.nodes.builtin.LengthNodeFactory;
import at.fh.hagenberg.aist.gce.minic.nodes.builtin.PrintNodeFactory;
import at.fh.hagenberg.aist.gce.minic.nodes.impl.control.MinicBlockNode;
import at.fh.hagenberg.aist.gce.minic.nodes.impl.control.MinicWhileNode;
import at.fh.hagenberg.aist.gce.minic.nodes.impl.function.MinicReadFunctionArgumentNodeFactory;
import at.fh.hagenberg.aist.gce.minic.nodes.impl.literals.MinicSimpleLiteralNode;
import at.fh.hagenberg.aist.gce.minic.nodes.impl.vars.*;
import at.fh.hagenberg.aist.gce.minic.types.integer.signed.MinicIntNode;
import at.fh.hagenberg.aist.gce.optimization.util.ExtendedNodeUtil;
import at.fh.hagenberg.aist.gce.optimization.util.TruffleLanguageInformation;
import at.fh.hagenberg.aist.gce.optimization.util.TruffleLanguageSearchSpace;
import at.fh.hagenberg.aist.gce.optimization.util.strategy.*;
import at.fh.hagenberg.aist.gce.optimization.util.strategy.other.DefaultFrameSlotStrategy;
import at.fh.hagenberg.aist.gce.optimization.util.strategy.other.StaticObjectStrategy;
import at.fh.hagenberg.aist.gce.optimization.util.strategy.values.*;
import at.fh.hagenberg.machinelearning.analytics.graph.nodes.DataFlowUtil;
import at.fh.hagenberg.machinelearning.analytics.graph.nodes.NodeWrapper;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.MaterializedFrame;
import com.oracle.truffle.api.nodes.Node;
import freemarker.core.UnexpectedTypeException;
import org.graalvm.polyglot.Context;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author Oliver Krauss on 04.01.2019
 */
public class TruffleEntryPointStrategyTest {

    @BeforeClass
    public static void setUp() {
        Context context = Context.newBuilder().out(System.out).build();
        context.initialize(MinicLanguage.ID);
    }


    private TruffleLanguageSearchSpace searchSpace = null;

    private TruffleLanguageSearchSpace getSearchSpace() {
        if (searchSpace != null) {
            return searchSpace;
        }

        TruffleLanguageInformation tli = TruffleLanguageInformation.getLanguageInformation(MinicLanguage.ID);

        // we have a problem here. The TLI only contains the data on argument classes when it has been set up
        tli = TruffleLanguageInformation.getLanguageInformation(MinicLanguage.ID);
        if (tli.getClass(MinicReadNodeFactory.MinicCharReadNodeGen.class).getWritePairings().size() == 0) {
            tli.getClass(MinicReadNodeFactory.MinicCharReadNodeGen.class).getWritePairings().add(tli.getClass(MinicWriteNodeFactory.MinicCharWriteNodeGen.class));
            tli.getClass(MinicReadNodeFactory.MinicIntReadNodeGen.class).getWritePairings().add(tli.getClass(MinicWriteNodeFactory.MinicIntWriteNodeGen.class));
            tli.getClass(MinicReadNodeFactory.MinicDoubleReadNodeGen.class).getWritePairings().add(tli.getClass(MinicWriteNodeFactory.MinicDoubleWriteNodeGen.class));
            tli.getClass(MinicReadNodeFactory.MinicFloatReadNodeGen.class).getWritePairings().add(tli.getClass(MinicWriteNodeFactory.MinicFloatWriteNodeGen.class));
            tli.getClass(MinicReadNodeFactory.MinicStringReadNodeGen.class).getWritePairings().add(tli.getClass(MinicWriteNodeFactory.MinicStringWriteNodeGen.class));
        }

        return searchSpace = new TruffleLanguageSearchSpace(tli, null);
    }

    TruffleMasterStrategy masterStrategy;

    private TruffleMasterStrategy createMasterStrategy() {
        if (masterStrategy != null) {
            return masterStrategy;
        }

        CreationConfiguration configuration = new CreationConfiguration(10, 10, Double.MAX_VALUE);

        Map<String, TruffleVerifyingStrategy> strategies = new HashMap<>();
        strategies.put("int", new KnownValueStrategy<Integer>(new IntDefault().getValues()));
        strategies.put("char", new KnownValueStrategy<Character>(new CharDefault().getValues()));
        strategies.put("double", new KnownValueStrategy<Double>(new DoubleDefault().getValues()));
        strategies.put("float", new KnownValueStrategy<Float>(new FloatDefault().getValues()));
        strategies.put("java.lang.String", new KnownValueStrategy<String>(new StringDefault().getValues()));
        strategies.put("com.oracle.truffle.api.frame.MaterializedFrame", new StaticObjectStrategy<MaterializedFrame>(null));
        FrameDescriptor f = new FrameDescriptor();
        f.addFrameSlot("asdf");
        strategies.put("com.oracle.truffle.api.frame.FrameSlot", new DefaultFrameSlotStrategy(f));
        strategies.put("at.fh.hagenberg.aist.gce.minic.language.MinicContext", new StaticObjectStrategy<MinicContext>(MinicLanguage.getCurrentContext()));
        return masterStrategy = TruffleMasterStrategy.createFromTLI(configuration, getSearchSpace(), new ArrayList<>(), strategies);
    }

    @Test
    public void testInitializeEntryPointStrategyNoParent() {
        // given
        TruffleMasterStrategy ms = createMasterStrategy();
        CreationConfiguration cc = new CreationConfiguration(10, 10, Double.MAX_VALUE);
        Node node = new MinicBlockNode();

        // when
        TruffleEntryPointStrategy strategy = new TruffleEntryPointStrategy(getSearchSpace(), null, node, ms, cc);

        // then
        Assert.assertNotNull(strategy);
        Assert.assertEquals(ms.getManagedClasses().size(), strategy.getManagedClasses().size());
    }

    @Test
    public void testInitializeEntryPointStrategyChild() {
        // given
        TruffleMasterStrategy ms = createMasterStrategy();
        CreationConfiguration cc = new CreationConfiguration(10, 10, Double.MAX_VALUE);
        MinicExpressionNode node = new MinicSimpleLiteralNode.MinicIntLiteralNode(1);
        Node parent = new MinicRootNode(MinicLanguage.INSTANCE, null, null, node, "test");
        parent.adoptChildren();

        // when
        TruffleEntryPointStrategy strategy = new TruffleEntryPointStrategy(getSearchSpace(), null, node, ms, cc);

        // then
        Assert.assertNotNull(strategy);
        Assert.assertNotEquals(ms.getManagedClasses().size(), strategy.getManagedClasses().size());
    }

    @Test
    public void testInitializeEntryPointStrategyChildren() {
        // given
        TruffleMasterStrategy ms = createMasterStrategy();
        CreationConfiguration cc = new CreationConfiguration(10, 10, Double.MAX_VALUE);
        MinicNode a = new MinicSimpleLiteralNode.MinicCharLiteralNode('c');
        MinicExpressionNode node = new MinicSimpleLiteralNode.MinicIntLiteralNode(1);
        MinicNode b = new MinicBlockNode();
        Node parent = new MinicBlockNode(a, node, b);
        parent.adoptChildren();

        // when
        TruffleEntryPointStrategy strategy = new TruffleEntryPointStrategy(getSearchSpace(), null, node, ms, cc);

        // then
        Assert.assertNotNull(strategy);
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
        strategies.put("at.fh.hagenberg.aist.gce.minic.language.MinicContext", new StaticObjectStrategy<MinicContext>(MinicLanguage.getCurrentContext()));
        return TruffleMasterStrategy.createFromTLI(configuration, getSearchSpace(), new ArrayList<>(), strategies);
    }

    @Test
    public void testFixDataFlowGraph() {
        // given
        CreationConfiguration cc = new CreationConfiguration(4, 4, Double.MAX_VALUE);

        FrameDescriptor globalFrameDescriptor = new FrameDescriptor();
        MaterializedFrame globalFrame = Truffle.getRuntime().createMaterializedFrame(null, globalFrameDescriptor);
        FrameSlot slotA = globalFrameDescriptor.findOrAddFrameSlot("intVar");
        FrameDescriptor localFrameDescriptor = new FrameDescriptor();
        FrameSlot slotB = localFrameDescriptor.findOrAddFrameSlot("charVar");
        FrameSlot slotC = localFrameDescriptor.findOrAddFrameSlot("dblVar");
        MinicWriteNode.MinicCharWriteNode breakoff = MinicWriteNodeFactory.MinicCharWriteNodeGen.create(new MinicSimpleLiteralNode.MinicCharLiteralNode('c'), slotB);
        TruffleMasterStrategy ms = createDataFlowMasterStrategy(localFrameDescriptor);

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
        n.adoptChildren();

        // when
        TruffleEntryPointStrategy strategy = new TruffleEntryPointStrategy(getSearchSpace(), null, breakoff, ms, cc);
        strategy.setDataFlowGraph(DataFlowUtil.constructDataFlowGraph(getSearchSpace().getInformation(), n, breakoff, null));
        Node created = strategy.next();
        breakoff.replace(created);

        // then
        Assert.assertNotNull(created);
        boolean satisfied = DataFlowUtil.findUnsatisfiedDataItems(getSearchSpace().getInformation(), n, null).isEmpty() ||
            ExtendedNodeUtil.flatten(created) // TODO #179 -> We sometimes create nodes that are disregarded by "overfilling" arrays
                .anyMatch(x -> x.getClass().equals(PrintNodeFactory.PrintNodeGen.class)
                    || x.getClass().equals(LengthNodeFactory.LengthNodeGen.class));
        if (!satisfied) {
            System.out.println(NodeWrapper.wrap(created).humanReadableTree());
        }
        Assert.assertTrue(satisfied);
        // validate if we managed to insert the missing char write.
        boolean charWriteInjected = ExtendedNodeUtil.flatten(created).anyMatch(x -> x.getClass().equals(MinicWriteNodeFactory.MinicCharWriteNodeGen.class) ||
                // TODO #179 -> We sometimes create nodes that are disregarded by "overfilling" arrays
                x.getClass().equals(PrintNodeFactory.PrintNodeGen.class)
                || x.getClass().equals(LengthNodeFactory.LengthNodeGen.class));
        if (!charWriteInjected) {
            System.out.println(NodeWrapper.wrap(created).humanReadableTree());
        }
        Assert.assertTrue(charWriteInjected);
    }

}
