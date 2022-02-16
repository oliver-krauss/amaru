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

import at.fh.hagenberg.aist.gce.pattern.PatternNodeWrapper;
import at.fh.hagenberg.aist.gce.pattern.TruffleDifferentialPatternSolution;
import at.fh.hagenberg.aist.gce.pattern.TrufflePattern;
import at.fh.hagenberg.aist.gce.pattern.TrufflePatternProblem;
import at.fh.hagenberg.aist.gce.pattern.selection.TrufflePatternSearchSpace;
import at.fh.hagenberg.aist.gce.science.statistics.data.template.PatternAnalysisPrinter;
import at.fh.hagenberg.machinelearning.analytics.graph.nodes.NodeWrapper;
import at.fh.hagenberg.machinelearning.analytics.graph.nodes.OrderedRelationship;
import at.fh.hagenberg.machinelearning.core.ProblemGene;
import at.fh.hagenberg.machinelearning.core.Solution;
import at.fh.hagenberg.machinelearning.core.SolutionGene;
import at.fh.hagenberg.machinelearning.core.fitness.Cachet;
import at.fh.hagenberg.util.Pair;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.IOException;
import java.io.StringWriter;
import java.util.*;

/**
 * @author Oliver Krauss on 06.11.2019
 */

public class PatternAnalysisPrinterTest {

    @Test
    public void testPrintPattern() throws IOException {
        // given
        PatternAnalysisPrinter printer = new PatternAnalysisPrinter(null);
        StringWriter writer = new StringWriter();
        printer.setWriter(writer);

        // when
        printer.printPattern(getTestPattern());

        // then
        String s = writer.toString();
        Assert.assertEquals(s, testPatternString());
    }

    @Test
    public void testDebugPrintPattern() throws IOException {
        // given
        PatternAnalysisPrinter printer = new PatternAnalysisPrinter(null, true);
        StringWriter writer = new StringWriter();
        printer.setWriter(writer);

        // when
        printer.printPattern(getTestPattern());

        // then
        String s = writer.toString();
        Assert.assertEquals(s, testDebugPatternString());
    }

    @Test
    public void testPrintDifferentialPattern() throws IOException {
        // given
        PatternAnalysisPrinter printer = new PatternAnalysisPrinter(null, true);
        StringWriter writer = new StringWriter();
        printer.setWriter(writer);

        // when
        printer.printDifferentialPattern(getTestDiffPattern());

        // then
        String s = writer.toString();
        Assert.assertTrue(s.startsWith(testDiffPatternStringHtml()));
    }

    @Test
    public void testPrintSolution() throws IOException {
        // given
        PatternAnalysisPrinter printer = new PatternAnalysisPrinter(null, false);
        StringWriter writer = new StringWriter();
        printer.setWriter(writer);

        // when
        printer.printPatternSolution(getSolution());

        // then
        String s = writer.toString();
        Assert.assertEquals(s, testPatternSolutionStringHtml());
    }

    private String testPatternSolutionStringHtml() {
        return "<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.01 Transitional//EN\" \"http://www.w3.org/TR/html4/loose.dtd\">\n" +
            "<html lang=\"en\">\n" +
            "<head>\n" +
            "    <meta http-equiv=\"content-type\" content=\"text/html; charset=utf-8\">\n" +
            "    <title>Pattern Solution</title>\n" +
            "    <script type=\"text/javascript\" src=\"https://cdnjs.cloudflare.com/ajax/libs/viz.js/2.1.2/viz.js\"></script>\n" +
            "    <script type=\"text/javascript\" src=\"https://cdnjs.cloudflare.com/ajax/libs/viz.js/2.1.2/full.render.js\"></script>\n" +
            "    <script type=\"text/javascript\" src=\"https://cdnjs.cloudflare.com/ajax/libs/plotly.js/1.33.1/plotly.min.js\"></script>\n" +
            "<style>\n" +
            "    .wrapper {\n" +
            "        display: grid;\n" +
            "        grid-template-columns: repeat(3, 1fr);\n" +
            "        grid-gap: 10px;\n" +
            "    }\n" +
            "\n" +
            "    /* Style the tab */\n" +
            "    .tab {\n" +
            "        overflow: hidden;\n" +
            "        border: 1px solid #ccc;\n" +
            "    }\n" +
            "\n" +
            "    /* Style the buttons that are used to open the tab content */\n" +
            "    .tab button {\n" +
            "        background-color: inherit;\n" +
            "        float: left;\n" +
            "        border: none;\n" +
            "        outline: none;\n" +
            "        cursor: pointer;\n" +
            "        padding: 14px 16px;\n" +
            "        transition: 0.3s;\n" +
            "    }\n" +
            "\n" +
            "    /* Change background color of buttons on hover */\n" +
            "    .tab button:hover {\n" +
            "        background-color: #ddd;\n" +
            "    }\n" +
            "\n" +
            "    /* Create an active/current tablink class */\n" +
            "    .tab button.active {\n" +
            "        background-color: #ccc;\n" +
            "    }\n" +
            "\n" +
            "    /* Style the tab content */\n" +
            "    .tabcontent .tabcontent-0 .tabcontent-1 .tabcontent-2 .tabcontent-3 {\n" +
            "        display: none;\n" +
            "        padding: 6px 12px;\n" +
            "        border: 1px solid #ccc;\n" +
            "        border-top: none;\n" +
            "        animation: fadeEffect 1s; /* Fading effect takes 1 second */\n" +
            "    }\n" +
            "\n" +
            "\n" +
            "    /* Go from zero to full opacity */\n" +
            "    @keyframes fadeEffect {\n" +
            "        from {opacity: 0;}\n" +
            "        to {opacity: 1;}\n" +
            "    }\n" +
            "</style></head>\n" +
            "<body>\n" +
            "\n" +
            "    <table border=\"1\" cellborder=\"1\" cellspacing=\"0\" cellpadding=\"4\">\n" +
            "        <tr>\n" +
            "            <td colspan=\"2\">4.1</td>\n" +
            "        </tr>\n" +
            "            <tr>\n" +
            "                <td>niceness</td>\n" +
            "                <td>2.8</td>\n" +
            "            </tr>\n" +
            "            <tr>\n" +
            "                <td>neatness</td>\n" +
            "                <td>1.3</td>\n" +
            "            </tr>\n" +
            "    </table>\n" +
            "\n" +
            "<!-- Tab links -->\n" +
            "<div class=\"tab\">\n" +
            "    <button class=\"tablinks-0\" onclick=\"openTab(event, 'ProblemDef', 0)\">Problem Definitions</button>\n" +
            "    <button id=\"defaultTab\" class=\"tablinks-0\" onclick=\"openTab(event, 'Patterns', 0)\">Patterns</button>\n" +
            "</div>\n" +
            "\n" +
            "<div id=\"ProblemDef\" class=\"tabcontent-0\">\n" +
            "\n" +
            "    <div class=\"tab\">\n" +
            "            <button class=\"tablinks-1\" onclick=\"openTab(event, 'problemdef-A', 1)\">A</button>\n" +
            "            <button class=\"tablinks-1\" onclick=\"openTab(event, 'problemdef-B', 1)\">B</button>\n" +
            "    </div>\n" +
            "\n" +
            "<h1>Problem Definitions</h1>\n" +
            "<div id=\"problemdef-A\" class=\"tabcontent-1\">\n" +
            "<h2>A - (2 trees)</h2>\n" +
            "    \n" +
            "<div class=\"graphviz\">\n" +
            "digraph G {\n" +
            "    1[label=\"+\"]\n" +
            "    2[label=\"1\"]\n" +
            "    3[label=\"x\"]\n" +
            "    1->2\n" +
            "    1->3\n" +
            "}\n" +
            "</div>\n" +
            "<div class=\"graphviz\">\n" +
            "digraph G {\n" +
            "    5[label=\"-\"]\n" +
            "    6[label=\"5\"]\n" +
            "    7[label=\"y\"]\n" +
            "    5->6\n" +
            "    5->7\n" +
            "}\n" +
            "</div>\n" +
            "</div>\n" +
            "<div id=\"problemdef-B\" class=\"tabcontent-1\">\n" +
            "<h2>B - (0 trees)</h2>\n" +
            "    \n" +
            "</div>\n" +
            "</div>\n" +
            "\n" +
            "<div id=\"Patterns\" class=\"tabcontent-0\">\n" +
            "<h1>Patterns</h1>\n" +
            "\n" +
            "    <div class=\"tab\">\n" +
            "            <button class=\"tablinks-2\" onclick=\"openTab(event, 'patterns-A', 2)\">A</button>\n" +
            "            <button class=\"tablinks-2\" onclick=\"openTab(event, 'patterns-B', 2)\">B</button>\n" +
            "    </div>\n" +
            "\n" +
            "<div id=\"patterns-A\" class=\"tabcontent-2\">\n" +
            "<h2>A</h2>\n" +
            " <div class=\"wrapper\">\n" +
            "        <div>\n" +
            "<table class=\"tg\" idList=\"1,2,3,4,5,6,7,8,9,10,11,12,13,14,15\">\n" +
            "    <tr>\n" +
            "        <td>Occurence / Trees</td>\n" +
            "        <td>5 / 5</td>\n" +
            "    </tr>\n" +
            "</table>\n" +
            "<div class=\"graphviz\">\n" +
            "digraph G {\n" +
            "    _1_2_3_4_5[label=\"+\"]\n" +
            "        _1_2_3_4_5->_6_7_8_9_10\n" +
            "    _6_7_8_9_10[label=\"1\"]\n" +
            "        _1_2_3_4_5->_11_12_13_14_15\n" +
            "    _11_12_13_14_15[label=\"x\"]\n" +
            "}\n" +
            "</div>        </div>\n" +
            "        <div>\n" +
            "<table class=\"tg\" idList=\"1,2,3,4,5,6,7,8,9,10,11,12,13,14,15\">\n" +
            "    <tr>\n" +
            "        <td>Occurence / Trees</td>\n" +
            "        <td>5 / 5</td>\n" +
            "    </tr>\n" +
            "</table>\n" +
            "<div class=\"graphviz\">\n" +
            "digraph G {\n" +
            "    _1_2_3_4_5[label=\"+\"]\n" +
            "        _1_2_3_4_5->_6_7_8_9_10\n" +
            "    _6_7_8_9_10[label=\"1\"]\n" +
            "        _1_2_3_4_5->_11_12_13_14_15\n" +
            "    _11_12_13_14_15[label=\"x\"]\n" +
            "}\n" +
            "</div>        </div>\n" +
            " </div>\n" +
            "</div>\n" +
            "<div id=\"patterns-B\" class=\"tabcontent-2\">\n" +
            "<h2>B</h2>\n" +
            " <div class=\"wrapper\">\n" +
            "        <div>\n" +
            "<table class=\"tg\" idList=\"1,2,3,4,5,6,7,8,9,10,11,12,13,14,15\">\n" +
            "    <tr>\n" +
            "        <td>Occurence / Trees</td>\n" +
            "        <td>5 / 5</td>\n" +
            "    </tr>\n" +
            "</table>\n" +
            "<div class=\"graphviz\">\n" +
            "digraph G {\n" +
            "    _1_2_3_4_5[label=\"+\"]\n" +
            "        _1_2_3_4_5->_6_7_8_9_10\n" +
            "    _6_7_8_9_10[label=\"1\"]\n" +
            "        _1_2_3_4_5->_11_12_13_14_15\n" +
            "    _11_12_13_14_15[label=\"x\"]\n" +
            "}\n" +
            "</div>        </div>\n" +
            "        <div>\n" +
            "<table class=\"tg\" idList=\"1,2,3,4,5,6,7,8,9,10,11,12,13,14,15\">\n" +
            "    <tr>\n" +
            "        <td>Occurence / Trees</td>\n" +
            "        <td>5 / 5</td>\n" +
            "    </tr>\n" +
            "</table>\n" +
            "<div class=\"graphviz\">\n" +
            "digraph G {\n" +
            "    _1_2_3_4_5[label=\"+\"]\n" +
            "        _1_2_3_4_5->_6_7_8_9_10\n" +
            "    _6_7_8_9_10[label=\"1\"]\n" +
            "        _1_2_3_4_5->_11_12_13_14_15\n" +
            "    _11_12_13_14_15[label=\"x\"]\n" +
            "}\n" +
            "</div>        </div>\n" +
            " </div>\n" +
            "</div>\n" +
            "</div>\n" +
            "\n" +
            "    <script>\n" +
            "        // start listening to keypresses\n" +
            "        document.addEventListener(\"keydown\", logKeyDown);\n" +
            "        document.addEventListener(\"keyup\", logKeyUp);\n" +
            "        var pressCtrl = false;\n" +
            "\n" +
            "        function logKeyDown(e) {\n" +
            "            if (e.keyCode === 17) {\n" +
            "                pressCtrl = true;\n" +
            "            }\n" +
            "        }\n" +
            "\n" +
            "        function logKeyUp(e) {\n" +
            "            if (e.keyCode === 17) {\n" +
            "                pressCtrl = false;\n" +
            "            }\n" +
            "        }\n" +
            "\n" +
            "        var idList = [];\n" +
            "        var callerList = [];\n" +
            "\n" +
            "        function toggleNode(caller, toggleId) {\n" +
            "\n" +
            "            if (!Array.isArray(toggleId)) {\n" +
            "                if (toggleId.startsWith('_')) {\n" +
            "                    toggleId = toggleId.substring(1).split('_');\n" +
            "                } else {\n" +
            "                    toggleId = [toggleId];\n" +
            "                }\n" +
            "            }\n" +
            "            // clean id list\n" +
            "            if (!pressCtrl) {\n" +
            "                idList = toggleId\n" +
            "                callerList = [caller];\n" +
            "            } else {\n" +
            "                Array.prototype.push.apply(idList, toggleId);\n" +
            "                callerList.push(caller);\n" +
            "            }\n" +
            "\n" +
            "            // mark all nodes that are in ID list as blue\n" +
            "            Array.from(document.getElementsByClassName(\"node\")).forEach(function (node) {\n" +
            "                let id = node.children[0].innerHTML;\n" +
            "                if (id.startsWith('_')) {\n" +
            "                    let idArray = id.substring(1).split('_');\n" +
            "                    if (idList.some(value => idArray.includes(value))) {\n" +
            "                        node.children[1].style.fill = \"lightblue\"\n" +
            "                    } else {\n" +
            "                        node.children[1].style.fill = \"none\"\n" +
            "                    }\n" +
            "                } else {\n" +
            "                    if (idList.includes(id)) {\n" +
            "                        node.children[1].style.fill = \"lightblue\"\n" +
            "                    } else {\n" +
            "                        node.children[1].style.fill = \"none\"\n" +
            "                    }\n" +
            "                }\n" +
            "            });\n" +
            "\n" +
            "            // mark all nodes that we clicked on originally as red\n" +
            "            callerList.forEach(function (caller) {\n" +
            "                if (caller.children[1].nodeName === \"ellipse\") {\n" +
            "                    caller.children[1].style.fill = \"lightsalmon\"\n" +
            "                } else {\n" +
            "                    Array.from(caller.children).forEach(function (g) {\n" +
            "                        if (g.classList.contains(\"node\")) {\n" +
            "                            g.children[1].style.fill = \"lightsalmon\"\n" +
            "                        }\n" +
            "                    })\n" +
            "                }\n" +
            "            });\n" +
            "        }\n" +
            "\n" +
            "        var viz = new Viz();\n" +
            "        Array.from(document.getElementsByClassName(\"graphviz\")).forEach(function (graph) {\n" +
            "            viz.renderSVGElement(graph.textContent)\n" +
            "                .then(function (element) {\n" +
            "                    // add onclick to nodes\n" +
            "                    Array.from(element.children[0].children).forEach(function (node) {\n" +
            "                        if (node.classList.contains(\"node\")) {\n" +
            "                            let id = node.children[0].innerHTML;\n" +
            "                            node.onclick = function (event) {\n" +
            "                                event.stopPropagation();\n" +
            "                                toggleNode(node, id);\n" +
            "                            }\n" +
            "                        }\n" +
            "                    });\n" +
            "                    // add onclick to graph -> either as graph or as pattern\n" +
            "                    if (graph.parentNode.children[0].nodeName === \"TABLE\" && graph.parentNode.children[0].attributes.idList != null) {\n" +
            "                        let idList = graph.parentNode.children[0].attributes.idList.value.split(\",\");\n" +
            "                        element.children[0].onclick = function () {\n" +
            "                            toggleNode(element.children[0], idList);\n" +
            "                        };\n" +
            "                    } else {\n" +
            "                        let ids = [];\n" +
            "                        // collect ids\n" +
            "                        Array.from(element.children[0].children).forEach(function (node) {\n" +
            "                            if (node.classList.contains(\"node\")) {\n" +
            "                                ids.push(node.children[0].innerHTML);\n" +
            "                            }\n" +
            "                        });\n" +
            "                        element.children[0].onclick = function () {\n" +
            "                            toggleNode(element.children[0], ids);\n" +
            "                        };\n" +
            "                    }\n" +
            "                    graph.parentNode.replaceChild(element, graph);\n" +
            "                });\n" +
            "        });\n" +
            "    </script>\n" +
            "    <script>\n" +
            "        function openTab(evt, tabName, level) {\n" +
            "            // Declare all variables\n" +
            "            var i, tabcontent, tablinks;\n" +
            "\n" +
            "            // Get all elements with class=\"tabcontent\" and hide them\n" +
            "            tabcontent = document.getElementsByClassName(\"tabcontent-\" + level);\n" +
            "            for (i = 0; i < tabcontent.length; i++) {\n" +
            "                tabcontent[i].style.display = \"none\";\n" +
            "            }\n" +
            "\n" +
            "            // Get all elements with class=\"tablinks\" and remove the class \"active\"\n" +
            "            tablinks = document.getElementsByClassName(\"tablinks-\" + level);\n" +
            "            for (i = 0; i < tablinks.length; i++) {\n" +
            "                tablinks[i].className = tablinks[i].className.replace(\" active\", \"\");\n" +
            "            }\n" +
            "\n" +
            "            // Show the current tab, and add an \"active\" class to the button that opened the tab\n" +
            "            document.getElementById(tabName).style.display = \"block\";\n" +
            "            evt.currentTarget.className += \" active\";\n" +
            "        }\n" +
            "\n" +
            "        // Get the element with id=\"defaultOpen\" and click on it\n" +
            "        document.getElementById(\"defaultTab\").click();\n" +
            "    </script>\n" +
            "</body>\n" +
            "</html>";
    }

