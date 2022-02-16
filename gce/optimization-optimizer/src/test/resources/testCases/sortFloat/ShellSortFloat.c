array shellSortFloat(float x[], int len) {
    int h, i, j, cont;
    float tmp;
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
    return x;
}