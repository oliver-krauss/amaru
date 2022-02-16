int x[10];

int main() {

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
    int len, i, j;
    len = 10;
    i = 1;
    j = 0;

    // build max heap
    while (i < len) {
        if (x[i] > x[(i - 1) / 2]) {
            j = i;
            while (x[j] > x[(j - 1) / 2]){
                int tmp;
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
        int tmp;
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