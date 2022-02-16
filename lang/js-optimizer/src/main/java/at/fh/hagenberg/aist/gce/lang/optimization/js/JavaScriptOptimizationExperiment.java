/*
 * Copyright (c) 2022 the original author or authors.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

package at.fh.hagenberg.aist.gce.lang.optimization.js;

import at.fh.hagenberg.aist.gce.optimization.TruffleOptimizationSolution;
import at.fh.hagenberg.aist.gce.optimization.language.JavaScriptAccessor;
import at.fh.hagenberg.aist.gce.optimization.language.util.EngineConfig;
import at.fh.hagenberg.aist.gce.optimization.run.TruffleLanguageOptimizer;
import at.fh.hagenberg.aist.gce.optimization.run.TruffleLanguageTestfileOptimizer;
import at.fh.hagenberg.aist.gce.optimization.test.TruffleOptimizationTest;
import at.fh.hagenberg.aist.gce.optimization.util.JavaAssistUtil;
import at.fh.hagenberg.aist.gce.optimization.util.TruffleLanguageInformation;
import at.fh.hagenberg.aist.gce.optimization.util.TruffleLanguageSearchSpace;
import at.fh.hagenberg.aist.gce.optimization.util.strategy.KnownValueStrategy;
import at.fh.hagenberg.aist.gce.optimization.util.strategy.TruffleSimpleStrategy;
import at.fh.hagenberg.aist.gce.optimization.util.strategy.TruffleVerifyingStrategy;
import at.fh.hagenberg.aist.gce.optimization.util.strategy.other.StaticObjectStrategy;
import com.oracle.truffle.api.TrufflePublicAccess;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.js.builtins.DataViewPrototypeBuiltinsFactory;
import com.oracle.truffle.js.builtins.JSBuiltinsContainer;
import com.oracle.truffle.js.lang.JavaScriptLanguage;
import com.oracle.truffle.js.nodes.access.GetViewValueNode;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRealm;
import com.oracle.truffle.js.runtime.builtins.JSFunctionLookup;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.URL;
import java.util.*;

/**
 * @author Oliver Krauss on 26.12.2018
 */
public class JavaScriptOptimizationExperiment {

    private static int DEPTH = 5;
    private static int WIDTH = 5;

    private class JavaScriptTestfileOptimizer extends TruffleLanguageTestfileOptimizer {

        private String methodName;

        private TrufflePublicAccess<JavaScriptLanguage, JSRealm> access = new TrufflePublicAccess<>(JavaScriptAccessor.class);

        public JavaScriptTestfileOptimizer(String fileName, String methodName, Node bestKnownSolution, int algorithm) {
            super(fileName, bestKnownSolution, DEPTH, WIDTH, "js", EngineConfig.ROOT_LOCATION + "/Amaru/lang/js-optimizer/src/main/resources/testCases/");
            this.methodName = methodName;
            init();
        }

        @Override
        protected String getDescription() {
            return NAME + " " + methodName;
        }

