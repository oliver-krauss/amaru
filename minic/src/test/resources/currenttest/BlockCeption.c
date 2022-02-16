

int main() {
    int x;
    x = 3;
    print(x);

    {
        int x;
        x = 5;
        print(x);
    }

    print(x);

    return 0;
}