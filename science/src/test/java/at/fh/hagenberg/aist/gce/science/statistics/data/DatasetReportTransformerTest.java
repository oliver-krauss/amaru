/*
 * Copyright (c) 2022 the original author or authors.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

package at.fh.hagenberg.aist.gce.science.statistics.data;

import at.fh.hagenberg.aist.gce.science.statistics.DatasetDependentTest;
import at.fh.hagenberg.aist.gce.science.statistics.Report;
import at.fh.hagenberg.aist.gce.science.statistics.data.template.DatasetReportTransformer;
import at.fh.hagenberg.aist.gce.science.statistics.data.template.LatexPreprocessor;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.StringWriter;

/**
 * @author Oliver Krauss on 23.10.2019
 */
public class DatasetReportTransformerTest extends DatasetDependentTest {

    @Test
    public void testSaveDataset() {
        // given
        DatasetReportTransformer store = new DatasetReportTransformer(null);
        StringWriter writer = new StringWriter();
        store.setWriter(writer);

        // when
        store.transform(getDataset());

        // then
        Assert.assertEquals(writer.toString(), getCSV());
    }

    @Test
    public void testSaveReport() {
        // given
        DatasetReportTransformer store = new DatasetReportTransformer(null);
        StringWriter writer = new StringWriter();
        store.setWriter(writer);

        // when
        store.transform(getReport());

        // then
        Assert.assertEquals(writer.toString(), getReportString());
    }

    @Test
    public void testSaveBoth() {
        // given
        DatasetReportTransformer store = new DatasetReportTransformer(null);
        StringWriter writer = new StringWriter();
        store.setWriter(writer);

        // when
        store.transform(getDataset(), getReport());

        // then
        Assert.assertEquals(writer.toString(), getCSV() + getReportString());
    }

    @Test
    public void testLatexDataset() {
        // given
        DatasetReportTransformer store = new DatasetReportTransformer(null);
        StringWriter writer = new StringWriter();
        store.setWriter(writer);
        store.setDatasetTemplate("latexDataset");
        store.addAdditionalTemplateValue("caption", "Test Caption");
        store.addAdditionalTemplateValue("label", "tab:testTable");

        // when
        store.transform(getShortDataset());

        // then
        Assert.assertEquals(writer.toString(), latexTable());
    }

    @Test
    public void testLatexReport() {
        // given
        DatasetReportTransformer store = new DatasetReportTransformer(null);
        store.setTemplatePreprocessor(new LatexPreprocessor());
        StringWriter writer = new StringWriter();
        store.setWriter(writer);
        store.setReportTemplate("latexReport");

        Report r = getReport();
        r.addReport("label", "String", "tab:testTable");

        // when
        store.transform(r);

        // then
        Assert.assertEquals(writer.toString(), reportTable());
    }

    private String latexTable() {
        return "\\begin{table}[]\n" +
            "\\begin{center}\n" +
            "\\begin{tabular}{|l|l|l|}\n" +
            "\\hline\n" +
            "  A&B&C \\\\ \\hline\n" +
            "  488&512&489 \\\\ \\hline\n" +
            "  486&512&490 \\\\ \\hline\n" +
            "  492&512&486 \\\\ \\hline\n" +
            "\\end{tabular}\n" +
            "\\caption{Test Caption}\n" +
            "\\label{tab:testTable}\n" +
            "\\end{center}\n" +
            "\\end{table}";
    }

    private String reportTable() {
        return "\\begin{table}[]\n" +
            "\\begin{center}\n" +
            "\\begin{tabular}{|l|l|l|l|}\n" +
            "\\hline\n" +
            "          & \\textbf{A} & \\textbf{B} & \\textbf{C}  \\\\ \\hline\n" +
            "  rankSum & 9242.5     & 25050.0    & 10857.5      \\\\ \\hline\n" +
            "    count & 100        & 100        & 100          \\\\ \\hline\n" +
            "\\end{tabular}\n" +
            "\n" +
            "\\begin{tabular}{|l|l|}\n" +
            "\\hline\n" +
            "  areDifferentDistributions & true \\\\ \\hline\n" +
            "                      count & 300 \\\\ \\hline\n" +
            "                          H & 201.0685880398671 \\\\ \\hline\n" +
            "           degreesOfFreedom & 2 \\\\ \\hline\n" +
            "                     pValue & 1.0808837047626696E-8 \\\\ \\hline\n" +
            "                 pThreshold & 0.05 \\\\ \\hline\n" +
            "\\end{tabular}\n" +
            "\\caption{FreemarkerTestReport}\n" +
            "\\label{tab:testTable}\n" +
            "\\end{center}\n" +
            "\\end{table}";
    }
}