    @Test
    public void testPrintSolutionMd() throws IOException {
        // given
        PatternAnalysisPrinter printer = new PatternAnalysisPrinter(null, false);
        printer.setFormat("md");
        StringWriter writer = new StringWriter();
        printer.setWriter(writer);

        // when
        printer.printPatternSolution(getSolution());

        // then
        String s = writer.toString();
        Assert.assertEquals(s, testPatternSolutionStringMd());
    }

    private String testPatternSolutionStringMd() {
        return "\n" +
            "    <table border=\"1\" cellborder=\"1\" cellspacing=\"0\" cellpadding=\"4\">\n" +
            "        <tr>\n" +
            "            <td colspan=\"2\">4.1</td>\n" +
            "        </tr>\n" +
            "            <tr>\n" +
            "                <td>niceness</td>\n" +
            "                <td>2.8</td>\n" +
            "            </tr>\n" +
            "            <tr>\n" +
            "                <td>neatness</td>\n" +
            "                <td>1.3</td>\n" +
            "            </tr>\n" +
            "    </table>\n" +
            "\n" +
            "# Problem Definitions\n" +
            "## A\n" +
            "    \n" +
            "```dot\n" +
            "digraph G {\n" +
            "    1[label=\"+\"]\n" +
            "    2[label=\"1\"]\n" +
            "    3[label=\"x\"]\n" +
            "    1->2\n" +
            "    1->3\n" +
            "}\n" +
            "```\n" +
            "```dot\n" +
            "digraph G {\n" +
            "    5[label=\"-\"]\n" +
            "    6[label=\"5\"]\n" +
            "    7[label=\"y\"]\n" +
            "    5->6\n" +
            "    5->7\n" +
            "}\n" +
            "```\n" +
            "\n" +
            "## B\n" +
            "    \n" +
            "\n" +
            "# Patterns per Problem\n" +
            "\n" +
            "## A\n" +
            "<table class=\"tg\" idList=\"1,2,3,4,5,6,7,8,9,10,11,12,13,14,15\">\n" +
            "    <tr>\n" +
            "        <td>Occurence / Trees</td>\n" +
            "        <td>5 / 5</td>\n" +
            "    </tr>\n" +
            "</table>\n" +
            "```dot\n" +
            "digraph G {\n" +
            "    _1_2_3_4_5[label=\"+\"]\n" +
            "        _1_2_3_4_5->_6_7_8_9_10\n" +
            "    _6_7_8_9_10[label=\"1\"]\n" +
            "        _1_2_3_4_5->_11_12_13_14_15\n" +
            "    _11_12_13_14_15[label=\"x\"]\n" +
            "}\n" +
            "```<table class=\"tg\" idList=\"1,2,3,4,5,6,7,8,9,10,11,12,13,14,15\">\n" +
            "    <tr>\n" +
            "        <td>Occurence / Trees</td>\n" +
            "        <td>5 / 5</td>\n" +
            "    </tr>\n" +
            "</table>\n" +
            "```dot\n" +
            "digraph G {\n" +
            "    _1_2_3_4_5[label=\"+\"]\n" +
            "        _1_2_3_4_5->_6_7_8_9_10\n" +
            "    _6_7_8_9_10[label=\"1\"]\n" +
            "        _1_2_3_4_5->_11_12_13_14_15\n" +
            "    _11_12_13_14_15[label=\"x\"]\n" +
            "}\n" +
            "```\n" +
            "## B\n" +
            "<table class=\"tg\" idList=\"1,2,3,4,5,6,7,8,9,10,11,12,13,14,15\">\n" +
            "    <tr>\n" +
            "        <td>Occurence / Trees</td>\n" +
            "        <td>5 / 5</td>\n" +
            "    </tr>\n" +
            "</table>\n" +
            "```dot\n" +
            "digraph G {\n" +
            "    _1_2_3_4_5[label=\"+\"]\n" +
            "        _1_2_3_4_5->_6_7_8_9_10\n" +
            "    _6_7_8_9_10[label=\"1\"]\n" +
            "        _1_2_3_4_5->_11_12_13_14_15\n" +
            "    _11_12_13_14_15[label=\"x\"]\n" +
            "}\n" +
            "```<table class=\"tg\" idList=\"1,2,3,4,5,6,7,8,9,10,11,12,13,14,15\">\n" +
            "    <tr>\n" +
            "        <td>Occurence / Trees</td>\n" +
            "        <td>5 / 5</td>\n" +
            "    </tr>\n" +
            "</table>\n" +
            "```dot\n" +
            "digraph G {\n" +
            "    _1_2_3_4_5[label=\"+\"]\n" +
            "        _1_2_3_4_5->_6_7_8_9_10\n" +
            "    _6_7_8_9_10[label=\"1\"]\n" +
            "        _1_2_3_4_5->_11_12_13_14_15\n" +
            "    _11_12_13_14_15[label=\"x\"]\n" +
            "}\n" +
            "```\n";
    }

    private Solution<TrufflePattern, TrufflePatternProblem> getSolution() {
        Solution s = new Solution();
        s.getCachets().add(new Cachet(2.8, "niceness"));
        s.getCachets().add(new Cachet(1.3, "neatness"));
        s.setQuality(1.3 + 2.8);

        TrufflePatternSearchSpace ssA = getTestSearchSpace();
        TrufflePatternSearchSpace ssB = new TrufflePatternSearchSpace();
        TrufflePatternProblem problemA = new TrufflePatternProblem("c", ssA, "A");
        TrufflePatternProblem problemB = new TrufflePatternProblem("c", ssB, "B");


        s.addGene(new SolutionGene(getTestPattern(), Arrays.asList(new ProblemGene(problemA))));
        s.addGene(new SolutionGene(getTestPattern(), Arrays.asList(new ProblemGene(problemB))));
        s.addGene(new SolutionGene(getTestPattern(), Arrays.asList(new ProblemGene(problemA), new ProblemGene(problemB))));

        return s;
    }

