// note that this solution is intentionally stupid for optimization purposes
// what we want to find is an algorithm that just swaps the characters in the string
string stringRevert(string x) {
    string reverted;
    int i;
    char c[1];

    reverted = "";
    i = length(x) - 1;

    while (i >= 0) {
      c[0] = x[i];
      reverted = reverted + (string)c;
      i = i - 1;
    }

    return reverted;
}

string main(string i) {
    return stringRevert(i);
}