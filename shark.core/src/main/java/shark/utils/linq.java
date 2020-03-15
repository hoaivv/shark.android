package shark.utils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import shark.delegates.Function;
import shark.delegates.Function1;
import shark.delegates.Function2;

/**
 * Provides LINQ supports
 * @param <T> type of collection data
 */
public final class linq<T> implements Iterable<T> {

    private final Function<Collection<T>> generator;

    @SafeVarargs
    private final T[] sample(T... sample) {
        return sample;
    }

    private linq(Function<Collection<T>> source) {
        this.generator = source;
    }

    /**
     * Integer adder used for {@link #sum(Function1, Function2)}
     * and {@link #average(Function1, Function2, Function2)}
     */
    public static final Function2<Integer, Integer, Integer> IntegerAdder = (a,b) -> (a == null ? 0 : a) + (b == null ? 0 : b);

    /**
     * Integer divider used for {@link #average(Function1, Function2, Function2)}
     */
    public static final Function2<Integer, Integer, Integer> IntegerDivider = (a,b) -> (a == null ? 0 : a) / (b == null ? 0 : b);

    /**
     * Long adder used for {@link #sum(Function1, Function2)}
     * and {@link #average(Function1, Function2, Function2)}
     */
    public static final Function2<Long, Long, Long> LongAdder = (a,b) -> (a == null ? 0 : a) + (b == null ? 0 : b);

    /**
     * Long divider used for {@link #average(Function1, Function2, Function2)}
     */
    public static final Function2<Long, Integer, Long> LongDivider = (a,b) -> (a == null ? 0 : a) / (b == null ? 0 : b);

    /**
     * Float adder used for {@link #sum(Function1, Function2)}
     * and {@link #average(Function1, Function2, Function2)}
     */
    public static final Function2<Float, Float, Float> FloatAdder = (a,b) -> (a == null ? 0 : a) + (b == null ? 0 : b);

    /**
     * Float divider used for {@link #average(Function1, Function2, Function2)}
     */
    public static final Function2<Float, Integer, Float> FloatDivider= (a,b) -> (a == null ? 0 : a) / (b == null ? 0 : b);

    /**
     * Double adder used for {@link #sum(Function1, Function2)}
     * and {@link #average(Function1, Function2, Function2)}
     */
    public static final Function2<Double, Double, Double> DoubleAdder = (a,b) -> (a == null ? 0 : a) + (b == null ? 0 : b);

    /**
     * Double divider used for {@link #average(Function1, Function2, Function2)}
     */
    public static final Function2<Double, Integer, Double> DoubleDivider = (a,b) -> (a == null ? 0 : a) / (b == null ? 0 : b);

    /**
     * Applies LINQ to a collection
     * @param collection collection to be applied
     * @param <T> type of collection element
     * @return instance of {@link linq}
     */
    @SuppressWarnings("WeakerAccess")
    public static <T> linq<T> of(Collection<T> collection) {

        return new linq<>(() -> collection);
    }

    /**
     * Applies LINQ to a collection
     * @param array collection to be applied
     * @param <T> type of collection element
     * @return instance of {@link linq}
     */
    public static <T> linq<T> of(T[] array) {

        Collection<T> collection = new Collection<T>() {
            @Override
            public int size() {
                return array.length;
            }

            @Override
            public boolean isEmpty() {
                return size() == 0;
            }

            @Override
            public boolean contains(Object o) {

                for(T one : array) if (one == o) return true;
                return false;
            }

            @Override
            public Iterator<T> iterator() {

                return new Iterator<T>() {

                    private int index = 0;

                    @Override
                    public boolean hasNext() {
                        synchronized (this) {
                            return index < array.length;
                        }
                    }

                    @Override
                    public T next() {
                        synchronized (this) {
                            return array[index++];
                        }
                    }
                };
            }

            @Override
            public Object[] toArray() {
                return array;
            }

            @Override
            public <T1> T1[] toArray(T1[] a) {
                //noinspection unchecked
                return (T1[])array;
            }

            @Override
            public boolean add(T t) {
                return false;
            }

            @Override
            public boolean remove(Object o) {
                return false;
            }

            @Override
            public boolean containsAll(Collection<?> c) {

                for(Object one : c) if (!contains(one)) return false;
                return true;
            }

            @Override
            public boolean addAll(Collection<? extends T> c) {
                return false;
            }

            @Override
            public boolean removeAll(Collection<?> c) {
                return false;
            }

            @Override
            public boolean retainAll(Collection<?> c) {
                return false;
            }

            @Override
            public void clear() {
                throw new UnsupportedOperationException();
            }
        };

        return new linq<>(() -> collection);
    }

