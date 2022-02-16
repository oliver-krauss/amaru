array bubbleSortFloat(float x[], int len) {
    int i, j;
    float tmp;
    for (i = 0; i < len -1; i = i + 1) {
        for (j = 0; j < len - i - 1; j = j + 1) {
            if(x[j] > x[j+1]){
                tmp = x[j];
                x[j] = x[j+1];
                x[j+1] = tmp;
            }
        }
    }
    return x;
}