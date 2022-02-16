int a() {
    return 3 + 5;
}

int main() {
    char x[10], y[2][3][4], z[a()];
    x[3] = 'a';
    y[1][2][3] = 'b';

    print(x[3]);
    print(y[1][2][3]);
    return 0;
}