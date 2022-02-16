/*
 * Copyright (c) 2022 the original author or authors.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

package at.fh.hagenberg.aist.gce.science.statistics.data.store;

import at.fh.hagenberg.aist.gce.science.statistics.Report;
import at.fh.hagenberg.aist.gce.science.statistics.data.Dataset;
import at.fh.hagenberg.aist.gce.science.statistics.data.template.DatasetReportTransformer;
import at.fh.hagenberg.aist.gce.science.statistics.data.template.FreemarkerPrinter;
import at.fh.hagenberg.aist.gce.science.statistics.data.template.Transformer;
import at.fh.hagenberg.aist.gce.science.statistics.data.load.CSVLoader;
import at.fh.hagenberg.aist.gce.science.statistics.data.load.Loader;

import java.io.File;
import java.io.FileReader;

/**
 * Enables read/write from files.
 * Default setttings work with CSV
 * <p>
 * Warning: Any save operation will OVERRIDE the selected file
 *
 * @author Oliver Krauss on 23.10.2019
 */
public class FileStore implements Store {

    /**
     * Transformer that will be applied to the datasets and reports
     */
    private Transformer transformer = new DatasetReportTransformer(null);

    /**
     * Loader responsible for transforming files to Datasets
     */
    private Loader loader = new CSVLoader();

    /**
     * File that is managed (applies to both load and save)
     */
    private File file;

    /**
     * File Processor that may do additional things to the file during save operations
     */
    private FileProcessor processor = new StoreProcessor();

    @Override
    public Dataset load() {
        if (file == null) {
            return null;
        }

        try {

            FileReader reader = new FileReader(file);
            loader.setReader(reader);
            Dataset load = loader.load();
            reader.close();

            return load;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public void save(Dataset dataset) {
        if (file == null) {
            return;
        }
        processor.preProcessing(file, transformer);
        transformer.transform(dataset);
        processor.postProcessing(file, transformer);
    }

    @Override
    public void save(Report report) {
        if (file == null) {
            return;
        }

        processor.preProcessing(file, transformer);
        transformer.transform(report);
        processor.postProcessing(file, transformer);
    }

    @Override
    public void save(Dataset dataset, Report report) {
        if (file == null) {
            return;
        }
        processor.preProcessing(file, transformer);
        transformer.transform(dataset, report);
        processor.postProcessing(file, transformer);
    }

    public Transformer getTransformer() {
        return transformer;
    }

    public void setTransformer(Transformer transformer) {
        this.transformer = transformer;
    }

    public File getFile() {
        return file;
    }

    public void setFile(File file) {
        this.file = file;
    }

    public void setFile(String file) {
        this.file = new File(file);
    }

    public Loader getLoader() {
        return loader;
    }

    public void setLoader(Loader loader) {
        this.loader = loader;
    }

    public FileProcessor getProcessor() {
        return processor;
    }

    public void setProcessor(FileProcessor processor) {
        this.processor = processor;
    }
}
