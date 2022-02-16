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

import java.text.DecimalFormat;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Nano profiler that supports checking performance of code
 * @author Oliver Krauss on 24.03.2021
 */
public class NanoProfiler {

    /**
     * When silenced the nano profiler won't print any info.
     * Mostly for disabling it during testing / production.
     */
    public static boolean SILENCE = false;

    /**
     * Name if you utilize multiple profilers at the same time
     */
    private String name = "";

    /**
     * if > -1 the profiler will report at that interval of calls to profile()
     */
    private int cycle = -1;

    private int count = 0;

    public NanoProfiler() {
    }

    public NanoProfiler(String name, int cycle) {
        this.name = name;
        this.cycle = cycle;
    }

    private HashMap<String, Long> profile = new HashMap<>();

    public long start() {
        return System.currentTimeMillis();
    }

    public long profile(String position, long last) {
        long l = System.currentTimeMillis();
        profile.put(position, (l - last) + profile.getOrDefault(position, 0L));
        if (cycle > -1) {
            count++;
            if (count > cycle) {
                count = 0;
                report();
            }
        }
        return l;
    }

    public void report() {
        if (SILENCE) {
            return;
        }
        System.out.println(" Heap " + DecimalFormat.getNumberInstance().format(Runtime.getRuntime().freeMemory()));
        System.out.println("Performance Report " + name);
        System.out.println(profile.entrySet().stream().sorted((a, b) -> Long.compare(b.getValue(), a.getValue())).map(x -> "  " + x.getKey() + " " + DecimalFormat.getNumberInstance().format(x.getValue())).collect(Collectors.joining(System.lineSeparator())));
    };

    public void reset() {
        profile.clear();
    }

}
