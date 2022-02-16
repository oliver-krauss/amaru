float sq_fn(float result) {
    return result * result;
}

float sq_der(float result) {
    return 2 * result;
}

float sqrt_nolookup(float x) {
    float result, h;
    int i, tablePosition;

    result = x;

    for (i = 0; i < 40; i = i + 1) {
        h = (sq_fn(result) - x) / sq_der(result);
        result = result - h;
    }

    return result;
}

float sqrt_nolookup_benchmark(float x) {
    int i;
    float sum;
    sum = 0.0;
    for (i = 0; i < 100; i = i + 1)  {
        sum = sum + sqrt_nolookup(x+i) + sqrt_nolookup(x+i+1) + sqrt_nolookup(x+i+2) + sqrt_nolookup(x+i+3) + sqrt_nolookup(x+i+4) + sqrt_nolookup(x+i+5) + sqrt_nolookup(x+i+6) + sqrt_nolookup(x+i+7) + sqrt_nolookup(x+i+8) + sqrt_nolookup(x+i+9);
    }
    return sum;
}