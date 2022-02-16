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

import at.fh.hagenberg.aist.gce.optimization.language.WeightWatcherWorker;
import at.fh.hagenberg.aist.gce.optimization.language.JavassistWorker;
import at.fh.hagenberg.aist.gce.optimization.language.util.CommandProcessor;
import at.fh.hagenberg.aist.gce.optimization.language.util.WeightWatcherCallback;
import at.fh.hagenberg.aist.gce.optimization.language.util.EngineConfig;
import at.fh.hagenberg.aist.gce.optimization.util.ClassLoadingHelper;
import at.fh.hagenberg.aist.gce.optimization.util.TruffleLanguageInformation;
import science.aist.seshat.Logger;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.RootNode;
import javassist.*;

import java.io.DataOutputStream;
import java.io.File;
import java.net.URI;
import java.nio.file.*;
import java.util.*;

/**
 * Executor that uses Javassist to rewrite the node classes and inject an interceptor that FORCES
 * Graal to use our nodes instead of throwing them away.
 * This class should be solely used for performance measuring and nothing else.
 *
 * @author Oliver Krauss on 28.10.2019
 */
public class WeightWatcherExecutor extends AbstractExecutor {

    /**
     * Prefix that the Executor will insert in pre-processed languages
     */
    protected static final String DUMMY_PREFIX = "weight-";

    protected static final String CALLBACK_FIELDNAME = "_CALLBACK";

    /**
     * Where the JAR with {@link JavassistWorker} is located.
     */
    private String languageLocation;

    private Logger logger = Logger.getInstance();

    List<String> methods = Arrays.asList("execute", "AndSpecialize");

    /**
     * Where the JAR is located that was preprocessed by Javassist
     */
    private String analysisLanguageLocation;

    public WeightWatcherExecutor(String languageId, String code, String entryPoint, String function, String languageLocation) {
        super(languageId, code,entryPoint, function);

        if (languageLocation == null) {
            languageLocation = new File(EngineConfig.DIST_LOCATION).getAbsolutePath() + "/" + ACCESSOR_PREFIX + this.languageId + ".jar";
            analysisLanguageLocation = new File(EngineConfig.DIST_LOCATION).getAbsolutePath() + "/" + DUMMY_PREFIX + ACCESSOR_PREFIX + this.languageId + ".jar";
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
                        CtField f = new CtField(classPool.get(WeightWatcherCallback.class.getName()), CALLBACK_FIELDNAME, ctClass);
                        f.setModifiers(Modifier.PUBLIC); // AccessFlag.SYNTHETIC +
                        ctClass.addField(f, CtField.Initializer.byCall(classPool.get(WeightWatcherCallback.class.getName()), "getInterceptor"));

                        for (CtMethod method : ctClass.getDeclaredMethods()) {
                            if (methods.stream().anyMatch(x -> method.getName().contains(x)) && !Modifier.isAbstract(method.getModifiers())) {
                                String callback = CALLBACK_FIELDNAME + ".beforeIntercept();";
                                method.insertBefore(callback);
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

    @Override
    public ExecutionResult conductTest(Node node, Object[] input) {
        // start up jar file
        Runtime rt = Runtime.getRuntime();
        long[] performance = new long[repeats];
        Throwable e = null;

        try {
            // create process and send node
            Process pr = rt.exec(EngineConfig.JAVA_LOCATION + " " + EngineConfig.JAVA_CALL_PARAMS + " -cp " + analysisLanguageLocation + " " + WeightWatcherWorker.class.getName(), CommandProcessor.prepareCommand(this.languageId, this.code, this.function, this.repeats, input));
            CommandProcessor.sendNode(node);

            // parse results
            return CommandProcessor.receiveExecutionResult();

        } catch (Exception ex) {
            e = ex;
        }

        return new TraceExecutionResult(e, null, performance, false, -1, -1, null);
    }
}