    /**
     * Applies LINQ to a collection
     * @param map collection to be applied
     * @param <K> key type of the collection
     * @param <V> value type of the collection
     * @return instance of {@link linq}
     */
    public static <K,V> linq<Map.Entry<K,V>> of(Map<K,V> map) {

        return new linq<>(map::entrySet);
    }

    /**
     * Converts each of the collection elements to an element of a specified type
     * @param selector function, provides element conversion operation
     * @param <E> type of data, to which the collection elements to be converted
     * @return instance of {@link linq} which manages a collection of converted elements
     */
    public <E> linq<E> select(Function1<T, E> selector) {

        return new linq<>(() -> {

            ArrayList<E> results = new ArrayList<>();

            synchronized (generator) {
                for (T one : this.generator.run()) results.add(selector.run(one));
            }

            return results;
        });
    }

    /**
     * Converts each of collection elements to a multiple elements of a specified type
     * @param selector function, provides element conversion operation
     * @param <E> type of data, to which the collection elements to be converted
     * @return instance of {@link linq} which manages a collection of converted elements
     */
    public <E> linq<E> selectMany(Function1<T, Iterable<E>> selector) {

        return new linq<>(() -> {

            ArrayList<E> results = new ArrayList<>();

            synchronized (generator) {
                for (T one : this.generator.run()) {
                    for (E result : selector.run(one)) results.add(result);
                }
            }

            return results;
        });
    }

    /**
     * Reverses the element order of the collection
     * @return instance of {@link linq} which manages the reversed collection
     */
    @SuppressWarnings("WeakerAccess")
    public linq<T> reverse() {

        return new linq<>(() -> {

            LinkedList<T> results = new LinkedList<>();

            synchronized (generator){
                for(T one : generator.run()) results.addFirst(one);
            }

            return results;
        });
    }

    /**
     * Filters collection elements
     * @param predicate function, which determines whether an element should be filtered or not
     * @return instance of {@link linq} which manages the filtered collection
     */
    public linq<T> where(Function1<T, Boolean> predicate) {

        return new linq<>(() -> {

            ArrayList<T> results = new ArrayList<>();

            synchronized (generator) {
                for (T one : this.generator.run()) if (predicate.run(one)) results.add(one);
            }

            return results;
        });
    }

    /**
     * Filters collection elements
     * @param predicate function, which determines whether an element should be filtered or not
     * @return instance of {@link linq} which manages the filtered collection
     */
    public linq<T> where(Function2<Integer, T, Boolean> predicate) {

        return new linq<>(() -> {

            ArrayList<T> results = new ArrayList<>();
            int index = 0;

            synchronized (generator) {
                for (T one : this.generator.run()) if (predicate.run(index++, one)) results.add(one);
            }

            return results;
        });

    }

    /**
     * Sorts collection elements in ascending order
     * @param selector function, provides the value used for sorting operation
     * @param <R> type of data used for sorting operation
     * @return instance of {@link linq} which manages the sorted collection
     */
    public <R extends Comparable<R>> linq<T> orderBy(Function1<T, R> selector) {

        return new linq<>(() -> {

            ArrayList<T> values;

            synchronized (generator) {
                values = new ArrayList<>(generator.run());
            }

            for (int i = 0; i < values.size() - 1; i++) {

                for(int j = i + 1; j < values.size(); j++) {

                    if (selector.run(values.get(i)).compareTo(selector.run(values.get(j))) > 0) {

                        T buffer = values.get(i);
                        values.set(i, values.get(j));
                        values.set(j, buffer);
                    }
                }
            }

            return values;
        });
    }

