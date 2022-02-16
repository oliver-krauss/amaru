int main() {
    print(1 || 0);
    print(1 && 0);
    print(1 && 1);
    print(0 && 0);
    print(1 || 1 + 1);
    print(1 - 1 || 1 + 1);
    print(-1 || 1 + 1);
    print((1 || 0));
    print(1 && 0);
    print((1) && 1);
    print((0) && ((0)));
    print((1 || 1) + 1);
    print(1 - 1 || (1 + 1));
    print((-1 || 1 + 1));
    return 0;
}