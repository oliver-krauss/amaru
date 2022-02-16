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
    i = 1;
    j = 0;

    int aux[len];
    int sz, lo;


    for (sz = 1; sz < len; sz = sz + sz) {
        for (lo = 0; lo < len - sz; lo = lo + sz + sz) {
            int i, j, mid, hi, k;
            hi = lo + sz + sz - 1;
            if (len - 1 < hi) {
                hi = len - 1;
            }
            mid = lo + sz - 1;
            i = lo;
            j = mid + 1;
            for (k = lo; k <= hi; k = k + 1) {
                aux[k] = x[k];
            }
            for (k = lo; k <= hi; k = k + 1) {
                if (i > mid) {
                    x[k] = aux[j];
                    j = j + 1;
                } else {
                    if (j > hi) {
                        x[k] = aux[i];
                        i = i + 1;
                    } else {
                        if (aux[j] < aux[i]) {
                            x[k] = aux[j];
                            j = j + 1;
                        } else {
                            x[k] = aux[i];
                            i = i + 1;
                        }
                    }
                }
            }
        }
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