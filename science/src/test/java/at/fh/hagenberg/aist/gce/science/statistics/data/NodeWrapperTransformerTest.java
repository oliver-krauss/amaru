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
import at.fh.hagenberg.aist.gce.minic.nodes.impl.literals.MinicSimpleLiteralNode;
import at.fh.hagenberg.aist.gce.minic.nodes.impl.op.arith.integer.signed.MinicIntArithmeticNodeFactory;
import at.fh.hagenberg.aist.gce.optimization.util.TruffleLanguageInformation;
import at.fh.hagenberg.aist.gce.science.statistics.DatasetDependentTest;
import at.fh.hagenberg.aist.gce.science.statistics.Report;
import at.fh.hagenberg.aist.gce.science.statistics.data.template.DatasetReportTransformer;
import at.fh.hagenberg.aist.gce.science.statistics.data.template.LatexPreprocessor;
import at.fh.hagenberg.aist.gce.science.statistics.data.template.NodeWrapperPrinter;
import at.fh.hagenberg.machinelearning.analytics.graph.nodes.NodeWrapper;
import guru.nidi.graphviz.engine.Format;
import guru.nidi.graphviz.engine.Graphviz;
import guru.nidi.graphviz.engine.Rasterizer;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;

/**
 * @author Oliver Krauss on 23.10.2019
 */
public class NodeWrapperTransformerTest {

    @Test
    public void testSaveSimple() {
        // given
        NodeWrapperPrinter printer = new NodeWrapperPrinter(null);
        StringWriter writer = new StringWriter();
        printer.setWriter(writer);

        // when
        printer.printNodeWrapper(getNodeWrapper());

        // then
        Assert.assertEquals(writer.toString(), simpleGraph());
    }

    @Test
    public void testSaveComplex() {
        // given
        NodeWrapperPrinter printer = new NodeWrapperPrinter(null, true);
        StringWriter writer = new StringWriter();
        printer.setWriter(writer);

        // when
        printer.printNodeWrapper(getNodeWrapper());

        // then
        Assert.assertEquals(writer.toString(), complexGraph());
    }

    @Test
    public void testWithLanguageSimple() {
        // given
        NodeWrapperPrinter printer = new NodeWrapperPrinter(null, false);
        printer.setTli(TruffleLanguageInformation.getLanguageInformation(MinicLanguage.ID));
        NodeWrapper wrapper = NodeWrapper.wrap(MinicIntArithmeticNodeFactory.MinicIntAddNodeGen.create(
            new MinicSimpleLiteralNode.MinicIntLiteralNode(3),
            new MinicSimpleLiteralNode.MinicIntLiteralNode(5)));
        StringWriter writer = new StringWriter();
        printer.setWriter(writer);

        // when
        printer.printNodeWrapper(wrapper);

        // then
        Assert.assertEquals(writer.toString(), getNodeWrapperGraphSimple());
    }

    @Test
    public void testWithLanguageComplex() {
        // given
        NodeWrapperPrinter printer = new NodeWrapperPrinter(null, true);
        printer.setTli(TruffleLanguageInformation.getLanguageInformation(MinicLanguage.ID));
        NodeWrapper wrapper = NodeWrapper.wrap(MinicIntArithmeticNodeFactory.MinicIntAddNodeGen.create(
            new MinicSimpleLiteralNode.MinicIntLiteralNode(3),
            new MinicSimpleLiteralNode.MinicIntLiteralNode(5)));
        StringWriter writer = new StringWriter();
        printer.setWriter(writer);

        // when
        printer.printNodeWrapper(wrapper);

        // then
        Assert.assertEquals(writer.toString(), getNodeWrapperGraphComplex());
    }

    // Test printing to pdf file
    @Test
    public void testPrint() throws IOException {
        // given
        String complex = getNodeWrapperGraphComplex();
        File file = new File("test.pdf");
        if (file.exists()) {
            file.delete();
        }

        // when
        Graphviz.fromString(complex).rasterize(Rasterizer.builtIn("pdf")).toFile(file);

        // then
        Assert.assertNotNull(file);
        Assert.assertTrue(file.exists());
        file.delete();
    }


