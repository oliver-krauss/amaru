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

import science.aist.neo4j.annotations.Converter;
import science.aist.neo4j.reflective.ReflectiveNeo4JNodeRepositoryImpl;
import at.fh.hagenberg.aist.neo4j.reflective.converter.JVMTypeConverer;
import at.fh.hagenberg.aist.neo4j.reflective.converter.OSTypeConverter;
import org.neo4j.ogm.annotation.Id;
import org.neo4j.ogm.annotation.NodeEntity;
import org.neo4j.ogm.annotation.Relationship;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Information on the system that processes run on.
 * Mainly concerns:
 * - {RuntimeProfile} - to determine where the evaluation happened, and possible differences in processor architectures
 * - {@link at.fh.hagenberg.aist.gce.optimization.util.TruffleClassInformation} - to determine where the language was processed
 * <p>
 * Note: we are not collecting the RAM because this requires root level access and "lshw" is generally not installed in docker
 * we are also not collecting the hostname as this also not possible in docker (and not relevant info)
 *
 * @author Oliver Krauss on 29.12.2019
 */
@NodeEntity
public class SystemInformation {

    private static SystemInformation currentSystem = null;

    /**
     * Generates the SystemInformation of the current JVM.
     * @return SystemInformation
     */
    public static SystemInformation getCurrentSystem() {
        if (currentSystem != null) {
            return currentSystem;
        }
        return currentSystem = new SystemInformation();
    }

    /**
     * Database ID
     */
    @Id
    protected Long id;

    /**
     * CPU Architecture can be x86, x64 or x86_64 (cpu that supports both 32 and 64 bit)
     */
    private String cpuArchitecture;

    /**
     * Total number of available cores, including multiple threads per physical cpu
     */
    private int cpuCores = Runtime.getRuntime().availableProcessors();

    /**
     * Total number of real cores per socket
     */
    private int cpuPhysicalCores;

    /**
     * Total number of sockets (usually 1)
     */
    private int cpuSockets;

    /**
     * Maximum Operating Frequency in MHz
     */
    private String cpuOperatingFrequency;

    /**
     * Size of L1 Data cache
     */
    private String cpuCacheL1d;

    /**
     * Size of L1 Instruction cache
     */
    private String cpuCacheL1i;

    /**
     * Size of L2 cache
     */
    private String cpuCacheL2;

    /**
     * Size of L3 cache
     */
    private String cpuCacheL3;

    /**
     * Vendor name of CPU
     */
    private String cpuName;

    /**
     * RAM available to the JVM.
     * Note: We intentionally ignore the additional information with "sudo lshw -C memory" as this information is available ONLY to root users
     * It COULD give us the Size, Width (64bit usually), Clock, product and vendor ids
     */
    private long ramSize = Runtime.getRuntime().maxMemory();

    @Relationship(type = "OPENCL")
    List<OpenCLInformation> openclDevices = new ArrayList<>();

    /**
     * OS Name. Not necessarily "Ubuntu 18" but rather "Linux"
     */
    private String osName = System.getProperty("os.name");

    /**
     * Type of OS we are running on
     */
    @Converter(converter = OSTypeConverter.class)
    private OSType osType = osName.toLowerCase().contains("linux") ? OSType.Linux : (
        osName.toLowerCase().contains("unix") ? OSType.Unix : (
            osName.toLowerCase().contains("win") ? OSType.Windows : (
                osName.toLowerCase().contains("mac") ? OSType.Mac : OSType.Unknown
            )));

    /**
     * Type of JVM we are running on
     */
    @Converter(converter = JVMTypeConverer.class)
    private JVMType jvmType = System.getProperty("jvmci.Compiler") != null ? JVMType.Graal :
        (System.getProperty("java.vm.name").contains("OpenJDK") ? JVMType.OpenJDK :
            (System.getProperty("java.vm.name").contains("Java HotSpot(TM)") ? JVMType.OracleJDK : JVMType.Unknown));

    /**
     * Human readable name of JVM (ex Java HotSpot(TM) 64-Bit Server VM)
     */
    private String jvmName = System.getProperty("java.vm.name");

    /**
     * "Big" java version (i.e. Java 8 -> 1.8; Java 11 -> 1.11)
     */
    private String javaVersion = System.getProperty("java.specification.version");

    /**
     * Exact version of Java we are running
     */
    private String javaExactVersion = System.getProperty("java.version");

    /**
     * Architecture (32 or 64 bit) that the JVM is running.
     * SHOULD be the same as the CPU Architectur!
     */
    private String jvmArchitecture = System.getProperty("sun.arch.data.model");


    public boolean isGraal() {
        return this.jvmType.equals(JVMType.Graal);
    }

