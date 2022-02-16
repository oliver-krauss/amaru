/*
 * Copyright (c) 2022 the original author or authors.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

package at.fh.hagenberg.aist.gce.science.statistics.data.template;

import at.fh.hagenberg.aist.gce.pattern.PatternNodeWrapper;
import at.fh.hagenberg.aist.gce.pattern.TruffleDifferentialPatternSolution;
import at.fh.hagenberg.aist.gce.pattern.TrufflePattern;
import at.fh.hagenberg.aist.gce.pattern.TrufflePatternProblem;
import at.fh.hagenberg.machinelearning.analytics.graph.nodes.NodeWrapper;
import at.fh.hagenberg.machinelearning.analytics.graph.nodes.OrderedRelationship;
import at.fh.hagenberg.machinelearning.core.Gene;
import at.fh.hagenberg.machinelearning.core.Solution;
import at.fh.hagenberg.util.Pair;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Printer for Pattern Analysis reports
 *
 * @author Oliver Krauss on 23.10.2019
 */
public class PatternAnalysisPrinter extends FreemarkerPrinter {

    /**
     * Identifier of truffle pattern template
     */
    private static final String TRUFFLE_PATTERN_TEMPLATE = "trufflePattern";

    /**
     * Identifier of truffle solution (Solution<TrufflePattern, TrufflePatternProblem>) template
     */
    private static final String TRUFFLE_SOLUTION_TEMPLATE = "truffleSolution";

    /**
     * Identifier of search space template
     */
    private static final String TRUFFLE_SEARCHSPACE_TEMPLATE = "truffleSearchSpace";

    /**
     * Identifier of differential pattern template
     */
    private static final String TRUFFLE_DIFFERENTIAL_PATTERN_TEMPLATE = "truffleDifferentialPattern";

    /**
     * Debug flag, adding additional information to the prints
     */
    private boolean debug = false;

    /**
     * Flag if the calculation how the different groups match should be printed
     */
    private boolean printDiff = true;

    /**
     * Flag if the search space shall be shown. Since this can be many thousands of trees, you may not always want to
     */
    private boolean omitSearchSpace = false;

    /**
     * Flag if the PPP shall be shown. This can be still hundreds of trees, and may prevent the browser from rendering
     */
    private boolean omitPatternsPerProblem = false;

    /**
     * Flag if all ids shall be printed, which allows us tacing patterns in the search space
     */
    private boolean omitTracability = false;

    public PatternAnalysisPrinter(String templateDirectory) {
        this(templateDirectory, false);
    }

    public PatternAnalysisPrinter(String templateDirectory, boolean debug) {
        super(templateDirectory);
        this.setSupportedFormats(Arrays.asList("md", "html"));
        this.setFormat("html");
        this.addTemplate(TRUFFLE_PATTERN_TEMPLATE, TRUFFLE_PATTERN_TEMPLATE);
        this.addTemplate(TRUFFLE_SOLUTION_TEMPLATE, TRUFFLE_SOLUTION_TEMPLATE);
        this.addTemplate(TRUFFLE_SEARCHSPACE_TEMPLATE, TRUFFLE_SEARCHSPACE_TEMPLATE);
        this.addTemplate(TRUFFLE_DIFFERENTIAL_PATTERN_TEMPLATE, TRUFFLE_DIFFERENTIAL_PATTERN_TEMPLATE);
        this.configuration.setAPIBuiltinEnabled(true);
        this.setDebug(debug);
        this.setPrintDiff(printDiff);
    }

    /**
     * @param debug if debug output should be printed as well
     */
    public PatternAnalysisPrinter(boolean debug) {
        this(null, debug);
    }

    public PatternAnalysisPrinter() {
        this(null);
    }

    /**
     * Prints the entire class hierarchy and all entry points (basically everything that ever was used by any node, incl. interface)
     *
     * @param pattern to be printed
     */
    public void printPattern(TrufflePattern pattern) {
        super.transform(TRUFFLE_PATTERN_TEMPLATE, pattern);
    }

    /**
     * Prints a solution of pattern problems and their corresponding patterns
     *
     * @param solution to be printed
     */
    public void printPatternSolution(Solution<TrufflePattern, TrufflePatternProblem> solution) {
        List<TrufflePatternProblem> problems = solution.getSolutionGenes().stream().flatMap(x -> x.getProblemGenes().stream()).map(Gene::getGene).distinct().collect(Collectors.toList());
        this.addAdditionalTemplateValue("problems", problems);
        this.addAdditionalTemplateValue("helper", new PatternPrinterHelper());
        super.transform(TRUFFLE_SOLUTION_TEMPLATE, solution);
    }