    private String testDiffPatternStringHtml() {
        return "<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.01 Transitional//EN\" \"http://www.w3.org/TR/html4/loose.dtd\">\n" +
            "<html lang=\"en\">\n" +
            "<head>\n" +
            "    <meta http-equiv=\"content-type\" content=\"text/html; charset=utf-8\">\n" +
            "    <title>Differential Pattern Analysis</title>\n" +
            "    <script type=\"text/javascript\" src=\"https://cdnjs.cloudflare.com/ajax/libs/viz.js/2.1.2/viz.js\"></script>\n" +
            "    <script type=\"text/javascript\" src=\"https://cdnjs.cloudflare.com/ajax/libs/viz.js/2.1.2/full.render.js\"></script>\n" +
            "    <script type=\"text/javascript\" src=\"https://cdnjs.cloudflare.com/ajax/libs/plotly.js/1.33.1/plotly.min.js\"></script>\n" +
            "<style>\n" +
            "    .wrapper {\n" +
            "        display: grid;\n" +
            "        grid-template-columns: repeat(3, 1fr);\n" +
            "        grid-gap: 10px;\n" +
            "    }\n" +
            "\n" +
            "    /* Style the tab */\n" +
            "    .tab {\n" +
            "        overflow: hidden;\n" +
            "        border: 1px solid #ccc;\n" +
            "    }\n" +
            "\n" +
            "    /* Style the buttons that are used to open the tab content */\n" +
            "    .tab button {\n" +
            "        background-color: inherit;\n" +
            "        float: left;\n" +
            "        border: none;\n" +
            "        outline: none;\n" +
            "        cursor: pointer;\n" +
            "        padding: 14px 16px;\n" +
            "        transition: 0.3s;\n" +
            "    }\n" +
            "\n" +
            "    /* Change background color of buttons on hover */\n" +
            "    .tab button:hover {\n" +
            "        background-color: #ddd;\n" +
            "    }\n" +
            "\n" +
            "    /* Create an active/current tablink class */\n" +
            "    .tab button.active {\n" +
            "        background-color: #ccc;\n" +
            "    }\n" +
            "\n" +
            "    /* Style the tab content */\n" +
            "    .tabcontent .tabcontent-0 .tabcontent-1 .tabcontent-2 .tabcontent-3 {\n" +
            "        display: none;\n" +
            "        padding: 6px 12px;\n" +
            "        border: 1px solid #ccc;\n" +
            "        border-top: none;\n" +
            "        animation: fadeEffect 1s; /* Fading effect takes 1 second */\n" +
            "    }\n" +
            "\n" +
            "\n" +
            "    /* Go from zero to full opacity */\n" +
            "    @keyframes fadeEffect {\n" +
            "        from {opacity: 0;}\n" +
            "        to {opacity: 1;}\n" +
            "    }\n" +
            "</style></head>\n" +
            "<body>\n" +
            "\n" +
            "<!-- Tab links -->\n" +
            "<div class=\"tab\">\n" +
            "    <button class=\"tablinks-0\" onclick=\"openTab(event, 'ProblemDef', 0)\">Problem Definitions</button>\n" +
            "    <button class=\"tablinks-0\" onclick=\"openTab(event, 'Patterns', 0)\">Patterns per Problem</button>\n" +
            "    <button id=\"defaultTab\" class=\"tablinks-0\" onclick=\"openTab(event, 'Diff', 0)\">Differential between Problems</button>\n" +
            "</div>\n" +
            "\n" +
            "<div id=\"ProblemDef\" class=\"tabcontent-0\">\n" +
            "\n" +
            "    <div class=\"tab\">\n" +
            "            <button class=\"tablinks-1\" onclick=\"openTab(event, 'problemdef-A', 1)\">A</button>\n" +
            "            <button class=\"tablinks-1\" onclick=\"openTab(event, 'problemdef-B', 1)\">B</button>\n" +
            "            <button class=\"tablinks-1\" onclick=\"openTab(event, 'problemdef-C', 1)\">C</button>\n" +
            "    </div>\n" +
            "\n" +
            "<h1>Problem Definitions</h1>\n" +
            "<div id=\"problemdef-A\" class=\"tabcontent-1\">\n" +
            "<h2>A - (2 trees)</h2>\n" +
            "    MATCH (n) WHERE id(n) IN [1, 5] RETURN (n)\n" +
            "<div class=\"graphviz\">\n" +
            "digraph G {\n" +
            "    1[label=\"+\"]\n" +
            "    2[label=\"1\"]\n" +
            "    3[label=\"x\"]\n" +
            "    1->2\n" +
            "    1->3\n" +
            "}\n" +
            "</div>\n" +
            "<div class=\"graphviz\">\n" +
            "digraph G {\n" +
            "    5[label=\"-\"]\n" +
            "    6[label=\"5\"]\n" +
            "    7[label=\"y\"]\n" +
            "    5->6\n" +
            "    5->7\n" +
            "}\n" +
            "</div>\n" +
            "</div>\n" +
            "<div id=\"problemdef-B\" class=\"tabcontent-1\">\n" +
            "<h2>B - (0 trees)</h2>\n" +
            "    MATCH (n) WHERE id(n) IN [] RETURN (n)\n" +
            "</div>\n" +
            "<div id=\"problemdef-C\" class=\"tabcontent-1\">\n" +
            "<h2>C - (0 trees)</h2>\n" +
            "    MATCH (n) WHERE id(n) IN [] RETURN (n)\n" +
            "</div>\n" +
            "</div>\n" +
            "\n" +
            "<div id=\"Patterns\" class=\"tabcontent-0\">\n" +
            "<h1>Patterns per Problem</h1>\n" +
            "\n" +
            "<div class=\"wrapper\">\n" +
            "    <div id=\"overlapRadarChart\"></div>\n" +
            "    <script>\n" +
            "        data = [\n" +
            "            {\n" +
            "            type: 'scatterpolar',\n" +
            "            r: [5],\n" +
            "            theta: [0,360],\n" +
            "            thetaunit: \"degrees\",\n" +
            "            fill: 'toself',\n" +
            "            name: \"A\"\n" +
            "        },\n" +
            "            {\n" +
            "            type: 'scatterpolar',\n" +
            "            r: [5],\n" +
            "            theta: [0,360],\n" +
            "            thetaunit: \"degrees\",\n" +
            "            fill: 'toself',\n" +
            "            name: \"B\"\n" +
            "        },\n" +
            "            {\n" +
            "            type: 'scatterpolar',\n" +
            "            r: [5],\n" +
            "            theta: [0,360],\n" +
            "            thetaunit: \"degrees\",\n" +
            "            fill: 'toself',\n" +
            "            name: \"C\"\n" +
            "        }\n" +
            "        ]\n" +
            "        layout = {\n" +
            "            width: 500,\n" +
            "            height: 500,\n" +
            "            polar: {\n" +
            "                radialaxis: {\n" +
            "                    visible: true,\n" +
            "                    range: [0, 5]\n" +
            "                }\n" +
            "            }\n" +
            "        }\n" +
            "\n" +
            "        Plotly.newPlot('overlapRadarChart', data, layout);\n" +
            "    </script>\n" +
            "    <div id=\"fullOverlapRadarChart\"></div>\n\n" +
            "    <script>\n" +
            "        data = [\n" +
            "            {\n" +
            "                type: 'scatterpolar',\n" +
            "                r: [5,5,0,5,5,5],\n" +
            "                theta: [0,72,144,216,288,360,432],\n" +
            "                thetaunit: \"degrees\",\n" +
            "                fill: 'toself',\n" +
            "                name: \"A\"\n" +
            "            },\n" +
            "            {\n" +
            "                type: 'scatterpolar',\n" +
            "                r: [0,0,5,5,0,5],\n" +
            "                theta: [0,72,144,216,288,360,432],\n" +
            "                thetaunit: \"degrees\",\n" +
            "                fill: 'toself',\n" +
            "                name: \"B\"\n" +
            "            },\n" +
            "            {\n" +
            "                type: 'scatterpolar',\n" +
            "                r: [0,0,0,0,5,5],\n" +
            "                theta: [0,72,144,216,288,360,432],\n" +
            "                thetaunit: \"degrees\",\n" +
            "                fill: 'toself',\n" +
            "                name: \"C\"\n" +
            "            }\n" +
            "        ]\n" +
            "        layout = {\n" +
            "            width: 500,\n" +
            "            height: 500,\n" +
            "            polar: {\n" +
            "                radialaxis: {\n" +
            "                    visible: true,\n" +
            "                    range: [0, 5]\n" +
            "                }\n" +
            "            }\n" +
            "        }\n" +
            "\n" +
            "        Plotly.newPlot('fullOverlapRadarChart', data, layout);\n" +
            "    </script>\n" +
            "    <div id=\"patternGroupings\"></div>\n" +
            "    <script>\n" +
            "        var data = [\n" +
            "            {\n" +
            "                x: ['A','B','C','A-B','A-C','B-C','A-B-C'],\n" +
            "                y: [2,1,0,1,1,0,1],\n" +
            "                type: 'bar'\n" +
            "            }\n" +
            "        ];\n" +
            "\n" +
            "        Plotly.newPlot('patternGroupings', data);\n" +
            "    </script>\n" +
            "</div>\n" +
            "\n" +
            "    <div class=\"tab\">\n" +
            "            <button class=\"tablinks-2\" onclick=\"openTab(event, 'patterns-A', 2)\">A</button>\n" +
            "            <button class=\"tablinks-2\" onclick=\"openTab(event, 'patterns-B', 2)\">B</button>\n" +
            "            <button class=\"tablinks-2\" onclick=\"openTab(event, 'patterns-C', 2)\">C</button>\n" +
            "    </div>\n" +
            "\n" +
            "<div id=\"patterns-A\" class=\"tabcontent-2\">\n" +
            "<h2>A - (5 patterns)</h2>\n" +
            " <div class=\"wrapper\">\n" +
            "        <div>\n" +
            "<table class=\"tg\" idList=\"1,2,3,4,5,6,7,8,9,10,11,12,13,14,15\">\n" +
            "    <tr>\n" +
            "        <td>Occurence / Trees</td>\n" +
            "        <td>5 / 5</td>\n" +
            "    </tr>\n" +
            "    <tr>\n" +
            "        <td>Trees</td>\n" +
            "        <td>1, 2, 3, 4, 5</td>\n" +
            "    </tr>\n" +
            "    <tr>\n" +
            "        <td>Nodes</td>\n" +
            "        <td>1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15</td>\n" +
            "    </tr>\n" +
            "    <tr>\n" +
            "        <td>Debug Queries:</td>\n" +
            "        <td>\n" +
            "            <table class=\"nested\">\n" +
            "                <tr>\n" +
            "                    <td>Pattern in DB</td>\n" +
            "                    <td>MATCH (n) WHERE id(n) IN [1, 2, 3, 4, 5] RETURN (n)</td>\n" +
            "                </tr>\n" +
            "                <tr>\n" +
            "                    <td>All Nodes in DB</td>\n" +
            "                    <td>MATCH (n) WHERE id(n) IN [1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15] RETURN (n)</td>\n" +
            "                </tr>\n" +
            "            </table>\n" +
            "        </td>\n" +
            "    </tr>\n" +
            "</table>\n" +
            "<div class=\"graphviz\">\n" +
            "digraph G {\n" +
            "    _1_2_3_4_5[shape=none, margin=0, label=<\n" +
            "<table border=\"0\" cellborder=\"1\" cellspacing=\"0\" cellpadding=\"4\">\n" +
            "    <tr>\n" +
            "        <td colspan=\"3\">+</td>\n" +
            "    </tr>\n" +
            "</table>\n" +
            ">]\n" +
            "        _1_2_3_4_5->_6_7_8_9_10\n" +
            "    _6_7_8_9_10[shape=none, margin=0, label=<\n" +
            "<table border=\"0\" cellborder=\"1\" cellspacing=\"0\" cellpadding=\"4\">\n" +
            "    <tr>\n" +
            "        <td colspan=\"3\">1</td>\n" +
            "    </tr>\n" +
            "</table>\n" +
            ">]\n" +
            "        _1_2_3_4_5->_11_12_13_14_15\n" +
            "    _11_12_13_14_15[shape=none, margin=0, label=<\n" +
            "<table border=\"0\" cellborder=\"1\" cellspacing=\"0\" cellpadding=\"4\">\n" +
            "    <tr>\n" +
            "        <td colspan=\"3\">x</td>\n" +
            "    </tr>\n" +
            "</table>\n" +
            ">]\n" +
            "}\n" +
            "</div>        </div>\n" +
            "        <div>\n" +
            "<table class=\"tg\" idList=\"1,2,3,4,5,6,7,8,9,10,11,12,13,14,15\">\n" +
            "    <tr>\n" +
            "        <td>Occurence / Trees</td>\n" +
            "        <td>5 / 5</td>\n" +
            "    </tr>\n" +
            "    <tr>\n" +
            "        <td>Trees</td>\n" +
            "        <td>1, 2, 3, 4, 5</td>\n" +
            "    </tr>\n" +
            "    <tr>\n" +
            "        <td>Nodes</td>\n" +
            "        <td>1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15</td>\n" +
            "    </tr>\n" +
            "    <tr>\n" +
            "        <td>Debug Queries:</td>\n" +
            "        <td>\n" +
            "            <table class=\"nested\">\n" +
            "                <tr>\n" +
            "                    <td>Pattern in DB</td>\n" +
            "                    <td>MATCH (n) WHERE id(n) IN [1, 2, 3, 4, 5] RETURN (n)</td>\n" +
            "                </tr>\n" +
            "                <tr>\n" +
            "                    <td>All Nodes in DB</td>\n" +
            "                    <td>MATCH (n) WHERE id(n) IN [1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15] RETURN (n)</td>\n" +
            "                </tr>\n" +
            "            </table>\n" +
            "        </td>\n" +
            "    </tr>\n" +
            "</table>\n" +
            "<div class=\"graphviz\">\n" +
            "digraph G {\n" +
            "    _1_2_3_4_5[shape=none, margin=0, label=<\n" +
            "<table border=\"0\" cellborder=\"1\" cellspacing=\"0\" cellpadding=\"4\">\n" +
            "    <tr>\n" +
            "        <td colspan=\"3\">+</td>\n" +
            "    </tr>\n" +
            "</table>\n" +
            ">]\n" +
            "        _1_2_3_4_5->_6_7_8_9_10\n" +
            "    _6_7_8_9_10[shape=none, margin=0, label=<\n" +
            "<table border=\"0\" cellborder=\"1\" cellspacing=\"0\" cellpadding=\"4\">\n" +
            "    <tr>\n" +
            "        <td colspan=\"3\">1</td>\n" +
            "    </tr>\n" +
            "</table>\n" +
            ">]\n" +
            "        _1_2_3_4_5->_11_12_13_14_15\n" +
            "    _11_12_13_14_15[shape=none, margin=0, label=<\n" +
            "<table border=\"0\" cellborder=\"1\" cellspacing=\"0\" cellpadding=\"4\">\n" +
            "    <tr>\n" +
            "        <td colspan=\"3\">x</td>\n" +
            "    </tr>\n" +
            "</table>\n" +
            ">]\n" +
            "}\n" +
            "</div>        </div>\n" +
            "        <div>\n" +
            "<table class=\"tg\" idList=\"1,2,3,4,5,6,7,8,9,10,11,12,13,14,15\">\n" +
            "    <tr>\n" +
            "        <td>Occurence / Trees</td>\n" +
            "        <td>5 / 5</td>\n" +
            "    </tr>\n" +
            "    <tr>\n" +
            "        <td>Trees</td>\n" +
            "        <td>1, 2, 3, 4, 5</td>\n" +
            "    </tr>\n" +
            "    <tr>\n" +
            "        <td>Nodes</td>\n" +
            "        <td>1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15</td>\n" +
            "    </tr>\n" +
            "    <tr>\n" +
            "        <td>Debug Queries:</td>\n" +
            "        <td>\n" +
            "            <table class=\"nested\">\n" +
            "                <tr>\n" +
            "                    <td>Pattern in DB</td>\n" +
            "                    <td>MATCH (n) WHERE id(n) IN [1, 2, 3, 4, 5] RETURN (n)</td>\n" +
            "                </tr>\n" +
            "                <tr>\n" +
            "                    <td>All Nodes in DB</td>\n" +
            "                    <td>MATCH (n) WHERE id(n) IN [1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15] RETURN (n)</td>\n" +
            "                </tr>\n" +
            "            </table>\n" +
            "        </td>\n" +
            "    </tr>\n" +
            "</table>\n" +
            "<div class=\"graphviz\">\n" +
            "digraph G {\n" +
            "    _1_2_3_4_5[shape=none, margin=0, label=<\n" +
            "<table border=\"0\" cellborder=\"1\" cellspacing=\"0\" cellpadding=\"4\">\n" +
            "    <tr>\n" +
            "        <td colspan=\"3\">+</td>\n" +
            "    </tr>\n" +
            "</table>\n" +
            ">]\n" +
            "        _1_2_3_4_5->_6_7_8_9_10\n" +
            "    _6_7_8_9_10[shape=none, margin=0, label=<\n" +
            "<table border=\"0\" cellborder=\"1\" cellspacing=\"0\" cellpadding=\"4\">\n" +
            "    <tr>\n" +
            "        <td colspan=\"3\">1</td>\n" +
            "    </tr>\n" +
            "</table>\n" +
            ">]\n" +
            "        _1_2_3_4_5->_11_12_13_14_15\n" +
            "    _11_12_13_14_15[shape=none, margin=0, label=<\n" +
            "<table border=\"0\" cellborder=\"1\" cellspacing=\"0\" cellpadding=\"4\">\n" +
            "    <tr>\n" +
            "        <td colspan=\"3\">x</td>\n" +
            "    </tr>\n" +
            "</table>\n" +
            ">]\n" +
            "}\n" +
            "</div>        </div>\n" +
            "        <div>\n" +
            "<table class=\"tg\" idList=\"1,2,3,4,5,6,7,8,9,10,11,12,13,14,15\">\n" +
            "    <tr>\n" +
            "        <td>Occurence / Trees</td>\n" +
            "        <td>5 / 5</td>\n" +
            "    </tr>\n" +
            "    <tr>\n" +
            "        <td>Trees</td>\n" +
            "        <td>1, 2, 3, 4, 5</td>\n" +
            "    </tr>\n" +
            "    <tr>\n" +
            "        <td>Nodes</td>\n" +
            "        <td>1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15</td>\n" +
            "    </tr>\n" +
            "    <tr>\n" +
            "        <td>Debug Queries:</td>\n" +
            "        <td>\n" +
            "            <table class=\"nested\">\n" +
            "                <tr>\n" +
            "                    <td>Pattern in DB</td>\n" +
            "                    <td>MATCH (n) WHERE id(n) IN [1, 2, 3, 4, 5] RETURN (n)</td>\n" +
            "                </tr>\n" +
            "                <tr>\n" +
            "                    <td>All Nodes in DB</td>\n" +
            "                    <td>MATCH (n) WHERE id(n) IN [1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15] RETURN (n)</td>\n" +
            "                </tr>\n" +
            "            </table>\n" +
            "        </td>\n" +
            "    </tr>\n" +
            "</table>\n" +
            "<div class=\"graphviz\">\n" +
            "digraph G {\n" +
            "    _1_2_3_4_5[shape=none, margin=0, label=<\n" +
            "<table border=\"0\" cellborder=\"1\" cellspacing=\"0\" cellpadding=\"4\">\n" +
            "    <tr>\n" +
            "        <td colspan=\"3\">+</td>\n" +
            "    </tr>\n" +
            "</table>\n" +
            ">]\n" +
            "        _1_2_3_4_5->_6_7_8_9_10\n" +
            "    _6_7_8_9_10[shape=none, margin=0, label=<\n" +
            "<table border=\"0\" cellborder=\"1\" cellspacing=\"0\" cellpadding=\"4\">\n" +
            "    <tr>\n" +
            "        <td colspan=\"3\">1</td>\n" +
            "    </tr>\n" +
            "</table>\n" +
            ">]\n" +
            "        _1_2_3_4_5->_11_12_13_14_15\n" +
            "    _11_12_13_14_15[shape=none, margin=0, label=<\n" +
            "<table border=\"0\" cellborder=\"1\" cellspacing=\"0\" cellpadding=\"4\">\n" +
            "    <tr>\n" +
            "        <td colspan=\"3\">x</td>\n" +
            "    </tr>\n" +
            "</table>\n" +
            ">]\n" +
            "}\n" +
            "</div>        </div>\n" +
            "        <div>\n" +
            "<table class=\"tg\" idList=\"1,2,3,4,5,6,7,8,9,10,11,12,13,14,15\">\n" +
            "    <tr>\n" +
            "        <td>Occurence / Trees</td>\n" +
            "        <td>5 / 5</td>\n" +
            "    </tr>\n" +
            "    <tr>\n" +
            "        <td>Trees</td>\n" +
            "        <td>1, 2, 3, 4, 5</td>\n" +
            "    </tr>\n" +
            "    <tr>\n" +
            "        <td>Nodes</td>\n" +
            "        <td>1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15</td>\n" +
            "    </tr>\n" +
            "    <tr>\n" +
            "        <td>Debug Queries:</td>\n" +
            "        <td>\n" +
            "            <table class=\"nested\">\n" +
            "                <tr>\n" +
            "                    <td>Pattern in DB</td>\n" +
            "                    <td>MATCH (n) WHERE id(n) IN [1, 2, 3, 4, 5] RETURN (n)</td>\n" +
            "                </tr>\n" +
            "                <tr>\n" +
            "                    <td>All Nodes in DB</td>\n" +
            "                    <td>MATCH (n) WHERE id(n) IN [1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15] RETURN (n)</td>\n" +
            "                </tr>\n" +
            "            </table>\n" +
            "        </td>\n" +
            "    </tr>\n" +
            "</table>\n" +
            "<div class=\"graphviz\">\n" +
            "digraph G {\n" +
            "    _1_2_3_4_5[shape=none, margin=0, label=<\n" +
            "<table border=\"0\" cellborder=\"1\" cellspacing=\"0\" cellpadding=\"4\">\n" +
            "    <tr>\n" +
            "        <td colspan=\"3\">+</td>\n" +
            "    </tr>\n" +
            "</table>\n" +
            ">]\n" +
            "        _1_2_3_4_5->_6_7_8_9_10\n" +
            "    _6_7_8_9_10[shape=none, margin=0, label=<\n" +
            "<table border=\"0\" cellborder=\"1\" cellspacing=\"0\" cellpadding=\"4\">\n" +
            "    <tr>\n" +
            "        <td colspan=\"3\">1</td>\n" +
            "    </tr>\n" +
            "</table>\n" +
            ">]\n" +
            "        _1_2_3_4_5->_11_12_13_14_15\n" +
            "    _11_12_13_14_15[shape=none, margin=0, label=<\n" +
            "<table border=\"0\" cellborder=\"1\" cellspacing=\"0\" cellpadding=\"4\">\n" +
            "    <tr>\n" +
            "        <td colspan=\"3\">x</td>\n" +
            "    </tr>\n" +
            "</table>\n" +
            ">]\n" +
            "}\n" +
            "</div>        </div>\n" +
            " </div>\n" +
            "</div>\n" +
            "<div id=\"patterns-B\" class=\"tabcontent-2\">\n" +
            "<h2>B - (3 patterns)</h2>\n" +
            " <div class=\"wrapper\">\n" +
            "        <div>\n" +
            "<table class=\"tg\" idList=\"1,2,3,4,5,6,7,8,9,10,11,12,13,14,15\">\n" +
            "    <tr>\n" +
            "        <td>Occurence / Trees</td>\n" +
            "        <td>5 / 5</td>\n" +
            "    </tr>\n" +
            "    <tr>\n" +
            "        <td>Trees</td>\n" +
            "        <td>1, 2, 3, 4, 5</td>\n" +
            "    </tr>\n" +
            "    <tr>\n" +
            "        <td>Nodes</td>\n" +
            "        <td>1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15</td>\n" +
            "    </tr>\n" +
            "    <tr>\n" +
            "        <td>Debug Queries:</td>\n" +
            "        <td>\n" +
            "            <table class=\"nested\">\n" +
            "                <tr>\n" +
            "                    <td>Pattern in DB</td>\n" +
            "                    <td>MATCH (n) WHERE id(n) IN [1, 2, 3, 4, 5] RETURN (n)</td>\n" +
            "                </tr>\n" +
            "                <tr>\n" +
            "                    <td>All Nodes in DB</td>\n" +
            "                    <td>MATCH (n) WHERE id(n) IN [1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15] RETURN (n)</td>\n" +
            "                </tr>\n" +
            "            </table>\n" +
            "        </td>\n" +
            "    </tr>\n" +
            "</table>\n" +
            "<div class=\"graphviz\">\n" +
            "digraph G {\n" +
            "    _1_2_3_4_5[shape=none, margin=0, label=<\n" +
            "<table border=\"0\" cellborder=\"1\" cellspacing=\"0\" cellpadding=\"4\">\n" +
            "    <tr>\n" +
            "        <td colspan=\"3\">+</td>\n" +
            "    </tr>\n" +
            "</table>\n" +
            ">]\n" +
            "        _1_2_3_4_5->_6_7_8_9_10\n" +
            "    _6_7_8_9_10[shape=none, margin=0, label=<\n" +
            "<table border=\"0\" cellborder=\"1\" cellspacing=\"0\" cellpadding=\"4\">\n" +
            "    <tr>\n" +
            "        <td colspan=\"3\">1</td>\n" +
            "    </tr>\n" +
            "</table>\n" +
            ">]\n" +
            "        _1_2_3_4_5->_11_12_13_14_15\n" +
            "    _11_12_13_14_15[shape=none, margin=0, label=<\n" +
            "<table border=\"0\" cellborder=\"1\" cellspacing=\"0\" cellpadding=\"4\">\n" +
            "    <tr>\n" +
            "        <td colspan=\"3\">x</td>\n" +
            "    </tr>\n" +
            "</table>\n" +
            ">]\n" +
            "}\n" +
            "</div>        </div>\n" +
            "        <div>\n" +
            "<table class=\"tg\" idList=\"1,2,3,4,5,6,7,8,9,10,11,12,13,14,15\">\n" +
            "    <tr>\n" +
            "        <td>Occurence / Trees</td>\n" +
            "        <td>5 / 5</td>\n" +
            "    </tr>\n" +
            "    <tr>\n" +
            "        <td>Trees</td>\n" +
            "        <td>1, 2, 3, 4, 5</td>\n" +
            "    </tr>\n" +
            "    <tr>\n" +
            "        <td>Nodes</td>\n" +
            "        <td>1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15</td>\n" +
            "    </tr>\n" +
            "    <tr>\n" +
            "        <td>Debug Queries:</td>\n" +
            "        <td>\n" +
            "            <table class=\"nested\">\n" +
            "                <tr>\n" +
            "                    <td>Pattern in DB</td>\n" +
            "                    <td>MATCH (n) WHERE id(n) IN [1, 2, 3, 4, 5] RETURN (n)</td>\n" +
            "                </tr>\n" +
            "                <tr>\n" +
            "                    <td>All Nodes in DB</td>\n" +
            "                    <td>MATCH (n) WHERE id(n) IN [1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15] RETURN (n)</td>\n" +
            "                </tr>\n" +
            "            </table>\n" +
            "        </td>\n" +
            "    </tr>\n" +
            "</table>\n" +
            "<div class=\"graphviz\">\n" +
            "digraph G {\n" +
            "    _1_2_3_4_5[shape=none, margin=0, label=<\n" +
            "<table border=\"0\" cellborder=\"1\" cellspacing=\"0\" cellpadding=\"4\">\n" +
            "    <tr>\n" +
            "        <td colspan=\"3\">+</td>\n" +
            "    </tr>\n" +
            "</table>\n" +
            ">]\n" +
            "        _1_2_3_4_5->_6_7_8_9_10\n" +
            "    _6_7_8_9_10[shape=none, margin=0, label=<\n" +
            "<table border=\"0\" cellborder=\"1\" cellspacing=\"0\" cellpadding=\"4\">\n" +
            "    <tr>\n" +
            "        <td colspan=\"3\">1</td>\n" +
            "    </tr>\n" +
            "</table>\n" +
            ">]\n" +
            "        _1_2_3_4_5->_11_12_13_14_15\n" +
            "    _11_12_13_14_15[shape=none, margin=0, label=<\n" +
            "<table border=\"0\" cellborder=\"1\" cellspacing=\"0\" cellpadding=\"4\">\n" +
            "    <tr>\n" +
            "        <td colspan=\"3\">x</td>\n" +
            "    </tr>\n" +
            "</table>\n" +
            ">]\n" +
            "}\n" +
            "</div>        </div>\n" +
            "        <div>\n" +
            "<table class=\"tg\" idList=\"1,2,3,4,5,6,7,8,9,10,11,12,13,14,15\">\n" +
            "    <tr>\n" +
            "        <td>Occurence / Trees</td>\n" +
            "        <td>5 / 5</td>\n" +
            "    </tr>\n" +
            "    <tr>\n" +
            "        <td>Trees</td>\n" +
            "        <td>1, 2, 3, 4, 5</td>\n" +
            "    </tr>\n" +
            "    <tr>\n" +
            "        <td>Nodes</td>\n" +
            "        <td>1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15</td>\n" +
            "    </tr>\n" +
            "    <tr>\n" +
            "        <td>Debug Queries:</td>\n" +
            "        <td>\n" +
            "            <table class=\"nested\">\n" +
            "                <tr>\n" +
            "                    <td>Pattern in DB</td>\n" +
            "                    <td>MATCH (n) WHERE id(n) IN [1, 2, 3, 4, 5] RETURN (n)</td>\n" +
            "                </tr>\n" +
            "                <tr>\n" +
            "                    <td>All Nodes in DB</td>\n" +
            "                    <td>MATCH (n) WHERE id(n) IN [1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15] RETURN (n)</td>\n" +
            "                </tr>\n" +
            "            </table>\n" +
            "        </td>\n" +
            "    </tr>\n" +
            "</table>\n" +
            "<div class=\"graphviz\">\n" +
            "digraph G {\n" +
            "    _1_2_3_4_5[shape=none, margin=0, label=<\n" +
            "<table border=\"0\" cellborder=\"1\" cellspacing=\"0\" cellpadding=\"4\">\n" +
            "    <tr>\n" +
            "        <td colspan=\"3\">+</td>\n" +
            "    </tr>\n" +
            "</table>\n" +
            ">]\n" +
            "        _1_2_3_4_5->_6_7_8_9_10\n" +
            "    _6_7_8_9_10[shape=none, margin=0, label=<\n" +
            "<table border=\"0\" cellborder=\"1\" cellspacing=\"0\" cellpadding=\"4\">\n" +
            "    <tr>\n" +
            "        <td colspan=\"3\">1</td>\n" +
            "    </tr>\n" +
            "</table>\n" +
            ">]\n" +
            "        _1_2_3_4_5->_11_12_13_14_15\n" +
            "    _11_12_13_14_15[shape=none, margin=0, label=<\n" +
            "<table border=\"0\" cellborder=\"1\" cellspacing=\"0\" cellpadding=\"4\">\n" +
            "    <tr>\n" +
            "        <td colspan=\"3\">x</td>\n" +
            "    </tr>\n" +
            "</table>\n" +
            ">]\n" +
            "}\n" +
            "</div>        </div>\n" +
            " </div>\n" +
            "</div>\n" +
            "<div id=\"patterns-C\" class=\"tabcontent-2\">\n" +
            "<h2>C - (2 patterns)</h2>\n" +
            " <div class=\"wrapper\">\n" +
            "        <div>\n" +
            "<table class=\"tg\" idList=\"1,2,3,4,5,6,7,8,9,10,11,12,13,14,15\">\n" +
            "    <tr>\n" +
            "        <td>Occurence / Trees</td>\n" +
            "        <td>5 / 5</td>\n" +
            "    </tr>\n" +
            "    <tr>\n" +
            "        <td>Trees</td>\n" +
            "        <td>1, 2, 3, 4, 5</td>\n" +
            "    </tr>\n" +
            "    <tr>\n" +
            "        <td>Nodes</td>\n" +
            "        <td>1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15</td>\n" +
            "    </tr>\n" +
            "    <tr>\n" +
            "        <td>Debug Queries:</td>\n" +
            "        <td>\n" +
            "            <table class=\"nested\">\n" +
            "                <tr>\n" +
            "                    <td>Pattern in DB</td>\n" +
            "                    <td>MATCH (n) WHERE id(n) IN [1, 2, 3, 4, 5] RETURN (n)</td>\n" +
            "                </tr>\n" +
            "                <tr>\n" +
            "                    <td>All Nodes in DB</td>\n" +
            "                    <td>MATCH (n) WHERE id(n) IN [1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15] RETURN (n)</td>\n" +
            "                </tr>\n" +
            "            </table>\n" +
            "        </td>\n" +
            "    </tr>\n" +
            "</table>\n" +
            "<div class=\"graphviz\">\n" +
            "digraph G {\n" +
            "    _1_2_3_4_5[shape=none, margin=0, label=<\n" +
            "<table border=\"0\" cellborder=\"1\" cellspacing=\"0\" cellpadding=\"4\">\n" +
            "    <tr>\n" +
            "        <td colspan=\"3\">+</td>\n" +
            "    </tr>\n" +
            "</table>\n" +
            ">]\n" +
            "        _1_2_3_4_5->_6_7_8_9_10\n" +
            "    _6_7_8_9_10[shape=none, margin=0, label=<\n" +
            "<table border=\"0\" cellborder=\"1\" cellspacing=\"0\" cellpadding=\"4\">\n" +
            "    <tr>\n" +
            "        <td colspan=\"3\">1</td>\n" +
            "    </tr>\n" +
            "</table>\n" +
            ">]\n" +
            "        _1_2_3_4_5->_11_12_13_14_15\n" +
            "    _11_12_13_14_15[shape=none, margin=0, label=<\n" +
            "<table border=\"0\" cellborder=\"1\" cellspacing=\"0\" cellpadding=\"4\">\n" +
            "    <tr>\n" +
            "        <td colspan=\"3\">x</td>\n" +
            "    </tr>\n" +
            "</table>\n" +
            ">]\n" +
            "}\n" +
            "</div>        </div>\n" +
            "        <div>\n" +
            "<table class=\"tg\" idList=\"1,2,3,4,5,6,7,8,9,10,11,12,13,14,15\">\n" +
            "    <tr>\n" +
            "        <td>Occurence / Trees</td>\n" +
            "        <td>5 / 5</td>\n" +
            "    </tr>\n" +
            "    <tr>\n" +
            "        <td>Trees</td>\n" +
            "        <td>1, 2, 3, 4, 5</td>\n" +
            "    </tr>\n" +
            "    <tr>\n" +
            "        <td>Nodes</td>\n" +
            "        <td>1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15</td>\n" +
            "    </tr>\n" +
            "    <tr>\n" +
            "        <td>Debug Queries:</td>\n" +
            "        <td>\n" +
            "            <table class=\"nested\">\n" +
            "                <tr>\n" +
            "                    <td>Pattern in DB</td>\n" +
            "                    <td>MATCH (n) WHERE id(n) IN [1, 2, 3, 4, 5] RETURN (n)</td>\n" +
            "                </tr>\n" +
            "                <tr>\n" +
            "                    <td>All Nodes in DB</td>\n" +
            "                    <td>MATCH (n) WHERE id(n) IN [1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15] RETURN (n)</td>\n" +
            "                </tr>\n" +
            "            </table>\n" +
            "        </td>\n" +
            "    </tr>\n" +
            "</table>\n" +
            "<div class=\"graphviz\">\n" +
            "digraph G {\n" +
            "    _1_2_3_4_5[shape=none, margin=0, label=<\n" +
            "<table border=\"0\" cellborder=\"1\" cellspacing=\"0\" cellpadding=\"4\">\n" +
            "    <tr>\n" +
            "        <td colspan=\"3\">+</td>\n" +
            "    </tr>\n" +
            "</table>\n" +
            ">]\n" +
            "        _1_2_3_4_5->_6_7_8_9_10\n" +
            "    _6_7_8_9_10[shape=none, margin=0, label=<\n" +
            "<table border=\"0\" cellborder=\"1\" cellspacing=\"0\" cellpadding=\"4\">\n" +
            "    <tr>\n" +
            "        <td colspan=\"3\">1</td>\n" +
            "    </tr>\n" +
            "</table>\n" +
            ">]\n" +
            "        _1_2_3_4_5->_11_12_13_14_15\n" +
            "    _11_12_13_14_15[shape=none, margin=0, label=<\n" +
            "<table border=\"0\" cellborder=\"1\" cellspacing=\"0\" cellpadding=\"4\">\n" +
            "    <tr>\n" +
            "        <td colspan=\"3\">x</td>\n" +
            "    </tr>\n" +
            "</table>\n" +
            ">]\n" +
            "}\n" +
            "</div>        </div>\n" +
            " </div>\n" +
            "</div>\n" +
            "</div>\n" +
            "\n" +
            "<div id=\"Diff\" class=\"tabcontent-0\">\n" +
            "<h1>Differential between Problems</h1>\n" +
            "\n" +
            "<div class=\"wrapper\">\n" +
            "    <div style=\"border-style: dashed\">\n" +
            "<table class=\"tg\" idList=\"1,2,3,4,5,6,7,8,9,10,11,12,13,14,15\">\n" +
            "    <tr>\n" +
            "        <td>Occurence / Trees</td>\n" +
            "        <td>5 / 5</td>\n" +
            "    </tr>\n" +
            "    <tr>\n" +
            "        <td>Trees</td>\n" +
            "        <td>1, 2, 3, 4, 5</td>\n" +
            "    </tr>\n" +
            "    <tr>\n" +
            "        <td>Nodes</td>\n" +
            "        <td>1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15</td>\n" +
            "    </tr>\n" +
            "    <tr>\n" +
            "        <td>Debug Queries:</td>\n" +
            "        <td>\n" +
            "            <table class=\"nested\">\n" +
            "                <tr>\n" +
            "                    <td>Pattern in DB</td>\n" +
            "                    <td>MATCH (n) WHERE id(n) IN [1, 2, 3, 4, 5] RETURN (n)</td>\n" +
            "                </tr>\n" +
            "                <tr>\n" +
            "                    <td>All Nodes in DB</td>\n" +
            "                    <td>MATCH (n) WHERE id(n) IN [1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15] RETURN (n)</td>\n" +
            "                </tr>\n" +
            "            </table>\n" +
            "        </td>\n" +
            "    </tr>\n" +
            "</table>";
    }