    /**
     * Sorts collection elements in descending order
     * @param selector function, provides the value used for sorting operation
     * @param <R> type of data used for sorting operation
     * @return instance of {@link linq} which manages the sorted collection
     */
    public <R extends Comparable<R>> linq<T> orderByDescending(Function1<T, R> selector) {

        return new linq<>(() -> {

            ArrayList<T> values;

            synchronized (generator) {
                values = new ArrayList<>(generator.run());
            }

            for (int i = 0; i < values.size() - 1; i++) {

                for(int j = i + 1; j < values.size(); j++) {

                    if (selector.run(values.get(i)).compareTo(selector.run(values.get(j))) < 0) {

                        T buffer = values.get(i);
                        values.set(i, values.get(j));
                        values.set(j, buffer);
                    }
                }
            }

            return values;
        });
    }

    /**
     * Groups elements of the collection
     * @param selector function, provides the value used for grouping operation
     * @param <R> type of data used for grouping operation
     * @return instance of {@link linq} which manages the grouped collection
     */
    public <R> linq<Map.Entry<R,linq<T>>> groupBy(Function1<T,R> selector) {

        return new linq<>(() -> {

            HashMap<R, linq<T>> results = new HashMap<>();

            synchronized (generator) {

                for(T one : generator.run()) {

                    R key = selector.run(one);

                    if (!results.containsKey(key)) {
                        results.put(key, linq.of(new ArrayList<>()));
                    }

                    //noinspection ConstantConditions
                    results.get(key).generator.run().add(one);
                }
            }

            return results.entrySet();
        });
    }

    /**
     * Gets first element of the collection
     * @return first element of the collection
     * @exception NoSuchElementException throws if the collection is empty
     */
    @SuppressWarnings("WeakerAccess")
    public T first() {

        synchronized (generator) {
            return generator.run().iterator().next();
        }
    }

    /**
     * Gets first element of the collection, which satisfied a specified condition
     * @param predicate function, determines whether a element is satisfied the condition or not
     * @return first element which satisfied the condition
     * @throws NoSuchElementException throws if no elements satisfied the condition
     */
    @SuppressWarnings("WeakerAccess")
    public T first(Function1<T, Boolean> predicate) {

        synchronized (generator) {

            for(T one : generator.run()) {
               if (predicate.run(one)) return one;
            }
        }

        throw new NoSuchElementException();
    }

    /**
     * Gets first element of the collection
      * @return first element of the collection or null if the collection is empty
     */
    @SuppressWarnings("WeakerAccess")
    public T firstOrDefault() {

        synchronized (generator) {

            Iterator<T> iterator = generator.run().iterator();
            return iterator.hasNext() ? iterator.next() : null;
        }
    }

    /**
     * Gets first element of the collection, which satisfied a specified condition
     * @param predicate function, determines whether an element is satisfied the condition or not
     * @return first element which satisfied the condition or null if no elements satisfied the
     * condition
     */
    @SuppressWarnings("WeakerAccess")
    public T firstOrDefault(Function1<T, Boolean> predicate) {

        synchronized (generator) {

            for(T one : generator.run()) {
                if (predicate.run(one)) return one;
            }
        }

        return null;
    }

    /**
     * Gets last element of the collection
     * @return last element of the collection
     * @exception NoSuchElementException throws if the collection is empty
     */
    public T last() {

        return reverse().first();
    }

    /**
     * Gets last element of the collection which satisfied a specified condition
     * @param predicate function, determines whether an element is satisfied the condition or not
     * @return last element of the collection, which satisfied the condition
     * @exception NoSuchElementException throws if no elements satisfied the condition
     */
    public T last(Function1<T, Boolean> predicate) {

        return reverse().first(predicate);
    }

    /**
     * Gets last element of the collection
     * @return last element of the collection or null if the collection is empty
     */
    public T lastOrDefault() {

        return reverse().firstOrDefault();
    }

    /**
     * Gets last element of the collection which satisfied a specified condition
     * @param predicate function, determines whether an element is satisfied the condition or not
     * @return last element of the collection, which satisfied the condition or null if no elements
     * satisfied the condition
     */
    public T lastOrDefault(Function1<T, Boolean> predicate) {

        return reverse().firstOrDefault(predicate);
    }

