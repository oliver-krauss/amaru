array selectionSortFloat(float x[], int len) {
    int i;
    for (i = 0; i < len; i = i + 1) {
        int min, j;
        min = i;
        for (j = i + 1; j < len; j = j + 1) {
            if (x[j] < x[min]) {
                min = j;
            }
        }
        float tmp;
        tmp = x[i];
        x[i] = x[min];
        x[min] = tmp;
    }
    return x;
}