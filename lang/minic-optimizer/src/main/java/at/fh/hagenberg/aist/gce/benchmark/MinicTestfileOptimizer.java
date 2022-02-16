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

import at.fh.hagenberg.aist.gce.minic.language.MinicContext;
import at.fh.hagenberg.aist.gce.minic.language.MinicLanguage;
import at.fh.hagenberg.aist.gce.minic.nodes.builtin.ReadNode;
import at.fh.hagenberg.aist.gce.minic.nodes.builtin.ReadNodeFactory;
import at.fh.hagenberg.aist.gce.minic.nodes.impl.function.MinicInvokeNodeFactory;
import at.fh.hagenberg.aist.gce.optimization.TruffleOptimizationProblem;
import at.fh.hagenberg.aist.gce.optimization.executor.ValueModifier;
import at.fh.hagenberg.aist.gce.optimization.run.TruffleLanguageTestfileOptimizer;
import at.fh.hagenberg.aist.gce.optimization.util.TruffleFunctionAnalyzer;
import at.fh.hagenberg.aist.gce.optimization.util.TruffleFunctionSignature;
import at.fh.hagenberg.aist.gce.optimization.util.TruffleLanguageInformation;
import at.fh.hagenberg.aist.gce.optimization.util.TruffleLanguageSearchSpace;
import at.fh.hagenberg.aist.gce.optimization.util.strategy.TruffleHierarchicalStrategy;
import at.fh.hagenberg.aist.gce.optimization.util.strategy.TruffleVerifyingStrategy;
import at.fh.hagenberg.aist.gce.optimization.util.strategy.other.StaticObjectStrategy;
import at.fh.hagenberg.aist.gce.optimization.util.strategy.random.MinicCantInvokeStrategy;
import at.fh.hagenberg.aist.gce.optimization.util.strategy.random.MinicFunctionLiteralStrategy;
import at.fh.hagenberg.aist.gce.optimization.util.strategy.random.MinicInvokeStrategy;
import at.fh.hagenberg.machinelearning.analytics.graph.MinicPatternRepository;
import at.fh.hagenberg.machinelearning.analytics.graph.PatternRepository;
import com.oracle.truffle.api.frame.MaterializedFrame;
import com.oracle.truffle.api.nodes.Node;
import org.graalvm.polyglot.Value;

import java.util.*;

/**
 * @author Oliver Krauss on 17.04.2019
 */
public class MinicTestfileOptimizer extends TruffleLanguageTestfileOptimizer {

    /**
     * Global setter that prevents us from incorrectly unwrapping in the performance pipeline by injecting the wrong strategies.
     */
    public static boolean PERFORMANCE_PIPE = false;

    public MinicTestfileOptimizer(String name, Node bestKnownSolution) {
        super(name, bestKnownSolution);

        // register the minic patterns
        if (PatternRepository.loadForLanguage(MinicLanguage.ID) == null) {
            PatternRepository.register(MinicLanguage.ID, new MinicPatternRepository());
        }

        init();
    }

    @Override
    protected TruffleLanguageSearchSpace getTruffleLanguageSearchSpace() {
        List<Class> excludes = new ArrayList<>();
        excludes.add(ReadNodeFactory.ReadNodeGen.class);
        excludes.add(ReadNode.class);
        TruffleLanguageInformation information = TruffleLanguageInformation.getLanguageInformation(MinicLanguage.ID);
        return new TruffleLanguageSearchSpace(information, excludes);
    }

    @Override
    protected String getLanguage() {
        return MinicLanguage.ID;
    }

    @Override
    protected List<TruffleHierarchicalStrategy> getStrategies() {
        MinicContext ctx = MinicLanguage.INSTANCE.getContextReference().get();
        List<TruffleHierarchicalStrategy> strategies = super.getStrategies();
        if (!PERFORMANCE_PIPE) {
            strategies.add(new MinicFunctionLiteralStrategy(ctx));
            Set<Class> invokeClasses = new HashSet<>();
            invokeClasses.add(MinicInvokeNodeFactory.MinicInvokeIntNodeGen.class);
            invokeClasses.add(MinicInvokeNodeFactory.MinicInvokeFloatNodeGen.class);
            invokeClasses.add(MinicInvokeNodeFactory.MinicInvokeVoidNodeGen.class);
            invokeClasses.add(MinicInvokeNodeFactory.MinicInvokeArrayNodeGen.class);
            invokeClasses.add(MinicInvokeNodeFactory.MinicInvokeCharNodeGen.class);
            invokeClasses.add(MinicInvokeNodeFactory.MinicInvokeStringNodeGen.class);
            invokeClasses.add(MinicInvokeNodeFactory.MinicInvokeDoubleNodeGen.class);

            // create dedicated invoke strategies for each method
            ctx.getFunctionRegistry().getFunctions().stream().filter(x -> !x.getName().equals("main") && !x.getName().equals("read") && !x.getName().endsWith("_benchmark") && !x.getName().endsWith("_entry")).forEach(x -> {
                MinicInvokeStrategy invoke = new MinicInvokeStrategy(x);
                strategies.add(invoke);
                invokeClasses.remove(invoke.getInvokeClazz());
            });
            // prevent other invokes if they have no valid impl:
            invokeClasses.forEach(x -> {
                strategies.add(new MinicCantInvokeStrategy(ctx, x));
            });
        }
        return strategies;
    }

    @Override
    protected Map<String, TruffleVerifyingStrategy> getTerminalStrategies() {
        Map<String, TruffleVerifyingStrategy> strategies = new HashMap<>();
        if (MinicLanguage.INSTANCE.getContextReference().get().getGlobalStorage() != null) {
            strategies.put("com.oracle.truffle.api.frame.MaterializedFrame", new StaticObjectStrategy<MaterializedFrame>(MinicLanguage.INSTANCE.getContextReference().get().getGlobalStorage()));
        }
        strategies.put("at.fh.hagenberg.aist.gce.minic.language.MinicContext", new StaticObjectStrategy<MinicContext>(MinicLanguage.INSTANCE.getContextReference().get()));
        return strategies;
    }

    public TruffleOptimizationProblem getProblem() {
        return this.createProblem();
    }
}
