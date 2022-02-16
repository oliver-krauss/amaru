array heapSortFloat(float x[], int len) {
    int i, j;
    i = 1;
    j = 0;

  // build max heap
    while (i < len) {
        if (x[i] > x[(i - 1) / 2]) {
            j = i;
            while (x[j] > x[(j - 1) / 2]){
                float tmp;
                tmp = x[j];
                x[j] = x[(j - 1) / 2];
                x[(j - 1) / 2] = tmp;
                j = (j - 1) / 2;
            }
        }
        i = i + 1;
    }

    // heap
    i = len - 1;
    while (i > 0) {
        float tmp;
        tmp = x[0];
        x[0] = x[i];
        x[i] = tmp;

        j = 0;
        int index;
        index = (2 * j + 1);

        // this is so weird because there is no do while
        while (index < i) {
            index = (2 * j + 1);

            if (index < i) {
                if (x[index] < x[index + 1] && index < (i - 1)) {
                    index = index + 1;
                }
                if (x[j] < x[index] && index < i) {
                    tmp = x[j];
                    x[j] = x[index];
                    x[index] = tmp;
                }
                j = index;
            }
        }
        i = i - 1;
    }
    return x;
}