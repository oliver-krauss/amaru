/*
 * Copyright (c) 2022 the original author or authors.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

package science.aist.seshat;

import at.fh.hagenberg.aist.gce.optimization.language.util.EngineConfig;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

/**
 * @author Oliver Krauss on 06.01.2020
 */

public class XESLogger extends SimpleFileLogger {

    /**
     * Formatter for timestamp
     */
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSxxx");

    public XESLogger(String name) {
        String description = "First XES test";

        init(LogConfiguration.LOG_LOCATION + name + ".xes");
        write("<?xml version=\"1.0\" encoding=\"UTF-8\" ?>");
        write("<log xes.version=\"1.0\" xmlns=\"http://code.deckfour.org/xes\" xes.creator=\"GCE\">");
        write("  <extension name=\"Concept\" prefix=\"concept\" uri=\"http://code.deckfour.org/xes/concept.xesext\"/>");
        write("  <extension name=\"Time\" prefix=\"time\" uri=\"http://code.deckfour.org/xes/time.xesext\"/>");
        write("  <global scope=\"trace\">");
        write("    <string key=\"concept:name\" value=\"name\"/>");
        write("  </global>");
        write("  <global scope=\"event\">");
        write("    <string key=\"concept:name\" value=\"name\"/>");
        write("    <date key=\"time:timestamp\" value=\"2011-04-13T14:02:31.199+02:00\"/>");
        write("    <string key=\"Activity\" value=\"string\"/>");
        write("  </global>");
        write("  <classifier name=\"Activity\" keys=\"Activity\"/>");
        write("  <classifier name=\"activity classifier\" keys=\"Activity\"/>");
        write("  <string key=\"creator\" value=\"GCE\"/>");
        write("  <trace>");
        write("    <string key=\"concept:name\" value=\"" + 0 + "\"/>");
        write("    <string key=\"creator\" value=\"GCE\"/>");

    }

    public void close() {
        write("  </trace>");
        write("</log>");
    }

    @Override
    public void log(LogLevel logLevel, String s) {
        log(logLevel, s, (Object) null);
    }

    @Override
    public void log(LogLevel logLevel, String s, Object... objects) {
        write("      <event>");
//        if (objects != null && objects.length > 0) {
//            write("        <Data>");
//            for (Object object : objects) {
//                write("          <Attribute name=\"Parameter\">" + (object == null ? "null" : object.toString()) + "</Attribute>");
//            }
//            write("        </Data>");
//        }
        String activity = s.substring(0, s.lastIndexOf("."));
        activity = activity.substring(activity.lastIndexOf(".") + 1);
        write("        <string key=\"concept:name\" value=\"" + s + "\"/>");
        write("        <date key=\"time:timestamp\" value=\"" + ZonedDateTime.now().format(formatter) + "\"/>");
        write("        <string key=\"Activity\" value=\"" + activity + "\"/>");
        write("      </event>");
    }
}
