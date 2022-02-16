void printchar(char x, char y, char z) {
    printsinglechar(x);
    printsinglechar(y);
    printsinglechar(z);
}

void printsinglechar(char x) {
    print(x);
}

void printint(int i) {
    print(i);
}

int main() {
    printint(51);
    printchar('a', 'b', 'c');
    return 0;
}