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
    int len, i, j;
    len = 10;
    j = 0;

    for (i = 1; i < len; i = i + 1) {
        j = i - 1;
        int key;
        key = x[i];
        int cont;
        cont = 1;

        while (j >= 0 && cont) {
            // this is so weird because the && node does not short circuit otherwise the if can be added to the while (and no cont variable)
            if (x[j] - key > 0) {
                x[j+1] = x[j];
                j = j - 1;
            } else {
                cont = 0;
            }

        }
        x[j+1] = key;
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