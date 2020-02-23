package shark.runtime;

import java.util.HashMap;
import java.util.HashSet;

import shark.delegates.Action;
import shark.delegates.Function;
import shark.utils.Log;

public final class Locker {

    private static HashMap<Class<?>, Integer> _mappedTypes = new HashMap<>();
    private HashSet<ITypeScopeDistinguishable> _objects = new HashSet<>();

    private static <T> T _process(Object[] objects, int index, Object handler) {

        synchronized (objects[index]) {
            if (objects.length > index + 1) {
                return _process(objects, index + 1, handler);
            }
            else {
                if (handler instanceof Runnable) {
                    ((Runnable)handler).run();
                    return null;
                }
                else {
                    return ((Function<T>)handler).run();
                }
            }
        }
    }

    private static ITypeScopeDistinguishable[] _sort(HashSet<ITypeScopeDistinguishable> objects) {

        HashMap<ITypeScopeDistinguishable, Integer> buffer1 = new HashMap<>();

        for (ITypeScopeDistinguishable obj : objects) {

            ITypeScopeDistinguishable instance = obj.getTypeScopeUniqueInstance();

            if (instance == null || buffer1.containsKey(instance)) continue;

            Class<?> T = instance.getClass();

            synchronized (_mappedTypes) {
                if (!_mappedTypes.containsKey(T)) _mappedTypes.put(T, _mappedTypes.size());
                buffer1.put(instance, _mappedTypes.get(T));
            }
        }

        ITypeScopeDistinguishable[] result = buffer1.keySet().toArray(new ITypeScopeDistinguishable[0]);

        for (int i = 0; i < result.length - 1; i++) {
            for (int j = i + 1; j < result.length; j++) {
                if (buffer1.get(result[i]) > buffer1.get(result[j]) || result[i].getTypeScopeUniqueIdentifier() > result[j].getTypeScopeUniqueIdentifier()){
                    ITypeScopeDistinguishable buffer2 = result[i];
                    result[i] = result[j];
                    result[j] = buffer2;
                }
            }
        }

        return result;
    }

    public Locker(){
    }

    public void add(ITypeScopeDistinguishable... objects) {
        for (ITypeScopeDistinguishable object : objects) {
            if (object != null) {

                synchronized (_objects) {
                    _objects.add(object);
                }
            }
        }
    }

    public void remove(ITypeScopeDistinguishable... objects) {
        for (ITypeScopeDistinguishable object : objects) {
            if (object != null) {

                synchronized (object) {
                    _objects.remove(object);
                }
            }
        }
    }

    public Locker acquire(ITypeScopeDistinguishable... objects) {
        add(objects);
        return this;
    }

    public Locker release(ITypeScopeDistinguishable... objects) {
        remove(objects);
        return this;
    }

    public Locker then(Action action) {
        if (action == null) throw new IllegalArgumentException();

        synchronized (_objects) {
            try {
                if (_objects.size() == 0) {
                    action.run();
                }
                else {
                    _process(_sort(_objects), 0, action);
                }
            }
            catch (Exception e) {
                Log.error(Locker.class,
                        "Error detected while running action",
                        "Action: " + action.getClass().getName(),
                        "Error: " + e.getMessage(),
                        Log.stringify(e.getStackTrace()));
                throw e;
            }
        }

        return this;
    }

    public <T> T then(final Function<T> function) {

        synchronized (_objects) {
            try {
                if (_objects.size() == 0) {
                    return function.run();
                }
                else {
                    return _process(_sort(_objects), 0, function);
                }
            }
            catch (Exception e) {
                Log.error(Locker.class,
                        "Error detected while running function",
                        "Function: " + function.getClass().getName(),
                        "Error: " + e.getMessage(),
                        Log.stringify(e.getStackTrace()));
                throw e;
            }
        }
    }
}
