package net.burningtnt.crowdinsynchronizer.utils.io;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.*;
import java.util.function.Consumer;

public interface NetIterator<E> {
    boolean hasNext() throws IOException;

    List<E> next() throws NoSuchElementException, IOException;

    int aboutSize() throws IOException;

    default void forEachRemaining(Consumer<? super E> action) throws IOException {
        Objects.requireNonNull(action);
        while (this.hasNext()) {
            for (E e : this.next()) {
                action.accept(e);
            }
        }
    }

    default List<E> collectAsList() throws IOException {
        List<E> list = new ArrayList<>();
        while (this.hasNext()) {
            list.addAll(this.next());
        }
        return Collections.unmodifiableList(list);
    }

    default Set<E> collectAsSet() throws IOException {
        Set<E> set = new HashSet<>();
        while (this.hasNext()) {
            set.addAll(this.next());
        }
        return Collections.unmodifiableSet(set);
    }

    default Iterator<E> wrap() {
        return new Iterator<>() {
            private final LinkedList<E> cache = new LinkedList<>();

            private void refillCache() {
                if (cache.size() != 0) {
                    return;
                }

                try {
                    if (NetIterator.this.hasNext()) {
                        cache.addAll(NetIterator.this.next());
                    }
                } catch (IOException e) {
                    throw new UncheckedIOException(e.getLocalizedMessage(), e);
                }
            }

            @Override
            public boolean hasNext() {
                refillCache();
                return this.cache.size() > 0;
            }

            @Override
            public E next() {
                refillCache();
                if (this.cache.size() == 0) {
                    throw new NoSuchElementException();
                }
                return this.cache.getLast();
            }
        };
    }
}