    public SystemInformation() {
        // get cpu info
        if (this.osType.equals(OSType.Linux) || this.osType.equals(OSType.Unix)) {
            try {
                Process process = Runtime.getRuntime().exec("lscpu");
                BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.contains("Architecture")) {
                        cpuArchitecture = line.substring(line.lastIndexOf(":") + 1).trim();
                    } else if (line.contains("Core(s) per socket")) {
                        cpuPhysicalCores = Integer.valueOf(line.substring(line.lastIndexOf(":") + 1).trim());
                    } else if (line.contains("Socket(s)")) {
                        cpuSockets = Integer.valueOf(line.substring(line.lastIndexOf(":") + 1).trim());
                    } else if (line.contains("CPU max MHz")) {
                        cpuOperatingFrequency = line.substring(line.lastIndexOf(":") + 1).trim();
                    } else if (line.contains("Model name")) {
                        cpuName = line.substring(line.lastIndexOf(":") + 1).trim();
                    } else if (line.contains("L1d cache")) {
                        cpuCacheL1d = line.substring(line.lastIndexOf(":") + 1).trim();
                    } else if (line.contains("L1i cache")) {
                        cpuCacheL1i = line.substring(line.lastIndexOf(":") + 1).trim();
                    } else if (line.contains("L2 cache")) {
                        cpuCacheL2 = line.substring(line.lastIndexOf(":") + 1).trim();
                    } else if (line.contains("L3 cache")) {
                        cpuCacheL3 = line.substring(line.lastIndexOf(":") + 1).trim();
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        // get opencl info
        if (this.osType.equals(OSType.Linux) || this.osType.equals(OSType.Unix)) {
            try {
                Process process = Runtime.getRuntime().exec("clinfo");
                BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                String line;
                OpenCLInformation info = null;
                while ((line = reader.readLine()) != null) {
                    if (line.contains("Device Name")) {
                        // signifies the start of a new platform (some infos are repeated, such as the vendor, etc.
                        info = new OpenCLInformation();
                        this.openclDevices.add(info);
                        info.deviceName = line.substring(line.lastIndexOf("Device Name") + 12).trim();
                    } else if (line.contains("Device Vendor  ")) {
                        info.vendorName = line.substring(line.lastIndexOf("Device Vendor") + 14).trim();
                    } else if (line.contains("Device Type")) {
                        info.type = line.substring(line.lastIndexOf("Device Type") + 12).trim();
                    } else if (line.contains("Max clock frequency")) {
                        info.operatingFrequency = line.substring(line.lastIndexOf("Max clock frequency") + 20).trim();
                    } else if (line.contains("Global memory size")) {
                        info.globalMemorySize = line.substring(line.lastIndexOf("Global memory size") + 18).trim();
                    } else if (line.contains("Global Memory cache size")) {
                        info.globalCacheSize = line.substring(line.lastIndexOf("Global Memory cache size") + 24).trim();
                    } else if (line.contains("Local memory size")) {
                        info.localMemorySize = line.substring(line.lastIndexOf("Local memory size") + 18).trim();
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public String toString() {
        return "SystemInformation{" +
            "id=" + id +
            ", cpuArchitecture='" + cpuArchitecture + '\'' +
            ", cpuCores=" + cpuCores +
            ", cpuPhysicalCores=" + cpuPhysicalCores +
            ", cpuSockets=" + cpuSockets +
            ", cpuOperatingFrequency='" + cpuOperatingFrequency + '\'' +
            ", cpuCacheL1d='" + cpuCacheL1d + '\'' +
            ", cpuCacheL1i='" + cpuCacheL1i + '\'' +
            ", cpuCacheL2='" + cpuCacheL2 + '\'' +
            ", cpuCacheL3='" + cpuCacheL3 + '\'' +
            ", cpuName='" + cpuName + '\'' +
            ", ramSize=" + ramSize +
            ", openclDevices=" + openclDevices.stream().map(x -> x.toString()).collect(Collectors.joining(",")) +
            ", osName='" + osName + '\'' +
            ", osType=" + osType +
            ", jvmType=" + jvmType +
            ", jvmName='" + jvmName + '\'' +
            ", javaVersion='" + javaVersion + '\'' +
            ", javaExactVersion='" + javaExactVersion + '\'' +
            ", jvmArchitecture='" + jvmArchitecture + '\'' +
            '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SystemInformation)) return false;
        SystemInformation that = (SystemInformation) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCpuArchitecture() {
        return cpuArchitecture;
    }

    public int getCpuCores() {
        return cpuCores;
    }

    public int getCpuPhysicalCores() {
        return cpuPhysicalCores;
    }

    public int getCpuSockets() {
        return cpuSockets;
    }

    public String getCpuOperatingFrequency() {
        return cpuOperatingFrequency;
    }

    public String getCpuCacheL1d() {
        return cpuCacheL1d;
    }

    public String getCpuCacheL1i() {
        return cpuCacheL1i;
    }

    public String getCpuCacheL2() {
        return cpuCacheL2;
    }

    public String getCpuCacheL3() {
        return cpuCacheL3;
    }

    public String getCpuName() {
        return cpuName;
    }

    public long getRamSize() {
        return ramSize;
    }

    public List<OpenCLInformation> getOpenclDevices() {
        return openclDevices;
    }

    public String getOsName() {
        return osName;
    }

    public OSType getOsType() {
        return osType;
    }

    public JVMType getJvmType() {
        return jvmType;
    }

    public String getJvmName() {
        return jvmName;
    }

    public String getJavaVersion() {
        return javaVersion;
    }

    public String getJavaExactVersion() {
        return javaExactVersion;
    }

    public String getJvmArchitecture() {
        return jvmArchitecture;
    }
}
