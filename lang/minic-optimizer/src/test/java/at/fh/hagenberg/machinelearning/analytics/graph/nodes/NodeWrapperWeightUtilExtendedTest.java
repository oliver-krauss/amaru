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
import at.fh.hagenberg.aist.gce.minic.nodes.impl.control.MinicWhileNode;
import at.fh.hagenberg.aist.gce.minic.nodes.impl.function.MinicFunctionBodyNode;
import at.fh.hagenberg.aist.gce.minic.nodes.impl.literals.MinicSimpleLiteralNode;
import at.fh.hagenberg.aist.gce.minic.nodes.impl.op.arith.integer.signed.MinicIntArithmeticNodeFactory;
import at.fh.hagenberg.aist.gce.minic.nodes.impl.op.rel.integer.signed.MinicIntRelationalNodeFactory;
import at.fh.hagenberg.aist.gce.minic.nodes.impl.vars.MinicReadNodeFactory;
import at.fh.hagenberg.aist.gce.minic.nodes.impl.vars.MinicWriteNodeFactory;
import at.fh.hagenberg.aist.gce.optimization.executor.JavassistExecutor;
import at.fh.hagenberg.aist.gce.optimization.executor.TraceExecutionResult;
import at.fh.hagenberg.aist.gce.optimization.runtime.SystemInformation;
import at.fh.hagenberg.aist.gce.optimization.util.TruffleLanguageInformation;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.nodes.Node;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.Map;

/**
 * @author Oliver Krauss on 02.01.2020
 */

public class NodeWrapperWeightUtilExtendedTest {

    @BeforeClass
    public void setUp() {
        TruffleLanguageInformation.getLanguageInformation(MinicLanguage.ID).getInstantiableNodes().values().forEach(x -> {
            if (!x.getWeight().containsKey(SystemInformation.getCurrentSystem())) {
                x.getWeight().put(SystemInformation.getCurrentSystem(), 2.2);
            }
        });
    }

    JavassistExecutor test = new JavassistExecutor("c",
        "int main() {\n" +
        "    int i;\n" +
        "    return 0;\n" +
        "}"
        , "main", "main", null);

    private Map<String, Integer> useJavassistExecutor(Node n) {
        TraceExecutionResult result = test.traceTest(n, null);

        Assert.assertNotNull(result);
        if (!result.isSuccess()) {
            System.out.println(result.getReturnValue());
        }
        Assert.assertTrue(result.isSuccess());
        return result.getNodeExecutions();
    }

    @Test(enabled = false) // TODO #257 Javassit worker is broken.
    public void testWeight() {
        // given
        Node n = new MinicSimpleLiteralNode.MinicIntLiteralNode(1);
        NodeWrapperWeightUtil util = new NodeWrapperWeightUtil(MinicLanguage.ID);

        // when
        double weight = util.weight(NodeWrapper.wrap(n), useJavassistExecutor(n));

        // then
        Assert.assertEquals(weight, 2.2);
    }

    @Test(enabled = false) // TODO #257 Javassit worker is broken.
    public void testWeightBranch() {
        // given
        Node n = new MinicFunctionBodyNode(new MinicIfNode(
            new MinicSimpleLiteralNode.MinicIntLiteralNode(2),
            MinicIntRelationalNodeFactory.MinicIntEqualsNodeGen.create(new MinicSimpleLiteralNode.MinicIntLiteralNode(2), new MinicSimpleLiteralNode.MinicIntLiteralNode(3)),
            MinicIntRelationalNodeFactory.MinicIntEqualsNodeGen.create(new MinicSimpleLiteralNode.MinicIntLiteralNode(4), new MinicSimpleLiteralNode.MinicIntLiteralNode(5))
        ));
        NodeWrapperWeightUtil util = new NodeWrapperWeightUtil(MinicLanguage.ID);

        // when
        double weight = util.weight(NodeWrapper.wrap(n), useJavassistExecutor(n));

        // then
        Assert.assertEquals(weight, 13.2);
    }

    @Test(enabled = false) // TODO #257 Javassit worker is broken.
    public void testWeightLoop() {
        // given
        FrameSlot islot = test.getRoot().getFrameDescriptor().findFrameSlot("i");
        Node n = new MinicFunctionBodyNode(new MinicBlockNode(
            MinicWriteNodeFactory.MinicIntWriteNodeGen.create(new MinicSimpleLiteralNode.MinicIntLiteralNode(0), islot),
            new MinicWhileNode(MinicIntRelationalNodeFactory.MinicIntLtENodeGen.create(MinicReadNodeFactory.MinicIntReadNodeGen.create(islot), new MinicSimpleLiteralNode.MinicIntLiteralNode(5)),
            new MinicBlockNode(
                MinicWriteNodeFactory.MinicIntWriteNodeGen.create(
                    MinicIntArithmeticNodeFactory.MinicIntAddNodeGen.create(new MinicSimpleLiteralNode.MinicIntLiteralNode(1), MinicReadNodeFactory.MinicIntReadNodeGen.create(islot))
                    , islot),
                new MinicSimpleLiteralNode.MinicIntLiteralNode(5),
                new MinicSimpleLiteralNode.MinicCharLiteralNode('c'),
                new MinicSimpleLiteralNode.MinicDoubleLiteralNode(4.5)
            ))));
        NodeWrapperWeightUtil util = new NodeWrapperWeightUtil(MinicLanguage.ID);

        // when
        double weight = util.weight(NodeWrapper.wrap(n), useJavassistExecutor(n));

        // then
        Assert.assertEquals(weight, 112.2,.01);
    }

}
