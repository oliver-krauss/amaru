float fn(float x) {
    // TODO lookup table to init result
    float result, h;
    int i;
    result = x;

    for (i = 0; i < 3; i = i + 1) {
        h = (result * result  - x) / (2 * result);
        result = result - h;
    }

    return result;
}

float sqrt_inline_lookup(float x) {
    int i;
    float sum;
    sum = 0.0;
    for (i = 0; i < 1000; i = i + 1)  {
        sum = sum + fn(x+i) + fn(x+i+1) + fn(x+i+2) + fn(x+i+3) + fn(x+i+4) + fn(x+i+5) + fn(x+i+6) + fn(x+i+7) + fn(x+i+8) + fn(x+i+9);
    }
    return sum;
}