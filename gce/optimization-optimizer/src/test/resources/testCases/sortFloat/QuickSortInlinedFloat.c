array quickSortInlinedFloat(float x[], int len) {
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
        int i, j;
        float v;
        i = l;
        j = h + 1;
        v = x[l];
        int cont, cont1, cont2;
        cont = 1;
        while (cont) {
            cont1 = 1;
            i = i + 1;
            while(x[i] < v && cont1) {
                if (i == h) {
                    cont1 = 0;
                } else {
                    i = i + 1;
                }
            }
            j = j - 1;
            cont2 = 1;
            while(v < x[j] && cont2) {
                if (j == l) {
                    cont2 = 0;
                } else {
                    j = j - 1;
                }
            }

            if (i >= j) {
                cont = 0;
            } else {
                float tmp;
                tmp = x[i];
                x[i] = x[j];
                x[j] = tmp;
            }
        }
        float tmp;
        tmp = x[l];
        x[l] = x[j];
        x[j] = tmp;
        p = j;

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