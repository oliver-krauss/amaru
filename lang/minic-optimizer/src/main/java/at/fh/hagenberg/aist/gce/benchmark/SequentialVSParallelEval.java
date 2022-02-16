/*
 * Copyright (c) 2022 the original author or authors.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

package at.fh.hagenberg.aist.gce.benchmark;

import at.fh.hagenberg.aist.gce.context.ApplicationContextProvider;
import at.fh.hagenberg.aist.gce.lang.GraalBackdoorSystem;
import at.fh.hagenberg.aist.gce.minic.language.MinicLanguage;
import at.fh.hagenberg.aist.gce.minic.nodes.impl.function.MinicFunctionLiteralNode;
import at.fh.hagenberg.aist.gce.minic.nodes.impl.function.MinicFunctionNode;
import at.fh.hagenberg.aist.gce.optimization.TruffleEvaluatorImpl;
import at.fh.hagenberg.aist.gce.optimization.TruffleOptimizationProblem;
import at.fh.hagenberg.aist.gce.optimization.TruffleOptimizationSolution;
import at.fh.hagenberg.aist.gce.optimization.TruffleOptimizationSolutionRepository;
import at.fh.hagenberg.aist.gce.optimization.cachet.PerformanceCachetEvaluator;
import at.fh.hagenberg.aist.gce.optimization.executor.MessageExecutor;
import at.fh.hagenberg.aist.gce.optimization.language.util.EngineConfig;
import at.fh.hagenberg.aist.gce.optimization.run.TruffleAlgorithmFactory;
import at.fh.hagenberg.aist.gce.optimization.runtime.RuntimeProfile;
import at.fh.hagenberg.aist.gce.optimization.runtime.SystemInformation;
import at.fh.hagenberg.aist.gce.optimization.test.*;
import at.fh.hagenberg.aist.gce.optimization.util.ExtendedNodeUtil;
import at.fh.hagenberg.aist.gce.optimization.util.JavaAssistUtil;
import at.fh.hagenberg.aist.gce.optimization.util.TruffleLanguageInformation;
import at.fh.hagenberg.aist.gce.pattern.TrufflePatternDetector;
import at.fh.hagenberg.aist.gce.pattern.constraint.CachetConstraint;
import at.fh.hagenberg.aist.gce.pattern.constraint.ProblemConstraint;
import at.fh.hagenberg.aist.gce.pattern.selection.TrufflePatternSearchSpaceDefinition;
import at.fh.hagenberg.aist.gce.science.statistics.AutoStatistics;
import at.fh.hagenberg.aist.gce.science.statistics.Report;
import at.fh.hagenberg.aist.gce.science.statistics.data.Dataset;
import at.fh.hagenberg.aist.gce.science.statistics.data.template.DatasetReportTransformer;
import at.fh.hagenberg.aist.gce.science.statistics.data.template.NodeWrapperPrinter;
import at.fh.hagenberg.aist.gce.science.statistics.data.template.RuntimeProfilePrinter;
import at.fh.hagenberg.machinelearning.analytics.graph.SystemInformationRepository;
import at.fh.hagenberg.machinelearning.analytics.graph.nodes.NodeWrapper;
import at.fh.hagenberg.machinelearning.core.ProblemGene;
import at.fh.hagenberg.machinelearning.core.Solution;
import at.fh.hagenberg.machinelearning.core.SolutionGene;
import at.fh.hagenberg.machinelearning.core.fitness.CachetEvaluator;
import at.fh.hagenberg.util.Pair;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.RootNode;
import org.neo4j.driver.Values;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import science.aist.neo4j.reflective.ReflectiveNeo4JNodeRepositoryImpl;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Performance Evaluation pipeline.
 * Exists to add performance values to already existing stuff in the database
 *
 * @author Oliver Krauss on 11.07.2020
 */
public class SequentialVSParallelEval {

    private static final String REPORT_LOCATION = EngineConfig.ROOT_LOCATION + "/Amaru.LOGS/PerformanceProfiles/math/performance";

