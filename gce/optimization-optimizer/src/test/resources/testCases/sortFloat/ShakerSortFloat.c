array shakerSortFloat(float x[], int len) {
    int i, swapped, cont,j;
    float tmp;
    cont = 1;

    for (i = 0; i < (len / 2) && cont; i = i + 1) {
        swapped = 0;
        for (j = i; j < len - i - 1; j = j + 1) {
            if (x[j] > x[j + 1]) {
                tmp = x[j];
                x[j] = x[j + 1];
                x[j + 1] = tmp;
                swapped = 1;
            }
        }
        for (j = len - 2 - i; j > i; j = j - 1) {
             if (x[j] < x[j - 1]) {
                tmp = x[j];
                x[j] = x[j - 1];
                x[j - 1] = tmp;
                swapped = 1;
            }
        }

        if (!swapped) {
            cont = 0;
        }
    }
    return x;
}