/*
 * Copyright (c) 2022 the original author or authors.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

package at.fh.hagenberg.aist.gce.minic.test;

import at.fh.hagenberg.aist.gce.minic.language.MinicLanguage;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Source;
import org.graalvm.polyglot.Value;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Oliver Krauss on 26.07.2019
 */

public class MinicSourcefileBench {

    static class TestCase {
        protected String code;
        protected String sourceName;
        protected String[] testInput;
        protected String expectedOutput;

        protected TestCase(String sourceName) {
            this.sourceName = sourceName;
        }

        @Override
        public String toString() {
            return sourceName;
        }
    }

    private static final String SrcEnding = MinicLanguage.ID;
    private static final String InEnding = "input";
    private static final String OutEnding = "output";

    @DataProvider(name = "sourcefileProvider")
    public Object[] dataProvider() throws IOException {
        URL testFolder = this.getClass().getClassLoader().getResource("mathBench");
        Map<String, TestCase> tests = new HashMap<>();
        for (File file : new File(testFolder.getPath()).listFiles()) {
            if (file.isFile() && file.getName().contains(".")) {
                String name = file.getName().substring(0, file.getName().lastIndexOf("."));
                String ending = file.getName().substring(file.getName().lastIndexOf(".") + 1);
                if (!tests.containsKey(name)) {
                    tests.put(name, new TestCase(name));
                }
                switch (ending) {
                    case SrcEnding:
                        tests.get(name).code = readAllLines(file.toPath());
                        break;
                    case InEnding:
                        tests.get(name).testInput = Files.readAllLines(file.toPath(), Charset.defaultCharset()).toArray(new String[]{});
                        break;
                    case OutEnding:
                        tests.get(name).expectedOutput = readAllLines(file.toPath());
                        System.out.println(tests.get(name).expectedOutput);
                        break;
                    default:
                        tests.remove(name);
                }
            }
        }


        return tests.values().toArray();
    }

    @Test(dataProvider = "sourcefileProvider")
    public void test(TestCase tc) throws IOException {

        // given
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PrintStream stream = new PrintStream(out);
        Context ctx = tc.testInput == null ? Context.newBuilder(MinicLanguage.ID).out(stream).build() :
            Context.newBuilder(MinicLanguage.ID).arguments(MinicLanguage.ID, tc.testInput).out(stream).build();
        ctx.initialize(MinicLanguage.ID);
        Source src = Source.newBuilder(MinicLanguage.ID, tc.code, "test").build();

        // when
        long start = System.nanoTime();
        for (int i = 0; i < 100000; i++) {
            Value returnValue = ctx.eval(src);

            // then
            Assert.assertEquals(returnValue.toString(), tc.expectedOutput.trim());
        }
        long end = System.nanoTime();
        System.out.println("PERFORMANCE " + (end - start));
        start = System.nanoTime();
        for (int i = 0; i < 100000; i++) {
            Value returnValue = ctx.eval(src);

            // then
            Assert.assertEquals(returnValue.toString(), tc.expectedOutput.trim());
        }
        end = System.nanoTime();
        System.out.println("OPT PERFORMANCE " + (end - start));
    }

    private static String readAllLines(Path file) throws IOException {
        // fix line feeds for non unix os
        StringBuilder outFile = new StringBuilder();
        for (String line : Files.readAllLines(file, Charset.defaultCharset())) {
            outFile.append(line).append(System.getProperty("line.separator"));
        }
        return outFile.toString();
    }
}
