array mergeSortInlined(int x[], int len) {
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
    return x;
}