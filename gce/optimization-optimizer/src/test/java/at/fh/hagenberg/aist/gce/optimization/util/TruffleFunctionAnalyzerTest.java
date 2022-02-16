/*
 * Copyright (c) 2022 the original author or authors.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

package at.fh.hagenberg.aist.gce.optimization.util;

import at.fh.hagenberg.aist.gce.minic.language.MinicLanguage;
import at.fh.hagenberg.aist.gce.minic.nodes.base.MinicRootNode;
import at.fh.hagenberg.aist.gce.minic.nodes.impl.control.MinicBlockNode;
import at.fh.hagenberg.aist.gce.minic.nodes.impl.control.MinicIfNode;
import at.fh.hagenberg.aist.gce.minic.nodes.impl.control.MinicReturnNode;
import at.fh.hagenberg.aist.gce.minic.nodes.impl.function.MinicFunctionBodyNode;
import at.fh.hagenberg.aist.gce.minic.nodes.impl.function.MinicReadFunctionArgumentNode;
import at.fh.hagenberg.aist.gce.minic.nodes.impl.function.MinicReadFunctionArgumentNodeFactory;
import at.fh.hagenberg.aist.gce.minic.nodes.impl.literals.MinicSimpleLiteralNode;
import at.fh.hagenberg.aist.gce.minic.nodes.impl.op.arith.floating.MinicDoubleArithmeticNodeFactory;
import at.fh.hagenberg.aist.gce.minic.nodes.impl.op.arith.integer.signed.MinicIntArithmeticNodeFactory;
import at.fh.hagenberg.aist.gce.minic.nodes.impl.op.rel.floating.MinicDoubleRelationalNodeFactory;
import at.fh.hagenberg.aist.gce.minic.nodes.impl.vars.MinicReadNode;
import at.fh.hagenberg.aist.gce.minic.nodes.impl.vars.MinicReadNodeFactory;
import at.fh.hagenberg.aist.gce.minic.nodes.impl.vars.MinicWriteNodeFactory;
import at.fh.hagenberg.aist.gce.optimization.test.TruffleOptimizationTest;
import at.fh.hagenberg.aist.gce.optimization.test.TruffleOptimizationTestComplexity;
import at.fh.hagenberg.aist.gce.optimization.test.TruffleTestValue;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.nodes.RootNode;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Source;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.ByteArrayOutputStream;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static org.testng.Assert.*;

public class TruffleFunctionAnalyzerTest {

    private TruffleLanguageInformation tli;

    @BeforeClass
    public void setUp() {
        // we have a problem here. The TLI only contains the data on argument classes when it has been set up
        tli = TruffleLanguageInformation.getLanguageInformation(MinicLanguage.ID);
        if (tli.getClass(MinicReadFunctionArgumentNodeFactory.MinicReadFunctionArgumentDoubleNodeGen.class).getArgumentReadClasses().size() == 0) {
            tli.getClass(MinicReadFunctionArgumentNodeFactory.MinicReadFunctionArgumentDoubleNodeGen.class).getArgumentReadClasses().add("double");
            tli.getClass(MinicReadFunctionArgumentNodeFactory.MinicReadFunctionArgumentFloatNodeGen.class).getArgumentReadClasses().add("float");
            tli.getClass(MinicReadFunctionArgumentNodeFactory.MinicReadFunctionArgumentIntNodeGen.class).getArgumentReadClasses().add("int");
            tli.getClass(MinicReadFunctionArgumentNodeFactory.MinicReadFunctionArgumentStringNodeGen.class).getArgumentReadClasses().add("java.lang.String");
            tli.getClass(MinicReadFunctionArgumentNodeFactory.MinicReadFunctionArgumentCharNodeGen.class).getArgumentReadClasses().add("char");
            tli.getClass(MinicReadFunctionArgumentNodeFactory.MinicReadGenericFunctionArgumentNodeGen.class).getArgumentReadClasses().addAll(Arrays.asList("double", "float", "int", "java.lang.String", "char"));
        }

        // init Language
        // enter or create context
        Context ctx = Context.newBuilder().out(new ByteArrayOutputStream()).build();
        ctx.initialize(MinicLanguage.ID);
        int i = 1;
    }


    @Test
    public void testGetSignatureCode() {
        // given
        RootNode timesTwo = getTimesTwo();

        // when
        TruffleFunctionSignature signature = TruffleFunctionAnalyzer.getSignature(timesTwo, tli);

        // then
        Assert.assertNotNull(signature);
        Assert.assertEquals(signature.size(), 1);
        Assert.assertEquals(signature.getArguments()[0], "int");
    }

    @Test
    public void testGetSignatureCodePythagoras() {
        // given
        RootNode timesTwo = getPseudoPyhtagoras();

        // when
        TruffleFunctionSignature signature = TruffleFunctionAnalyzer.getSignature(timesTwo, tli);

        // then
        Assert.assertNotNull(signature);
        Assert.assertEquals(signature.size(), 2);
        Assert.assertEquals(signature.getArguments()[0], "double");
        Assert.assertEquals(signature.getArguments()[1], "double");
    }

    @Test
    public void testGetSignatureCodeFourIn() {
        // given
        RootNode timesTwo = getFourInputs();

        // when
        TruffleFunctionSignature signature = TruffleFunctionAnalyzer.getSignature(timesTwo, tli);

        // then
        Assert.assertNotNull(signature);
        Assert.assertEquals(signature.size(), 4);
        Assert.assertEquals(signature.getArguments()[0], "double");
        Assert.assertEquals(signature.getArguments()[1], "double");
        Assert.assertEquals(signature.getArguments()[2], "java.lang.String");
        Assert.assertEquals(signature.getArguments()[3], "float");
    }

    @Test
    public void testGetSignatureTestCasesTimesTwo() {
        // given
        RootNode timesTwo = getTimesTwo();
        Set<TruffleOptimizationTestComplexity> cases = new HashSet<>();
        cases.add(new TruffleOptimizationTestComplexity(null, new TruffleOptimizationTest(Arrays.asList(new TruffleTestValue(1, "int")), new TruffleTestValue(2, "int"))));
        cases.add(new TruffleOptimizationTestComplexity(null, new TruffleOptimizationTest(Arrays.asList(new TruffleTestValue(2, "int")), new TruffleTestValue(4, "int"))));
        cases.add(new TruffleOptimizationTestComplexity(null, new TruffleOptimizationTest(Arrays.asList(new TruffleTestValue(3, "int")), new TruffleTestValue(6, "int"))));
        cases.add(new TruffleOptimizationTestComplexity(null, new TruffleOptimizationTest(Arrays.asList(new TruffleTestValue(4, "int")), new TruffleTestValue(8, "int"))));
        cases.add(new TruffleOptimizationTestComplexity(null, new TruffleOptimizationTest(Arrays.asList(new TruffleTestValue(5, "int")), new TruffleTestValue(10, "int"))));

        // when
        TruffleFunctionSignature signature = TruffleFunctionAnalyzer.getSignature(timesTwo, cases);

        // then
        Assert.assertNotNull(signature);
        Assert.assertEquals(signature.size(), 1);
        Assert.assertEquals(signature.getArguments()[0], "int");
    }

    @Test
    public void testGetSignatureTestCasesPythagoras() {
        // given
        RootNode timesTwo = getPseudoPyhtagoras();
        Set<TruffleOptimizationTestComplexity> cases = new HashSet<>();
        cases.add(new TruffleOptimizationTestComplexity(null, new TruffleOptimizationTest(Arrays.asList(new TruffleTestValue(1.0, "double"), new TruffleTestValue(1.0, "double")), new TruffleTestValue(2.0, "double"))));
        cases.add(new TruffleOptimizationTestComplexity(null, new TruffleOptimizationTest(Arrays.asList(new TruffleTestValue(2.0, "double"), new TruffleTestValue(2.0, "double")), new TruffleTestValue(8.0, "double"))));
        cases.add(new TruffleOptimizationTestComplexity(null, new TruffleOptimizationTest(Arrays.asList(new TruffleTestValue(3.0, "double"), new TruffleTestValue(3.0, "double")), new TruffleTestValue(18.0, "double"))));
        cases.add(new TruffleOptimizationTestComplexity(null, new TruffleOptimizationTest(Arrays.asList(new TruffleTestValue(4.0, "double"), new TruffleTestValue(4.0, "double")), new TruffleTestValue(32.0, "double"))));
        cases.add(new TruffleOptimizationTestComplexity(null, new TruffleOptimizationTest(Arrays.asList(new TruffleTestValue(5.0, "double"), new TruffleTestValue(5.0, "double")), new TruffleTestValue(50.0, "double"))));

        // when
        TruffleFunctionSignature signature = TruffleFunctionAnalyzer.getSignature(timesTwo, cases);

        // then
        Assert.assertNotNull(signature);
        Assert.assertEquals(signature.size(), 2);
        Assert.assertEquals(signature.getArguments()[0], "double");
        Assert.assertEquals(signature.getArguments()[1], "double");
    }

    @Test(expectedExceptions = RuntimeException.class)
    public void testIncompatibleSignatures() {
        // given
        RootNode timesTwo = getPseudoPyhtagoras();
        Set<TruffleOptimizationTestComplexity> cases = new HashSet<>();
        cases.add(new TruffleOptimizationTestComplexity(null, new TruffleOptimizationTest(Arrays.asList(new TruffleTestValue(1.0, "double"), new TruffleTestValue(1.0, "double")), new TruffleTestValue(2.0, "double"))));
        cases.add(new TruffleOptimizationTestComplexity(null, new TruffleOptimizationTest(Arrays.asList(new TruffleTestValue(2, "int"), new TruffleTestValue(2, "int")), new TruffleTestValue(8.0, "double"))));

        // when
        TruffleFunctionSignature signature = TruffleFunctionAnalyzer.getSignature(timesTwo, cases);

        // then
        // this should throw an error
    }

    @Test
    public void testGetSignatureTestCasesFourIn() {
        // given
        RootNode timesTwo = getTimesTwo();
        Set<TruffleOptimizationTestComplexity> cases = new HashSet<>();
        cases.add(new TruffleOptimizationTestComplexity(null, new TruffleOptimizationTest(Arrays.asList(new TruffleTestValue(1.0, "double"), new TruffleTestValue(1.0, "double"), new TruffleTestValue("hello", "java.lang.String"), new TruffleTestValue(1.0f, "float")), new TruffleTestValue(2.0, "double"))));
        cases.add(new TruffleOptimizationTestComplexity(null, new TruffleOptimizationTest(Arrays.asList(new TruffleTestValue(1.0, "double"), new TruffleTestValue(1.0, "double"), new TruffleTestValue("hello", "java.lang.String")), new TruffleTestValue(2.0, "double"))));
        cases.add(new TruffleOptimizationTestComplexity(null, new TruffleOptimizationTest(Arrays.asList(new TruffleTestValue(1.0, "double"), new TruffleTestValue(1.0, "double"), new TruffleTestValue(null, null), new TruffleTestValue(1.0f, "float")), new TruffleTestValue(2.0, "double"))));

        // when
        TruffleFunctionSignature signature = TruffleFunctionAnalyzer.getSignature(timesTwo, cases);

        // then
        Assert.assertNotNull(signature);
        Assert.assertEquals(signature.size(), 4);
        Assert.assertEquals(signature.getArguments()[0], "double");
        Assert.assertEquals(signature.getArguments()[1], "double");
        Assert.assertEquals(signature.getArguments()[2], "java.lang.String");
        Assert.assertEquals(signature.getArguments()[3], "float");
    }

    /**
     * this one is like the parser -> has a nice read fn arg and add it to the frame descriptor
     * @return x * 2
     */
    private RootNode getTimesTwo() {
        FrameDescriptor descriptor = new FrameDescriptor();
        FrameSlot slotX = descriptor.findOrAddFrameSlot("x");
        MinicBlockNode block = new MinicBlockNode(MinicWriteNodeFactory.MinicIntWriteNodeGen.create(MinicReadFunctionArgumentNodeFactory.MinicReadFunctionArgumentIntNodeGen.create(0), slotX),
                new MinicReturnNode(MinicIntArithmeticNodeFactory.MinicIntMulNodeGen.create(
                        MinicReadNodeFactory.MinicIntReadNodeGen.create(slotX), new MinicSimpleLiteralNode.MinicIntLiteralNode(2)
                )));
        MinicFunctionBodyNode body = new MinicFunctionBodyNode(block);
        return new MinicRootNode(MinicLanguage.INSTANCE, null, descriptor, body, "timesTwo");
    }

    /**
     * this one is shorthand we skip writing to stack and directly read arguments
         * @return a² + b²
     */
    private RootNode getPseudoPyhtagoras() {
        FrameDescriptor descriptor = new FrameDescriptor();
        MinicBlockNode block = new MinicBlockNode(
                new MinicReturnNode(
                        MinicDoubleArithmeticNodeFactory.MinicDoubleAddNodeGen.create(
                                MinicDoubleArithmeticNodeFactory.MinicDoubleMulNodeGen.create(MinicReadFunctionArgumentNodeFactory.MinicReadFunctionArgumentDoubleNodeGen.create(0), MinicReadFunctionArgumentNodeFactory.MinicReadFunctionArgumentDoubleNodeGen.create(0)),
                                MinicDoubleArithmeticNodeFactory.MinicDoubleMulNodeGen.create(MinicReadFunctionArgumentNodeFactory.MinicReadFunctionArgumentDoubleNodeGen.create(1), MinicReadFunctionArgumentNodeFactory.MinicReadFunctionArgumentDoubleNodeGen.create(1))
                        )
                ));
        MinicFunctionBodyNode body = new MinicFunctionBodyNode(block);
        return new MinicRootNode(MinicLanguage.INSTANCE, null, descriptor, body, "timesTwo");
    }

    /**
     * this one is just nonsense. We inject 4 reads randomly with different types
     * @return fn with four inputs (double, double, string, float)
     */
    private RootNode getFourInputs() {
        FrameDescriptor descriptor = new FrameDescriptor();
        FrameSlot slotX = descriptor.findOrAddFrameSlot("x");
        FrameSlot slotY = descriptor.findOrAddFrameSlot("y");
        MinicBlockNode block = new MinicBlockNode(
                MinicWriteNodeFactory.MinicDoubleWriteNodeGen.create(MinicReadFunctionArgumentNodeFactory.MinicReadFunctionArgumentDoubleNodeGen.create(0), slotX),
                new MinicIfNode(MinicDoubleRelationalNodeFactory.MinicDoubleLtENodeGen.create(MinicReadNodeFactory.MinicDoubleReadNodeGen.create(slotX), MinicReadFunctionArgumentNodeFactory.MinicReadFunctionArgumentDoubleNodeGen.create(1)),
                        new MinicBlockNode(MinicReadFunctionArgumentNodeFactory.MinicReadFunctionArgumentStringNodeGen.create(2)),
                        new MinicBlockNode(MinicWriteNodeFactory.MinicFloatWriteNodeGen.create(MinicReadFunctionArgumentNodeFactory.MinicReadFunctionArgumentFloatNodeGen.create(3), slotY))),
                new MinicReturnNode(
                        MinicDoubleArithmeticNodeFactory.MinicDoubleAddNodeGen.create(
                                MinicDoubleArithmeticNodeFactory.MinicDoubleMulNodeGen.create(MinicReadNodeFactory.MinicDoubleReadNodeGen.create(slotX), MinicReadFunctionArgumentNodeFactory.MinicReadFunctionArgumentDoubleNodeGen.create(1)),
                                MinicDoubleArithmeticNodeFactory.MinicDoubleMulNodeGen.create(MinicReadNodeFactory.MinicDoubleReadNodeGen.create(slotX), MinicReadFunctionArgumentNodeFactory.MinicReadFunctionArgumentDoubleNodeGen.create(1))
                        )
                ));
        MinicFunctionBodyNode body = new MinicFunctionBodyNode(block);
        return new MinicRootNode(MinicLanguage.INSTANCE, null, descriptor, body, "timesTwo");
    }
}