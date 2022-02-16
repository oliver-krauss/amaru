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

import at.fh.hagenberg.aist.gce.science.statistics.data.template.Transformer;

import java.io.*;

/**
 * Injects what the transformer spits out at a specific position in the required file
 *
 * @author Oliver Krauss on 24.10.2019
 */
public class InjectorProcessor implements FileProcessor {

    /**
     * Temp file for injection
     */
    private File tmp;

    /**
     * Match where the transformer-values will be inserted after (the begin line will remain in the target file!)
     */
    private String begin;

    /**
     * Match where the transformer-values will be inserted before (the end line will remain in the target file!)
     */
    private String end;

    @Override
    public void preProcessing(File file, Transformer transformer) {
        try {
            tmp = new File(file.getAbsolutePath() + "Tmp");
            FileWriter writer = new FileWriter(tmp);

            // add beginning of original file
            if (begin != null && !begin.isEmpty()) {
                try {
                    FileReader fileReader = new FileReader(file);
                    BufferedReader reader = new BufferedReader(fileReader);
                    String val;
                    while ((val = reader.readLine()) != null) {
                        writer.append(val).append(System.lineSeparator());
                        if (val.contains(begin)) {
                            break;
                        }
                    }
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
            }

            transformer.setWriter(writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void postProcessing(File file, Transformer transformer) {
        try {
            FileWriter writer = (FileWriter) transformer.getWriter();

            // add end of original file
            try {
                FileReader fileReader = new FileReader(file);
                BufferedReader reader = new BufferedReader(fileReader);
                String val;
                boolean append = false;
                while ((val = reader.readLine()) != null) {
                    if (!append && val.contains(end)) {
                        writer.append(System.lineSeparator());
                        append = true;
                    }
                    if (append) {
                        writer.append(val).append(System.lineSeparator());
                    }
                }
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }

            writer.close();
            file.delete();
            tmp.renameTo(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String getBegin() {
        return begin;
    }

    public void setBegin(String begin) {
        this.begin = begin;
    }

    public String getEnd() {
        return end;
    }

    public void setEnd(String end) {
        this.end = end;
    }
}
