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

import at.fh.hagenberg.aist.gce.science.statistics.Report;
import at.fh.hagenberg.aist.gce.science.statistics.data.Dataset;

import java.util.Map;

/**
 * Preprocessor that automatically extracts values from dataset or report and makes it available to the template
 * @author Oliver Krauss on 24.10.2019
 */
public interface TemplatePreprocessor {

    /**
     * Processes dataset and report (both may be null!)
     * @param ds dataset to process
     * @param r  report to process
     */
    Map<String, Object> process(Dataset ds, Report r);
}
