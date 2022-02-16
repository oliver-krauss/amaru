int main() {
    int x, y;
    for (x = 0; x < 3; x = x + 1) {
        for (y = 0; y < 3; y = y + 1) {
            print(x + y);
        }
    }
    return 0;
}