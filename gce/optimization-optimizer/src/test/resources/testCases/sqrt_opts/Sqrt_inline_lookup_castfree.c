float fn(float x) {
    // TODO lookup table to init result
    float result, h;
    int i;
    result = x;

    for (i = 0; i < 3; i = i + 1) {
        h = (result * result  - x) / (2.0 * result);
        result = result - h;
    }

    return result;
}

float sqrt_inline_lookup_castfree(float x) {
    float i;
    float sum;
    sum = 0.0;
    for (i = 0.0; i < 1000.0; i = i + 1.0)  {
        sum = sum + fn(x+i) + fn(x+i+1.0) + fn(x+i+2.0) + fn(x+i+3.0) + fn(x+i+4.0) + fn(x+i+5.0) + fn(x+i+6.0) + fn(x+i+7.0) + fn(x+i+8.0) + fn(x+i+9.0);
    }
    return sum;
}