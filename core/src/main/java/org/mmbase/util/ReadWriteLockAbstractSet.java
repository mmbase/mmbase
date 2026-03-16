package org.mmbase.util;

import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;

/**
 * @since MMBase-1.9.7
 * @author Michiel Meeuwissen
 * @param <E>
 */
public class ReadWriteLockAbstractSet<E> extends AbstractSet<E> {

    private final Lock readLock;
    private final Lock writeLock;
    private final Set<E> backing;

    public ReadWriteLockAbstractSet(ReadWriteLock rwLock, Set<E> backing) {
        this(rwLock.readLock(), rwLock.writeLock(), backing);
    }

    public ReadWriteLockAbstractSet(Lock readLock, Lock writeLock, Set<E> backing) {
        this.readLock = readLock;
        this.writeLock = writeLock;
        this.backing = backing;
    }


    @Override
    public Iterator<E> iterator() {
        // Snapshot the keys under read lock, return an iterator that supports remove via the outer map
        readLock.lock();
        final List<E> keys;
        try {
            keys = new ArrayList<>(backing);
        } finally {
            readLock.unlock();
        }
        return new Iterator<E>() {
            private final Iterator<E> it = keys.iterator();
            private E current;
            private boolean canRemove = false;

            @Override
            public boolean hasNext() {
                return it.hasNext();
            }

            @Override
            public E next() {
                current = it.next();
                canRemove = true;
                return current;
            }

            @Override
            public void remove() {
                if (!canRemove) {
                    throw new IllegalStateException("next() has not been called, or element already removed");
                }
                writeLock.lock();
                try {
                    backing.remove(current);
                } finally {
                    writeLock.unlock();
                }
                canRemove = false;
            }
        };
    }

    @Override
    public int size() {
        readLock.lock();
        try {
            return backing.size();
        } finally {
            readLock.unlock();
        }

    }

    @Override
    public boolean contains(Object o) {
        readLock.lock();
        try {
            return backing.contains(o);
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public boolean remove(Object o) {
        writeLock.lock();
        try {
            return backing.remove(o);
        } finally {
            writeLock.unlock();
        }

    }

    @Override
    public void clear() {
        writeLock.lock();
        try {
            backing.clear();
        } finally {
            writeLock.unlock();
        }
    }




}
