float sq_fn(float result) {
    return result * result;
}

float sq_der(float result) {
    return 2 * result;
}

int calcLookupTablePosition(float val) {
    float stepSize,position;
    int tablePosition;

    float higher_end;
    higher_end = 2.0;
    float lower_end;
    lower_end = 0.5;
    int table_size;
    table_size = 512;

    stepSize = (higher_end - lower_end) / table_size;
    position = (val - lower_end) / stepSize;
    tablePosition = (int) position;

    if (tablePosition <= -1) {
        tablePosition = 0;
    }
    if (tablePosition >= table_size) {
        tablePosition = table_size - 1;
    }

    return tablePosition;
}

float fn(float x) {
    float result, h;
    int i, tablePosition;
    tablePosition = calcLookupTablePosition(x);

    print(tablePosition);

    result = x;

    for (i = 0; i < 6; i = i + 1) {
        h = (sq_fn(result) - x) / sq_der(result);
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