    @Test
    public void testPrintSearchSpace() throws IOException {
        // given
        PatternAnalysisPrinter printer = new PatternAnalysisPrinter(null, true);
        StringWriter writer = new StringWriter();
        printer.setWriter(writer);

        // when
        printer.printSearchSpace(getTestSearchSpace().getSearchSpace());

        // then
        String s = writer.toString();
        Assert.assertEquals(s, testSearchSpaceStringHTML());
    }

    private String testSearchSpaceStringHTML() {
        return "<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.01 Transitional//EN\" \"http://www.w3.org/TR/html4/loose.dtd\">\n" +
            "<html lang=\"en\">\n" +
            "    <head>\n" +
            "        <meta http-equiv=\"content-type\" content=\"text/html; charset=utf-8\">\n" +
            "        <title>Search Space (2 trees)</title>\n" +
            "    <script type=\"text/javascript\" src=\"https://cdnjs.cloudflare.com/ajax/libs/viz.js/2.1.2/viz.js\"></script>\n" +
            "    <script type=\"text/javascript\" src=\"https://cdnjs.cloudflare.com/ajax/libs/viz.js/2.1.2/full.render.js\"></script>\n" +
            "    </head>\n" +
            "    <body>\n" +
            "    MATCH (n) WHERE id(n) IN [1, 5] RETURN (n)\n" +
            "<div class=\"graphviz\">\n" +
            "digraph G {\n" +
            "    1[label=\"+\"]\n" +
            "    2[label=\"1\"]\n" +
            "    3[label=\"x\"]\n" +
            "    1->2\n" +
            "    1->3\n" +
            "}\n" +
            "</div>\n" +
            "<div class=\"graphviz\">\n" +
            "digraph G {\n" +
            "    5[label=\"-\"]\n" +
            "    6[label=\"5\"]\n" +
            "    7[label=\"y\"]\n" +
            "    5->6\n" +
            "    5->7\n" +
            "}\n" +
            "</div>\n" +
            "    <script>\n" +
            "        // start listening to keypresses\n" +
            "        document.addEventListener(\"keydown\", logKeyDown);\n" +
            "        document.addEventListener(\"keyup\", logKeyUp);\n" +
            "        var pressCtrl = false;\n" +
            "\n" +
            "        function logKeyDown(e) {\n" +
            "            if (e.keyCode === 17) {\n" +
            "                pressCtrl = true;\n" +
            "            }\n" +
            "        }\n" +
            "\n" +
            "        function logKeyUp(e) {\n" +
            "            if (e.keyCode === 17) {\n" +
            "                pressCtrl = false;\n" +
            "            }\n" +
            "        }\n" +
            "\n" +
            "        var idList = [];\n" +
            "        var callerList = [];\n" +
            "\n" +
            "        function toggleNode(caller, toggleId) {\n" +
            "\n" +
            "            if (!Array.isArray(toggleId)) {\n" +
            "                if (toggleId.startsWith('_')) {\n" +
            "                    toggleId = toggleId.substring(1).split('_');\n" +
            "                } else {\n" +
            "                    toggleId = [toggleId];\n" +
            "                }\n" +
            "            }\n" +
            "            // clean id list\n" +
            "            if (!pressCtrl) {\n" +
            "                idList = toggleId\n" +
            "                callerList = [caller];\n" +
            "            } else {\n" +
            "                Array.prototype.push.apply(idList, toggleId);\n" +
            "                callerList.push(caller);\n" +
            "            }\n" +
            "\n" +
            "            // mark all nodes that are in ID list as blue\n" +
            "            Array.from(document.getElementsByClassName(\"node\")).forEach(function (node) {\n" +
            "                let id = node.children[0].innerHTML;\n" +
            "                if (id.startsWith('_')) {\n" +
            "                    let idArray = id.substring(1).split('_');\n" +
            "                    if (idList.some(value => idArray.includes(value))) {\n" +
            "                        node.children[1].style.fill = \"lightblue\"\n" +
            "                    } else {\n" +
            "                        node.children[1].style.fill = \"none\"\n" +
            "                    }\n" +
            "                } else {\n" +
            "                    if (idList.includes(id)) {\n" +
            "                        node.children[1].style.fill = \"lightblue\"\n" +
            "                    } else {\n" +
            "                        node.children[1].style.fill = \"none\"\n" +
            "                    }\n" +
            "                }\n" +
            "            });\n" +
            "\n" +
            "            // mark all nodes that we clicked on originally as red\n" +
            "            callerList.forEach(function (caller) {\n" +
            "                if (caller.children[1].nodeName === \"ellipse\") {\n" +
            "                    caller.children[1].style.fill = \"lightsalmon\"\n" +
            "                } else {\n" +
            "                    Array.from(caller.children).forEach(function (g) {\n" +
            "                        if (g.classList.contains(\"node\")) {\n" +
            "                            g.children[1].style.fill = \"lightsalmon\"\n" +
            "                        }\n" +
            "                    })\n" +
            "                }\n" +
            "            });\n" +
            "        }\n" +
            "\n" +
            "        var viz = new Viz();\n" +
            "        Array.from(document.getElementsByClassName(\"graphviz\")).forEach(function (graph) {\n" +
            "            viz.renderSVGElement(graph.textContent)\n" +
            "                .then(function (element) {\n" +
            "                    // add onclick to nodes\n" +
            "                    Array.from(element.children[0].children).forEach(function (node) {\n" +
            "                        if (node.classList.contains(\"node\")) {\n" +
            "                            let id = node.children[0].innerHTML;\n" +
            "                            node.onclick = function (event) {\n" +
            "                                event.stopPropagation();\n" +
            "                                toggleNode(node, id);\n" +
            "                            }\n" +
            "                        }\n" +
            "                    });\n" +
            "                    // add onclick to graph -> either as graph or as pattern\n" +
            "                    if (graph.parentNode.children[0].nodeName === \"TABLE\" && graph.parentNode.children[0].attributes.idList != null) {\n" +
            "                        let idList = graph.parentNode.children[0].attributes.idList.value.split(\",\");\n" +
            "                        element.children[0].onclick = function () {\n" +
            "                            toggleNode(element.children[0], idList);\n" +
            "                        };\n" +
            "                    } else {\n" +
            "                        let ids = [];\n" +
            "                        // collect ids\n" +
            "                        Array.from(element.children[0].children).forEach(function (node) {\n" +
            "                            if (node.classList.contains(\"node\")) {\n" +
            "                                ids.push(node.children[0].innerHTML);\n" +
            "                            }\n" +
            "                        });\n" +
            "                        element.children[0].onclick = function () {\n" +
            "                            toggleNode(element.children[0], ids);\n" +
            "                        };\n" +
            "                    }\n" +
            "                    graph.parentNode.replaceChild(element, graph);\n" +
            "                });\n" +
            "        });\n" +
            "    </script>\n" +
            "    </body>\n" +
            "</html>\n" +
            "\n";
    }

