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
public class LatexInjectorProcessor implements FileProcessor {

    /**
     * Temp file for injection
     */
    private File tmp;

    /**
     * Match where the transformer-values will be inserted after (the begin line will remain in the target file!)
     */
    private String begin = "\\begin{table}";

    /**
     * Match where the transformer-values will be inserted before (the end line will remain in the target file!)
     */
    private String end = "\\end{table}";

    private String label;

    private int tablePos = 0;

    @Override
    public void preProcessing(File file, Transformer transformer) {
        try {
            tmp = new File(file.getAbsolutePath() + "Tmp");
            FileWriter writer = new FileWriter(tmp);

            if (label != null) {
                // find table
                FileReader fileReader = new FileReader(file);
                BufferedReader reader = new BufferedReader(fileReader);
                String val;
                int tablePos = -1;
                int i = 0;
                while ((val = reader.readLine()) != null && !val.contains("\\label{" + label + "}")) {
                    if (val.contains(begin)) {
                        tablePos = i;
                    }
                    i++;
                }

                // load until table
                i = 0;
                reader.close();
                fileReader.close();
                fileReader = new FileReader(file);
                reader = new BufferedReader(fileReader);
                while ((val = reader.readLine()) != null && i < tablePos) {
                    i++;
                    writer.append(val).append(System.lineSeparator());
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
                int i = 0;
                while ((val = reader.readLine()) != null) {
                    i++;
                    if (!append && i >= tablePos && val.contains(end)) {
                        val = reader.readLine();
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

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }
}
