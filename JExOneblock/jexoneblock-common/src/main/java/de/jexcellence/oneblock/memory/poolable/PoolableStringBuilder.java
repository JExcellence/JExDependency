package de.jexcellence.oneblock.memory.poolable;

import de.jexcellence.oneblock.memory.ObjectPoolManager;
import org.jetbrains.annotations.NotNull;

/**
 * Poolable StringBuilder
 * 
 * A StringBuilder wrapper that can be reused through object pooling
 * to reduce garbage collection pressure from frequent string operations.
 * 
 * @author JExcellence
 * @version 2.0.0
 * @since 2.0.0
 */
public class PoolableStringBuilder implements ObjectPoolManager.Poolable {
    
    private final StringBuilder builder;
    private static final int DEFAULT_CAPACITY = 256;
    
    public PoolableStringBuilder() {
        this(DEFAULT_CAPACITY);
    }
    
    public PoolableStringBuilder(int capacity) {
        this.builder = new StringBuilder(capacity);
    }
    
    @Override
    public void reset() {
        builder.setLength(0);
        // Trim capacity if it's grown too large
        if (builder.capacity() > DEFAULT_CAPACITY * 4) {
            builder.trimToSize();
        }
    }
    
    // StringBuilder delegation methods
    
    @NotNull
    public PoolableStringBuilder append(@NotNull String str) {
        builder.append(str);
        return this;
    }
    
    @NotNull
    public PoolableStringBuilder append(char c) {
        builder.append(c);
        return this;
    }
    
    @NotNull
    public PoolableStringBuilder append(int i) {
        builder.append(i);
        return this;
    }
    
    @NotNull
    public PoolableStringBuilder append(long l) {
        builder.append(l);
        return this;
    }
    
    @NotNull
    public PoolableStringBuilder append(double d) {
        builder.append(d);
        return this;
    }
    
    @NotNull
    public PoolableStringBuilder append(boolean b) {
        builder.append(b);
        return this;
    }
    
    @NotNull
    public PoolableStringBuilder append(@NotNull Object obj) {
        builder.append(obj);
        return this;
    }
    
    @NotNull
    public PoolableStringBuilder insert(int offset, @NotNull String str) {
        builder.insert(offset, str);
        return this;
    }
    
    @NotNull
    public PoolableStringBuilder delete(int start, int end) {
        builder.delete(start, end);
        return this;
    }
    
    @NotNull
    public PoolableStringBuilder deleteCharAt(int index) {
        builder.deleteCharAt(index);
        return this;
    }
    
    @NotNull
    public PoolableStringBuilder replace(int start, int end, @NotNull String str) {
        builder.replace(start, end, str);
        return this;
    }
    
    @NotNull
    public PoolableStringBuilder reverse() {
        builder.reverse();
        return this;
    }
    
    public int length() {
        return builder.length();
    }
    
    public int capacity() {
        return builder.capacity();
    }
    
    public char charAt(int index) {
        return builder.charAt(index);
    }
    
    @NotNull
    public String substring(int start) {
        return builder.substring(start);
    }
    
    @NotNull
    public String substring(int start, int end) {
        return builder.substring(start, end);
    }
    
    public int indexOf(@NotNull String str) {
        return builder.indexOf(str);
    }
    
    public int lastIndexOf(@NotNull String str) {
        return builder.lastIndexOf(str);
    }
    
    @Override
    @NotNull
    public String toString() {
        return builder.toString();
    }
}