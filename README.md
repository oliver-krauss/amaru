# Amaru

Amaru is a framework for [Genetic Improvement](http://geneticimprovementofsoftware.com/) using [Truffle](https://www.graalvm.org/22.0/graalvm-as-a-platform/language-implementation-framework/) Languages, and operating in the [Graal VM](https://www.graalvm.org/).
There is also a [connector](https://raw.githubusercontent.com/oliver-krauss/heuristiclabconnector) to [HeuristicLab](https://dev.heuristiclab.com) available.

Disclaimer: This is the version of Amaru, as was used in my PHD thesis for replication purposes. Please note that in the future there will be significant (breaking) changes, including all package names which will be migrated to dev.amaru.

Repository is available here: https://github.com/oliver-krauss/amaru

## Setup:

**The following prerequisites apply:**

> GraalVM 21.1.0 

Get it here: https://github.com/graalvm/graalvm-ce-builds/releases You can try newer versions, but this is the latest one that this project was tested on.

> maven

If you work with java you know maven - https://maven.apache.org/download.cgi

> docker 

From here: https://docs.docker.com/get-docker/ + https://docs.docker.com/compose/install/

Optional for the database. Otherwise you need to manually install [Neo4J](https://neo4j.com/docs/operations-manual/current/installation/)

**Environment Variables:**

You will need to provide several environment variables. 
These are primarily where you want to print log output,
or where you have source code stored:

> ROOT_LOCATION 

the location where you checked out Amaru e.g. /home/user/gitRepos/Amaru -> ROOT_LOCATION =  /home/user/gitRepos

GRAAL_LOC 

Location of the Graal Java binary. If "java" points to Graal, you don't need to set this.

> DIST_DIR 

Where your Truffle language distribution jar files are stored. For the example language MiniC this will be ROOT_LOCATION/Amaru/dists/

## Getting Started

Start the Neo4J Database!
(either manually or in ANY of the docker/experiment_... folders -> docker-compose up -d)

To build:
> mvn clean install

To build faster:
> mvn clean install -DskipTests

To play with MiniC best take a look at the MinicSourcefileTest test class, and corresponding code in the resources folder.
> at.fh.hagenberg.aist.gce.minic.test.MinicSourcefileTest

To run GI Experiments take a look at MinicExperiments. 
> at.fh.hagenberg.aist.gce.benchmark.MinicExperiments

The Experiments run on a distributed system. If you want to run them you also need to start:
> at.fh.hagenberg.aist.gce.optimization.infrastructure.MessageBroker

> at.fh.hagenberg.aist.gce.optimization.infrastructure.MessageCommandModule

To do performance profiling (only works after you did a GI experiment): 
> at.fh.hagenberg.aist.gce.benchmark.MinicPerformanceEvaluationPipeline

To run pattern mining experiments (only works after you did a GI experiment)

Mine bugs:
> at.fh.hagenberg.aist.gce.benchmark.MinicFaultDetection

Mine performance patterns (first do performance profiling!):
> at.fh.hagenberg.aist.gce.benchmark.MinicPerformanceDetection

## Contributing

If you want to contribute feel free to [contact me](https://github.com/oliver-krauss). I will add you as contributor.

Feel free to open issues directly in the repository, both for questions, or for feature requests / bug reports.

Please note that this is (currently) a one person project, on which I work in my free time. I am delighted in your interest, but I can't promise anything will happen fast.

## License

Copyright (c) 2022 the original author or authors. DO NOT ALTER OR REMOVE COPYRIGHT NOTICES.

This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/.

## Research

If you are going to use this project as part of a research paper, we would ask you to reference this project by citing
it.

If you arrived here, you already got the Zenodo DOI.

### List of publications Amaru was used in:

- [Toward Knowlege Guided Genetic Improvement](https://dl.acm.org/doi/10.1145/3387940.3392172)
- [Integrating HeuristicLab with Compilers and Interpreters for Non-Functional Code Optimization](https://dl.acm.org/doi/10.1145/3377929.3398103)
- [Mining Patterns form Genetic Improvement Experiments](https://ieeexplore.ieee.org/document/8823638)
- [Dynamic Fitness FUnctions for Genetic Improvement in Compilers and Interpeters](https://dl.acm.org/doi/10.1145/3205651.3208308)
- [Towards a Framework for Stochastic Performance Optimizations in Compilers and Interpreters - An Architecture Overview](https://dl.acm.org/doi/10.1145/3237009.3237024)
- Upcoming: PhD Thesis - will be published here after the defense.

(Additional work on GI not directly involving Amaru: http://gpbib.cs.ucl.ac.uk/gp-html/OliverKrauss.html)
