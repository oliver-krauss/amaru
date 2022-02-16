int fibonacci(int n) {
    int i, now, prev, next;
    prev = 0;
    now = 1;
    i = 0;
    while (i < n){
      next = now + prev;
      prev = now;
      now = next;
      i = i + 1;
    }
    return prev;
}

int main() {
    int i;
    i = 2;
    while (i < 20) {
        print(fibonacci(i));
        i = i + 1;
    }
    return 0;
}