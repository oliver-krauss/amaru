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

    int len, h, i, j, cont, tmp;
    len = 10;
    h = 1;
    while (h < len / 3) {
        h = 3 * h + 1;
    }

    while (h >= 1) {
        i = h;
        while (i < len) {
            j = i;
            cont = 1
            while (j >= h && cont) {
                if (x[j] < x[j - h]) {
                    tmp = x[j];
                    x[j] = x[j - h];
                    x[j - h] = tmp;
                    j = j - h;
                } else {
                    cont = 0;
                }
            }
            i = i + 1;
        }
        h = h / 3;
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