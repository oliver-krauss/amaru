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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Printer that is based on freemarker. It will persist according to the given template files
 *
 * @author Oliver Krauss on 23.10.2019
 */
public class FreemarkerPrinter {

    protected Configuration configuration;

    /**
     * Map of all templates loaded into this printer. Per convention the name of the template
     * is also the variable-name of what the object to be transformed will be loaded into
     */
    private Map<String, String> templateMap = new HashMap<>();

    /**
     * Additional values to be set manually that the template may need.
     * This also allows overriding any values that the templatePreprocessor may generate
     */
    private Map<String, Object> additionalTemplateValues = new HashMap<>();

    /**
     * If this printer supports more than one output this list provides the supported ones
     */
    private List<String> supportedFormats = new ArrayList<>();

    /**
     * Selected format out of the supported ones
     */
    private String format = "";

    /**
     * Writer for writing to "something"
     */
    private Writer writer;

    /**
     * preprocessor for Templates
     */
    private TemplatePreprocessor templatePreprocessor = null;

    public FreemarkerPrinter(String templateDirectory) {
        // default config of Freemarker
        configuration = new Configuration(Configuration.VERSION_2_3_29);
        configuration.setLogTemplateExceptions(false);
        configuration.setWrapUncheckedExceptions(true);
        configuration.setFallbackOnNullLoopVariable(false);

        // configure loading of testFiles
        if (templateDirectory == null) {
            configuration.setClassForTemplateLoading(this.getClass(), "/templates/");
        } else {
            try {
                configuration.setDirectoryForTemplateLoading(new File(templateDirectory));
            } catch (IOException e) {
                throw new IllegalArgumentException("Template directory " + templateDirectory + " does not exist!");
            }
        }
    }

    public void transform(String template, Object o) {
        if (!templateMap.containsKey(template)) {
            throw new RuntimeException("template unknown. Cannot transform");
        }

        Map<String, Object> data = new HashMap<>();
        data.put(template, o);
        applyTemplate(templateMap.get(template), data);
    }

    /**
     * Helper function to actually apply the template
     *
     * @param templateName to be applied
     * @param data         to be printed into the template
     */
    protected void applyTemplate(String templateName, Map<String, Object> data) {
        if (writer == null) {
            // without anywhere to push we don't want to do anything
            return;
        }
        try {
            if (templatePreprocessor != null) {
                data.putAll(templatePreprocessor.process((data.containsKey("dataset") ? (Dataset) data.get("dataset") : null),
                    data.containsKey("report") ? (Report) data.get("report") : null));
            }
            data.putAll(additionalTemplateValues);
            Template template = configuration.getTemplate((format.length() > 0 ? format + "/" : "") + templateName + ".ftlh");
            template.process(data, writer);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (TemplateException e) {
            e.printStackTrace();
        }
    }

    public Map<String, String> getTemplateMap() {
        return templateMap;
    }

    public void addTemplate(String name, String templateFile) {
        templateMap.put(name, templateFile);
    }

    public Writer getWriter() {
        return writer;
    }

    public void setWriter(Writer writer) {
        this.writer = writer;
    }

    public Map<String, Object> getAdditionalTemplateValues() {
        return additionalTemplateValues;
    }

    /**
     * Adds a value that has to be used by the template (does override if exists)
     *
     * @param key   to be used
     * @param value to be applied to the template
     */
    public void addAdditionalTemplateValue(String key, Object value) {
        additionalTemplateValues.put(key, value);
    }

    /**
     * Removes a value from the template
     *
     * @param key to be removed
     */
    public void removeAdditionalTemplateValue(String key) {
        additionalTemplateValues.remove(key);
    }

    public TemplatePreprocessor getTemplatePreprocessor() {
        return templatePreprocessor;
    }

    public void setTemplatePreprocessor(TemplatePreprocessor templatePreprocessor) {
        this.templatePreprocessor = templatePreprocessor;
    }

    public List<String> getSupportedFormats() {
        return supportedFormats;
    }

    public void setSupportedFormats(List<String> supportedFormats) {
        this.supportedFormats = supportedFormats;
    }

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        if (format == null) {
            format = "";
        }
        if (!supportedFormats.contains(format)) {
            throw new IllegalArgumentException("Format " + format + " not supported.");
        }
        this.format = format;
    }
}