    /**
     * Gets an element at a specified index from the collection
     * @param index index of the element
     * @return element at the specified index
     * @exception NoSuchElementException throws if the index is out of collection bounds
     */
    public T get(int index) {

        synchronized (generator) {

            Collection<T> collection = generator.run();

            if (index < 0 || index >= collection.size()) throw new NoSuchElementException();

            if (collection instanceof List) return ((List<T>)collection).get(index);

            Iterator<T> iterator = collection.iterator();

            T result;

            do {
                result = iterator.next();
            }
            while (--index > -1);

            return result;
        }
    }

    /**
     * Gets an element of the collection at a specified index
     * @param index index of the element
     * @return element at the specified index or null if the index is out of collection bounds
     */
    public T getOrDefault(int index) {

        synchronized (generator) {

            Collection<T> collection = generator.run();

            if (index < 0 || index >= collection.size()) return null;

            if (collection instanceof List) return ((List<T>)collection).get(index);

            Iterator<T> iterator = collection.iterator();

            T result;

            do {
                result = iterator.next();
            }
            while (--index > -1);

            return result;
        }
    }

    /**
     * Checks whether the collection contains a specified element or not
     * @param o element to be checked
     * @return true if the provided element is contained in the collection; otherwise false
     */
    public boolean contains(T o) {

        synchronized (generator) {

            return generator.run().contains(o);
        }
    }

    /**
     * Removes a number of elements at the beginning of the collection
     * @param count number of elements to be skipped
     * @return instance of {@link linq} manages the truncated collection
     */
    public linq<T> skip(int count) {

        return new linq<>(() -> {
            ArrayList<T> buffer = new ArrayList<>();

            int skipped = 0;

            synchronized (generator) {

                for (T one : generator.run()) {
                    if (skipped++ < count) continue;
                    buffer.add(one);
                }
            }

            return buffer;
        });
    }

    /**
     * Removes elements at the beginning of the collection until a specified condition reached
     * @param predicate function, determines whether an element should be removed or not
     * @return instance of {@link linq} manages the truncated collection
     */
    public linq<T> skipWhile(Function1<T,Boolean> predicate) {

        return new linq<>(() -> {
            ArrayList<T> buffer = new ArrayList<>();

            boolean skip = true;

            synchronized (generator) {

                for(T one : generator.run()) {
                    if (skip) skip = predicate.run(one);
                    if (!skip) buffer.add(one);
                }
            }

            return buffer;
        });
    }

    /**
     * Removes elements at the beginning of the collection until a specified condition reached
     * @param predicate function, determines whether an element should be removed or not
     * @return instance of {@link linq} manages the truncated collection
     */
    public linq<T> skipWhile(Function2<Integer, T, Boolean> predicate) {

        return new linq<>(() -> {
            ArrayList<T> buffer = new ArrayList<>();

            boolean skip = true;
            int index = 0;

            synchronized (generator) {

                for(T one : generator.run()) {
                    if (skip) skip = predicate.run(index++, one);
                    if (!skip) buffer.add(one);
                }
            }

            return buffer;
        });
    }

    /**
     * Gets a number of elements at the beginning of the collection
     * @param count number of elements
     * @return instance of {@link linq} manages the collection of gotten elements
     */
    public linq<T> take(int count) {

        return new linq<>(() -> {
            ArrayList<T> buffer = new ArrayList<>();

            int taken = 0;

            synchronized (generator) {

                for (T one : generator.run()) {
                    if (taken++ >= count) break;
                    buffer.add(one);
                }
            }

            return buffer;
        });
    }

    /**
     * Gets elements at the beginning of the collection until a specified condition reached
     * @param predicate function, determines whether an element should be gotten or not
     * @return instance of {@link linq} manages the collection of gotten elements
     */
    public linq<T> takeWhile(Function1<T, Boolean> predicate) {

        return new linq<>(() -> {
            ArrayList<T> buffer = new ArrayList<>();

            synchronized (generator) {

                for(T one : generator.run()) {

                    if (!predicate.run(one)) break;
                    buffer.add(one);
                }
            }

            return buffer;
        });
    }

    /**
     * Gets elements at the beginning of the collection until a specified condition reached
     * @param predicate function, determines whether an element should be gotten or not
     * @return instance of {@link linq} manages the collection of gotten elements
     */
    public linq<T> takeWhile(Function2<Integer, T, Boolean> predicate) {

        return new linq<>(() -> {
            ArrayList<T> buffer = new ArrayList<>();
            int index = 0;

            synchronized (generator) {

                for(T one : generator.run()) {

                    if (!predicate.run(index++, one)) break;
                    buffer.add(one);
                }
            }

            return buffer;
        });
    }

