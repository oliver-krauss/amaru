float sqrt_java(float x) {
    // not using it, direct call instead
    return sqrt(x);
}

float sqrt_java_benchmark(float x) {
    int i;
    float sum;
    sum = 0.0;
    for (i = 0; i < 100; i = i + 1)  {
        sum = sum + sqrt_java(x+i) + sqrt_java(x+i+1) + sqrt_java(x+i+2) + sqrt_java(x+i+3) + sqrt_java(x+i+4) + sqrt_java(x+i+5) + sqrt_java(x+i+6) + sqrt_java(x+i+7) + sqrt_java(x+i+8) + sqrt_java(x+i+9);
    }
    return sum;
}