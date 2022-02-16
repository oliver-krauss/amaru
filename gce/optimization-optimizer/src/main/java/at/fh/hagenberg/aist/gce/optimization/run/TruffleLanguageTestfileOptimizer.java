/*
 * Copyright (c) 2022 the original author or authors.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

package at.fh.hagenberg.aist.gce.optimization.run;


import at.fh.hagenberg.aist.gce.optimization.TruffleOptimizationProblem;
import at.fh.hagenberg.aist.gce.optimization.language.util.EngineConfig;
import at.fh.hagenberg.aist.gce.optimization.test.OrderedTestInput;
import at.fh.hagenberg.aist.gce.optimization.test.TruffleOptimizationTest;
import at.fh.hagenberg.aist.gce.optimization.test.TruffleTestValue;
import at.fh.hagenberg.aist.gce.optimization.test.ValueDefinitions;
import at.fh.hagenberg.aist.gce.optimization.util.ExtendedNodeUtil;
import at.fh.hagenberg.aist.gce.optimization.util.TruffleLanguageInformation;
import at.fh.hagenberg.aist.gce.optimization.util.strategy.CreationConfiguration;
import at.fh.hagenberg.util.Pair;
import com.oracle.truffle.api.nodes.Node;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * @author Oliver Krauss on 21.12.2018
 * Little helper class that loads from file.
 */
public abstract class TruffleLanguageTestfileOptimizer extends TruffleLanguageOptimizer {

    /**
     * The NAME must be equal to the function we want to optimize
     */
    protected final String NAME;

    protected final String FILENAME;

    protected String ENDING = "c";

    protected static final String IN = "input";

    protected static final String OUT = "output";

    // TODO #74 this should not be hardcoded
    protected String PATH = EngineConfig.ROOT_LOCATION + "/Amaru/gce/optimization-optimizer/src/test/resources/testCases/";

    private TruffleLanguageInformation information;

    protected Node bestKnownSolution;

    public TruffleLanguageTestfileOptimizer(String name, Node bestKnownSolution) {
        if (name.contains("/")) {
            int namePos = name.lastIndexOf("/") + 1;
            this.NAME = name.substring(namePos);
            this.FILENAME = name.substring(0, namePos) + Character.toUpperCase(name.charAt(namePos)) + name.substring(namePos + 1);
        } else {
            this.NAME = name;
            this.FILENAME = NAME.substring(0, 1).toUpperCase() + NAME.substring(1);
        }
        this.bestKnownSolution = bestKnownSolution;
    }

    public TruffleLanguageTestfileOptimizer(String name, Node bestKnownSolution, int depth, int width, String ending, String path) {
        ENDING = ending;
        PATH = path;
        this.NAME = name;
        this.FILENAME = NAME.substring(0, 1).toUpperCase() + NAME.substring(1);
        this.bestKnownSolution = bestKnownSolution;
    }

    @Override
    protected TruffleOptimizationProblem createProblem() {
        TruffleOptimizationProblem problem = super.createProblem();
        problem.setDescription(getDescription());
        return problem;
    }

    protected String getDescription() {
        return NAME;
    }

    protected File getSourceFile() {
        return new File(PATH + FILENAME + "." + ENDING);
    }

    @Override
    protected Set<TruffleOptimizationTest> getTestCases() {
        Set<TruffleOptimizationTest> cases = new HashSet<>();

        List<String> in = readAllLines(Paths.get(PATH + FILENAME + "." + IN));
        List<String> out = readAllLines(Paths.get(PATH + FILENAME + "." + OUT));

        Iterator<String> inIt = in.iterator();
        Iterator<String> outIt = out.iterator();

        while (inIt.hasNext() && outIt.hasNext()) {
            Set<OrderedTestInput> inList = new HashSet<>();
            TruffleOptimizationTest truffleOptimizationTest = new TruffleOptimizationTest(inList, cast(outIt.next()).get(0));
            AtomicInteger i = new AtomicInteger();
            inList.addAll(cast(inIt.next()).stream().map(x -> new OrderedTestInput(truffleOptimizationTest, x, i.getAndIncrement())).collect(Collectors.toList()));
            truffleOptimizationTest.setInput(inList);
            cases.add(truffleOptimizationTest);
        }

        return cases;
    }


    @Override
    protected String getCode() {
        try {
            return new String(Files.readAllBytes(getSourceFile().toPath()));
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    protected String getFunction() {
        return NAME;
    }

    @Override
    protected CreationConfiguration getConfiguration() {
        if (problem != null) {
            return problem.getConfiguration();
        }
        Node nodeToOptimize = getNodeToOptimize();
        // per default set the max depth and with to a little larger than the given search space
        return new CreationConfiguration(ExtendedNodeUtil.maxDepth(nodeToOptimize) + 1, ExtendedNodeUtil.maxWidth(nodeToOptimize) + 1, Double.MAX_VALUE);
    }

    private List<TruffleTestValue> cast(String s) {
        List<TruffleTestValue> inputs = new ArrayList<>();
        String[] split = s.split(";");
        for (String rawVal : split) {
            Pair<String, Object> val = ValueDefinitions.stringToValueTyped(rawVal);
            inputs.add(new TruffleTestValue(val.getValue(), val.getKey()));
        }
        return inputs;
    }


    private final String LF = System.getProperty("line.separator");

    private List<String> readAllLines(Path file) {
        // fix line feeds for non unix os
        List<String> strings = null;
        try {
            strings = Files.readAllLines(file, Charset.defaultCharset());
        } catch (IOException e) {
            e.printStackTrace();
        }
        return strings;
    }

    @Override
    protected Node getBestKnownSolution() {
        return bestKnownSolution;
    }
}