    @Test
    public void testPrintPatternMd() throws IOException {
        // given
        PatternAnalysisPrinter printer = new PatternAnalysisPrinter(null);
        printer.setFormat("md");
        StringWriter writer = new StringWriter();
        printer.setWriter(writer);

        // when
        printer.printPattern(getTestPattern());

        // then
        String s = writer.toString();
        Assert.assertEquals(s, testPatternStringMd());
    }

    private String testPatternStringMd() {
        return "<table class=\"tg\" idList=\"1,2,3,4,5,6,7,8,9,10,11,12,13,14,15\">\n" +
            "    <tr>\n" +
            "        <td>Occurence / Trees</td>\n" +
            "        <td>5 / 5</td>\n" +
            "    </tr>\n" +
            "</table>\n" +
            "```dot\n" +
            "digraph G {\n" +
            "    _1_2_3_4_5[label=\"+\"]\n" +
            "        _1_2_3_4_5->_6_7_8_9_10\n" +
            "    _6_7_8_9_10[label=\"1\"]\n" +
            "        _1_2_3_4_5->_11_12_13_14_15\n" +
            "    _11_12_13_14_15[label=\"x\"]\n" +
            "}\n" +
            "```";
    }

    @Test
    public void testDebugPrintPatternMd() throws IOException {
        // given
        PatternAnalysisPrinter printer = new PatternAnalysisPrinter(null, true);
        printer.setFormat("md");
        StringWriter writer = new StringWriter();
        printer.setWriter(writer);

        // when
        printer.printPattern(getTestPattern());

        // then
        String s = writer.toString();
        Assert.assertEquals(s, testDebugPatternStringMd());
    }

