float abs(float val) {
    if (val < 0) {
        return -val;
    }
    return val;
}

float ln(float x) {
    float result, h;
    int i, tablePosition;

    result = x / 100;
    h = (powf(2.718281828459045, result) - x) / (powf(2.718281828459045, result));
    result = result - h;

    while (abs(h) > 0.000001) {
        h = (powf(2.718281828459045, result) - x) / (powf(2.718281828459045, result));
        result = result - h;
    }

    return result;
}

float ln_benchmark(float x) {
    int i;
    float sum;
    sum = 0.0;
    for (i = 0; i < 100; i = i + 1)  {
        sum = sum + ln(x+i) + ln(x+i+1) + ln(x+i+2) + ln(x+i+3) + ln(x+i+4) + ln(x+i+5) + ln(x+i+6) + ln(x+i+7) + ln(x+i+8) + ln(x+i+9);
    }
    return sum;
}