float fn(float x) {
    // not using it, direct call instead
    return sqrt(x);
}

float sqrt_java(float x) {
    int i;
    float sum;
    sum = 0.0;
    for (i = 0; i < 1000; i = i + 1)  {
        sum = sum + sqrt(x+i) + sqrt(x+i+1) + sqrt(x+i+2) + sqrt(x+i+3) + sqrt(x+i+4) + sqrt(x+i+5) + sqrt(x+i+6) + sqrt(x+i+7) + sqrt(x+i+8) + sqrt(x+i+9);
    }
    return sum;
}