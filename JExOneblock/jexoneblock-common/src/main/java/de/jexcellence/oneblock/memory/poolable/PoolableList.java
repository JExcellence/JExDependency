package de.jexcellence.oneblock.memory.poolable;

import de.jexcellence.oneblock.memory.ObjectPoolManager;
import org.jetbrains.annotations.NotNull;

import java.util.*;

/**
 * Poolable List
 * 
 * A List wrapper that can be reused through object pooling
 * to reduce garbage collection pressure from frequent list operations.
 * 
 * @author JExcellence
 * @version 2.0.0
 * @since 2.0.0
 */
public class PoolableList<T> implements ObjectPoolManager.Poolable, List<T> {
    
    private final List<T> list;
    private static final int DEFAULT_CAPACITY = 16;
    
    public PoolableList() {
        this(DEFAULT_CAPACITY);
    }
    
    public PoolableList(int capacity) {
        this.list = new ArrayList<>(capacity);
    }
    
    @Override
    public void reset() {
        list.clear();
        // Trim capacity if it's grown too large
        if (list instanceof ArrayList) {
            ArrayList<T> arrayList = (ArrayList<T>) list;
            if (arrayList.size() == 0 && arrayList instanceof ArrayList) {
                arrayList.trimToSize();
            }
        }
    }
    
    // List interface implementation
    
    @Override
    public int size() {
        return list.size();
    }
    
    @Override
    public boolean isEmpty() {
        return list.isEmpty();
    }
    
    @Override
    public boolean contains(Object o) {
        return list.contains(o);
    }
    
    @Override
    @NotNull
    public Iterator<T> iterator() {
        return list.iterator();
    }
    
    @Override
    @NotNull
    public Object[] toArray() {
        return list.toArray();
    }
    
    @Override
    @NotNull
    public <T1> T1[] toArray(@NotNull T1[] a) {
        return list.toArray(a);
    }
    
    @Override
    public boolean add(T t) {
        return list.add(t);
    }
    
    @Override
    public boolean remove(Object o) {
        return list.remove(o);
    }
    
    @Override
    public boolean containsAll(@NotNull Collection<?> c) {
        return list.containsAll(c);
    }
    
    @Override
    public boolean addAll(@NotNull Collection<? extends T> c) {
        return list.addAll(c);
    }
    
    @Override
    public boolean addAll(int index, @NotNull Collection<? extends T> c) {
        return list.addAll(index, c);
    }
    
    @Override
    public boolean removeAll(@NotNull Collection<?> c) {
        return list.removeAll(c);
    }
    
    @Override
    public boolean retainAll(@NotNull Collection<?> c) {
        return list.retainAll(c);
    }
    
    @Override
    public void clear() {
        list.clear();
    }
    
    @Override
    public T get(int index) {
        return list.get(index);
    }
    
    @Override
    public T set(int index, T element) {
        return list.set(index, element);
    }
    
    @Override
    public void add(int index, T element) {
        list.add(index, element);
    }
    
    @Override
    public T remove(int index) {
        return list.remove(index);
    }
    
    @Override
    public int indexOf(Object o) {
        return list.indexOf(o);
    }
    
    @Override
    public int lastIndexOf(Object o) {
        return list.lastIndexOf(o);
    }
    
    @Override
    @NotNull
    public ListIterator<T> listIterator() {
        return list.listIterator();
    }
    
    @Override
    @NotNull
    public ListIterator<T> listIterator(int index) {
        return list.listIterator(index);
    }
    
    @Override
    @NotNull
    public List<T> subList(int fromIndex, int toIndex) {
        return list.subList(fromIndex, toIndex);
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PoolableList<?> that = (PoolableList<?>) o;
        return Objects.equals(list, that.list);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(list);
    }
    
    @Override
    public String toString() {
        return list.toString();
    }
}