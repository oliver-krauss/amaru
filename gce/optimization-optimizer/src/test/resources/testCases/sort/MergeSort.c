void merge(int x[], int lo, int mid, int hi, int len) {
    int i, j, k;
    int aux[len];
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

array mergeSort(int x[], int len) {
    int sz, lo;
    for (sz = 1; sz < len; sz = sz + sz) {
        for (lo = 0; lo < len - sz; lo = lo + sz + sz) {
            int mid, hi;
            hi = lo + sz + sz - 1;
            if (len - 1 < hi) {
                hi = len - 1;
            }
            mid = lo + sz - 1;
            merge(x, lo, mid, hi, len);
        }
    }
    return x;
}