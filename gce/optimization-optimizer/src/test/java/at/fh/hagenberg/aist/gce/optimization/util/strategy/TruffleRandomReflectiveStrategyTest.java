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
import at.fh.hagenberg.aist.gce.minic.nodes.base.MinicExpressionNode;
import at.fh.hagenberg.aist.gce.minic.nodes.base.MinicNode;
import at.fh.hagenberg.aist.gce.minic.nodes.impl.control.MinicBlockNode;
import at.fh.hagenberg.aist.gce.minic.nodes.impl.control.MinicWhileNode;
import at.fh.hagenberg.aist.gce.minic.nodes.impl.literals.MinicSimpleLiteralNode;
import at.fh.hagenberg.aist.gce.minic.nodes.impl.op.rel.integer.signed.MinicIntRelationalNodeFactory;
import at.fh.hagenberg.aist.gce.minic.nodes.impl.op.unary.floating.MinicFloatUnaryNodeFactory;
import at.fh.hagenberg.aist.gce.minic.nodes.impl.vars.*;
import at.fh.hagenberg.aist.gce.minic.types.integer.signed.MinicIntNode;
import at.fh.hagenberg.aist.gce.optimization.util.TruffleLanguageInformation;
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
import org.junit.Assert;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Oliver Krauss on 11.08.2020
 */

public class TruffleRandomReflectiveStrategyTest {

    private TruffleLanguageSearchSpace searchSpace = null;

    private TruffleLanguageSearchSpace getSearchSpace() {
        if (searchSpace != null) {
            return searchSpace;
        }

        TruffleLanguageInformation information = TruffleLanguageInformation.getLanguageInformation(MinicLanguage.ID);
        return searchSpace = new TruffleLanguageSearchSpace(information, null);
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
    public void testCanCreateWithDataRequirement(){
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
        TruffleHierarchicalStrategy strategy = ms.getStrategies().stream()
            .filter(x -> x instanceof RandomReflectiveSubtreeStrategy && ((RandomReflectiveSubtreeStrategy) x).getInformation().getClazz().equals(MinicIntRelationalNodeFactory.MinicIntGtNodeGen.class))
            .findFirst().orElse(null);
        TruffleHierarchicalStrategy strategy2 = ms.getStrategies().stream()
            .filter(x -> x instanceof RandomReflectiveSubtreeStrategy && ((RandomReflectiveSubtreeStrategy) x).getInformation().getClazz().equals(MinicFloatUnaryNodeFactory.MinicFloatLogicalNotNodeGen.class))
            .findFirst().orElse(null);

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

        DataFlowGraph dfg = DataFlowUtil.constructDataFlowGraph(getSearchSpace().getInformation(), n, breakoff, null);
        RequirementInformation requirements = new RequirementInformation(null);
        dfg.getRequiredDataItems().forEach((k, v) -> {
            if (v != null) {
                v.forEach(s -> requirements.addRequirement(new Requirement(Requirement.REQ_DATA_WRITE).addProperty(Requirement.REQPR_SLOT, s)));
            }
        });
        CreationInformation information = new CreationInformation(null, null, requirements, dfg, MinicExpressionNode.class, 0, cc, 0);
        RequirementInformation copy = requirements.copy();
        Requirement r = copy.getRequirements().keySet().iterator().next();
        copy.addDegreeOfFreedom(r);
        CreationInformation informationFree = new CreationInformation(null, null, copy, dfg, MinicExpressionNode.class, 0, cc, 0);
        CreationInformation information2 = new CreationInformation(null, null, copy, dfg, MinicNode.class, 1, cc, 0);

        // when
        RequirementInformation shouldNotWork = strategy.canCreateVerbose(information);
        RequirementInformation shouldWork = strategy.canCreateVerbose(informationFree);
        RequirementInformation shouldNotWork2 = strategy2.canCreateVerbose(information2);

        // then
        Assert.assertNotNull(shouldNotWork);
        Assert.assertFalse(shouldNotWork.fullfillsAll());
        Assert.assertNotNull(shouldWork);
        Assert.assertTrue(shouldWork.fullfillsAll());
        Assert.assertNotNull(shouldNotWork2);
        // TODO #216 this should be false as the data types are not equivalent
        // Assert.assertFalse(shouldNotWork2.fullfillsAll());
    }

}
