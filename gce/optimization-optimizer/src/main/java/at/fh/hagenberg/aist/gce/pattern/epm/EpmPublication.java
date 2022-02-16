/*
 * Copyright (c) 2022 the original author or authors.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

package at.fh.hagenberg.aist.gce.pattern.epm;

import at.fh.hagenberg.aist.gce.context.ApplicationContextProvider;
import at.fh.hagenberg.aist.gce.optimization.language.util.EngineConfig;
import at.fh.hagenberg.aist.gce.pattern.*;
import at.fh.hagenberg.aist.gce.pattern.constraint.ProblemConstraint;
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
public class EpmPublication {

    /**
     * All algorithms compared by Andi
     */
    public static ProblemConstraint comparableSelectionSort = new ProblemConstraint("at.mana.sort.comparable.SelectionSort");
    public static ProblemConstraint comparableHeapSort = new ProblemConstraint("at.mana.sort.comparable.HeapSort");
    public static ProblemConstraint comparableQuickSortIterative = new ProblemConstraint("at.mana.sort.comparable.QuickSortIterative");
    //    public static ProblemConstraint comparableQuickSortThreeWay = new ProblemConstraint("at.mana.sort.comparable.QuickSortThreeWay");
    public static ProblemConstraint comparableShellSort = new ProblemConstraint("at.mana.sort.comparable.ShellSort");
    public static ProblemConstraint comparableShakerSort = new ProblemConstraint("at.mana.sort.comparable.ShakerSort");
    //    public static ProblemConstraint comparableQuickSort = new ProblemConstraint("at.mana.sort.comparable.QuickSort");
    public static ProblemConstraint comparableInsertionSort = new ProblemConstraint("at.mana.sort.comparable.InsertionSort");
    public static ProblemConstraint comparableMergeSortIterative = new ProblemConstraint("at.mana.sort.comparable.MergeSortIterative");
    //    public static ProblemConstraint comparableMergeSort = new ProblemConstraint("at.mana.sort.comparable.MergeSort");
    public static ProblemConstraint comparableBubbleSort = new ProblemConstraint("at.mana.sort.comparable.BubbleSort");
    //
    public static ProblemConstraint integerMergeSortIterative = new ProblemConstraint("at.mana.sort.integer.MergeSortIterative");
    public static ProblemConstraint integerShakerSort = new ProblemConstraint("at.mana.sort.integer.ShakerSort");
    public static ProblemConstraint integerQuickSortIterative = new ProblemConstraint("at.mana.sort.integer.QuickSortIterative");
    public static ProblemConstraint integerSelectionSort = new ProblemConstraint("at.mana.sort.integer.SelectionSort");
    //    public static ProblemConstraint integerQuickSortThreeWay = new ProblemConstraint("at.mana.sort.integer.QuickSortThreeWay");
    public static ProblemConstraint integerBubbleSort = new ProblemConstraint("at.mana.sort.integer.BubbleSort");
    public static ProblemConstraint integerHeapSort = new ProblemConstraint("at.mana.sort.integer.HeapSort");
    //    public static ProblemConstraint integerMergeSort = new ProblemConstraint("at.mana.sort.integer.MergeSort");
    public static ProblemConstraint integerInsertionSort = new ProblemConstraint("at.mana.sort.integer.InsertionSort");
    public static ProblemConstraint integerShellSort = new ProblemConstraint("at.mana.sort.integer.ShellSort");
    //    public static ProblemConstraint integerQuickSort = new ProblemConstraint("at.mana.sort.integer.QuickSort");
//
    public static ProblemConstraint dbleSelectionSort = new ProblemConstraint("at.mana.sort.dble.SelectionSort");
    public static ProblemConstraint dbleHeapSort = new ProblemConstraint("at.mana.sort.dble.HeapSort");
    public static ProblemConstraint dbleQuickSortIterative = new ProblemConstraint("at.mana.sort.dble.QuickSortIterative");
    public static ProblemConstraint dbleMergeSortIterative = new ProblemConstraint("at.mana.sort.dble.MergeSortIterative");
    public static ProblemConstraint dbleShakerSort = new ProblemConstraint("at.mana.sort.dble.ShakerSort");
    public static ProblemConstraint dbleBubbleSort = new ProblemConstraint("at.mana.sort.dble.BubbleSort");
    //    public static ProblemConstraint dbleQuickSortThreeWay = new ProblemConstraint("at.mana.sort.dble.QuickSortThreeWay");
//    public static ProblemConstraint dbleMergeSort = new ProblemConstraint("at.mana.sort.dble.MergeSort");
    public static ProblemConstraint dbleInsertionSort = new ProblemConstraint("at.mana.sort.dble.InsertionSort");
    public static ProblemConstraint dbleShellSort = new ProblemConstraint("at.mana.sort.dble.ShellSort");