    /**
     * Gets a minimum value of a specified type from the collection
     * @param selector function, which extract the value to be used in the operation
     * @param <R> type of value, minimum value of which to be retrieved
     * @return minimum value
     * @exception NoSuchElementException throws if the collection is empty
     */
    public <R extends Comparable<R>> R min(Function1<T, R> selector) {

        R min = null;

        synchronized (generator) {

            for (T one : generator.run()) {

                R candidate = selector.run(one);
                if (candidate == null) continue;
                if (min == null || candidate.compareTo(min) < 0) min = candidate;
            }
        }

        if (min == null) throw new NoSuchElementException();

        return min;
    }

    /**
     * Gets a minimum value of a specified type from the collection
     * @param selector function, which extract the value to be used in the operation
     * @param <R> type of value, minimum value of which to be retrieved
     * @return minimum value or null if the collection is empty
     */
    public <R extends Comparable<R>> R minOrDefault(Function1<T, R> selector) {

        R min = null;

        synchronized (generator) {

            for (T one : generator.run()) {

                R candidate = selector.run(one);
                if (candidate == null) continue;
                if (min == null || candidate.compareTo(min) < 0) min = candidate;
            }
        }

        return min;
    }

    /**
     * Gets a maximum value of a specified type from the collection
     * @param selector function, which extract the value to be used in the operation
     * @param <R> type of value, maximum value of which to be retrieved
     * @return maximum value
     * @exception NoSuchElementException throws if the collection is empty
     */
    public <R extends Comparable<R>> R max(Function1<T, R> selector) {

        R max = null;

        synchronized (generator) {

            for (T one : generator.run()) {

                R candidate = selector.run(one);
                if (candidate == null) continue;
                if (max == null || candidate.compareTo(max) > 0) max = candidate;
            }
        }

        if (max == null) throw new NoSuchElementException();

        return max;
    }

    /**
     * Gets a maximum value of a specified type from the collection
     * @param selector function, which extract the value to be used in the operation
     * @param <R> type of value, maximum value of which to be retrieved
     * @return maximum value or null if the collection is empty
     */
    public <R extends Comparable<R>> R maxOrDefault(Function1<T, R> selector) {

        R max = null;

        synchronized (generator) {

            for (T one : generator.run()) {

                R candidate = selector.run(one);
                if (candidate == null) continue;
                if (max == null || candidate.compareTo(max) > 0) max = candidate;
            }
        }

        return max;
    }

    /**
     * Gets summary of a specified value type from the collection
     * @param selector function, which extract the value to be used in the operation
     * @param adder function, which provides adding operation between the value of specified type
     * @param <R> type of value, summary of which to be retrieved
     * @return summary of extracted values
     */
    public <R> R sum(Function1<T, R> selector, Function2<R,R,R> adder) {

        R sum = adder.run(null, null);

        synchronized (generator) {
            for(T one : generator.run()) sum = adder.run(sum, selector.run(one));
        }

        return sum;
    }

    /**
     * Gets average of a specified value type from the collection
     * @param selector function, which extract the value to be used in the operation
     * @param adder function, which provides adding operation between the value of specified type
     * @param divider function, which provides division operation between the value of specified
     *                type and an {@link Integer}
     * @param <R> type of value, average of which to be retrieved
     * @return average of extracted values
     */
    public <R> R average(Function1<T, R> selector, Function2<R,R,R> adder, Function2<R,Integer,R> divider) {

        R sum = adder.run(null, null);
        int count = 0;

        synchronized (generator) {
            for(T one : generator.run()) {
                sum = adder.run(sum, selector.run(one));
                count++;
            }
        }

        return count > 0 ? divider.run(sum, count) : sum;
    }

    /**
     * Gets collection iterator
     * @return collection iterator
     */
    public Iterator<T> iterator() {
        synchronized (generator) {
            return generator.run().iterator();
        }
    }

