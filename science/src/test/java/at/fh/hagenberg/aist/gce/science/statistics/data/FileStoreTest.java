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
import at.fh.hagenberg.aist.gce.science.statistics.data.store.FileStore;
import at.fh.hagenberg.aist.gce.science.statistics.data.store.InjectorProcessor;
import at.fh.hagenberg.aist.gce.science.statistics.data.store.LatexInjectorProcessor;
import at.fh.hagenberg.aist.gce.science.statistics.data.template.DatasetReportTransformer;
import at.fh.hagenberg.aist.gce.science.statistics.data.template.FreemarkerPrinter;
import at.fh.hagenberg.aist.gce.science.statistics.data.template.LatexPreprocessor;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.File;
import java.nio.file.Files;

/**
 * Test for reading and writing files
 *
 * @author Oliver Krauss on 24.10.2019
 */
public class FileStoreTest extends DatasetDependentTest {

    @Test
    public void testLoad() {
        // given
        FileStore store = new FileStore();
        store.setFile("./src/test/resources/testFiles/fsTestStore.csv");

        // when
        Dataset ds = store.load();

        // then
        Assert.assertEquals(ds, getDataset());
    }

    @Test
    public void testSave() {
        // given
        FileStore store = new FileStore();
        store.setFile("./src/test/resources/testFiles/fsTestStore.csv");

        // when
        store.save(getDataset());

        // then
        Assert.assertEquals(store.load(), getDataset());
    }

    @Test
    public void testSaveReport() throws Exception {
        // given
        FileStore store = new FileStore();
        store.setFile("./src/test/resources/testFiles/fsTestStoreReport.txt");

        // when
        store.save(getReport());

        // then
        Assert.assertTrue(store.getFile().exists());
        Assert.assertEquals(new String(Files.readAllBytes(store.getFile().toPath())), getReportString());
    }

    @Test
    public void testSaveBoth() throws Exception {
        // given
        FileStore store = new FileStore();
        store.setFile("./src/test/resources/testFiles/fsFullReportTest.txt");

        // when
        store.save(getDataset(), getReport());

        // then
        Assert.assertTrue(store.getFile().exists());
        Assert.assertEquals(new String(Files.readAllBytes(store.getFile().toPath())), getCSV() + getReportString());;
    }

    @Test
    public void testSaveInject() throws Exception {
        // given
        FileStore store = new FileStore();
        InjectorProcessor processor = new InjectorProcessor();
        processor.setBegin("before");
        processor.setEnd("after");
        store.setProcessor(processor);
        store.setFile("./src/test/resources/testFiles/fsTestStoreReportInBetween.txt");

        // when
        store.save(getReport());

        // then
        Assert.assertTrue(store.getFile().exists());
        Assert.assertEquals(new String(Files.readAllBytes(store.getFile().toPath())),
            new String(Files.readAllBytes(new File("./src/test/resources/testFiles/fsTestStoreReportInBetweenSuccess.txt").toPath())));
    }


    @Test
    public void testSaveInjectLatex() throws Exception {
        // given
        DatasetReportTransformer transformer = new DatasetReportTransformer(null);
        transformer.setTemplatePreprocessor(new LatexPreprocessor());
        transformer.setReportTemplate("latexReport");
        FileStore store = new FileStore();
        LatexInjectorProcessor processor = new LatexInjectorProcessor();
        processor.setLabel("tab:testLabel");
        store.setProcessor(processor);
        store.setFile("./src/test/resources/testFiles/latexTable.tex");
        store.setTransformer(transformer);

        Report r = getReport();
        r.addReport("label", "String", "tab:testTable");

        // when
        store.save(r);

        // then
        Assert.assertTrue(store.getFile().exists());
        Assert.assertEquals(new String(Files.readAllBytes(store.getFile().toPath())),
            new String(Files.readAllBytes(new File("./src/test/resources/testFiles/latexTableSuccess.tex").toPath())));
    }
}
