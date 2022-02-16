/*
 * Copyright (c) 2022 the original author or authors.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

package at.fh.hagenberg.aist.gce.science.statistics.data;

import at.fh.hagenberg.aist.gce.minic.language.MinicLanguage;
import at.fh.hagenberg.aist.gce.minic.nodes.impl.control.MinicIfNode;
import at.fh.hagenberg.aist.gce.minic.types.integer.signed.MinicCharNode;
import at.fh.hagenberg.aist.gce.minic.types.integer.signed.MinicIntNode;
import at.fh.hagenberg.aist.gce.optimization.util.TruffleClassInformation;
import at.fh.hagenberg.aist.gce.optimization.util.TruffleLanguageInformation;
import at.fh.hagenberg.aist.gce.science.statistics.data.template.NodeWrapperPrinter;
import at.fh.hagenberg.aist.gce.science.statistics.data.template.TruffleLanguagePrinter;
import guru.nidi.graphviz.engine.Engine;
import guru.nidi.graphviz.engine.Format;
import guru.nidi.graphviz.engine.Graphviz;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.util.Map;

/**
 * @author Oliver Krauss on 06.11.2019
 */

public class TruffleLanguagePrinterTest {

    @Test
    public void testPrintLanguageHierarchy() throws IOException {
        // given
        TruffleLanguagePrinter printer = new TruffleLanguagePrinter(null);
        StringWriter writer = new StringWriter();
        printer.setWriter(writer);

        // when
        printer.printClassHierarchy(TruffleLanguageInformation.getLanguageInformation(MinicLanguage.ID));

        // then
        String s = writer.toString();
        // cannot assert entire string as map-contents change order but we want all 356 classes.
        Assert.assertTrue(s.contains("356"));
    }

    @Test
    public void testPrintLanguageHierarchyPartial() throws IOException {
        // given
        TruffleLanguagePrinter printer = new TruffleLanguagePrinter(null);
        StringWriter writer = new StringWriter();
        printer.setWriter(writer);

        // when
        printer.printClassHierarchy(TruffleLanguageInformation.getLanguageInformation(MinicLanguage.ID), MinicCharNode.class);

        // then
        String s = writer.toString();
        Assert.assertEquals(s.length(), partialClassHierarchyString().length());
    }

    @Test
    public void testPrintClassInformation() throws IOException {
        // given
        TruffleLanguagePrinter printer = new TruffleLanguagePrinter(null);
        StringWriter writer = new StringWriter();
        printer.setWriter(writer);
        TruffleClassInformation tci = TruffleLanguageInformation.getLanguageInformation(MinicLanguage.ID).getInstantiableNodes().get(MinicIfNode.class);

        // when
        printer.printClassInformation(tci);

        // then
        String s = writer.toString();
        Assert.assertEquals(s, classInfo());
    }

    @Test
    public void testPrintSpace() throws IOException {
        // given
        TruffleLanguagePrinter printer = new TruffleLanguagePrinter(null);
        StringWriter writer = new StringWriter();
        printer.setWriter(writer);
        Map<Class, TruffleClassInformation> minic = TruffleLanguageInformation.getLanguageInformation(MinicLanguage.ID).getInstantiableNodes();

        // when
        printer.printLanguageSpace(minic);

        // then
        String s = writer.toString();
        // we hope for all nodes, but the values are map-dependent and can't be checked with string compare
        Assert.assertTrue(s.contains("648"));
    }

    private String classInfo() {
        return "digraph G {\n" +
            "0[shape=octagon, label=\"at.fh.hagenberg.aist.gce.minic.nodes.impl.control.MinicIfNode\"]\n" +
            "    0->1\n" +
            "    1[shape=invtriangle, label=\"constructor\"]\n" +
            "        1->2\n" +
            "        2[shape=house, label=\"condition : at.fh.hagenberg.aist.gce.minic.nodes.base.MinicExpressionNode\"]\n" +
            "        1->3\n" +
            "        3[shape=house, label=\"thenPath : at.fh.hagenberg.aist.gce.minic.nodes.base.MinicNode\"]\n" +
            "        1->4\n" +
            "        4[shape=house, label=\"elsePath : at.fh.hagenberg.aist.gce.minic.nodes.base.MinicNode\"]\n" +
            "}";
    }

