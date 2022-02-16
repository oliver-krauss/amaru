array insertionSortFloat(float x[], int len) {
    int i, j;

    for (i = 1; i < len; i = i + 1) {
        for (j = i; j > 0; j = j - 1) {
            if(x[j] < x[j-1]){
                float tmp;
                tmp = x[j];
                x[j] = x[j-1];
                x[j-1] = tmp;
            }
        }
    }
    return x;
}