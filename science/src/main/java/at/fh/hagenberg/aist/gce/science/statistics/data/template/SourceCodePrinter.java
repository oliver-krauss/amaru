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

import at.fh.hagenberg.aist.gce.optimization.util.TruffleClassInformation;
import at.fh.hagenberg.aist.gce.optimization.util.TruffleLanguageInformation;
import at.fh.hagenberg.machinelearning.analytics.graph.nodes.NodeWrapper;

import java.util.Arrays;
import java.util.Collection;
import java.util.stream.Collectors;

/**
 * Printer for the Node Wrapper class based on freemarker
 *
 * @author Oliver Krauss on 23.10.2019
 */
public class SourceCodePrinter extends FreemarkerPrinter {

    /**
     * Identifier of simple template
     */
    private static final String SOURCE_CODE_TEMPLATE = "sourceCode";

    /**
     * Debug flag, adding additional information to the prints
     */
    private boolean debug = false;

    /**
     * TLI for more exhaustive parsing of truffle language
     */
    private TruffleLanguageInformation tli;

    public SourceCodePrinter(String templateDirectory) {
        this(templateDirectory, false);
    }

    public SourceCodePrinter(String templateDirectory, boolean debug) {
        super(templateDirectory);
        this.setSupportedFormats(Arrays.asList("html"));
        this.addTemplate(SOURCE_CODE_TEMPLATE, SOURCE_CODE_TEMPLATE);
        this.setDebug(debug);
    }

    public void printAst(NodeWrapper node) {
        this.addAdditionalTemplateValue("tli", tli);
        super.transform(SOURCE_CODE_TEMPLATE, node);
    }

    public TruffleLanguageInformation getTli() {
        return tli;
    }

    public void setTli(TruffleLanguageInformation tli) {
        this.tli = tli;
    }


    public boolean isDebug() {
        return debug;
    }

    public void setDebug(boolean debug) {
        this.debug = debug;
        this.addAdditionalTemplateValue("debug", debug);
    }
}
