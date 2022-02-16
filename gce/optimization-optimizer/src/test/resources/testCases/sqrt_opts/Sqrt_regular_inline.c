float fn(float n) {
    float sgn, val, last, absVal;
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

    absVal = val - last;
    if (absVal < 0) {
      absVal = 0 - absVal;
    }

    while (!(absVal < 0.000000001)) {
        last = val;
        val = (val + n / val) * 0.5;

        absVal = val - last;
        if (absVal < 0) {
          absVal = 0 - absVal;
        }
    }

    return val;
}

float sqrt_regular_inline(float x) {
    int i;
    float sum;
    sum = 0.0;
    for (i = 0; i < 1000; i = i + 1)  {
        sum = sum + fn(x+i) + fn(x+i+1) + fn(x+i+2) + fn(x+i+3) + fn(x+i+4) + fn(x+i+5) + fn(x+i+6) + fn(x+i+7) + fn(x+i+8) + fn(x+i+9);
    }
    return sum;
}