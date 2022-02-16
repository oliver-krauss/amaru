int incrementalConstruct(int n) {
    if (n == 0) {
        return 1;
    }
    if (n == 1) {
        return 2;
    }
    if (n == 2) {
        return n + 1;
    }
    return n * n;
}

int main(int i) {
    return incrementalConstruct(i);
}