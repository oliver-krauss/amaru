/*
 * Copyright (c) 2022 the original author or authors.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

package at.fh.hagenberg.aist.gce.optimization.util.strategy;

import zmq.socket.reqrep.Req;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Helper class for Strategies. Contains information if a given requirement can be fullfilled
 *
 * @author Oliver Krauss on 31.07.2020
 */
public class RequirementInformation {

    /**
     * Map of Requirement and the degrees of freedom that are available for it
     * 0 means that the requirement can not be met (this may be true for a subset of requirements)
     * 1+ means that in different canCreate branches the requirement can be met
     */
    private Map<Requirement, Integer> requirements = new HashMap<>();

    private Integer requirementsHash;

    public RequirementInformation(Collection<Requirement> requirements) {
        if (requirements != null) {
            requirements.forEach(x -> this.requirements.put(x, 0));
        }
    }

    private RequirementInformation() {
        // only used to copy fast
    }

    public void addDegreeOfFreedom(Requirement requirement) {
        addDegreeOfFreedom(requirement, 1);
    }

    public void addDegreeOfFreedom(Requirement requirement, Integer dof) {
        requirementsHash = null;
        // weirdedst bug EVER -> requirements does NOT WORK DETERMINISTICALLY
        requirements.entrySet().forEach((e) -> {
            if (e.getKey().equals(requirement)) {
                e.setValue(e.getValue() + dof);
            }
        });
    }

    public Integer getDegreesOfFreedom(Requirement requirement) {
        return requirements.entrySet().stream().filter((e) -> e.getKey().equals(requirement)).map(Map.Entry::getValue).findFirst().orElse(null);
    }


    public void addRequirement(Requirement requirement) {
        requirementsHash = null;
        requirements.put(requirement, 0);
    }

    /**
     * Allows adding requirement that has immediate DOF
     * @param requirement to be added
     * @param dof         to be added with
     */
    public void addRequirement(Requirement requirement, Integer dof) {
        requirementsHash = null;
        requirements.put(requirement, dof);
    }

    public Map<Requirement, Integer> getRequirements() {
        return requirements;
    }

    public Collection<Requirement> getRequirements(String name) {
        return requirements.keySet().stream().filter(x -> x.name.equals(name)).collect(Collectors.toList());
    }

    /**
     * Returns Anti-Patterns and Patterns
     * @return all pattern related requirements
     */
    public Collection<Requirement> getPatternRequirements() {
        return requirements.keySet().stream().filter(x -> x.name.equals(Requirement.REQ_ANTIPATTERN) || x.name.equals(Requirement.REQ_PATTERN)).collect(Collectors.toList());
    }

    /**
     * Returns all requirements that only have one degree of freedom of the given type
     *
     * @param name of requirement
     * @return all requirements that MUST be fulfilled NOW as there is only one degree of freedom left
     */
    public Collection<Requirement> getNecessaryRequirements(String name) {
        // collect all that may be needed
        List<Map.Entry<Requirement, Integer>> collect = requirements.entrySet().stream().filter(x -> x.getKey().name.equals(name)).collect(Collectors.toList());
        // DOF add -> so if we have 2 options with 2 DOF one MUST be fulfilled now, as the other DOF is taken up by the other
        return collect.stream().filter(x -> x.getValue() <= collect.size()).map(Map.Entry::getKey).collect(Collectors.toList());
    }

    /**
     * Returns a random requirements that only have one degree of freedom of the given type
     *
     * @param name of requirement
     * @return any requirement that MUST be fulfilled NOW as there is only one degree of freedom left or null if there is none
     */
    public Requirement getAnyNecessaryRequirement(String name) {
        return requirements.entrySet().stream().filter(x -> x.getKey().name.equals(name) && x.getValue().equals(1)).map(Map.Entry::getKey).findAny().orElse(null);
    }

    public RequirementInformation copy() {
        RequirementInformation requirementInformation = new RequirementInformation();
        requirementInformation.requirementsHash = this.requirementsHash;
        requirementInformation.requirements = new HashMap<>();
        this.requirements.forEach((k, v) -> requirementInformation.requirements.put(k.copy(), v));
        return requirementInformation;
    }

    public boolean fullfillsAll() {
        return requirements.values().stream().allMatch(x -> x > 0);
    }

    public void fullfill(Requirement requirement) {
        // weirdedst bug EVER -> requirements.remove(x) does NOT WORK DETERMINISTICALLY
        requirementsHash = null;
        requirements.keySet().removeIf(r -> r == requirement);
    }

    public int getDegreesOfFreedom() {
        return requirements.values().stream().mapToInt(Integer::intValue).sum();
    }

    @Override
    public String toString() {
        return "RequirementInformation{" +
                "requirements=" + requirements.entrySet().stream().map(x -> x.getKey().getName() + " " + x.getValue()).collect(Collectors.joining("; ")) +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof RequirementInformation)) return false;
        RequirementInformation that = (RequirementInformation) o;
        return hashCode() == that.hashCode();
    }

    @Override
    public int hashCode() {
        if (requirementsHash == null) {
            requirementsHash = requirements.hashCode();
        }
        return requirementsHash;
    }

}