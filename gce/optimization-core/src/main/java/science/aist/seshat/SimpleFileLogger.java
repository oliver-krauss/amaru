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

import java.io.*;
import java.util.Date;
import java.util.UUID;

/**
 * @author Oliver Krauss on 04.01.2020
 */

public class SimpleFileLogger implements Logger {

    protected File logfile;

    protected FileWriter fr;

    private static SimpleFileLogger logger;

    public static SimpleFileLogger getLogger() {
        return logger;
    }

    public SimpleFileLogger() {
        init(LogConfiguration.LOG_LOCATION + UUID.randomUUID() + ".log");
    }

    public SimpleFileLogger(String name) {
        init(LogConfiguration.LOG_LOCATION + name + ".log");
    }

    public SimpleFileLogger(Class<?> clazz) {
        init(LogConfiguration.LOG_LOCATION + clazz.getName() + ".log");
    }

    protected void init(String name) {
        logger = this;
        logfile = new File(name);
        try {
            fr = new FileWriter(logfile, true);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    protected void write(String s) {
        try {
            fr.write(s + System.lineSeparator());
            fr.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void log(LogLevel logLevel, String s) {
        write(new Date().toString() + " " + logLevel + ": " + s);
    }

    @Override
    public void log(LogLevel logLevel, String s, Exception e) {
        write(new Date().toString() + " " + logLevel + ": " + s);
        StringWriter errors = new StringWriter();
        e.printStackTrace(new PrintWriter(errors));
        write(errors.toString());
    }

    @Override
    public void log(LogLevel logLevel, Exception e) {
        write(new Date().toString() + " " + logLevel + ": Error:");
        StringWriter errors = new StringWriter();
        e.printStackTrace(new PrintWriter(errors));
        write(errors.toString());
    }

    @Override
    public void log(LogLevel logLevel, String s, Object... objects) {
        write(new Date().toString() + " " + logLevel + ": Error:");
        for (Object object : objects) {
            write(object.toString());
        }
    }
}
