import random


def clean_text(text_file: str = "hoffman.txt"):
    """input file can be found in:
    https://drive.google.com/file/d/1VNediZphusn0h7g1gW-8CD5S6yJTmsWi/view?usp=sharing
    """
    with open(text_file, "r") as f:
        text = f.read()
    return text.lower()


LOOK_BACK = 2


def calc_prefix_counts(text):
    counts = dict()
    for word in text.split():
        word_pad = word + " "
        key = " " * LOOK_BACK
        for c in word_pad:
            counts_char = counts.get(key, dict())
            n = counts_char.get(c, 0)
            counts_char[c] = n + 1
            counts[key] = counts_char
            key = key[1:] + c
    return counts


def get_words(counts):
    key = ""
    word = ""
    key = " " * LOOK_BACK
    while True:
        if key not in counts:
            break
        counts_char = counts[key]
        chars_to_choose = list(counts_char.keys())
        weights = list(counts_char.values())
        items = random.choices(chars_to_choose, weights=weights)
        next_char = items[0]
        if next_char == " ":
            break
        word += next_char
        key = key[1:] + next_char
    return word


def main():
    text = clean_text()
    counts = calc_prefix_counts(text)
    for _ in range(30):
        word = get_words(counts)
        print(word)


if __name__ == "__main__":
    main()
