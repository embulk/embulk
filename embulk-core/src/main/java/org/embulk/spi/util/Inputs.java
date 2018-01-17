package org.embulk.spi.util;

import java.util.Iterator;
import java.util.NoSuchElementException;
import org.embulk.spi.Buffer;
import org.embulk.spi.FileInput;

public class Inputs {
    private abstract static class AbstractPollIterator<E> implements Iterator<E> {
        private E next;

        protected abstract E poll();

        @Override
        public boolean hasNext() {
            if (next != null) {
                return true;
            } else {
                next = poll();
                return next != null;
            }
        }

        @Override
        public E next() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }
            E l = next;
            next = null;
            return l;
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }
    }

    public static Iterable<Buffer> each(final FileInput input) {
        return new Iterable<Buffer>() {
            public Iterator<Buffer> iterator() {
                return new AbstractPollIterator<Buffer>() {
                    public Buffer poll() {
                        return input.poll();
                    }
                };
            }
        };
    }
}