    // Test rendering png
    @Test
    public void testRenderPng() throws IOException {
        // given
        String complex = getNodeWrapperGraphComplex();
        File file = new File("test.png");
        if (file.exists()) {
            file.delete();
        }

        // when
        Graphviz.fromString(complex).fontAdjust(0.80).render(Format.PNG).toFile(file);

        // then
        Assert.assertNotNull(file);
        Assert.assertTrue(file.exists());
        file.delete();
    }


    private String simpleGraph() {
        return "digraph G {\n" +
            "    0[label=\"If\"]\n" +
            "        0->1\n" +
            "    1[label=\"Const\"]\n" +
            "        0->2\n" +
            "    2[label=\"!=\"]\n" +
            "        2->3\n" +
            "    3[label=\"Variable\"]\n" +
            "        2->4\n" +
            "    4[label=\"Const\"]\n" +
            "        0->5\n" +
            "    5[label=\"!=\"]\n" +
            "        5->6\n" +
            "    6[label=\"Variable\"]\n" +
            "        5->7\n" +
            "    7[label=\"Const\"]\n" +
            "}";
    }

    private String complexGraph() {
        return "digraph G {\n" +
            "    0[shape=none, margin=0, label=<\n" +
            "<table border=\"0\" cellborder=\"1\" cellspacing=\"0\" cellpadding=\"4\">\n" +
            "    <tr>\n" +
            "        <td colspan=\"3\">If</td>\n" +
            "    </tr>\n" +
            "</table>\n" +
            ">]\n" +
            "        0->1\n" +
            "    1[shape=none, margin=0, label=<\n" +
            "<table border=\"0\" cellborder=\"1\" cellspacing=\"0\" cellpadding=\"4\">\n" +
            "    <tr>\n" +
            "        <td colspan=\"3\">Const</td>\n" +
            "    </tr>\n" +
            "    <tr>\n" +
            "        <td>value</td>\n" +
            "        <td></td>\n" +
            "        <td>true</td>\n" +
            "    </tr>\n" +
            "</table>\n" +
            ">]\n" +
            "        0->2\n" +
            "    2[shape=none, margin=0, label=<\n" +
            "<table border=\"0\" cellborder=\"1\" cellspacing=\"0\" cellpadding=\"4\">\n" +
            "    <tr>\n" +
            "        <td colspan=\"3\">!=</td>\n" +
            "    </tr>\n" +
            "</table>\n" +
            ">]\n" +
            "        2->3\n" +
            "    3[shape=none, margin=0, label=<\n" +
            "<table border=\"0\" cellborder=\"1\" cellspacing=\"0\" cellpadding=\"4\">\n" +
            "    <tr>\n" +
            "        <td colspan=\"3\">Variable</td>\n" +
            "    </tr>\n" +
            "    <tr>\n" +
            "        <td>value</td>\n" +
            "        <td></td>\n" +
            "        <td>x</td>\n" +
            "    </tr>\n" +
            "</table>\n" +
            ">]\n" +
            "        2->4\n" +
            "    4[shape=none, margin=0, label=<\n" +
            "<table border=\"0\" cellborder=\"1\" cellspacing=\"0\" cellpadding=\"4\">\n" +
            "    <tr>\n" +
            "        <td colspan=\"3\">Const</td>\n" +
            "    </tr>\n" +
            "    <tr>\n" +
            "        <td>value</td>\n" +
            "        <td></td>\n" +
            "        <td>123</td>\n" +
            "    </tr>\n" +
            "</table>\n" +
            ">]\n" +
            "        0->5\n" +
            "    5[shape=none, margin=0, label=<\n" +
            "<table border=\"0\" cellborder=\"1\" cellspacing=\"0\" cellpadding=\"4\">\n" +
            "    <tr>\n" +
            "        <td colspan=\"3\">!=</td>\n" +
            "    </tr>\n" +
            "</table>\n" +
            ">]\n" +
            "        5->6\n" +
            "    6[shape=none, margin=0, label=<\n" +
            "<table border=\"0\" cellborder=\"1\" cellspacing=\"0\" cellpadding=\"4\">\n" +
            "    <tr>\n" +
            "        <td colspan=\"3\">Variable</td>\n" +
            "    </tr>\n" +
            "    <tr>\n" +
            "        <td>value</td>\n" +
            "        <td></td>\n" +
            "        <td>x</td>\n" +
            "    </tr>\n" +
            "</table>\n" +
            ">]\n" +
            "        5->7\n" +
            "    7[shape=none, margin=0, label=<\n" +
            "<table border=\"0\" cellborder=\"1\" cellspacing=\"0\" cellpadding=\"4\">\n" +
            "    <tr>\n" +
            "        <td colspan=\"3\">Const</td>\n" +
            "    </tr>\n" +
            "    <tr>\n" +
            "        <td>value</td>\n" +
            "        <td></td>\n" +
            "        <td>456</td>\n" +
            "    </tr>\n" +
            "</table>\n" +
            ">]\n" +
            "}";
    }

