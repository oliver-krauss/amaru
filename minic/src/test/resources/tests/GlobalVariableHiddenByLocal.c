char x;

void setVar() {
    x = 'y';
}

void useVar() {
    char x;
    x = 'c';
    print(x);
}

int main() {
    setVar();
    useVar();
    print(x);
    return 0;
}