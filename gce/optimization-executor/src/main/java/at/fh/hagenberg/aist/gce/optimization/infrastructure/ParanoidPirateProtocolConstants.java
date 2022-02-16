/*
 * Copyright (c) 2022 the original author or authors.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

package at.fh.hagenberg.aist.gce.optimization.infrastructure;

/**
 * Paranoid Pirate Protocol constants
 *
 * @author Daniel Dorfmeister on 2019-09-11
 */
public class ParanoidPirateProtocolConstants {

    // original protocol constants
    public final static String PPP_READY = "\001"; // Signals worker is ready
    public final static String PPP_HEARTBEAT = "\002"; // Signals worker heartbeat

    // message requests for eval
    public static final String PPP_INIT = "\003"; // Initialization request for code;
    public static final String PPP_RUN = "\004"; // Request to run code;
    public static final String PPP_CONF = "\005"; // Request to set a configuration
    public static final String PPP_INVESTIGATE = "\006"; // Request to reboot a worker (from broker to command module)
    public static final String PPP_BOOT = "\007"; // Request to boot a new worker (from broker to command module; only used when workers shutdown after a single request)

    // message responses for PPP_RUN
    public static final String PPP_RUN_SUCCESS = "\008"; // run succeeded. What follows is the run data
    public static final String PPP_RUN_FAILURE = "\009"; // run failed. What follows is the execption message.
    public static final String PPP_RUN_FATAL = "\010"; // fatal exception, happening for example if the infrastructure is not running

    public static final String PPP_INVESTIGATE_ERROR = "\011"; // failed to investigate the exception
    public static final String PPP_INVESTIGATE_SUCCESS = "\012"; // succeded investigation

    public static final String PPP_INIT_ACCEPTED = "\013"; // initialization is finished, worker is ready to be accepted into the ready queue
}