    public static void printTable(double[][] table) {
        for (double[] row : table) {
            for (double i : row) {
                System.out.print(i);
                System.out.print("\t");
            }
            System.out.println();
        }
    }


    private static void printInRow(double[][] sequential, double[][] parallel) {
        for (int i = 0; i < sequential.length; i++) {
            for (int j = 0; j < sequential[i].length; j++) {
                System.out.print(sequential[i][j]);
                System.out.print("\t");
            }
            for (int j = 0; j < sequential[i].length; j++) {
                System.out.print(parallel[i][j]);
                System.out.print("\t");
            }
            System.out.println();
        }
    }

    private static void printAvg(double[][] sequential, double[][] parallel) {
        System.out.println("Sequential \t Parallel \t S/P");
        for (int i = 0; i < sequential.length; i++) {
            double seq = Arrays.stream(sequential[i]).average().getAsDouble();
            double par = Arrays.stream(parallel[i]).average().getAsDouble();
            System.out.print(seq);
            System.out.print("\t");
            System.out.print(par);
            System.out.print("\t");
            System.out.print(seq / par);
            System.out.println();
        }
    }

    public static void main(String[] args) throws IOException {

        HashMap<Long, Integer> index = new HashMap<>();
        HashMap<Long, Integer> position = new HashMap<>();
        HashMap<Long, Integer> parallelPosition = new HashMap<>();

        double[][] sequential = new double[10][10];
        double[][] parallel = new double[10][10];

        File[] files = new File(REPORT_LOCATION).listFiles();
        int pos = 0;
        for (File file : files) {
            if (file.getName().endsWith("_opt.html")) {
                // load position in table
                Long id = Long.parseLong(file.getName().substring(5, file.getName().lastIndexOf("_") - 2));
                boolean isParallel = id < 0;
                if (isParallel) {
                    id = id * -1;
                }
                if (!index.containsKey(id)) {
                    index.put(id, pos++);
                    position.put(id, 0);
                    parallelPosition.put(id, 0);
                }
                int tableRowPos = index.get(id);
                int tableColPos = isParallel ? parallelPosition.get(id) : position.get(id);
                if (isParallel) {
                    parallelPosition.put(id, tableColPos + 1);
                } else {
                    position.put(id, tableColPos + 1);
                }

                // load file content
                String content = Files.readString(file.toPath());
                int find = content.indexOf("1st Quartile");
                int find_end = content.indexOf("</tr>", find);
                content = content.substring(find, find_end);
                content = content.substring(content.indexOf("lax\">") + 5, content.lastIndexOf("<"));
                if (isParallel) {
                    parallel[tableRowPos][tableColPos] += Double.parseDouble(content);
                } else {
                    sequential[tableRowPos][tableColPos] += Double.parseDouble(content);
                }
            }
        }

        printTable(sequential);
        System.out.println();
        printTable(parallel);
        System.out.println();
        printInRow(sequential, parallel);
        System.out.println();
        printAvg(sequential, parallel);

        String[] header = new String[10];
        for (int i = 0; i < header.length; i++) {
            int finalI = i;
            header[i] = String.valueOf(index.entrySet().stream().filter(x -> x.getValue().equals(finalI)).findFirst().get().getKey());
        }
        System.out.println(Arrays.toString(header));

        DatasetReportTransformer transformer = new DatasetReportTransformer(null);
        transformer.setWriter(new PrintWriter(System.out));
        AutoStatistics autoStatistics = new AutoStatistics();

//        for (int i = 0; i < header.length; i++) {
//            System.out.println();
//            System.out.println();
//            System.out.println("ANALYSIS FOR AST " + header[i]);
//            Report report = autoStatistics.report(new Dataset(new double[][] {sequential[i], parallel[i]}, new String[]{"sequential", "parallel"}));
//            transformer.transform(report.getReport(0));
//            transformer.transform(report.getReport(1));
//        }
    }

}