    private String getNodeWrapperGraphComplex() {
        return "digraph G {\n" +
            "    0[shape=none, margin=0, label=<\n" +
            "<table border=\"0\" cellborder=\"1\" cellspacing=\"0\" cellpadding=\"4\">\n" +
            "    <tr>\n" +
            "        <td colspan=\"3\">at.fh.hagenberg.aist.gce.minic.nodes.impl.op.arith.integer.signed.MinicIntArithmeticNodeFactory$MinicIntAddNodeGen</td>\n" +
            "    </tr>\n" +
            "</table>\n" +
            ">]\n" +
            "        0->1\n" +
            "    1[shape=none, margin=0, label=<\n" +
            "<table border=\"0\" cellborder=\"1\" cellspacing=\"0\" cellpadding=\"4\">\n" +
            "    <tr>\n" +
            "        <td colspan=\"3\">at.fh.hagenberg.aist.gce.minic.nodes.impl.literals.MinicSimpleLiteralNode$MinicIntLiteralNode</td>\n" +
            "    </tr>\n" +
            "    <tr>\n" +
            "        <td>value</td>\n" +
            "        <td>int</td>\n" +
            "        <td>3</td>\n" +
            "    </tr>\n" +
            "</table>\n" +
            ">]\n" +
            "        0->2\n" +
            "    2[shape=none, margin=0, label=<\n" +
            "<table border=\"0\" cellborder=\"1\" cellspacing=\"0\" cellpadding=\"4\">\n" +
            "    <tr>\n" +
            "        <td colspan=\"3\">at.fh.hagenberg.aist.gce.minic.nodes.impl.literals.MinicSimpleLiteralNode$MinicIntLiteralNode</td>\n" +
            "    </tr>\n" +
            "    <tr>\n" +
            "        <td>value</td>\n" +
            "        <td>int</td>\n" +
            "        <td>5</td>\n" +
            "    </tr>\n" +
            "</table>\n" +
            ">]\n" +
            "}";
    }

    private String getNodeWrapperGraphSimple() {
        return "digraph G {\n" +
            "    0[label=\"+\"]\n" +
            "        0->1\n" +
            "    1[label=\"IntLiteral\"]\n" +
            "        0->2\n" +
            "    2[label=\"IntLiteral\"]\n" +
            "}";
    }

    private NodeWrapper getNodeWrapper() {
        // this is a copy paste from pattern Mining Test
        NodeWrapper t4 = new NodeWrapper("If");
        NodeWrapper t41 = new NodeWrapper("Const");
        t41.getValues().put("value", "true");
        NodeWrapper t42 = new NodeWrapper("!=");
        NodeWrapper t421 = new NodeWrapper("Variable");
        t421.getValues().put("value", "x");
        NodeWrapper t422 = new NodeWrapper("Const");
        t422.getValues().put("value", "123");
        NodeWrapper t43 = new NodeWrapper("!=");
        NodeWrapper t431 = new NodeWrapper("Variable");
        t431.getValues().put("value", "x");
        NodeWrapper t432 = new NodeWrapper("Const");
        t432.getValues().put("value", "456");
        t4.addChild(t41, "condition", 0);
        t4.addChild(t42, "dhen", 0);
        t4.addChild(t43, "else", 0);
        t42.addChild(t421, "field", 0);
        t42.addChild(t422, "value", 0);
        t43.addChild(t431, "field", 0);
        t43.addChild(t432, "value", 0);
        return t4;
    }
}
