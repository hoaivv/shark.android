package shark.runtime;

import java.util.ArrayList;
import java.util.HashMap;

import shark.Framework;
import shark.components.IServiceHandler;
import shark.components.SharkService;
import shark.components.SharkServiceAlternativeName;
import shark.utils.Log;

public final class Services {

    private Services() {
    }

    private static Services signature = new Services();

    private static HashMap<Integer, Service> _id2service = new HashMap<>();
    private static HashMap<String, Service> _key2service = new HashMap<>();
    private static HashMap<String, String> _alt2org = new HashMap<>();

    private static int _nexRegistrationId = 1;

    public static int register(IServiceHandler handler){

        synchronized (signature) {

            if (handler == null) {
                if (Framework.debug) Log.warning(Services.class,
                        "Invalid registration",
                        "Issue: attempt to register null as a service");

                return 0;
            }

            SharkService nameAnnotation = handler.getClass().getAnnotation(SharkService.class);

            if (nameAnnotation == null) {
                if (Framework.debug) Log.warning(Services.class,
                        "Invalid registration",
                        "Issue: SharkService annotation not found",
                        "Class: " + handler.getClass().getName());

                return 0;
            }

            String name = nameAnnotation.value();

            if (name == null || name.length() == 0) {
                if (Framework.debug) Log.warning(Services.class,
                        "Invalid registration",
                        "Issue: service name is null or empty",
                        "Class: " + handler.getClass().getName());

                return 0;
            }

            Class<?> dataClass = handler.getDataClass();

            if (dataClass == null) {
                if (Framework.debug) Log.warning(Services.class,
                        "Invalid registration",
                        "Issue: data type is null",
                        "Class: " + handler.getClass().getName());

                return 0;
            }

            String key = name + dataClass.getName() + dataClass.getPackage().getName();

            if (_key2service.containsKey(key)) {

                if (_key2service.get(key).getHandlerClass() != handler.getClass()) {

                    if (Framework.debug) Log.warning(Services.class,
                            "Invalid registration",
                            "Issue: Service name is already allocated for class " + _key2service.get(key).getHandlerClass(),
                            "Class: " + handler.getClass().getName());

                    return 0;
                }
                else {

                    if (Framework.debug) Log.warning(Services.class,
                            "Invalid registration",
                            "Issue: duplicated registration",
                            "Class: " + handler.getClass().getName());

                    return _key2service.get(key).getId();
                }
            }

            ArrayList<String> registeredAlts = new ArrayList<>();

            SharkServiceAlternativeName alternativeAnnotation = handler.getClass().getAnnotation(SharkServiceAlternativeName.class);

            if (alternativeAnnotation != null) {

                String[] alts = alternativeAnnotation.value();

                if (alts == null) alts = new String[0];

                for (String alt : alts) {

                    if (_alt2org.containsKey(alt) && _alt2org.get(alt) != name) {

                        if (Framework.debug) Log.warning(Services.class,
                                "Invalid registration",
                                "Issue: Alternative name [" + alt + "] is occupied by [" + _alt2org.get(alt) + "]");

                        continue;
                    }

                    _alt2org.put(alt, name);
                    registeredAlts.add(alt);

                    if (Framework.debug) Log.information(Services.class,
                            "Alternative name [" + alt + "] is registered for [" + name + "]");
                }
            }

            Service service = new Service(_nexRegistrationId, name, registeredAlts.toArray(new String[0]), handler);

            _key2service.put(key, service);
            _id2service.put(_nexRegistrationId, service);

            if (Framework.debug) Log.information(Services.class,
                    "Service registered",
                    "Id: " + _nexRegistrationId,
                    "Name: " + name,
                    "Class: " + handler.getClass().getName());

            return _nexRegistrationId++;
        }
    }

    public static Service get(final int id) {

        synchronized (signature) {
            return _id2service.containsKey(id) ? _id2service.get(id) : null;
        }
    }

    public static Service get(String name, Class<?> dataClass) {

        if (name == null || name.length() == 0 || dataClass == null) return null;

        final String key = name + dataClass.getName() + dataClass.getPackage().getName();

        synchronized (signature) {
            return _key2service.containsKey(key) ? _key2service.get(key) : null;
        }
    }

    public static Service[] get(String name) {

        if (name == null || name.length() == 0) return new Service[0];

        ArrayList<Service> results = new ArrayList<>();
        for (Service s : getAll()) if (s.getName() == name) results.add(s);

        return results.toArray(new Service[0]);
    }

    public static Service[] getAll() {

        synchronized (signature) {
            return _id2service.values().toArray(new Service[0]);
        }
    }

    public static String getOriginalName(final String alternative) {

        if (alternative == null || alternative.length() == 0) return alternative;

        synchronized (signature) {
            return _alt2org.containsKey(alternative) ? _alt2org.get(alternative) : alternative;
        }
    }
}
