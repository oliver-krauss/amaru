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