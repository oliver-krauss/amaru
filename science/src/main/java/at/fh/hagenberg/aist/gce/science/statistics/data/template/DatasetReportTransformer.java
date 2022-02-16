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
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;

/**
 * Store that is based on freemarker. It will persist according to the given testFiles
 *
 * @author Oliver Krauss on 23.10.2019
 */
public class DatasetReportTransformer extends FreemarkerPrinter implements Transformer {

    /**
     * preprocessor for Templates
     */
    private TemplatePreprocessor templatePreprocessor = null;

    public DatasetReportTransformer(String templateDirectory) {
        super(templateDirectory);
        this.addTemplate("dataset", "csv");
        this.addTemplate("report", "report");
        this.addTemplate("combined", "combined");
    }

    @Override
    public void transform(Dataset dataset) {
        super.transform("dataset", dataset);
    }

    @Override
    public void transform(Report report) {
        super.transform("report", report);
    }

    @Override
    public void transform(Dataset dataset, Report report) {
        Map<String, Object> data = new HashMap<>();
        data.put("dataset", dataset);
        data.put("report", report);
        this.applyTemplate("combined", data);
    }

    public String getDatasetTemplate() {
        return this.getTemplateMap().get("dataset");
    }

    public void setDatasetTemplate(String datasetTemplate) {
        this.addTemplate("dataset", datasetTemplate);
    }

    public String getReportTemplate() {
        return this.getTemplateMap().get("report");
    }

    public void setReportTemplate(String reportTemplate) {
        this.addTemplate("report", reportTemplate);
    }

    public String getCombinedTemplate() {
        return this.getTemplateMap().get("combined");
    }

    public void setCombinedTemplate(String combinedTemplate) {
        this.addTemplate("combined", combinedTemplate);
    }
}
