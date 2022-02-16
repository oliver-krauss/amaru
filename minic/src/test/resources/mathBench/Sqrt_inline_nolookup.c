float fn(float x) {
    float result, h;
    int i, tablePosition;

    result = x;

    for (i = 0; i < 6; i = i + 1) {
        h = ((result * result) - x) / (2 * result);
        result = result - h;
    }

    return result;
}

float main() {
    int i;
    float sum, x;
    sum = 0.0;
    x = 0.5;
    for (i = 0; i < 1000; i = i + 1)  {
        sum = sum + fn(x) + fn(x+0.00015) + fn(x+0.0003) + fn(x+0.00045) + fn(x+0.0006) + fn(x+0.00075) + fn(x+0.0009) + fn(x+0.00105) + fn(x+0.0012) + fn(x+0.00135);
        x = x + 0.0015;
    }
    return sum;
}