        @Override
        protected TruffleLanguageSearchSpace getTruffleLanguageSearchSpace() {
            List<Class> excludes = new ArrayList<>();
            excludes.add(GetViewValueNode.class);
            excludes.add(DataViewPrototypeBuiltinsFactory.DataViewGetNodeGen.class);
            try {
                Enumeration<URL> urls = urls = this.getClass().getClassLoader().getResources("");
                while (urls.hasMoreElements()) {
                    File base = new File(urls.nextElement().getPath());
                    //excludes.addAll(findClassesInClasspath("com.oracle.truffle.js.builtins.simd".replace(".", "/"), base));
                    //excludes.addAll(findClassesInClasspath("com.oracle.truffle.js.nodes.access".replace(".", "/"), base));
                    //excludes.addAll(findClassesInClasspath("com.oracle.truffle.js.nodes.binary".replace(".", "/"), base));
                    //excludes.addAll(findClassesInClasspath("com.oracle.truffle.js.nodes.function".replace(".", "/"), base));
                    //excludes.addAll(findClassesInClasspath("com.oracle.truffle.js.builtins".replace(".", "/"), base));
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

            TruffleLanguageInformation information = TruffleLanguageInformation.getLanguageInformation(JavaScriptLanguage.ID);
            return new TruffleLanguageSearchSpace(information, excludes);
        }

        @Override
        protected Set<TruffleOptimizationTest> getTestCases() {
            // add input argument buffer, as JS expects two system arguments for every function call
            Set<TruffleOptimizationTest> cases = super.getTestCases();
            cases.forEach(x -> {
                Object[] inputArguments = x.getInputArguments();
                Object[] javaScriptFill = new Object[inputArguments.length + 2];
                System.arraycopy(inputArguments, 0, javaScriptFill, 2, inputArguments.length);
                x.setInputArguments(javaScriptFill);
            });
            return cases;
        }

        @Override
        protected String getLanguage() {
            return JavaScriptLanguage.ID;
        }

        @Override
        protected Map<String, TruffleVerifyingStrategy> getTerminalStrategies() {
            Map<String, TruffleVerifyingStrategy> strategies = new HashMap<>();
            strategies.put("com.oracle.truffle.js.runtime.JSContext", new StaticObjectStrategy<JSContext>(access.getCurrentContext().getContext()));

            LinkedList builtins = new LinkedList();
            try {
                JSFunctionLookup lookup = access.getCurrentContext().getContext().getFunctionLookup();
                Field f = lookup.getClass().getSuperclass().getDeclaredField("containers");
                Map<String, JSBuiltinsContainer> containers = (Map<String, JSBuiltinsContainer>) JavaAssistUtil.safeFieldAccess(f, lookup);
                containers.values().forEach(x -> {
                    x.forEachBuiltin(builtin ->
                        builtins.add(builtin));
                });
            } catch (NoSuchFieldException e) {
                e.printStackTrace();
            }
            strategies.put("com.oracle.truffle.js.nodes.function.JSBuiltin", new KnownValueStrategy(builtins));
            return strategies;
        }
    }

    private void test(String fileName, String methodName, Node bestKnownSolution) {
        try {

            System.out.println("Test " + fileName + " starting at: " + new Date());
            for (int i = 0; i < 3; i++) {
                // given
                TruffleLanguageOptimizer optimizer = new JavaScriptTestfileOptimizer(fileName, methodName, bestKnownSolution, i);

                // when
                TruffleOptimizationSolution solution = optimizer.optimize();

                // then
                if (solution.getTestResults() != null) {
                    System.out.println("Solved: " + solution.getTestResults().stream().allMatch(x -> x.solved()));
                }

                System.out.println("FINISHED TEST GROUP: " + fileName + " Alg - " + i + " at " + new Date().toString());
            }
        } catch (Exception e) {
            System.out.println("Run Failed at " + fileName);
            e.printStackTrace();
        }
    }

    public static void main(String[] args) throws IOException {
//    System.in.read();

        JavaScriptOptimizationExperiment test = new JavaScriptOptimizationExperiment();

        for (int i = 0; i <= 1; i++) {
            test.test("TimesTwo", "TimesDos", null);
//      test.test("threeDCube", "MMulti", null);
//      test.test("threeDCube", "DrawLine", null);
//      test.test("threeDCube", "CalcCross", null);
//      test.test("threeDCube", "CalcNormal", null);
//      test.test("threeDCube", "DrawQube", null);
            System.out.println("Ending: " + new Date().toString());
        }
    }


    /**
     * Finds all classes in the classpath
     *
     * @param packageName package that classes must be in
     * @param f           folder that is being searched
     */
    private static List<Class> findClassesInClasspath(String packageName, File f) {
        List<Class> classes = new ArrayList<>();
        for (File file : f.listFiles()) {
            if (file.isDirectory()) {
                classes.addAll(findClassesInClasspath(packageName, file));
            } else if (file.getPath().contains(packageName) && file.getName().endsWith(".class")) {
                try {
                    Class x = Class.forName(file.getPath().substring(file.getPath().indexOf("bin") + 4, file.getPath().lastIndexOf(".")).replace('/', '.'));
                    classes.add(x);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        return classes;
    }
}
