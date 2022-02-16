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

import at.fh.hagenberg.aist.gce.context.ApplicationContextProvider;
import at.fh.hagenberg.aist.gce.minic.language.MinicLanguage;
import at.fh.hagenberg.aist.gce.optimization.language.util.EngineConfig;
import at.fh.hagenberg.aist.gce.optimization.util.TruffleLanguageInformation;
import at.fh.hagenberg.aist.gce.pattern.*;
import at.fh.hagenberg.aist.gce.pattern.algorithm.editor.EPMReproductionEditor;
import at.fh.hagenberg.aist.gce.pattern.algorithm.metric.AbsDifferenceMetric;
import at.fh.hagenberg.aist.gce.pattern.algorithm.metric.DifferenceMustGrowMetric;
import at.fh.hagenberg.aist.gce.pattern.constraint.CachetConstraint;
import at.fh.hagenberg.aist.gce.pattern.constraint.ProblemConstraint;
import at.fh.hagenberg.aist.gce.pattern.encoding.BitwisePatternMeta;
import at.fh.hagenberg.aist.gce.pattern.encoding.BitwisePatternMetaLoader;
import at.fh.hagenberg.aist.gce.pattern.selection.TrufflePatternSearchSpace;
import at.fh.hagenberg.aist.gce.pattern.selection.TrufflePatternSearchSpaceDefinition;
import at.fh.hagenberg.aist.gce.science.statistics.data.template.PatternAnalysisPrinter;
import at.fh.hagenberg.machinelearning.core.Solution;
import at.fh.hagenberg.util.Pair;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Main class for the EPM publication
 *
 * @author Oliver Krauss on 01.04.2020
 */
public class EpmReproduction {

    /**
     * TODO #74 this should not be hardcoded
     * folder location where the output will be pushed to
     */
    private static final String LOCATION = EngineConfig.ROOT_LOCATION + "/Amaru.LOGS/EPMReproduction/";
    static String floatPostfix = "Float";

    // Due to a bug in the db, "QuickSort" is excluded for now
    // Pivot sort is excluded as this uses the Java internal implementation
    // THIS IS THE DISTRO ON Energy
//    static List<String> goodAlgorithms = Arrays.asList("heapSort", "quickSortInlined", "mergeSortInlined", "shellSort");
//    static List<String> badAlgorithms = Arrays.asList("selectionSort", "shakerSort", "bubbleSort", "insertionSort");
    // THIS IS THE DISTRO ON Performance.
    static List<String> goodAlgorithms = Arrays.asList( "shakerSort", "mergeSortInlined", "shellSort"); // "heapSort",
    static List<String> badAlgorithms = Arrays.asList("selectionSort", "bubbleSort", "insertionSort"); // "quickSortInlined",

    static List<String> algorithms;

    static final int MAX_SIZE = 15;

    static {
        algorithms = new ArrayList<>(goodAlgorithms);
        algorithms.addAll(badAlgorithms);
    }

    // Arrays.asList("SelectionSort", "HeapSort", "QuickSortIterative", "MergeSortIterative", "ShakerSort", "BubbleSort", "QuickSortThreeWay", "MergeSort", "InsertionSort", "ShellSort", "QuickSort");

    /**
     * Algorithm to compare the different trees
     */
    static TrufflePatternDetector detector = new TrufflePatternDetector();

    /**
     * Reporting tools
     */
    static FileWriter writer;

    static PatternAnalysisPrinter printer = new PatternAnalysisPrinter();

    /**
     * Compare the algorithms with themselves to check if we have any significant structural differences between datatypes
     *
     * @param algorithm
     */
    private static void incrementAndSearch(String algorithm) {
        int maxPatternSize = 0;
        Solution<TrufflePattern, TrufflePatternProblem> detect = new Solution<>();

        TrufflePatternSearchSpace searchSpace = detector.findSearchSpace(new TrufflePatternSearchSpaceDefinition(Arrays.asList(new CachetConstraint("Accuracy-0.4", 0.0, 0.0)), null, Arrays.asList(
                new ProblemConstraint(algorithm + floatPostfix), new ProblemConstraint(algorithm)
        ), null, null, false));

        while (detect.getSolutionGenes().size() == 0 && maxPatternSize < 5) {
            maxPatternSize++;
            detector.setMaxPatternSize(maxPatternSize);
            detector.injectMetric(null, 0, 0.65);
            detect = detector.findPatterns(MinicLanguage.ID, searchSpace, "algorithm");
            detect = detector.findSignificantPatterns(detect);
            searchSpace.reset();
        }

        try {
            writer.append("<h1>In " + algorithm + " found " + detect.getSolutionGenes().size() + " patterns</h1>");
        } catch (IOException e) {
            e.printStackTrace();
        }
        printer.printSearchSpace(searchSpace.getSearchSpace());
        detect.getSolutionGenes().forEach(x -> {
            try {
                writer.append("<p>Pattern occurs ").append(String.valueOf(x.getGene().getTreeCount())).append(" times (").append(x.getGene().getTreeIds().stream().map(Object::toString).collect(Collectors.joining(","))).append(")</p>");
            } catch (IOException e) {
                e.printStackTrace();
            }
            printer.printPattern(x.getGene());
        });
    }

