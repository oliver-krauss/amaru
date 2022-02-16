int fibonacci(int n) {
    if (n == 0) {
        return 0;
    }
    if (n == 1) {
        return 1;
    }
    return fibonacci(n - 1) + fibonacci(n - 2);
}

int main() {
    int i;
    i = 2;
    while (i < 20) {
        print(fibonacci(i));
        i = i + 1;
    }
    return 0;
}