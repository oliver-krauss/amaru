suite = {
  "mxversion" : "5.199.1",
  "name" : "minic",

  "imports":{
     "suites":[
        {
          "name":"truffle",
          "version":"8bcddb0da3f72e763bc30a9480aef189994c2cab",
          "subdir":"true",
          "urls":[
             {
                "url": "https://github.com/graalvm/graal",
                "kind" : "git"
             }
           ]
        },
	{
	  "name":"graal-js",
	  "enabled":"false",
	  "version":"f18c63f0318bc4fc695a7d73ec3b9ecc08e80049",
	  "subdir":"true",
	  "urls":[
	     {
		"url": "https://github.com/graalvm/graaljs",
		"kind" : "git"
	     }
	   ]
	},
      ],
   },

   "defaultLicense" : "GPLv2-CPE",

  "libraries" : {
    "JDK_TOOLS" : {
      "path" : "${JAVA_HOME}/lib/tools.jar",
      "sha1" : "NOCHECK",
    },
    "MACHINELEARNING" : {
      "path" : "./lib/machinelearning-algorithm-ga.jar",
      "sha1" : "NOCHECK",
    },
    "ANALYTICSGRAPH" : {
      "path" : "./lib/machinelearning-analytics-graph.jar",
      "sha1" : "NOCHECK",
    },
    "JAVASSIST" : {
      "path" : "./lib/javassist.jar",
      "sha1" : "NOCHECK",
    },
    "EHCACHE" : {
      "path" : "./lib/ehcache-3.5.2.jar",
      "sha1" : "NOCHECK",
    },
    "PROTOBUF" : {
      "path" : "./lib/protobuf-java-3.7.0.jar",
      "sha1" : "NOCHECK",
    },
    "JEROMQ" : {
      "path" : "./lib/jeromq-0.5.2.jar",
      "sha1" : "NOCHECK",
    },
  },

  "projects" : {
    "at.fh.hagenberg.aist.gce.optimization" : {
      "subDir":"gce",
      "sourceDirs" : ["src","resources"],
        "dependencies" : [
          "truffle:TRUFFLE_API",
          "MACHINELEARNING",
          "ANALYTICSGRAPH",
          "JAVASSIST",
          "EHCACHE"         
         ],
        "javaCompliance" : "1.8",
        "workingSets" : "GeneticCompilerEvolution",
    },
    "at.fh.hagenberg.aist.gce.optimization.external" : {
      "subDir":"gce",
      "sourceDirs" : ["src","resources"],
        "dependencies" : [
          "at.fh.hagenberg.aist.gce.optimization",
          "PROTOBUF",
          "JEROMQ"  
         ],
        "javaCompliance" : "1.8",
        "workingSets" : "GeneticCompilerEvolution",
    },
    "at.fh.hagenberg.aist.gce.optimization.test" : {
      "subDir":"gce",
      "sourceDirs" : ["src","resources"],
        "dependencies" : [
          "at.fh.hagenberg.aist.gce.optimization",
          "at.fh.hagenberg.aist.gce.minic",
          "mx:JUNIT"
         ],
        "javaCompliance" : "1.8",
        "workingSets" : "GeneticCompilerEvolution",
        "testProject": True
    },
    "at.fh.hagenberg.aist.gce.minic" : {
      "subDir":"minic",
      "sourceDirs" : ["src","resources"],
      "dependencies" : [
        "truffle:TRUFFLE_API",
        "JDK_TOOLS"
      ],
      "annotationProcessors" : ["truffle:TRUFFLE_DSL_PROCESSOR"],
      "javaCompliance" : "1.8",
      "workingSets" : "GeneticCompilerEvolution",
    },
    "at.fh.hagenberg.aist.gce.minic.test" : {
      "subDir":"minic",
      "sourceDirs" : ["src","resources"],
        "dependencies" : [
          "at.fh.hagenberg.aist.gce.minic",
          "mx:JUNIT"
         ],
        "javaCompliance" : "1.8",
        "workingSets" : "GeneticCompilerEvolution",
        "testProject": True
    },
    "at.fh.hagenberg.aist.gce.minic.benchmarks" : {
      "subDir":"minic",
      "sourceDirs" : ["src","resources"],
        "dependencies" : [
	  "at.fh.hagenberg.aist.gce.minic.test",
          "mx:JUNIT"
         ],
        "javaCompliance" : "1.8",
        "workingSets" : "GeneticCompilerEvolution",
        "testProject": True
    },
    "at.fh.hagenberg.aist.gce.lang.optimization.minic" : {
      "subDir":"lang",
      "sourceDirs" : ["src","resources"],
      "dependencies" : [
        "at.fh.hagenberg.aist.gce.optimization.external",
        "at.fh.hagenberg.aist.gce.minic",
        "mx:JUNIT"
      ],
      "annotationProcessors" : ["truffle:TRUFFLE_DSL_PROCESSOR"],
      "javaCompliance" : "1.8",
      "workingSets" : "GeneticCompilerEvolution",
    },
    "at.fh.hagenberg.aist.gce.lang.optimization.js" : {
      "subDir":"lang",
      "sourceDirs" : ["src","resources"],
      "dependencies" : [
        "graal-js:GRAALJS",
        "at.fh.hagenberg.aist.gce.optimization",
        "mx:JUNIT"
      ],
      "annotationProcessors" : ["truffle:TRUFFLE_DSL_PROCESSOR"],
      "javaCompliance" : "1.8",
      "workingSets" : "GeneticCompilerEvolution",
    },

    "at.fh.hagenberg.aist.gce.lang.optimization.sl" : {
      "subDir":"lang",
      "sourceDirs" : ["src","resources"],
      "dependencies" : [
        "truffle:TRUFFLE_SL",
        "at.fh.hagenberg.aist.gce.optimization",
        "mx:JUNIT"
      ],
      "annotationProcessors" : ["truffle:TRUFFLE_DSL_PROCESSOR"],
      "javaCompliance" : "1.8",
      "workingSets" : "GeneticCompilerEvolution",
    },
  },

  "distributions" : {
    "GeneticCompilerEvolution": {
       "dependencies": [
          "at.fh.hagenberg.aist.gce.lang.optimization.minic",
          "at.fh.hagenberg.aist.gce.lang.optimization.js",
	  "at.fh.hagenberg.aist.gce.lang.optimization.sl"
       ],
      "path" : "dists/gce.jar",
      "sourcesPath" : "dists/gce-sources.jar",
    },
    "Minic": {
       "dependencies": [
          "at.fh.hagenberg.aist.gce.minic",
       ],
      "path" : "dists/minic.jar",
      "sourcesPath" : "dists/minic-sources.jar",
    },
    "MinicOptimizerPreprocessor": {
      "mainClass" : "at.fh.hagenberg.aist.gce.lang.optimization.minic.Main",
       "dependencies": [
          "at.fh.hagenberg.aist.gce.lang.optimization.minic",
       ],
      "path" : "docker/worker-minic/minic-optimizer.jar",
      "sourcesPath" : "docker/worker-minic/minic-optimizer-sources.jar",
    },
    "JavaScriptOptimizer": {
       "dependencies": [
          "at.fh.hagenberg.aist.gce.lang.optimization.js",
       ],
      "path" : "dists/javascript-optimizer.jar",
      "sourcesPath" : "dists/javascript-optimizer-sources.jar",
    },
    "SimpleLanguageOptimizer": {
       "dependencies": [
          "at.fh.hagenberg.aist.gce.lang.optimization.sl",
       ],
      "path" : "dists/simplelanguage-optimizer.jar",
      "sourcesPath" : "dists/simplelanguage-optimizer-sources.jar",
    },
  }
}