    private static void algCompare(String filename, List<Pair<String, List<String>>> compare) throws IOException {
        writer.close();
        writer = new FileWriter(LOCATION + filename);
        printer.setWriter(writer);

        // get problem definition
        List<TrufflePatternProblem> problems = compare.stream().map(x ->
                new TrufflePatternProblem(MinicLanguage.ID,
                        detector.findSearchSpace(new TrufflePatternSearchSpaceDefinition(null, null, x.getValue().stream().map(ProblemConstraint::new).collect(Collectors.toList()), null, null, false)),
                        x.getKey(), meta)
        ).collect(Collectors.toList());

        detector.clearMetrics();
        detector.setMaxPatternSize(MAX_SIZE);
        detector.injectMetric(null, new AbsDifferenceMetric(null, 0.75, 1.0, problems));
        //detector.injectMetric(null, 0, 1.0, problems);
        detector.injectMetric(null, 0.75, 1.0);

        Solution<TruffleDifferentialPatternSolution, TrufflePatternProblem> significantDiff = detector.compareSignificantPatterns(problems);
        printer.printDifferentialPattern(significantDiff.getSolutionGenes().get(0).getGene());

        System.out.println("compared " + filename);
    }

    static BitwisePatternMeta meta;

    public static void main(String[] args) throws IOException {
        System.out.println("Starting: " + new Date().toString());

        // Identify the differences in the algorithms with their distinct types
        detector.setHierarchyFloor(1);
        detector.setHierarchyCeil(3);
        detector.setGrouping(SignificanceType.MAX);
        printer.setDebug(false);
        printer.setPrintDiff(false);
        printer.setFormat("html");
        writer = new FileWriter(LOCATION + "algorithm-self-comparison.html");
        printer.setWriter(writer);
        printer.setOmitPatternsPerProblem(true);
        printer.setOmitSearchSpace(false);
        printer.setOmitTracability(false);
//        algorithms.forEach(EpmReproduction::incrementAndSearch);

        detector.setEmbedded(false);
        detector.setMaxPatternSize(MAX_SIZE);
        detector.setGrouping(SignificanceType.MAX);
        TruffleLanguageInformation tli = TruffleLanguageInformation.getLanguageInformation(MinicLanguage.ID);
        List<String> datatypes = new ArrayList<>();
        datatypes.add("Char");
        datatypes.add("Int");
        datatypes.add("Float");
        datatypes.add("Double");
        datatypes.add("String");
        meta = BitwisePatternMetaLoader.loadDatatypeIndependent(MinicLanguage.ID, datatypes, false);
        detector.setEditor(new EPMReproductionEditor(meta, 1, 1));
        detector.setHierarchyCeil(1);
        detector.setHierarchyFloor(1);


        // compare integer algs
        algCompare("good-bad-comparison-integer.html", Arrays.asList(
                new Pair<>("good", goodAlgorithms.stream().collect(Collectors.toList())),
                new Pair<>("bad", badAlgorithms.stream().collect(Collectors.toList()))
        ));

        // compare double algs
        algCompare("good-bad-comparison-double.html", Arrays.asList(
                new Pair<>("good", goodAlgorithms.stream().map(x -> x + floatPostfix).collect(Collectors.toList())),
                new Pair<>("bad", badAlgorithms.stream().map(x -> x + floatPostfix).collect(Collectors.toList()))
        ));

        // compare full algs
        List<String> goodAlgs = goodAlgorithms.stream().collect(Collectors.toList());
        goodAlgs.addAll(goodAlgorithms.stream().map(x -> x + floatPostfix).collect(Collectors.toList()));
        List<String> badAlgs = badAlgorithms.stream().collect(Collectors.toList());
        badAlgs.addAll(badAlgorithms.stream().map(x -> x + floatPostfix).collect(Collectors.toList()));
        algCompare("good-bad-comparison.html", Arrays.asList(
                new Pair<>("good", goodAlgs),
                new Pair<>("bad", badAlgs)
        ));

        detector.setMaxPatternSize(MAX_SIZE);

//        // compare algorithms by datatype
//        algCompare("datatype-comparison-good.html", Arrays.asList(
//                new Pair<>("float", goodAlgorithms.stream().map(x -> x + floatPostfix).collect(Collectors.toList())),
//                new Pair<>("int", goodAlgorithms.stream().collect(Collectors.toList()))
//        ));
//        algCompare("datatype-comparison-bad.html", Arrays.asList(
//                new Pair<>("float", badAlgorithms.stream().map(x -> x + floatPostfix).collect(Collectors.toList())),
//                new Pair<>("int", badAlgorithms.stream().collect(Collectors.toList()))
//        ));
//        algCompare("datatype-comparison.html", Arrays.asList(
//                new Pair<>("float", algorithms.stream().map(x -> x + floatPostfix).collect(Collectors.toList())),
//                new Pair<>("int", algorithms.stream().collect(Collectors.toList()))
//        ));

        try {
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Next Steps:
        // UI improvements

        // Possible secondary test case -> JavaIO API Comparisons
        // Mine other projects for the found patterns -> see if we can identify energy issues

        System.out.println("Ending: " + new Date().toString());
        ApplicationContextProvider.getCtx().close();
    }
}