    private String testDebugPatternStringMd() {
        return "<table class=\"tg\" idList=\"1,2,3,4,5,6,7,8,9,10,11,12,13,14,15\">\n" +
            "    <tr>\n" +
            "        <td>Occurence / Trees</td>\n" +
            "        <td>5 / 5</td>\n" +
            "    </tr>\n" +
            "    <tr>\n" +
            "        <td>Trees</td>\n" +
            "        <td>1, 2, 3, 4, 5</td>\n" +
            "    </tr>\n" +
            "    <tr>\n" +
            "        <td>Nodes</td>\n" +
            "        <td>1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15</td>\n" +
            "    </tr>\n" +
            "    <tr>\n" +
            "        <td>Debug Queries:</td>\n" +
            "        <td>\n" +
            "            <table class=\"nested\">\n" +
            "                <tr>\n" +
            "                    <td>Pattern in DB</td>\n" +
            "                    <td>MATCH (n) WHERE id(n) IN [1, 2, 3, 4, 5] RETURN (n)</td>\n" +
            "                </tr>\n" +
            "                <tr>\n" +
            "                    <td>All Nodes in DB</td>\n" +
            "                    <td>MATCH (n) WHERE id(n) IN [1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15] RETURN (n)</td>\n" +
            "                </tr>\n" +
            "            </table>\n" +
            "        </td>\n" +
            "    </tr>\n" +
            "</table>\n" +
            "```dot\n" +
            "digraph G {\n" +
            "    _1_2_3_4_5[shape=none, margin=0, label=<\n" +
            "<table border=\"0\" cellborder=\"1\" cellspacing=\"0\" cellpadding=\"4\">\n" +
            "    <tr>\n" +
            "        <td colspan=\"3\">+</td>\n" +
            "    </tr>\n" +
            "</table>\n" +
            ">]\n" +
            "        _1_2_3_4_5->_6_7_8_9_10\n" +
            "    _6_7_8_9_10[shape=none, margin=0, label=<\n" +
            "<table border=\"0\" cellborder=\"1\" cellspacing=\"0\" cellpadding=\"4\">\n" +
            "    <tr>\n" +
            "        <td colspan=\"3\">1</td>\n" +
            "    </tr>\n" +
            "</table>\n" +
            ">]\n" +
            "        _1_2_3_4_5->_11_12_13_14_15\n" +
            "    _11_12_13_14_15[shape=none, margin=0, label=<\n" +
            "<table border=\"0\" cellborder=\"1\" cellspacing=\"0\" cellpadding=\"4\">\n" +
            "    <tr>\n" +
            "        <td colspan=\"3\">x</td>\n" +
            "    </tr>\n" +
            "</table>\n" +
            ">]\n" +
            "}\n" +
            "```";
    }

