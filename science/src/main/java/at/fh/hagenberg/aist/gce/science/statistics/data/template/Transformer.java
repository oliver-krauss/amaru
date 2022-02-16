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

import java.io.Writer;

/**
 * Transformer for Datasets and Reports
 * @author Oliver Krauss on 23.10.2019
 */
public interface Transformer {

    /**
     * Transforms a dataset
     * @param dataset to be transformed
     */
    void transform(Dataset dataset);

    /**
     * Transforms a report (incl. sub-reports)
     * @param report to be transformed
     */
    void transform(Report report);

    /**
     * Transforms a dataset with its corresponding report
     * @param dataset to be transformed
     * @param report to be transformed
     */
    void transform(Dataset dataset, Report report);

    /**
     * The transform process will print to this writer
     * @return writer
     */
    Writer getWriter();

    /**
     * The transform process will print to this writer
     * @param writer to be printed to
     */
    void setWriter(Writer writer);
}
