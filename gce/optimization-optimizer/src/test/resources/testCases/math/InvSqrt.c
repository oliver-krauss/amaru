float invSqrt(float x) {
    float result, h;
    int i, tablePosition;

    result = x;

    for (i = 0; i < 50; i = i + 1) {
        h = (result * result - x) / (2 * result);
        result = result - h;
    }

    return 1 / result;
}

float invSqrt_benchmark(float x) {
    int i;
    float sum;
    sum = 0.0;
    for (i = 0; i < 100; i = i + 1)  {
        sum = sum + invSqrt(x+i) + invSqrt(x+i+1) + invSqrt(x+i+2) + invSqrt(x+i+3) + invSqrt(x+i+4) + invSqrt(x+i+5) + invSqrt(x+i+6) + invSqrt(x+i+7) + invSqrt(x+i+8) + invSqrt(x+i+9);
    }
    return sum;
}