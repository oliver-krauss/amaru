float sq_fn() {
    // cheat the inlining by removing the params
    float result;
    return result * result;
}

float sq_der() {
    // cheat the inlining by removing the params
    float result;
    return 2 * result;
}

float fn() {
    // cheat the inlining by removing the params
    float result, h, x;
    int j;
    result = x;

    for (j = 0; j < 3; j = j + 1) {
        h = (sq_fn() - x) / sq_der();
        result = result - h;
    }

    return result;
}

float sqrt_lookup_manual(float x) {
    int i;
    float sum;

    sum = 0.0;
    for (i = 0; i < 1000; i = i + 1)  {
        sum = sum + fn(x+i) + fn(x+i+1) + fn(x+i+2) + fn(x+i+3) + fn(x+i+4) + fn(x+i+5) + fn(x+i+6) + fn(x+i+7) + fn(x+i+8) + fn(x+i+9);
    }
    return sum;
}

// NOTE: THE CODE TO MANUALLY INLINE:
 // Reproduce the Graal inlining to see if we can explain the performance diff
//        if (run.getKey().getFunction().equals("sqrt_lookup_manual")) {
//            System.out.println("MANUAL INLINING");
//            Set<Node> invokes = new HashSet<>();
//            invokes.add(executor.getOrigin().getParent().getParent());
//            findInvocations(invokes.iterator().next(), invokes);
//
//            // inline all the invokes together
//            invokes.forEach(ast -> {
//                List<Iterable<Node>> minicInvoke = ExtendedNodeUtil.flatten(ast).filter(x -> x.getClass().getName().contains("MinicInvoke")).map(Node::getChildren).collect(Collectors.toList());
//                List<String> inlinedFns = new ArrayList<>();
//                minicInvoke.forEach(invoke -> {
//                    invoke.iterator().forEachRemaining(y -> {
//                        if (y.getClass().getName().contains("MinicFunctionLiteral")) {
//                            String name = ((MinicFunctionLiteralNode) y).getName();
//                            if (!name.equals("fn")) {
//                                // replace everything except "fn" as "fn" is also not inlined in the sqrt_inline
//                                RootCallTarget callTarget = MinicLanguage.getCurrentContext().getFunctionRegistry().lookup(name).getCallTarget();
//                                MinicRootNode rootNode = (MinicRootNode) callTarget.getRootNode();
//                                MinicExpressionNode functionBodyNode = (MinicExpressionNode) rootNode.getChildren().iterator().next();
//                                MinicToFloatNode.MinicGenericToFloatNode replacement = MinicToFloatNodeFactory.MinicGenericToFloatNodeGen.create(functionBodyNode);
//                                MinicToFloatNode.MinicFunctionCallToFloatNode replacementOther = MinicToFloatNodeFactory.MinicFunctionCallToFloatNodeGen.create(rootNode);
//                                Node parent = invoke.iterator().next().getParent();
//                                if (parent.isSafelyReplaceableBy(replacementOther)) {
//                                    parent.replace(replacementOther);
//                                    parent.adoptChildren();
//                                    System.out.println("INLINED " + name);
//                                    inlinedFns.add(name);
//                                }
//                            }
//                        }
//                    });
//                });
//            });
//
//        }
