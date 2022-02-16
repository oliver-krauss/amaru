float cbrt(float x) {
    float result, h;
    int i, tablePosition;

    result = x;

    for (i = 0; i < 50; i = i + 1) {
        h = (result * result * result - x) / (3 * result * result);
        result = result - h;
    }

    return result;
}

float cbrt_benchmark(float x) {
    int i;
    float sum;
    sum = 0.0;
    for (i = 0; i < 100; i = i + 1)  {
        sum = sum + cbrt(x+i) + cbrt(x+i+1) + cbrt(x+i+2) + cbrt(x+i+3) + cbrt(x+i+4) + cbrt(x+i+5) + cbrt(x+i+6) + cbrt(x+i+7) + cbrt(x+i+8) + cbrt(x+i+9);
    }
    return sum;
}