    private String partialClassHierarchyString() {
        return "digraph G {\n" +
                "    0[label=\"at.fh.hagenberg.aist.gce.minic.types.integer.signed.MinicCharNode\"]\n" +
                "            0->1\n" +
                "    1[label=\"at.fh.hagenberg.aist.gce.minic.nodes.impl.cast.MinicToCharNode$MinicDoubleToCharNode\"]\n" +
                "            1->2\n" +
                "    2[label=\"at.fh.hagenberg.aist.gce.minic.nodes.impl.cast.MinicToCharNodeFactory$MinicDoubleToCharNodeGen\"]\n" +
                "            0->3\n" +
                "    3[label=\"at.fh.hagenberg.aist.gce.minic.nodes.impl.cast.MinicToCharNode$MinicFloatToCharNode\"]\n" +
                "            3->4\n" +
                "    4[label=\"at.fh.hagenberg.aist.gce.minic.nodes.impl.cast.MinicToCharNodeFactory$MinicFloatToCharNodeGen\"]\n" +
                "            0->5\n" +
                "    5[label=\"at.fh.hagenberg.aist.gce.minic.nodes.impl.cast.MinicToCharNode$MinicIntToCharNode\"]\n" +
                "            5->6\n" +
                "    6[label=\"at.fh.hagenberg.aist.gce.minic.nodes.impl.cast.MinicToCharNodeFactory$MinicIntToCharNodeGen\"]\n" +
                "            0->7\n" +
                "    7[label=\"at.fh.hagenberg.aist.gce.minic.nodes.impl.function.MinicInvokeNode$MinicInvokeCharNode\"]\n" +
                "            7->8\n" +
                "    8[label=\"at.fh.hagenberg.aist.gce.minic.nodes.impl.function.MinicInvokeNodeFactory$MinicInvokeCharNodeGen\"]\n" +
                "            0->9\n" +
                "    9[label=\"at.fh.hagenberg.aist.gce.minic.nodes.impl.function.MinicReadFunctionArgumentNode$MinicReadFunctionArgumentCharNode\"]\n" +
                "            9->10\n" +
                "    10[label=\"at.fh.hagenberg.aist.gce.minic.nodes.impl.function.MinicReadFunctionArgumentNodeFactory$MinicReadFunctionArgumentCharNodeGen\"]\n" +
                "            0->11\n" +
                "    11[label=\"at.fh.hagenberg.aist.gce.minic.nodes.impl.literals.MinicSimpleLiteralNode$MinicCharLiteralNode\"]\n" +
                "            0->12\n" +
                "    12[label=\"at.fh.hagenberg.aist.gce.minic.nodes.impl.vars.MinicReadGlobalNode$MinicCharReadGlobalNode\"]\n" +
                "            12->13\n" +
                "    13[label=\"at.fh.hagenberg.aist.gce.minic.nodes.impl.vars.MinicReadGlobalNodeFactory$MinicCharReadGlobalNodeGen\"]\n" +
                "            0->14\n" +
                "    14[label=\"at.fh.hagenberg.aist.gce.minic.nodes.impl.vars.MinicReadNode$MinicCharReadNode\"]\n" +
                "            14->15\n" +
                "    15[label=\"at.fh.hagenberg.aist.gce.minic.nodes.impl.vars.MinicReadNodeFactory$MinicCharReadNodeGen\"]\n" +
                "            0->16\n" +
                "    16[label=\"at.fh.hagenberg.aist.gce.minic.nodes.impl.vars.MinicReadArrayNode$MinicStringAsCharArrayReadNode\"]\n" +
                "            16->17\n" +
                "    17[label=\"at.fh.hagenberg.aist.gce.minic.nodes.impl.vars.MinicReadArrayNodeFactory$MinicStringAsCharArrayReadNodeGen\"]\n" +
                "            0->18\n" +
                "    18[label=\"at.fh.hagenberg.aist.gce.minic.nodes.impl.vars.MinicReadGlobalArrayNode$MinicStringAsCharArrayReadGlobalNode\"]\n" +
                "            18->19\n" +
                "    19[label=\"at.fh.hagenberg.aist.gce.minic.nodes.impl.vars.MinicReadGlobalArrayNodeFactory$MinicStringAsCharArrayReadGlobalNodeGen\"]\n" +
                "            0->20\n" +
                "    20[label=\"at.fh.hagenberg.aist.gce.minic.nodes.impl.vars.MinicReadArrayNode$MinicCharArrayReadNode\"]\n" +
                "            20->21\n" +
                "    21[label=\"at.fh.hagenberg.aist.gce.minic.nodes.impl.vars.MinicReadArrayNodeFactory$MinicCharArrayReadNodeGen\"]\n" +
                "            0->22\n" +
                "    22[label=\"at.fh.hagenberg.aist.gce.minic.nodes.impl.vars.MinicReadGlobalArrayNode$MinicCharArrayReadGlobalNode\"]\n" +
                "            22->23\n" +
                "    23[label=\"at.fh.hagenberg.aist.gce.minic.nodes.impl.vars.MinicReadGlobalArrayNodeFactory$MinicCharArrayReadGlobalNodeGen\"]\n" +
                "}";
    }
}
