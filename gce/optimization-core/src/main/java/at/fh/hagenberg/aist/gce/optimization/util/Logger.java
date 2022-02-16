/*
 * Copyright (c) 2022 the original author or authors.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

package at.fh.hagenberg.aist.gce.optimization.util;

import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.NodeUtil;

/**
 * @author Oliver Krauss on 07.11.2018
 */

public class Logger {

    /**
     * Log level where we print to console
     */
    private static final LogLevel LOG_LEVEL = LogLevel.INFO;

    /**
     * Log level for for logging
     */
    public enum LogLevel {
        OFF(7), FATAL(6), ERROR(5), WARN(4), INFO(3), DEBUG(2), TRACE(1), ALL(0);

        private final int level;

        LogLevel(int level) {
            this.level = level;
        }

        public int getLevel() {
            return level;
        }
    }

    /**
     * Helper function for logging to console
     *
     * @param level of log
     * @param s     message to be logged
     */
    public static void log(LogLevel level, String s) {
        if (level.getLevel() >= LOG_LEVEL.getLevel()) {
            System.out.println(s);
        }
    }

    /**
     * Helper function for logging to console
     *
     * @param level of log
     * @param s     message to be logged
     * @param e     error to be logged
     */
    public static void log(LogLevel level, String s, Exception e) {
        if (level.getLevel() >= LOG_LEVEL.getLevel()) {
            System.out.println(s);
            e.printStackTrace();
        }
    }

    /**
     * Helper function for logging to console
     *
     * @param level of log
     * @param e     error to be logged
     */
    public static void log(LogLevel level, Exception e) {
        if (level.getLevel() >= LOG_LEVEL.getLevel()) {
            e.printStackTrace();
        }
    }

    /**
     * Helper function for
     *
     * @param level of log
     * @param n     tree to be logged
     */
    public static void log(LogLevel level, Node n) {
        if (level.getLevel() >= LOG_LEVEL.getLevel()) {
            NodeUtil.printTree(System.out, n);
        }
    }

}
