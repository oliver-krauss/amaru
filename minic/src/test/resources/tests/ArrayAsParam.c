void param(char x[10]) {
    x[3] = 'a';
}

int main() {
    char x[10];
    param(x);
    print(x[3]);
    return 0;
}