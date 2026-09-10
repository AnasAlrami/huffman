package application;

public class MinPQ<E extends Comparable<E>> {

    private E[] storage;
    private int count;

    @SuppressWarnings("unchecked")
    public MinPQ() {
        storage = (E[]) new Comparable[4];
        count = 0;
    }

    /** Double the backing array when capacity is exhausted. */
    @SuppressWarnings("unchecked")
    private void expand() {
        E[] larger = (E[]) new Comparable[storage.length * 2];
        System.arraycopy(storage, 1, larger, 1, count);
        storage = larger;
    }

    /** Insert an element into the queue. Null values are ignored. */
    public void insert(E item) {
        if (item == null) return;
        if (count + 1 >= storage.length) expand();
        storage[++count] = item;
        bubbleUp(count);
    }

    /** Remove and return the smallest element, or null if empty. */
    public E extractMin() {
        if (count == 0) return null;
        E minimum = storage[1];
        exchange(1, count);
        storage[count--] = null;
        bubbleDown(1);
        return minimum;
    }

    /** Restore heap property upward from position pos. */
    private void bubbleUp(int pos) {
        while (pos > 1 && isLarger(pos / 2, pos)) {
            exchange(pos / 2, pos);
            pos /= 2;
        }
    }

    /** Restore heap property downward from position pos. */
    private void bubbleDown(int pos) {
        while (2 * pos <= count) {
            int smaller = 2 * pos;
            if (smaller < count && isLarger(smaller, smaller + 1))
                smaller++;
            if (!isLarger(pos, smaller)) break;
            exchange(pos, smaller);
            pos = smaller;
        }
    }

    /** Returns true if element at index a is greater than element at index b. */
    private boolean isLarger(int a, int b) {
        return storage[a].compareTo(storage[b]) > 0;
    }

    /** Swap two elements in the backing array. */
    private void exchange(int a, int b) {
        E temp = storage[a];
        storage[a] = storage[b];
        storage[b] = temp;
    }

    /** Number of elements currently in the queue. */
    public int count() {
        return count;
    }

    /** True if the queue contains no elements. */
    public boolean empty() {
        return count == 0;
    }
}