    /**
     * Prints a truffle differential pattern
     *
     * @param differentialPattern to be printed
     */
    public void printDifferentialPattern(TruffleDifferentialPatternSolution differentialPattern) {
        // reset iterators of search space to be sure
        differentialPattern.getPatternsPerProblem().keySet().forEach(x -> x.getSearchSpace().reset());

        Map<List<TrufflePatternProblem>, Long> patternGroupings = new HashMap<>();

        if (differentialPattern.getPatternsPerProblem().size() <= 5) {
            // add all combinations IF manageable
            differentialPattern.getPatternsPerProblem().keySet().forEach(x -> {
                new HashSet<>(patternGroupings.keySet()).forEach(y -> {
                    LinkedList<TrufflePatternProblem> key = new LinkedList<>(y);
                    key.add(x);
                    patternGroupings.put(key, 0L);
                });
                patternGroupings.put(Collections.singletonList(x), 0L);
            });
        } else {
            // add just the single combos and full combo
            differentialPattern.getPatternsPerProblem().keySet().forEach(x -> {
                patternGroupings.put(Collections.singletonList(x), 0L);
            });
            patternGroupings.put(new LinkedList<>(differentialPattern.getPatternsPerProblem().keySet()), 0L);
        }


        // for each pattern add 1 to overlapping combinations
        differentialPattern.getDifferential().keySet().stream().map(x -> x.getPatternNode().getHash()).forEach(x -> {

            final int[] size = {0};
            patternGroupings.entrySet().stream().sorted(Comparator.<Map.Entry<List<TrufflePatternProblem>, Long>, Integer>comparing(y -> y.getKey().size()).reversed()).forEach(set -> {
                // check if all problems contain the pattern
                if (set.getKey().size() >= size[0] && set.getKey().stream().allMatch(z -> differentialPattern.getPatternsPerProblem().get(z).stream().anyMatch(zp -> zp.getPatternNode().getHash().equals(x)))) {
                    // if yes update
                    patternGroupings.put(set.getKey(), set.getValue() + 1);
                    size[0] = set.getKey().size();
                }
            });
        });
        // transform to smth. readable
        LinkedList<Pair<String, Long>> patternGroupingsTemplate = new LinkedList<>();
        patternGroupings.entrySet().stream().sorted((o1, o2) -> {
            if (o1.getKey().size() == o2.getKey().size()) {
                String k1 = o1.getKey().stream().map(TrufflePatternProblem::getName).sorted().collect(Collectors.joining("-"));
                String k2 = o2.getKey().stream().map(TrufflePatternProblem::getName).sorted().collect(Collectors.joining("-"));
                return k1.compareTo(k2);
            }
            return Integer.compare(o1.getKey().size(), o2.getKey().size());
        }).forEachOrdered(x -> patternGroupingsTemplate.addLast(new Pair<>(x.getKey().stream().map(TrufflePatternProblem::getName).sorted().collect(Collectors.joining("-")), x.getValue())));
        this.addAdditionalTemplateValue("patternGroupings", patternGroupingsTemplate);

        if (omitTracability) {
            differentialPattern.getDifferential().keySet().forEach(x -> {
                NodeWrapper.flatten(x.getPatternNode()).forEach(n -> {
                    ((PatternNodeWrapper) n).getMatchedNodes().clear();
                    ((PatternNodeWrapper) n).getMatchedNodes().add(n.getId());
                });
            });
            differentialPattern.getPatternsPerProblem().values().forEach(x -> {
                x.forEach(y -> {
                    NodeWrapper.flatten(y.getPatternNode()).forEach(n -> {
                        ((PatternNodeWrapper) n).getMatchedNodes().clear();
                        ((PatternNodeWrapper) n).getMatchedNodes().add(n.getId());
                    });
                });
            });
        }

        super.transform(TRUFFLE_DIFFERENTIAL_PATTERN_TEMPLATE, differentialPattern);
    }

    /**
     * Prints the trees that made up the original search space in no particular order
     *
     * @param searchSpace to be printed
     */
    public void printSearchSpace(LinkedList<Pair<NodeWrapper[], OrderedRelationship[]>> searchSpace) {
        super.transform(TRUFFLE_SEARCHSPACE_TEMPLATE, searchSpace);
    }

    public boolean isDebug() {
        return debug;
    }

    public void setDebug(boolean debug) {
        this.debug = debug;
        this.addAdditionalTemplateValue("debug", debug);
    }

    public void setPrintDiff(boolean printDiff) {
        this.printDiff = printDiff;
        this.addAdditionalTemplateValue("printDiff", printDiff);
    }

    public void setOmitSearchSpace(boolean omit) {
        this.omitSearchSpace = omit;
        this.addAdditionalTemplateValue("omitSearchSpace", omit);
    }

    public void setOmitPatternsPerProblem(boolean omit) {
        this.omitPatternsPerProblem = omit;
        this.addAdditionalTemplateValue("omitPatternsPerProblem", omit);
    }

    public void setOmitTracability(boolean tracability) {
        this.omitTracability = tracability;
    }
}
