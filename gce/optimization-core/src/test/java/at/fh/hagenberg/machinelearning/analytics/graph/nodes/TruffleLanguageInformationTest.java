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
import at.fh.hagenberg.aist.gce.minic.nodes.builtin.PrintNodeFactory;
import at.fh.hagenberg.aist.gce.minic.nodes.builtin.ReadNodeFactory;
import at.fh.hagenberg.aist.gce.minic.nodes.impl.control.MinicIfNode;
import at.fh.hagenberg.aist.gce.minic.nodes.impl.control.MinicRepeatingNode;
import at.fh.hagenberg.aist.gce.minic.nodes.impl.control.MinicReturnNode;
import at.fh.hagenberg.aist.gce.minic.nodes.impl.function.MinicFunctionLiteralNode;
import at.fh.hagenberg.aist.gce.minic.nodes.impl.vars.*;
import at.fh.hagenberg.aist.gce.optimization.util.TruffleClassInformation;
import at.fh.hagenberg.aist.gce.optimization.util.TruffleClassProperty;
import at.fh.hagenberg.aist.gce.optimization.util.TruffleLanguageInformation;
import javassist.*;
import javassist.expr.ExprEditor;
import javassist.expr.MethodCall;
import javassist.expr.NewExpr;
import org.graalvm.polyglot.Source;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * @author Oliver Krauss on 01.11.2019
 */

public class TruffleLanguageInformationTest {

    @Test
    public void testCreateLanguage() {
        // given
        String id = MinicLanguage.ID;

        // when
        TruffleLanguageInformation information = TruffleLanguageInformation.getLanguageInformation(id);

        // then
        Assert.assertNotNull(information);
        Assert.assertNotNull(information.getInstantiableNodes());
        Assert.assertEquals(information.getInstantiableNodes().size(), 167);
        TruffleClassInformation tci = information.getInstantiableNodes().get(ReadNodeFactory.ReadNodeGen.class);
        Assert.assertNotNull(tci);
        Assert.assertTrue(tci.getProperties().contains(TruffleClassProperty.TRUFFLE_BOUNDARY));
        tci = information.getInstantiableNodes().get(PrintNodeFactory.PrintNodeGen.class);
        Assert.assertNotNull(tci);
        Assert.assertTrue(tci.getProperties().contains(TruffleClassProperty.TRUFFLE_BOUNDARY));
        tci = information.getInstantiableNodes().get(MinicIfNode.class);
        Assert.assertNotNull(tci);
        Assert.assertTrue(tci.getProperties().contains(TruffleClassProperty.BRANCH));
        Assert.assertTrue(tci.getProperties().contains(TruffleClassProperty.CONTROL_FLOW));
        tci = information.getInstantiableNodes().get(MinicRepeatingNode.class);
        Assert.assertNotNull(tci);
        Assert.assertTrue(tci.getProperties().contains(TruffleClassProperty.LOOP));
        Assert.assertTrue(tci.getProperties().contains(TruffleClassProperty.CONTROL_FLOW));
        tci = information.getInstantiableNodes().get(MinicReturnNode.class);
        Assert.assertNotNull(tci);
        Assert.assertTrue(tci.getProperties().contains(TruffleClassProperty.CONTROL_FLOW_EXCEPTION));
        Assert.assertTrue(tci.getProperties().contains(TruffleClassProperty.CONTROL_FLOW));
        tci = information.getInstantiableNodes().get(MinicWriteNodeFactory.MinicIntWriteNodeGen.class);
        Assert.assertNotNull(tci);
        Assert.assertTrue(tci.getProperties().contains(TruffleClassProperty.LOCAL_STATE));
        Assert.assertTrue(tci.getProperties().contains(TruffleClassProperty.STATE_WRITE));
        Assert.assertFalse(tci.getProperties().contains(TruffleClassProperty.GLOBAL_STATE));
        Assert.assertFalse(tci.getProperties().contains(TruffleClassProperty.STATE_READ));
        tci = information.getInstantiableNodes().get(MinicReadGlobalNodeFactory.MinicIntReadGlobalNodeGen.class);
        Assert.assertNotNull(tci);
        Assert.assertFalse(tci.getProperties().contains(TruffleClassProperty.LOCAL_STATE));
        Assert.assertFalse(tci.getProperties().contains(TruffleClassProperty.STATE_WRITE));
        Assert.assertTrue(tci.getProperties().contains(TruffleClassProperty.GLOBAL_STATE));
        Assert.assertTrue(tci.getProperties().contains(TruffleClassProperty.STATE_READ));
        tci = information.getInstantiableNodes().get(MinicWriteGlobalArrayNodeFactory.MinicDoubleArrayWriteGlobalNodeGen.class);
        Assert.assertNotNull(tci);
        Assert.assertFalse(tci.getProperties().contains(TruffleClassProperty.LOCAL_STATE));
        Assert.assertTrue(tci.getProperties().contains(TruffleClassProperty.STATE_WRITE));
        Assert.assertTrue(tci.getProperties().contains(TruffleClassProperty.GLOBAL_STATE));
        Assert.assertTrue(tci.getProperties().contains(TruffleClassProperty.STATE_READ));
        tci = information.getInstantiableNodes().get(MinicFunctionLiteralNode.class);
        Assert.assertNotNull(tci);
        Assert.assertTrue(tci.getProperties().contains(TruffleClassProperty.FUNCTION_CALL));
    }

}
