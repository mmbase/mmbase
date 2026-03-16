package org.mmbase.util;

import java.util.AbstractCollection;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;


/**
 * @since MMBase-1.9.7
 * @author Michiel Meeuwissen
 * @param <E>
 */
public class ReadWriteLockAbstractCollection<E> extends AbstractCollection<E> {

    private final Lock readLock;
    private final Lock writeLock;
    private final Collection<E> backing;

    public ReadWriteLockAbstractCollection(ReadWriteLock rwLock, Collection<E> backing) {
        this(rwLock.readLock(), rwLock.writeLock(), backing);
    }
    public ReadWriteLockAbstractCollection(Lock readLock, Lock writeLock, Collection<E> backing) {
        this.readLock = readLock;
        this.writeLock = writeLock;;
        this.backing = backing;
    }

    @Override
    public Iterator<E> iterator() {
        // Iterate over a snapshot for stability, but apply removals to the backing collection
        final Iterator<E> entryIt;
        readLock.lock();
        try {
            entryIt = new ArrayList<E>(backing).iterator();
        } finally {
            readLock.unlock();
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
                writeLock.lock();
                try {
                    backing.remove(lastReturned);
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
    public void clear() {
        writeLock.lock();
        try {
            backing.clear();
        } finally {
            writeLock.unlock();
        }
    }

}
