float fn(float x) {
    // TODO lookup table to init result
    float result, h;
    int i, reverse;
    result = x;
    reverse = 0;

    if (x < 0) {
        result = -result;
        x = -x;
        reverse = 1;
    }

    for (i = 0; i < 36; i = i + 1) {
        h = (result * result  - x) / (2 * result);
        result = result - h;
    }

    if (reverse) {
        result = -result;
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

int main() {
    print(fn(0.5));
    print(fn(1.0));
    print(fn(2.0));
    print(fn(3.0));
    print(fn(4.0));
    print(fn(5.0));
    print(fn(5000.0));
    print(fn(5000000.0));
    print(fn(5000000000.0));
    print(fn(3.4028234664E+18));
    print(fn(-0.5));
    print(fn(-1.0));
    print(fn(-2.0));
    print(fn(-3.0));
    print(fn(-4.0));
    print(fn(-5.0));
    print(fn(-5000.0));
    print(fn(-5000000.0));
    print(fn(-5000000000.0));
    return 0;
}