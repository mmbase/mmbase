package org.mmbase.util;

import java.util.*;
import java.util.concurrent.locks.ReadWriteLock;

/**
 * @since MMBase-1.9.7
 * @author Michiel Meeuwissen
 * @param <E>
 */
public class ReadWriteLockAbstractSet<E> extends AbstractSet<E> {

    private final ReadWriteLock rwLock;
    private final Set<E> backing;

    public ReadWriteLockAbstractSet(ReadWriteLock rwLock, Set<E> backing) {
        this.rwLock = rwLock;
        this.backing = backing;
    }

    @Override
    public Iterator<E> iterator() {
        // Snapshot the keys under read lock, return an iterator that supports remove via the outer map
        rwLock.readLock().lock();
        final List<E> keys;
        try {
            keys = new ArrayList<>(backing);
        } finally {
            rwLock.readLock().unlock();
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
                rwLock.writeLock().lock();
                try {
                    backing.remove(current);
                } finally {
                    rwLock.writeLock().unlock();
                }
                canRemove = false;
            }
        };
    }

    @Override
    public int size() {
        rwLock.readLock().lock();
        try {
            return backing.size();
        } finally {
            rwLock.readLock().unlock();
        }

    }

    @Override
    public boolean contains(Object o) {
        rwLock.readLock().lock();
        try {
            return backing.contains(o);
        } finally {
            rwLock.readLock().unlock();
        }
    }

    @Override
    public boolean remove(Object o) {
        rwLock.writeLock().lock();
        try {
            return backing.remove(o);
        } finally {
            rwLock.writeLock().unlock();
        }

    }

    @Override
    public void clear() {
        rwLock.writeLock().lock();
        try {
            backing.clear();
        } finally {
            rwLock.writeLock().unlock();
        }
    }




}
