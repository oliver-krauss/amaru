int main() {
    int x[10];
    x[0] = 8;
    x[1] = 2;
    x[2] = 7;
    x[3] = 1;
    x[4] = 5;
    x[5] = 4;
    x[6] = 6;
    x[7] = 3;
    x[8] = 9;
    x[9] = 0;

    int len;
    len = 10;

    int i;
    for (i = 0; i < len; i = i + 1) {
        int min, j;
        min = i;
        for (j = i + 1; j < len; j = j + 1) {
            if (x[j] < x[min]) {
                min = j;
            }
        }
        int tmp;
        tmp = x[i];
        x[i] = x[min];
        x[min] = tmp;
    }

    print(x[0]);
    print(x[1]);
    print(x[2]);
    print(x[3]);
    print(x[4]);
    print(x[5]);
    print(x[6]);
    print(x[7]);
    print(x[8]);
    print(x[9]);

    return 0;
}