char x[10], y[2][3][4];

void useX() {
    x[3] = 'a';
}

void useY() {
    y[1][2][3] = 'b';
}

int main() {
    useX();
    useY();

    print(x[3]);
    print(y[1][2][3]);
    return 0;
}