int dbl(int y);

int triple(int);

int main() {
    print(dbl(3));
    print(triple(3));
    return 0;
}

int dbl(int x) {
    return x * 2;
}

int triple(int x) {
    return x * 3;
}