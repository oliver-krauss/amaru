/*
 * Copyright (c) 2022 the original author or authors.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

package at.fh.hagenberg.aist.gce.optimization.runtime;

import org.neo4j.ogm.annotation.Id;
import org.neo4j.ogm.annotation.NodeEntity;

/**
 * Information about available OpenCL Devices
 * Mainly relevant for for LIFT
 * @author Oliver Krauss on 29.12.2019
 */
@NodeEntity
public class OpenCLInformation {

    /**
     * Database ID
     */
    @Id
    protected Long id;

    /**
     * Name of the device
     */
    protected String deviceName;

    /**
     * Name of the vendor that produced the devices
     */
    protected String vendorName;

    /**
     * Type of device. Usually GPU or CPU
     */
    protected String type;

    /**
     * Clock frequency. If CPU equals cpuOperatingFrequency.
     */
    protected String operatingFrequency;

    /**
     * Maximum available global memory
     */
    protected String globalMemorySize;

    /**
     * Size of global cache (there seems to be no local cache)s
     */
    protected String globalCacheSize;

    /**
     * Size of local memory
     */
    protected String localMemorySize;

    @Override
    public String toString() {
        return "OpenCLInformation{" +
            "id=" + id +
            ", deviceName='" + deviceName + '\'' +
            ", vendorName='" + vendorName + '\'' +
            ", type='" + type + '\'' +
            ", operatingFrequency='" + operatingFrequency + '\'' +
            ", globalMemorySize='" + globalMemorySize + '\'' +
            ", globalCacheSize='" + globalCacheSize + '\'' +
            ", localMemorySize='" + localMemorySize + '\'' +
            '}';
    }
}
