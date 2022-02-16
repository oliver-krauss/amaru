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

import java.util.Map;

/**
 * Printer for the Node Wrapper class based on freemarker
 *
 * @author Oliver Krauss on 23.10.2019
 */
public class TruffleLanguagePrinter extends FreemarkerPrinter {

    /**
     * Identifier of simple template
     */
    private static final String CLASS_HIERARCHY_ALL_TEMPLATE = "classHierarchyAll";

    /**
     * Identifier of simple template
     */
    private static final String CLASS_HIERARCHY_TEMPLATE = "classHierarchy";

    /**
     * Identifier of complex template
     */
    private static final String CLASS_INFORMATION_TEMPLATE = "classInformation";

    /**
     * Identifier of language space template
     */
    private static final String LANGUAGE_SPACE_TEMPLATE = "languageSpace";


    public TruffleLanguagePrinter(String templateDirectory) {
        super(templateDirectory);
        this.addTemplate(CLASS_HIERARCHY_TEMPLATE, CLASS_HIERARCHY_TEMPLATE);
        this.addTemplate(CLASS_INFORMATION_TEMPLATE, CLASS_INFORMATION_TEMPLATE);
        this.addTemplate(CLASS_HIERARCHY_ALL_TEMPLATE, CLASS_HIERARCHY_ALL_TEMPLATE);
        this.addTemplate(LANGUAGE_SPACE_TEMPLATE, LANGUAGE_SPACE_TEMPLATE);
        this.configuration.setAPIBuiltinEnabled(true);
    }

    /**
     * Prints the entire class hierarchy and all entry points (basically everything that ever was used by any node, incl. interface
     * Recommend printing this with the GraphViz Engine FDP as this is the only one able to render it!
     *
     * @param tli to be printed
     */
    public void printClassHierarchy(TruffleLanguageInformation tli) {
        super.transform(CLASS_HIERARCHY_ALL_TEMPLATE, tli);
    }

    /**
     * Prints the class hierarchy with a specific entry point. Only classes extending from there will be rendered.
     *
     * @param tli        to be printed
     * @param entryPoint entry class from which the hierarchy will be built
     */
    public void printClassHierarchy(TruffleLanguageInformation tli, Class entryPoint) {
        super.addAdditionalTemplateValue("entryPoint", entryPoint);
        super.transform(CLASS_HIERARCHY_TEMPLATE, tli);
    }

    public void printClassInformation(TruffleClassInformation tli) {
        super.transform(CLASS_INFORMATION_TEMPLATE, tli);
    }

    public void printLanguageSpace(Map<Class, TruffleClassInformation> space) {
        super.transform(LANGUAGE_SPACE_TEMPLATE, space);
    }
}
