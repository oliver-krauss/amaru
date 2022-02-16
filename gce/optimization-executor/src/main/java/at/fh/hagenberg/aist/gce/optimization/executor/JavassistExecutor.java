/*
 * Copyright (c) 2022 the original author or authors.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

package at.fh.hagenberg.aist.gce.optimization.executor;

import at.fh.hagenberg.aist.gce.optimization.language.JavassistWorker;
import at.fh.hagenberg.aist.gce.optimization.language.util.*;
import at.fh.hagenberg.aist.gce.optimization.util.ClassLoadingHelper;
import at.fh.hagenberg.aist.gce.optimization.util.TruffleLanguageInformation;
import science.aist.seshat.Logger;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.RootNode;
import javassist.*;

import java.io.*;
import java.net.URI;
import java.nio.file.*;
import java.util.*;

/**
 * Executor that uses Javassist to rewrite the node classes and inject an interceptor that allows us to collect
 * additional data on the runs. This executor should NEVER be used for performance measures. It is intended to
 * collect data on Truffle-Specializations, Hot Paths, and other features that we can't normally determine.
 *
 * @author Oliver Krauss on 28.10.2019
 */
public class JavassistExecutor extends AbstractExecutor {

    /**
     * Prefix that the Executor will insert in pre-processed languages
     */
    protected static final String JAVASSIST_PREFIX = "analysis-";

    protected static final String CALLBACK_FIELDNAME = "_CALLBACK";

    /**
     * Where the JAR with {@link at.fh.hagenberg.aist.gce.optimization.language.JavassistWorker} is located.
     */
    private String languageLocation;

    private Logger logger = Logger.getInstance();

    List<String> methods = Arrays.asList("execute", "AndSpecialize");

    /**
     * Where the JAR is located that was preprocessed by Javassist
     */
    private String analysisLanguageLocation;

    public JavassistExecutor(String languageId, String code, String entryPoint, String function, String languageLocation) {
        super(languageId, code, entryPoint, function);

        if (languageLocation == null) {
            languageLocation = new File(EngineConfig.DIST_LOCATION).getAbsolutePath() + "/" + ACCESSOR_PREFIX + this.languageId + ".jar";
            analysisLanguageLocation = new File(EngineConfig.DIST_LOCATION).getAbsolutePath() + "/" + JAVASSIST_PREFIX + ACCESSOR_PREFIX + this.languageId + ".jar";
        }
        this.languageLocation = languageLocation;

        File analysisLanguage = new File(analysisLanguageLocation);
        if (!analysisLanguage.exists()) {
            // pre-load the language as the class-modification will create errors otherwise in the current system
            TruffleLanguageInformation.getLanguageInformation(languageId);
            // if analysis language doesn't exist, create it and modify with Javassist
            try {
                // create new jar
                Files.copy(new File(languageLocation).toPath(), analysisLanguage.toPath());
                Map<String, String> env = new HashMap<>();
                env.put("create", "true");
                FileSystem newJar = FileSystems.newFileSystem(URI.create("jar:" + analysisLanguage.toURI()), env);

                // find classes to modify
                ClassLoadingHelper helper = new ClassLoadingHelper();
                helper.setPaths(new LinkedList<>());
                helper.getPaths().add(analysisLanguageLocation);
                helper.setParentClasses(Arrays.asList(Node.class, RootNode.class));
                helper.setPackages(Arrays.asList("com.oracle.truffle", "at.fh.hagenberg"));
                helper.setExcludes(Arrays.asList("com.oracle.truffle.polyglot", "com.oracle.truffle.api", "com.oracle.truffle.tck"));
                List<Class> classes = helper.findClasses();

                // get classpool of original jar
                ClassPool classPool = new ClassPool(ClassPool.getDefault());
                classPool.appendClassPath(languageLocation);

                // edit classes in new jar
                for (Class nodeClass : classes) {
                    logger.error("Modifying " + nodeClass.getName());
                    CtClass ctClass = classPool.get(nodeClass.getName());

                    if (!ctClass.isFrozen()) {

                        // add callback field
                        // add sync field
                        CtField f = new CtField(classPool.get(JavassistInterceptCallback.class.getName()), CALLBACK_FIELDNAME, ctClass);
                        f.setModifiers(Modifier.PUBLIC); // AccessFlag.SYNTHETIC +
                        ctClass.addField(f, CtField.Initializer.byCall(classPool.get(JavassistInterceptProvider.class.getName()), "getInterceptor"));

                        for (CtMethod method : ctClass.getDeclaredMethods()) {
                            if (methods.stream().anyMatch(x -> method.getName().contains(x)) && !Modifier.isAbstract(method.getModifiers())) {
                                String callback = CALLBACK_FIELDNAME + ".beforeIntercept(this, \"" + method.getName() + "\",";
                                if (method.getParameterTypes().length > 0) {
                                    // collect parameters
                                    String parameters = "";
                                    for (int i = 0; i < method.getParameterTypes().length; i++) {
                                        if (method.getParameterTypes()[i].isPrimitive()) {
                                            switch (method.getParameterTypes()[i].getName()) {
                                                case "int":
                                                    parameters += "args[" + i + "] = Integer.valueOf($" + (i + 1) + ");";
                                                    break;
                                                default:
                                                    parameters += "args[" + i + "] = $" + (i + 1) + ";";
                                            }
                                        } else {
                                            parameters += "args[" + i + "] = $" + (i + 1) + ";";
                                        }
                                    }
                                    method.insertBefore("Object[] args = new Object[" + method.getParameterTypes().length + "];" + parameters +
                                            callback + "args);");
                                } else {
                                    method.insertBefore(callback + "null);");
                                }
                            }
                        }

                        // override the class file
                        Path nf = newJar.getPath(ctClass.getName().replace(".", "/") + ".class");
                        DataOutputStream dataOutputStream = new DataOutputStream(Files.newOutputStream(nf, StandardOpenOption.CREATE));
                        ctClass.toBytecode(dataOutputStream);
                        dataOutputStream.close();
                    } else {
                        logger.error("Class is frozen");
                    }

                    logger.info("Successfully modified " + ctClass.getName());
                }

                // write
                newJar.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private String worker = JavassistWorker.class.getName();

    @Override
    public ExecutionResult conductTest(Node node, Object[] input) {
        // start up jar file
        Runtime rt = Runtime.getRuntime();
        long[] performance = new long[repeats];
        Throwable e = null;

        try {
            // create process and send node
            Process pr = rt.exec(EngineConfig.JAVA_LOCATION + " " + EngineConfig.JAVA_CALL_PARAMS + " -cp " + analysisLanguageLocation + " " + worker, CommandProcessor.prepareCommand(this.languageId, this.code, this.function, this.repeats, input));
            CommandProcessor.sendNode(node);

            // parse results
            return CommandProcessor.receiveExecutionResult();

        } catch (Exception ex) {
            e = ex;
        }

        return new TraceExecutionResult(e, null, performance, false, -1, -1, null);
    }

    public TraceExecutionResult traceTest(Node node, Object[] input) {
        // TODO #257 Javassit worker is broken. Most likely because the sockets don't play nice with each other anymore
        if (timeout == -1) {
            this.setTimeout(5000);
        }
        ExecutionResult executionResult = test(node, input);
        if (executionResult instanceof TraceExecutionResult) {
            return (TraceExecutionResult) executionResult;
        } else {
            return new TraceExecutionResult(executionResult.returnValue, null, executionResult.performance, false, -1, -1, null);
        }
    }

    public void setWorker(Class<? extends JavassistWorker> worker) {
        this.worker = worker.getName();
    }
}
