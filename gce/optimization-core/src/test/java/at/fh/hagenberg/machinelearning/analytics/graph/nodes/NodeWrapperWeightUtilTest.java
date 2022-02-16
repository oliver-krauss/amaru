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
import at.fh.hagenberg.aist.gce.minic.nodes.impl.literals.MinicSimpleLiteralNode;
import at.fh.hagenberg.aist.gce.minic.nodes.impl.op.rel.integer.signed.MinicIntRelationalNodeFactory;
import at.fh.hagenberg.aist.gce.optimization.runtime.SystemInformation;
import at.fh.hagenberg.aist.gce.optimization.util.TruffleLanguageInformation;
import com.oracle.truffle.api.nodes.Node;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * @author Oliver Krauss on 01.11.2019
 */

public class NodeWrapperWeightUtilTest {

    @BeforeClass
    public void setUp() {
        TruffleLanguageInformation.getLanguageInformation(MinicLanguage.ID).getInstantiableNodes().values().forEach(x -> {
            x.getWeight().put(SystemInformation.getCurrentSystem(), 2.2);
        });
    }

    @Test
    public void testFail() {
        // given
        NodeWrapperWeightUtil util = new NodeWrapperWeightUtil(MinicLanguage.ID);

        // when
        util.weight(new NodeWrapper("this.type.doesn.t.exist"));

        // then
        // we expect this not to fail, only to warn
    }

    @Test
    public void testWeight() {
        // given
        NodeWrapperWeightUtil util = new NodeWrapperWeightUtil(MinicLanguage.ID);

        // when
        double weight = util.weight(NodeWrapper.wrap(new MinicSimpleLiteralNode.MinicIntLiteralNode(1)));
        double weightNode = util.weight(new MinicSimpleLiteralNode.MinicIntLiteralNode(1));

        // then
        Assert.assertEquals(weight, 2.2);
        Assert.assertEquals(weightNode, weight);
    }

    @Test
    public void testWeightBranch() {
        // given
        NodeWrapperWeightUtil util = new NodeWrapperWeightUtil(MinicLanguage.ID);

        // when
        Node n = new MinicIfNode(
            new MinicSimpleLiteralNode.MinicIntLiteralNode(2),
            MinicIntRelationalNodeFactory.MinicIntEqualsNodeGen.create(new MinicSimpleLiteralNode.MinicIntLiteralNode(2), new MinicSimpleLiteralNode.MinicIntLiteralNode(3)),
            MinicIntRelationalNodeFactory.MinicIntEqualsNodeGen.create(new MinicSimpleLiteralNode.MinicIntLiteralNode(4), new MinicSimpleLiteralNode.MinicIntLiteralNode(5))
        );
        double weight = util.weight(NodeWrapper.wrap(n));
        double weightNode = util.weight(n);

        // then
        Assert.assertEquals(weight, 10.670000000000002);
        Assert.assertEquals(weightNode, weight);
    }

    @Test
    public void testWeightLoop() {
        // given
        NodeWrapperWeightUtil util = new NodeWrapperWeightUtil(MinicLanguage.ID);

        // when
        Node n = new MinicWhileNode(new MinicSimpleLiteralNode.MinicIntLiteralNode(2),
            new MinicBlockNode(
                new MinicSimpleLiteralNode.MinicIntLiteralNode(5),
                new MinicSimpleLiteralNode.MinicCharLiteralNode('c'),
                new MinicSimpleLiteralNode.MinicDoubleLiteralNode(4.5)
            ));
        double weight = util.weight(NodeWrapper.wrap(n));
        double weightNode = util.weight(n);

        // then
        // note: extra 2.2 because the MinicRepeatingNode is inserted below while and that node has the LOOP tag
        Assert.assertEquals(weight, 11 * 10 + 2.2 + 2.2);
        Assert.assertEquals(weight, weightNode);
    }
}
