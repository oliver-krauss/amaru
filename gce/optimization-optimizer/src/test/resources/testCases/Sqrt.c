float abs(float val) {
    if (val < 0) {
        return -val;
    }
    return val;
}

float sqrt(float n) {
    float sgn, val, last;
    sgn = 0.0;

    if (n == 0) {
        return n;
    }
    if (n < 0) {
        sgn = -1.0;
        n = -n;
    }

    val = n;
    last = 1000000.0;

    while (!(abs(val - last) < 0.000000001)) {
        last = val;
        val = (val + n / val) * 0.5;
    }

    return val;
}

float main(float i) {
    return sqrt(i);
}