    @Test
    public void testPrintDifferentialPatternMd() throws IOException {
        // given
        PatternAnalysisPrinter printer = new PatternAnalysisPrinter(null, true);
        printer.setFormat("md");
        StringWriter writer = new StringWriter();
        printer.setWriter(writer);

        // when
        printer.printDifferentialPattern(getTestDiffPattern());

        // then
        String s = writer.toString();
        Assert.assertTrue(s.startsWith(testDiffPatternString()));
    }

    @Test
    public void testPrintSearchSpaceMd() throws IOException {
        // given
        PatternAnalysisPrinter printer = new PatternAnalysisPrinter(null, true);
        printer.setFormat("md");
        StringWriter writer = new StringWriter();
        printer.setWriter(writer);

        // when
        printer.printSearchSpace(getTestSearchSpace().getSearchSpace());

        this.getTestDiffPattern().getPatternsPerProblem().isEmpty();
        // then
        String s = writer.toString();
        Assert.assertEquals(s, testSearchSpaceString());
    }

    private TrufflePatternSearchSpace getTestSearchSpace() {
        TrufflePatternSearchSpace searchSpace = new TrufflePatternSearchSpace();

        NodeWrapper a = new NodeWrapper("+");
        a.setId(1L);
        NodeWrapper b = new NodeWrapper("1");
        b.setId(2L);
        NodeWrapper c = new NodeWrapper("x");
        c.setId(3L);
        OrderedRelationship l = new OrderedRelationship(a, b, "left", 0);
        OrderedRelationship r = new OrderedRelationship(a, c, "right", 0);

        searchSpace.addTree(new NodeWrapper[]{a, b, c},
            new OrderedRelationship[]{l, r});

        NodeWrapper a1 = new NodeWrapper("-");
        a1.setId(5L);
        NodeWrapper b1 = new NodeWrapper("5");
        b1.setId(6L);
        NodeWrapper c1 = new NodeWrapper("y");
        c1.setId(7L);
        OrderedRelationship l1 = new OrderedRelationship(a1, b1, "left", 0);
        OrderedRelationship r1 = new OrderedRelationship(a1, c1, "right", 0);

        searchSpace.addTree(new NodeWrapper[]{a1, b1, c1},
            new OrderedRelationship[]{l1, r1});

        return searchSpace;
    }

    private TruffleDifferentialPatternSolution getTestDiffPattern() {
        Map<TrufflePatternProblem, List<TrufflePattern>> ppp = new HashMap<>();
        Map<TrufflePattern, Map<Pair<TrufflePatternProblem, TrufflePatternProblem>, Long>> differential = new HashMap<>();

        // will ignore search space for B and C as this is a separate test
        TrufflePatternSearchSpace ssA = getTestSearchSpace();
        TrufflePatternSearchSpace ssB = new TrufflePatternSearchSpace();
        TrufflePatternSearchSpace ssC = new TrufflePatternSearchSpace();
        TrufflePatternProblem problemA = new TrufflePatternProblem("c", ssA, "A");
        TrufflePatternProblem problemB = new TrufflePatternProblem("c", ssB, "B");
        TrufflePatternProblem problemC = new TrufflePatternProblem("c", ssC, "C");

        // Making multiple copies of same thing to test for overlap.
        TrufflePattern testPatternALL = getTestPattern();
        TrufflePattern testPatternAB = getTestPattern();
        TrufflePattern testPatternAC = getTestPattern();
        TrufflePattern testPatternA = getTestPattern();
        TrufflePattern testPatternA1 = getTestPattern();
        TrufflePattern testPatternB = getTestPattern();

        ppp.put(problemA, Arrays.asList(testPatternALL, testPatternAB, testPatternAC, testPatternA, testPatternA1));
        ppp.put(problemB, Arrays.asList(testPatternALL, testPatternAB, testPatternB));
        ppp.put(problemC, Arrays.asList(testPatternALL, testPatternAC));

        Map<Pair<TrufflePatternProblem, TrufflePatternProblem>, Long> mapALL = new HashMap<>();
        mapALL.put(new Pair<>(problemA, problemB), 0L);
        mapALL.put(new Pair<>(problemA, problemC), 0L);
        mapALL.put(new Pair<>(problemB, problemC), 0L);

        Map<Pair<TrufflePatternProblem, TrufflePatternProblem>, Long> mapAB = new HashMap<>();
        mapAB.put(new Pair<>(problemA, problemB), 0L);
        mapAB.put(new Pair<>(problemA, problemC), 1L);
        mapAB.put(new Pair<>(problemB, problemC), 1L);

        Map<Pair<TrufflePatternProblem, TrufflePatternProblem>, Long> mapAC = new HashMap<>();
        mapAC.put(new Pair<>(problemA, problemB), 1L);
        mapAC.put(new Pair<>(problemA, problemC), 0L);
        mapAC.put(new Pair<>(problemB, problemC), -1L);

        Map<Pair<TrufflePatternProblem, TrufflePatternProblem>, Long> mapA = new HashMap<>();
        mapA.put(new Pair<>(problemA, problemB), 1L);
        mapA.put(new Pair<>(problemA, problemC), 1L);
        mapA.put(new Pair<>(problemB, problemC), 0L);

        Map<Pair<TrufflePatternProblem, TrufflePatternProblem>, Long> mapA1 = new HashMap<>();
        mapA1.put(new Pair<>(problemA, problemB), 1L);
        mapA1.put(new Pair<>(problemA, problemC), 1L);
        mapA1.put(new Pair<>(problemB, problemC), 0L);

        Map<Pair<TrufflePatternProblem, TrufflePatternProblem>, Long> mapB = new HashMap<>();
        mapB.put(new Pair<>(problemA, problemB), -1L);
        mapB.put(new Pair<>(problemA, problemC), 0L);
        mapB.put(new Pair<>(problemB, problemC), 1L);

        differential.put(testPatternALL, mapALL);
        differential.put(testPatternAB, mapAB);
        differential.put(testPatternAC, mapAC);
        differential.put(testPatternA, mapA);
        differential.put(testPatternA1, mapA1);
        differential.put(testPatternB, mapB);
        return new TruffleDifferentialPatternSolution(ppp, differential);
    }

    // cheat to give a unique hash to nodes that should have the same hash (too lazy to create distinct patterns)
    private int x = 0;

    private TrufflePattern getTestPattern() {
        PatternNodeWrapper wrapper = new PatternNodeWrapper(new NodeWrapper("+"), Arrays.asList(1L, 2L, 3L, 4L, 5L));
        wrapper.setHash("TEST-" + x++);
        PatternNodeWrapper l = new PatternNodeWrapper(new NodeWrapper("1"), Arrays.asList(6L, 7L, 8L, 9L, 10L));
        wrapper.addChild(l, "left", 0);
        l.setHash("TEST-" + x++);
        PatternNodeWrapper r = new PatternNodeWrapper(new NodeWrapper("x"), Arrays.asList(11L, 12L, 13L, 14L, 15L));
        wrapper.addChild(r, "right", 0);
        r.setHash("TEST-" + x++);
        TrufflePattern trufflePattern = new TrufflePattern(1L, wrapper);
        trufflePattern.addTree(2L, wrapper);
        trufflePattern.addTree(3L, wrapper);
        trufflePattern.addTree(4L, wrapper);
        trufflePattern.addTree(5L, wrapper);
        return trufflePattern;
    }

    private String testPatternString() {
        return "<table class=\"tg\" idList=\"1,2,3,4,5,6,7,8,9,10,11,12,13,14,15\">\n" +
            "    <tr>\n" +
            "        <td>Occurence / Trees</td>\n" +
            "        <td>5 / 5</td>\n" +
            "    </tr>\n" +
            "</table>\n" +
            "<div class=\"graphviz\">\n" +
            "digraph G {\n" +
            "    _1_2_3_4_5[label=\"+\"]\n" +
            "        _1_2_3_4_5->_6_7_8_9_10\n" +
            "    _6_7_8_9_10[label=\"1\"]\n" +
            "        _1_2_3_4_5->_11_12_13_14_15\n" +
            "    _11_12_13_14_15[label=\"x\"]\n" +
            "}\n" +
            "</div>";
    }

    private String testSearchSpaceString() {
        return "### Search Space (2 trees)\n" +
            "    MATCH (n) WHERE id(n) IN [1, 5] RETURN (n)\n" +
            "```dot\n" +
            "digraph G {\n" +
            "    1[label=\"+\"]\n" +
            "    2[label=\"1\"]\n" +
            "    3[label=\"x\"]\n" +
            "    1->2\n" +
            "    1->3\n" +
            "}\n" +
            "```\n" +
            "```dot\n" +
            "digraph G {\n" +
            "    5[label=\"-\"]\n" +
            "    6[label=\"5\"]\n" +
            "    7[label=\"y\"]\n" +
            "    5->6\n" +
            "    5->7\n" +
            "}\n" +
            "```\n";
    }

