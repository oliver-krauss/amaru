char x;

void setVar() {
    x = 'y';
}

void useVar() {
    print(x);
}

int main() {
    setVar();
    useVar();
    return 0;
}