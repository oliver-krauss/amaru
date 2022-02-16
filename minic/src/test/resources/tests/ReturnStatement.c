char getChar() {
    return 'y';
}

void useVar(char c) {
    print(c);
}

int main() {
    useVar(getChar());
    return 0;
}