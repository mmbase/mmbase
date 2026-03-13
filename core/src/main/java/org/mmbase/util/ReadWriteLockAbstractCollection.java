package org.mmbase.util;

import java.util.AbstractCollection;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.concurrent.locks.ReadWriteLock;


/**
 * @since MMBase-1.9.7
 * @author Michiel Meeuwissen
 * @param <E>
 */
public class ReadWriteLockAbstractCollection<E> extends AbstractCollection<E> {

    private final ReadWriteLock rwLock;
    private final Collection<E> backing;

    public ReadWriteLockAbstractCollection(ReadWriteLock rwLock, Collection<E> backing) {
        this.rwLock = rwLock;
        this.backing = backing;
    }

    @Override
    public Iterator<E> iterator() {
        // Iterate over a snapshot for stability, but apply removals to the backing collection
        final Iterator<E> entryIt;
        rwLock.readLock().lock();
        try {
            entryIt = new ArrayList<E>(backing).iterator();
        } finally {
            rwLock.readLock().unlock();
        }
        return new Iterator<E>() {
            private E lastReturned = null;
            private boolean canRemove = false;

            @Override
            public boolean hasNext() {
                return entryIt.hasNext();
            }

            @Override
            public E next() {
                E e = entryIt.next();
                lastReturned = e;
                canRemove = true;
                return e;
            }

            @Override
            public void remove() {
                if (!canRemove) {
                    throw new IllegalStateException("next() has not been called, or remove() already called after the last next()");
                }
                rwLock.writeLock().lock();
                try {
                    backing.remove(lastReturned);
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
    public void clear() {
        rwLock.writeLock().lock();
        try {
            backing.clear();
        } finally {
            rwLock.writeLock().unlock();
        }
    }

}
