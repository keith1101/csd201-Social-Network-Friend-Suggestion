package model;

public class MyMaxHeap {
    private SuggestedFriend[] heap;
    private int size;
    private int capacity;

    public MyMaxHeap(int capacity) {
        this.capacity = capacity;
        this.heap = new SuggestedFriend[capacity];
        this.size = 0;
    }

    public boolean isEmpty() {
        return size == 0;
    }

    public void insert(SuggestedFriend item) {
        if (size >= capacity) {
            return;
        }
        heap[size] = item;
        heapifyUp(size);
        size++;
    }

    private void heapifyUp(int index) {
        int parentIndex = (index - 1) / 2;
        while (index > 0 && heap[index].getMutualCount() > heap[parentIndex].getMutualCount()) {
            swap(index, parentIndex);
            index = parentIndex;
            parentIndex = (index - 1) / 2;
        }
    }

    public SuggestedFriend extractMax() {
        if (isEmpty()) {
            return null;
        }
        SuggestedFriend maxItem = heap[0];
        heap[0] = heap[size - 1];
        heap[size - 1] = null;
        size--;
        if (size > 0) {
            heapifyDown(0);
        }
        return maxItem;
    }

    private void heapifyDown(int index) {
        while (true) {
            int leftChild = 2 * index + 1;
            int rightChild = 2 * index + 2;
            int largest = index;

            if (leftChild < size && heap[leftChild].getMutualCount() > heap[largest].getMutualCount()) {
                largest = leftChild;
            }
            if (rightChild < size && heap[rightChild].getMutualCount() > heap[largest].getMutualCount()) {
                largest = rightChild;
            }
            if (largest == index) {
                break;
            }
            swap(index, largest);
            index = largest;
        }
    }

    private void swap(int i, int j) {
        SuggestedFriend temp = heap[i];
        heap[i] = heap[j];
        heap[j] = temp;
    }
}