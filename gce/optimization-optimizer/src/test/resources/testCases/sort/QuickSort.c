void swap(int x[], int i, int j) {
    int tmp;
    tmp = x[i];
    x[i] = x[j];
    x[j] = tmp;
}

int partition(int x[], int lo, int hi) {
    int i, j, v;
    i = lo;
    j = hi + 1;
    v = x[lo];
    int cont, cont1, cont2;

    cont = 1;
    i = lo;

    while (cont) {
        cont1 = 1;
        i = i + 1;
        while(x[i] < v && cont1) {
            if (i == hi) {
                cont1 = 0;
            } else {
                i = i + 1;
            }
        }
        j = j - 1;
        cont2 = 1;
        while(v < x[j] && cont2) {
            if (j == lo) {
                cont2 = 0;
            } else {
                j = j - 1;
            }
        }

        if (i >= j) {
            cont = 0;
        } else {
            swap(x, i, j);
        }
    }
    swap(x, lo, j);
    return j;
}


array quickSort(int x[], int len) {
    int l, h;
    l = 0;
    h = len - 1;
    int stack[h - l + 1];

    int top;
    top = -1;
    top = top + 1;
    stack[top] = l;
    top = top + 1;
    stack[top] = h;

    while (top >= 0) {
        h = stack[top];
        top = top -1;
        l = stack[top];
        top = top -1;

        int p;
        p = partition(x, l, h);

        if (p - 1 > l) {
            top = top + 1;
            stack[top] = l;
            top = top + 1;
            stack[top] = p - 1;
        }

        if (p + 1 < h) {
            top = top + 1;
            stack[top] = p + 1;
            top = top + 1;
            stack[top] = h;
        }
    }
    return x;
}