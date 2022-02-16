float abs(float val) {
    if (val < 0) {
        return -val;
    }
    return val;
}

float log(float x) {
    float result, h;
    int i, tablePosition;

    result = x / 100;
    h = (powf(10.0, result) - x) / (powf(10.0, result) * 2.30258509);
    result = result - h;

    while (abs(h) > 0.000001) {
        h = (powf(10.0, result) - x) / (powf(10.0, result) * 2.30258509);
        result = result - h;
    }

    return result;
}

float log_benchmark(float x) {
    int i;
    float sum;
    sum = 0.0;
    for (i = 0; i < 100; i = i + 1)  {
        sum = sum + log(x+i) + log(x+i+1) + log(x+i+2) + log(x+i+3) + log(x+i+4) + log(x+i+5) + log(x+i+6) + log(x+i+7) + log(x+i+8) + log(x+i+9);
    }
    return sum;
}