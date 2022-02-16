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


import at.fh.hagenberg.util.Pair;

import java.util.Collection;

/**
 * The Exception printer generates a report of exceptions provided to it.
 * It automatically groups exceptions according to:
 *   - a KEY provided by a caller
 *   - the LOCATION the exception occured
 *   - the EXCEPTION that occured
 * @author Oliver Krauss on 04.12.2019
 */
public class ExceptionPrinter extends FreemarkerPrinter {

    /**
     * Identifier of exception template
     */
    private static final String EXCEPTION_TEMPLATE = "exception";

    public ExceptionPrinter(String templateDirectory) {
        super(templateDirectory);
        this.addTemplate(EXCEPTION_TEMPLATE, EXCEPTION_TEMPLATE);
    }

    /**
     * Prints the exceptions into the template
     * @param exceptions to be printed <Key, Exception>
     */
    public void print(Collection<Pair<String, Exception>> exceptions) {
        super.transform(EXCEPTION_TEMPLATE, exceptions);
    }

    public String getExceptionTemplate() {
        return this.getTemplateMap().get(EXCEPTION_TEMPLATE);
    }

    public void setExceptionTemplate(String template) {
        this.addTemplate(EXCEPTION_TEMPLATE, template);
    }
}