    private String testDebugPatternString() {
        return "<table class=\"tg\" idList=\"1,2,3,4,5,6,7,8,9,10,11,12,13,14,15\">\n" +
            "    <tr>\n" +
            "        <td>Occurence / Trees</td>\n" +
            "        <td>5 / 5</td>\n" +
            "    </tr>\n" +
            "    <tr>\n" +
            "        <td>Trees</td>\n" +
            "        <td>1, 2, 3, 4, 5</td>\n" +
            "    </tr>\n" +
            "    <tr>\n" +
            "        <td>Nodes</td>\n" +
            "        <td>1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15</td>\n" +
            "    </tr>\n" +
            "    <tr>\n" +
            "        <td>Debug Queries:</td>\n" +
            "        <td>\n" +
            "            <table class=\"nested\">\n" +
            "                <tr>\n" +
            "                    <td>Pattern in DB</td>\n" +
            "                    <td>MATCH (n) WHERE id(n) IN [1, 2, 3, 4, 5] RETURN (n)</td>\n" +
            "                </tr>\n" +
            "                <tr>\n" +
            "                    <td>All Nodes in DB</td>\n" +
            "                    <td>MATCH (n) WHERE id(n) IN [1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15] RETURN (n)</td>\n" +
            "                </tr>\n" +
            "            </table>\n" +
            "        </td>\n" +
            "    </tr>\n" +
            "</table>\n" +
            "<div class=\"graphviz\">\n" +
            "digraph G {\n" +
            "    _1_2_3_4_5[shape=none, margin=0, label=<\n" +
            "<table border=\"0\" cellborder=\"1\" cellspacing=\"0\" cellpadding=\"4\">\n" +
            "    <tr>\n" +
            "        <td colspan=\"3\">+</td>\n" +
            "    </tr>\n" +
            "</table>\n" +
            ">]\n" +
            "        _1_2_3_4_5->_6_7_8_9_10\n" +
            "    _6_7_8_9_10[shape=none, margin=0, label=<\n" +
            "<table border=\"0\" cellborder=\"1\" cellspacing=\"0\" cellpadding=\"4\">\n" +
            "    <tr>\n" +
            "        <td colspan=\"3\">1</td>\n" +
            "    </tr>\n" +
            "</table>\n" +
            ">]\n" +
            "        _1_2_3_4_5->_11_12_13_14_15\n" +
            "    _11_12_13_14_15[shape=none, margin=0, label=<\n" +
            "<table border=\"0\" cellborder=\"1\" cellspacing=\"0\" cellpadding=\"4\">\n" +
            "    <tr>\n" +
            "        <td colspan=\"3\">x</td>\n" +
            "    </tr>\n" +
            "</table>\n" +
            ">]\n" +
            "}\n" +
            "</div>";
    }

    private String testDiffPatternString() {
        return "# Problem Definitions\n" +
            "## A - (2 trees)\n" +
            "    MATCH (n) WHERE id(n) IN [1, 5] RETURN (n)\n" +
            "```dot\n" +
            "digraph G {\n" +
            "    1[label=\"+\"]\n" +
            "    2[label=\"1\"]\n" +
            "    3[label=\"x\"]\n" +
            "    1->2\n" +
            "    1->3\n" +
            "}\n" +
            "```\n" +
            "```dot\n" +
            "digraph G {\n" +
            "    5[label=\"-\"]\n" +
            "    6[label=\"5\"]\n" +
            "    7[label=\"y\"]\n" +
            "    5->6\n" +
            "    5->7\n" +
            "}\n" +
            "```\n" +
            "## B - (0 trees)\n" +
            "    MATCH (n) WHERE id(n) IN [] RETURN (n)\n" +
            "## C - (0 trees)\n" +
            "    MATCH (n) WHERE id(n) IN [] RETURN (n)\n" +
            "# Patterns per Problem\n" +
            "\n" +
            "```latex{cmd=true hide=true}\n" +
            "\\documentclass{standalone}\n" +
            "\\usepackage{tikz}\n" +
            "\\usepackage{pgfplots}\n" +
            "\\begin{document}\n" +
            "\\begin{tikzpicture}\n" +
            "\\begin{axis}[\n" +
            "  xtick={A,B,C,A-B,A-C,B-C,A-B-C},\n" +
            "  symbolic x coords = {A,B,C,A-B,A-C,B-C,A-B-C},\n" +
            "\tylabel=Patterns,\n" +
            "  xlabel=Problems,\n" +
            "  legend pos=outer north east,\n" +
            "\tybar,\n" +
            "]\n" +
            "\\addplot\n" +
            "\tcoordinates {(A, 2) (B, 1) (C, 0) (A-B, 1) (A-C, 1) (B-C, 0) (A-B-C, 1)};\n" +
            "\\end{axis}\n" +
            "\\end{tikzpicture}\n" +
            "\\end{document}\n" +
            "```\n" +
            "\n" +
            "## A - (5 patterns)\n" +
            "<table class=\"tg\" idList=\"1,2,3,4,5,6,7,8,9,10,11,12,13,14,15\">\n" +
            "    <tr>\n" +
            "        <td>Occurence / Trees</td>\n" +
            "        <td>5 / 5</td>\n" +
            "    </tr>\n" +
            "</table>\n" +
            "```dot\n" +
            "digraph G {\n" +
            "    _1_2_3_4_5[label=\"+\"]\n" +
            "        _1_2_3_4_5->_6_7_8_9_10\n" +
            "    _6_7_8_9_10[label=\"1\"]\n" +
            "        _1_2_3_4_5->_11_12_13_14_15\n" +
            "    _11_12_13_14_15[label=\"x\"]\n" +
            "}\n" +
            "```\n" +
            "<table class=\"tg\" idList=\"1,2,3,4,5,6,7,8,9,10,11,12,13,14,15\">\n" +
            "    <tr>\n" +
            "        <td>Occurence / Trees</td>\n" +
            "        <td>5 / 5</td>\n" +
            "    </tr>\n" +
            "</table>\n" +
            "```dot\n" +
            "digraph G {\n" +
            "    _1_2_3_4_5[label=\"+\"]\n" +
            "        _1_2_3_4_5->_6_7_8_9_10\n" +
            "    _6_7_8_9_10[label=\"1\"]\n" +
            "        _1_2_3_4_5->_11_12_13_14_15\n" +
            "    _11_12_13_14_15[label=\"x\"]\n" +
            "}\n" +
            "```\n" +
            "<table class=\"tg\" idList=\"1,2,3,4,5,6,7,8,9,10,11,12,13,14,15\">\n" +
            "    <tr>\n" +
            "        <td>Occurence / Trees</td>\n" +
            "        <td>5 / 5</td>\n" +
            "    </tr>\n" +
            "</table>\n" +
            "```dot\n" +
            "digraph G {\n" +
            "    _1_2_3_4_5[label=\"+\"]\n" +
            "        _1_2_3_4_5->_6_7_8_9_10\n" +
            "    _6_7_8_9_10[label=\"1\"]\n" +
            "        _1_2_3_4_5->_11_12_13_14_15\n" +
            "    _11_12_13_14_15[label=\"x\"]\n" +
            "}\n" +
            "```\n" +
            "<table class=\"tg\" idList=\"1,2,3,4,5,6,7,8,9,10,11,12,13,14,15\">\n" +
            "    <tr>\n" +
            "        <td>Occurence / Trees</td>\n" +
            "        <td>5 / 5</td>\n" +
            "    </tr>\n" +
            "</table>\n" +
            "```dot\n" +
            "digraph G {\n" +
            "    _1_2_3_4_5[label=\"+\"]\n" +
            "        _1_2_3_4_5->_6_7_8_9_10\n" +
            "    _6_7_8_9_10[label=\"1\"]\n" +
            "        _1_2_3_4_5->_11_12_13_14_15\n" +
            "    _11_12_13_14_15[label=\"x\"]\n" +
            "}\n" +
            "```\n" +
            "<table class=\"tg\" idList=\"1,2,3,4,5,6,7,8,9,10,11,12,13,14,15\">\n" +
            "    <tr>\n" +
            "        <td>Occurence / Trees</td>\n" +
            "        <td>5 / 5</td>\n" +
            "    </tr>\n" +
            "</table>\n" +
            "```dot\n" +
            "digraph G {\n" +
            "    _1_2_3_4_5[label=\"+\"]\n" +
            "        _1_2_3_4_5->_6_7_8_9_10\n" +
            "    _6_7_8_9_10[label=\"1\"]\n" +
            "        _1_2_3_4_5->_11_12_13_14_15\n" +
            "    _11_12_13_14_15[label=\"x\"]\n" +
            "}\n" +
            "```\n" +
            "\n" +
            "## B - (3 patterns)\n" +
            "<table class=\"tg\" idList=\"1,2,3,4,5,6,7,8,9,10,11,12,13,14,15\">\n" +
            "    <tr>\n" +
            "        <td>Occurence / Trees</td>\n" +
            "        <td>5 / 5</td>\n" +
            "    </tr>\n" +
            "</table>\n" +
            "```dot\n" +
            "digraph G {\n" +
            "    _1_2_3_4_5[label=\"+\"]\n" +
            "        _1_2_3_4_5->_6_7_8_9_10\n" +
            "    _6_7_8_9_10[label=\"1\"]\n" +
            "        _1_2_3_4_5->_11_12_13_14_15\n" +
            "    _11_12_13_14_15[label=\"x\"]\n" +
            "}\n" +
            "```\n" +
            "<table class=\"tg\" idList=\"1,2,3,4,5,6,7,8,9,10,11,12,13,14,15\">\n" +
            "    <tr>\n" +
            "        <td>Occurence / Trees</td>\n" +
            "        <td>5 / 5</td>\n" +
            "    </tr>\n" +
            "</table>\n" +
            "```dot\n" +
            "digraph G {\n" +
            "    _1_2_3_4_5[label=\"+\"]\n" +
            "        _1_2_3_4_5->_6_7_8_9_10\n" +
            "    _6_7_8_9_10[label=\"1\"]\n" +
            "        _1_2_3_4_5->_11_12_13_14_15\n" +
            "    _11_12_13_14_15[label=\"x\"]\n" +
            "}\n" +
            "```\n" +
            "<table class=\"tg\" idList=\"1,2,3,4,5,6,7,8,9,10,11,12,13,14,15\">\n" +
            "    <tr>\n" +
            "        <td>Occurence / Trees</td>\n" +
            "        <td>5 / 5</td>\n" +
            "    </tr>\n" +
            "</table>\n" +
            "```dot\n" +
            "digraph G {\n" +
            "    _1_2_3_4_5[label=\"+\"]\n" +
            "        _1_2_3_4_5->_6_7_8_9_10\n" +
            "    _6_7_8_9_10[label=\"1\"]\n" +
            "        _1_2_3_4_5->_11_12_13_14_15\n" +
            "    _11_12_13_14_15[label=\"x\"]\n" +
            "}\n" +
            "```\n" +
            "\n" +
            "## C - (2 patterns)\n" +
            "<table class=\"tg\" idList=\"1,2,3,4,5,6,7,8,9,10,11,12,13,14,15\">\n" +
            "    <tr>\n" +
            "        <td>Occurence / Trees</td>\n" +
            "        <td>5 / 5</td>\n" +
            "    </tr>\n" +
            "</table>\n" +
            "```dot\n" +
            "digraph G {\n" +
            "    _1_2_3_4_5[label=\"+\"]\n" +
            "        _1_2_3_4_5->_6_7_8_9_10\n" +
            "    _6_7_8_9_10[label=\"1\"]\n" +
            "        _1_2_3_4_5->_11_12_13_14_15\n" +
            "    _11_12_13_14_15[label=\"x\"]\n" +
            "}\n" +
            "```\n" +
            "<table class=\"tg\" idList=\"1,2,3,4,5,6,7,8,9,10,11,12,13,14,15\">\n" +
            "    <tr>\n" +
            "        <td>Occurence / Trees</td>\n" +
            "        <td>5 / 5</td>\n" +
            "    </tr>\n" +
            "</table>\n" +
            "```dot\n" +
            "digraph G {\n" +
            "    _1_2_3_4_5[label=\"+\"]\n" +
            "        _1_2_3_4_5->_6_7_8_9_10\n" +
            "    _6_7_8_9_10[label=\"1\"]\n" +
            "        _1_2_3_4_5->_11_12_13_14_15\n" +
            "    _11_12_13_14_15[label=\"x\"]\n" +
            "}\n" +
            "```\n" +
            "\n" +
            "\n" +
            "# Differential between Problems\n" +
            "\n" +
            "<table class=\"tg\" idList=\"1,2,3,4,5,6,7,8,9,10,11,12,13,14,15\">\n" +
            "    <tr>\n" +
            "        <td>Occurence / Trees</td>\n" +
            "        <td>5 / 5</td>\n" +
            "    </tr>\n" +
            "</table>\n" +
            "```dot\n" +
            "digraph G {\n" +
            "    _1_2_3_4_5[label=\"+\"]\n" +
            "        _1_2_3_4_5->_6_7_8_9_10\n" +
            "    _6_7_8_9_10[label=\"1\"]\n" +
            "        _1_2_3_4_5->_11_12_13_14_15\n" +
            "    _11_12_13_14_15[label=\"x\"]\n" +
            "}\n" +
            "```";
    }
}