//    public static ProblemConstraint dbleQuickSort = new ProblemConstraint("at.mana.sort.dble.QuickSort");

    /**
     * TODO #74 this should not be hardcoded
     * folder location where the output will be pushed to
     */
    private static final String LOCATION = EngineConfig.ROOT_LOCATION + "/Amaru.LOGS/EPM/";

    static String comparablePrefix = "at.mana.sort.comparable.";
    static String integerPrefix = "at.mana.sort.integer.";
    static String doublePrefix = "at.mana.sort.dble.";

    // Due to a bug in the db, "QuickSort" is excluded for now
    // Pivot sort is excluded as this uses the Java internal implementation
    static List<String> goodAlgorithms = Arrays.asList("HeapSort", "QuickSortIterative", "MergeSortIterative", "ShellSort");
    static List<String> badAlgorithms = Arrays.asList("SelectionSort", "ShakerSort", "BubbleSort", "InsertionSort");
    static List<String> algorithms;

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

        TrufflePatternSearchSpace searchSpace = detector.findSearchSpace(new TrufflePatternSearchSpaceDefinition(null, null, Arrays.asList(
                new ProblemConstraint(comparablePrefix + algorithm), new ProblemConstraint(doublePrefix + algorithm), new ProblemConstraint(integerPrefix + algorithm)
        ), null, null, true));

        while (detect.getSolutionGenes().size() == 0 && maxPatternSize < 5) {
            maxPatternSize++;
            detector.setMaxPatternSize(maxPatternSize);
            detector.injectMetric(null, 0, 0.65);
            detect = detector.findPatterns(null, searchSpace, "algorithm");
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
                new TrufflePatternProblem(null,
                        detector.findSearchSpace(new TrufflePatternSearchSpaceDefinition(null, null, x.getValue().stream().map(ProblemConstraint::new).collect(Collectors.toList()), null, null, true)),
                        x.getKey())
        ).collect(Collectors.toList());

        detector.clearMetrics();
        detector.setMaxPatternSize(5);
        detector.injectMetric(null, 0.65, 1, problems);
        detector.injectMetric(null, 0.75, 1);

        Solution<TruffleDifferentialPatternSolution, TrufflePatternProblem> significantDiff = detector.compareSignificantPatterns(problems);
        printer.printDifferentialPattern(significantDiff.getSolutionGenes().get(0).getGene());

        System.out.println("compared " + filename);
    }

    public static void main(String[] args) throws IOException {
        System.out.println("Starting: " + new Date().toString());

        // Identify the differences in the algorithms with their distinct types
        detector.setHierarchyFloor(1);
        detector.setGrouping(SignificanceType.MAX);
        printer.setDebug(false);
        printer.setPrintDiff(false);
        printer.setFormat("html");
        writer = new FileWriter(LOCATION + "algorithm-self-comparison.html");
        printer.setWriter(writer);
        algorithms.forEach(EpmPublication::incrementAndSearch);

        detector.setHierarchyFloor(1);
        detector.setMaxPatternSize(5);
        detector.setGrouping(SignificanceType.MAX);



        // compare comparable algs
        algCompare("good-bad-comparison-comparable.html", Arrays.asList(
                new Pair<>("good", goodAlgorithms.stream().map(x -> comparablePrefix + x).collect(Collectors.toList())),
                new Pair<>("bad", badAlgorithms.stream().map(x -> comparablePrefix + x).collect(Collectors.toList()))
        ));

        // compare integer algs
        algCompare("good-bad-comparison-integer.html", Arrays.asList(
                new Pair<>("good", goodAlgorithms.stream().map(x -> integerPrefix + x).collect(Collectors.toList())),
                new Pair<>("bad", badAlgorithms.stream().map(x -> integerPrefix + x).collect(Collectors.toList()))
        ));

        // compare double algs
        algCompare("good-bad-comparison-double.html", Arrays.asList(
                new Pair<>("good", goodAlgorithms.stream().map(x -> doublePrefix + x).collect(Collectors.toList())),
                new Pair<>("bad", badAlgorithms.stream().map(x -> doublePrefix + x).collect(Collectors.toList()))
        ));

        // compare full algs
        List<String> goodAlgs = goodAlgorithms.stream().map(x -> comparablePrefix + x).collect(Collectors.toList());
        goodAlgs.addAll(goodAlgorithms.stream().map(x -> integerPrefix + x).collect(Collectors.toList()));
        goodAlgs.addAll(goodAlgorithms.stream().map(x -> doublePrefix + x).collect(Collectors.toList()));
        List<String> badAlgs = badAlgorithms.stream().map(x -> comparablePrefix + x).collect(Collectors.toList());
        badAlgs.addAll(badAlgorithms.stream().map(x -> integerPrefix + x).collect(Collectors.toList()));
        badAlgs.addAll(badAlgorithms.stream().map(x -> doublePrefix + x).collect(Collectors.toList()));
        algCompare("good-bad-comparison.html", Arrays.asList(
                new Pair<>("good", goodAlgs),
                new Pair<>("bad", badAlgs)
        ));

        detector.setMaxPatternSize(5);

        // compare algorithms by datatype
        algCompare("datatype-comparison-good.html", Arrays.asList(
                new Pair<>("comparable", goodAlgorithms.stream().map(x -> comparablePrefix + x).collect(Collectors.toList())),
                new Pair<>("double", goodAlgorithms.stream().map(x -> doublePrefix + x).collect(Collectors.toList())),
                new Pair<>("int", goodAlgorithms.stream().map(x -> integerPrefix + x).collect(Collectors.toList()))
        ));
        algCompare("datatype-comparison-bad.html", Arrays.asList(
                new Pair<>("comparable", badAlgorithms.stream().map(x -> comparablePrefix + x).collect(Collectors.toList())),
                new Pair<>("double", badAlgorithms.stream().map(x -> doublePrefix + x).collect(Collectors.toList())),
                new Pair<>("int", badAlgorithms.stream().map(x -> integerPrefix + x).collect(Collectors.toList()))
        ));
        algCompare("datatype-comparison.html", Arrays.asList(
                new Pair<>("comparable", algorithms.stream().map(x -> comparablePrefix + x).collect(Collectors.toList())),
                new Pair<>("double", algorithms.stream().map(x -> doublePrefix + x).collect(Collectors.toList())),
                new Pair<>("int", algorithms.stream().map(x -> integerPrefix + x).collect(Collectors.toList()))
        ));

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
