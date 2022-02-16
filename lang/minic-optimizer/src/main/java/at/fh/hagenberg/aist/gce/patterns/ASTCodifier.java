/*
 * Copyright (c) 2022 the original author or authors.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

package at.fh.hagenberg.aist.gce.patterns;

import at.fh.hagenberg.aist.gce.benchmark.MinicMathBenchmarkSuite;
import at.fh.hagenberg.aist.gce.benchmark.MinicTestfileOptimizer;
import at.fh.hagenberg.aist.gce.minic.language.MinicLanguage;
import at.fh.hagenberg.aist.gce.optimization.TruffleOptimizationProblem;
import at.fh.hagenberg.aist.gce.optimization.executor.ExecutionResult;
import at.fh.hagenberg.aist.gce.optimization.executor.InternalExecutor;
import at.fh.hagenberg.aist.gce.optimization.language.util.EngineConfig;
import at.fh.hagenberg.aist.gce.optimization.runtime.RuntimeProfile;
import at.fh.hagenberg.aist.gce.optimization.util.TruffleLanguageInformation;
import at.fh.hagenberg.aist.gce.pattern.TrufflePatternDetector;
import at.fh.hagenberg.aist.gce.pattern.constraint.ExperimentConstraint;
import at.fh.hagenberg.aist.gce.pattern.selection.TrufflePatternSearchSpace;
import at.fh.hagenberg.aist.gce.pattern.selection.TrufflePatternSearchSpaceDefinition;
import at.fh.hagenberg.aist.gce.science.statistics.AutoStatistics;
import at.fh.hagenberg.aist.gce.science.statistics.Report;
import at.fh.hagenberg.aist.gce.science.statistics.data.Dataset;
import at.fh.hagenberg.aist.gce.science.statistics.data.template.DatasetReportTransformer;
import at.fh.hagenberg.aist.gce.science.statistics.data.template.NodeWrapperPrinter;
import at.fh.hagenberg.aist.gce.science.statistics.data.template.RuntimeProfilePrinter;
import at.fh.hagenberg.aist.gce.science.statistics.data.template.SourceCodePrinter;
import at.fh.hagenberg.machinelearning.analytics.graph.nodes.NodeWrapper;
import at.fh.hagenberg.util.Pair;
import com.oracle.truffle.api.nodes.Node;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;

/**
 * Performance Evaluation pipeline.
 * Exists to add performance values to already existing stuff in the database
 *
 * @author Oliver Krauss on 11.07.2020
 */
public class ASTCodifier {

    private static final String REPORT_LOCATION = EngineConfig.ROOT_LOCATION + "/Amaru.LOGS/BEST_ASTS/";

    private static TruffleLanguageInformation tli = TruffleLanguageInformation.getLanguageInformation(MinicLanguage.ID);

    public static void main(String[] args) throws IOException {

        // MATH ALGS
        printAST(8689L, "math/sqrt_java");
        printAST(9944410L, "math/sqrt_lookup");
        printAST(198786L, "math/sqrt_nolookup");
        printAST(408889L, "math/cbrt");
        printAST(485922L, "math/surt");
        printAST(1106920L, "math/invSqrt");
        printAST(677359L, "math/log");
        printAST(951655L, "math/ln");

        // SORT ALGS
        printAST(1237684L ,"sort/bubbleSort");
        printAST(1686853L ,"sort/heapSort");
        printAST(1837537L ,"sort/insertionSort");
        printAST(2190821L ,"sort/mergeSort");
        printAST(2310977L ,"sort/mergeSortInlined");
        printAST(2520429L ,"sort/quickSort");
        printAST(3013397L ,"sort/quickSortInlined");
        printAST(3382473L ,"sort/selectionSort");
        printAST(3745493L ,"sort/shakerSort");
        printAST(3990323L ,"sort/shellSort");

        // NN ALgorithms
        printAST(8734001L, "nn/nn_relu");
        printAST(9517916L, "nn/nn_lrelu");
        printAST(6424368L, "nn/nn_sigmoid");
        printAST(8342139L, "nn/nn_swish");
        printAST(6708623L, "nn/nn_tanh");
        printAST(5440372L, "nn/nn_fullinline");
        printAST(4623277L, "nn/nn_options");
    }

    private static void printAST(long id, String name) throws IOException {
        // lookup AST
        TrufflePatternSearchSpaceDefinition def = new TrufflePatternSearchSpaceDefinition();
        def.includeTree(id);
        def.setSolutionSpace(true);
        def.setExperiments(Collections.singletonList(new ExperimentConstraint("DOESNOTEXIST")));
        TrufflePatternDetector detector = new TrufflePatternDetector();
        TrufflePatternSearchSpace searchSpace = detector.findSearchSpace(def);

        // print ast
        NodeWrapper ast = searchSpace.toAst(searchSpace.getSearchSpace().getFirst());

        // re-evaluate ast to be SURE everything is good
//        MinicTestfileOptimizer optimizer = new MinicTestfileOptimizer(name, null);
//        TruffleOptimizationProblem problem = optimizer.getProblem();
//        InternalExecutor executor = new InternalExecutor(MinicLanguage.ID, problem.getCode(), name.startsWith("nn/") ? "nn_entry" : problem.getEntryPoint(), problem.getFunction());
//        Node node = NodeWrapper.unwrap(ast, executor.getRoot().getFrameDescriptor(), executor.getGlobalScope(), MinicLanguage.ID);
//        problem.getTests().forEach(test -> {
//            ExecutionResult executionResult = executor.test(node, test.getTest().getInputArguments());
//            if (executionResult.getReturnValue() instanceof int[]) {
//                int[] returnValue = (int[]) executionResult.getReturnValue();
//                for (int i = 1; i < returnValue.length; i++) {
//                    if (returnValue[i - 1] > returnValue[i]) {
//                        System.out.println("DANGER DANGER");
//                    }
//                }
//            } else if (!executionResult.getReturnValue().equals(test.getTest().getOutputValue())) {
//                System.out.println("DANGER DANGER");
//            }
//        });

        ast = searchSpace.toAst(searchSpace.getSearchSpace().getFirst());
        SourceCodePrinter printer = new SourceCodePrinter(null);
        printer.setTli(tli);
        printer.setFormat("html");
        File file = new File(REPORT_LOCATION + name + ".c");
        file.createNewFile();
        FileWriter writer = new FileWriter(file);
        printer.setWriter(writer);
        System.out.println("ALGORITHM " + name);
        printer.printAst(ast);

        NodeWrapperPrinter nPrint = new NodeWrapperPrinter(null);
        nPrint.setFormat("html");
        nPrint.setWriter(new FileWriter(new File(REPORT_LOCATION + name + ".html")));
        nPrint.printNodeWrapper(ast);
    }


}