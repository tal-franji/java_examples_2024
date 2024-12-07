package il.ac.tau.cs.experiment;

import java.util.*;

public class MultiMerge {
    // Franji's version  (63 lines)
    static class IteratorLookAhead<T> {
        public Iterator<T> iterator;
        public Optional<T> lookAhead = Optional.empty();

        IteratorLookAhead(Iterator<T> iterator) {
            this.iterator = iterator;
            peekNext();
        }

        public void peekNext() {
            if (iterator.hasNext()) {
                lookAhead = Optional.of(iterator.next());
            } else {
                lookAhead = Optional.empty();
            }
        }
    }

    static class Merger<T extends Comparable<T>> implements Iterator<T> {

        List<IteratorLookAhead<T>> iterators;

        public Merger(List<Iterator<T>> iterators) {
            this.iterators = new ArrayList<>();
            for (Iterator<T> iterator : iterators) {
                this.iterators.add(new IteratorLookAhead<>(iterator));
            }
        }

        private Optional<IteratorLookAhead<T>> getMinLookahead() {
            Optional<IteratorLookAhead<T>> minimal = Optional.empty();
            for (var iterator : iterators) {
                if (iterator.lookAhead.isEmpty()) {
                    continue;
                }
                if (minimal.isEmpty() || minimal.get().lookAhead.get().compareTo(iterator.lookAhead.get()) > 0) {
                    minimal = Optional.of(iterator);
                }
            }
            return minimal;
        }

        @Override
        public boolean hasNext() {
            var minIteratorOpt = getMinLookahead();
            if (minIteratorOpt.isEmpty()) {
                return false;
            }
            return true;
        }

        @Override
        public T next() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }
            var minIteratorOpt = getMinLookahead();
            var minIterator = minIteratorOpt.get();
            var value = minIterator.lookAhead.get();
            minIterator.peekNext();
            return value;
        }
    }

    // ChatGPT first version (52 lines)
    public static class MergerChatGPT1<T extends Comparable<T>> implements Iterator<T> {

        // A priority queue (min-heap) to store the current element of each iterator, along with the iterator's index
        private static class QueueElement<T> {
            T value;
            Iterator<T> iterator;

            QueueElement(T value, Iterator<T> iterator) {
                this.value = value;
                this.iterator = iterator;
            }
        }

        // Priority queue to store iterators and the current values they are pointing to
        private PriorityQueue<QueueElement<T>> minHeap;

        public MergerChatGPT1(List<Iterator<T>> iterators) {
            minHeap = new PriorityQueue<>(Comparator.comparing(element -> element.value));

            // Initialize the heap with the first element of each iterator (if available)
            for (Iterator<T> iterator : iterators) {
                if (iterator.hasNext()) {
                    minHeap.offer(new QueueElement<>(iterator.next(), iterator));
                }
            }
        }

        // Check if there are more elements to iterate over
        @Override
        public boolean hasNext() {
            return !minHeap.isEmpty();
        }

        // Retrieve the next smallest element in the merged sequence
        @Override
        public T next() {
            if (!hasNext()) {
                throw new NoSuchElementException("No more elements to iterate.");
            }

            // Pop the smallest element from the heap
            QueueElement<T> smallest = minHeap.poll();
            T result = smallest.value;

            // If the iterator has more elements, add the next one to the heap
            if (smallest.iterator.hasNext()) {
                minHeap.offer(new QueueElement<>(smallest.iterator.next(), smallest.iterator));
            }

            return result;
        }
    }

    // ChatGPT version without PriorityQueue (65 lines)
    public static class MergerByChatGPT2<T extends Comparable<T>> implements Iterator<T> {

        private List<Iterator<T>> iterators;  // List of iterators
        private List<T> currentValues;        // List of current elements of each iterator
        private int numIterators;             // Number of iterators

        // Constructor to initialize with a list of iterators
        public MergerByChatGPT2(List<Iterator<T>> iterators) {
            this.iterators = iterators;
            this.numIterators = iterators.size();
            this.currentValues = new ArrayList<>(numIterators);

            // Initialize the current values with the first element of each iterator (if any)
            for (Iterator<T> iterator : iterators) {
                if (iterator.hasNext()) {
                    currentValues.add(iterator.next());
                } else {
                    currentValues.add(null);  // If no element in iterator, add null
                }
            }
        }

        // Check if there are more elements to iterate over
        @Override
        public boolean hasNext() {
            // Check if there is any iterator with a non-null current value
            for (T value : currentValues) {
                if (value != null) {
                    return true;
                }
            }
            return false;
        }

        // Retrieve the next smallest element in the merged sequence
        @Override
        public T next() {
            if (!hasNext()) {
                throw new NoSuchElementException("No more elements to iterate.");
            }

            // Find the iterator with the smallest current value
            int minIndex = -1;
            T minValue = null;

            for (int i = 0; i < numIterators; i++) {
                T value = currentValues.get(i);
                if (value != null && (minValue == null || value.compareTo(minValue) < 0)) {
                    minValue = value;
                    minIndex = i;
                }
            }

            // Now minIndex refers to the iterator that gave the smallest value
            // Advance that iterator
            Iterator<T> minIterator = iterators.get(minIndex);
            if (minIterator.hasNext()) {
                currentValues.set(minIndex, minIterator.next());
            } else {
                currentValues.set(minIndex, null);  // No more elements in this iterator
            }

            return minValue;
        }
    }


    public static void main(String[] args) {
        Iterator<String> i1 = Arrays.asList(new String[]{"abcd", "efg", "hijk", "lmnop"}).iterator();
        Iterator<String> i2 = Arrays.asList(new String[]{"abra", "kadabra"}).iterator();
        Iterator<String> i3 = Arrays.asList(new String[]{"foo", "bar", "qrst", "uvw", "xyz"}).iterator();
        var merged = new Merger<>(Arrays.asList(i1, i2, i3));
        while (merged.hasNext()) {
            System.out.println(merged.next());
        }
    }
}

// Merger class implementing Iterator<T>