    /**
     * Gets number of elements satisfies a specified condition
     * @param predicate function, determines whether an element satisfies the condition or not
     * @return number of elements satisfies the condition
     */
    public int count(Function1<T, Boolean> predicate) {

        int count = 0;

        synchronized (generator) {
            for (T one : generator.run()) if (predicate.run(one)) count++;
        }

        return count;
    }

    /**
     * Gets number of elements of the collection
     * @return number of elements of the collection
     */
    public int count() {

        synchronized (generator) {
            return generator.run().size();
        }
    }

    /**
     * Converts the managed collection to a {@link HashMap}
     * @param keySelector function, which extracts key used to generate {@link HashMap} from
     *                    an element
     * @param valueSelector function, which extracts value to generate {@link HashMap} from
     *                      an element
     * @param <K> type of key
     * @param <V> type of value
     * @return instance of {@link HashMap}
     */
    public <K,V> HashMap<K,V> toHashMap(Function1<T,K> keySelector, Function1<T,V> valueSelector) {

        HashMap<K,V> results = new HashMap<>();

        synchronized (generator) {
            for (T one : generator.run()) results.put(keySelector.run(one), valueSelector.run(one));
        }

        return results;
    }

    /**
     * Converts the managed collection to a {@link HashSet}
     * @return instance of {@link HashSet}
     */
    public HashSet<T> toHashSet() {

        synchronized (generator) {
            return new HashSet<>(generator.run());
        }
    }

    /**
     * Converts the managed collection to an array
     * @return instance of {@link T[]}
     */
    public T[] toArray() {

        synchronized (generator) {
            //noinspection unchecked
            return generator.run().toArray(sample());
        }
    }

    /**
     * Unites the managed collection with a specified collection. The united collection will have
     * all elements of the managed collection and the specified collection. Duplicated elements from
     * source collections will be added only once to the united collection.
     * @param collection collection to be united with the managed collection
     * @return instance of {@link linq} which manages the united collection
     */
    public linq<T> union(Iterable<T> collection) {

        return new linq<>(() -> {

            HashSet<T> united = new HashSet<>(generator.run());

            for (T one : collection) united.add(one);

            return united;
        });
    }

    /**
     * Appends a specified collection to the end of the managed collection
     * @param collection collection to be appended to the end of the managed collection
     * @return instance of {@link linq} manages the appended collection
     */
    public linq<T> append(Iterable<T> collection) {

        return new linq<>(() -> {

            ArrayList<T> united = new ArrayList<>(generator.run());
            for (T one : collection) united.add(one);

            return united;
        });
    }

    /**
     * Appends a specified collection to the beginning of the managed collection
     * @param collection collection to be appended to the beginning of the managed collection
     * @return instance of {@link linq} manages the appended collection
     */
    public linq<T> prepend(Iterable<T> collection) {

        return new linq<>(() -> {

            ArrayList<T> united = new ArrayList<>();
            for (T one : collection) united.add(one);
            united.addAll(generator.run());

            return united;
        });
    }

    /**
     * Gets elements, available in all source collections. Source collections are the managed
     * collection of the {@link linq} object and a specified collection.
     * @param collection collection used to compare to the managed collection
     * @return instance of {@link linq} manages the collection consists of elements, available in
     * both of the source collections
     */
    public linq<T> similarities(Iterable<T> collection) {

        return new linq<>(() -> {

            HashSet<T> lookup = new HashSet<>(generator.run());
            HashSet<T> cross = new HashSet<>();

            for (T one : collection) if (lookup.contains(one)) cross.add(one);

            return cross;
        });
    }

    /**
     * Gets elements, available in only one of the source collections. Source collections are the managed
     * collection of the {@link linq} object and a specified collection.
     * @param collection collection used to compare to the managed collection
     * @return instance of {@link linq} manages the collection consists of elements, available in
     * only one of the source collections
     */
    public linq<T> differences(Iterable<T> collection) {

        return new linq<>(() -> {

            HashSet<T> lookup = new HashSet<>(generator.run());
            HashSet<T> cross = new HashSet<>();
            HashSet<T> diff = new HashSet<>();

            for (T one : collection) {
                if (lookup.contains(one)) {
                    cross.add(one);
                }
                else {
                    diff.add(one);
                }
            }

            for(T one : lookup) {
                if (!cross.contains(one)) diff.add(one);
            }

            return diff;
        });
    }
}
