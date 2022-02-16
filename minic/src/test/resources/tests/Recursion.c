
void recursion(int i) {
    print(i);
    if (i < 10) {
        recursion(i + 1);
    }
}

int main() {
    recursion(0);
    return 0;
}