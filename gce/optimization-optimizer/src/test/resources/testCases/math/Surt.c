float surt(float x) {
    float result, h;
    int i, tablePosition;

    result = x;

    for (i = 0; i < 60; i = i + 1) {
        h = (result * result * result * result - x) / (4 * result * result * result);
        result = result - h;
    }

    return result;
}

float surt_benchmark(float x) {
    int i;
    float sum;
    sum = 0.0;
    for (i = 0; i < 100; i = i + 1)  {
        sum = sum + surt(x+i) + surt(x+i+1) + surt(x+i+2) + surt(x+i+3) + surt(x+i+4) + surt(x+i+5) + surt(x+i+6) + surt(x+i+7) + surt(x+i+8) + surt(x+i+9);
    }
    return sum;
}