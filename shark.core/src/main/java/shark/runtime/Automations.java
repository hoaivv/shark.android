package shark.runtime;

import java.util.ArrayList;
import java.util.HashMap;

import shark.Framework;
import shark.components.IAutomation;
import shark.utils.Log;

public final class Automations {

    private Automations(){
    }

    private static HashMap<Class<?>, IAutomation> registeredAutomation = new HashMap<>();

    public static IAutomation[] getAll() {

        synchronized (registeredAutomation) {
            return registeredAutomation.values().toArray(new IAutomation[0]);
        }
    }

    public static IAutomation[] getRunningAutomations() {

        ArrayList<IAutomation> results = new ArrayList<>();
        for (IAutomation one : getAll()) if (one.isRunning()) results.add(one);

        return results.toArray(new IAutomation[0]);
    }

    public static boolean hasRunningAutomation() {

        for (IAutomation one : getAll()) if (one.isRunning()) return  true;
        return false;
    }

    public static int getRunningAutomationCount() {

        int count = 0;
        for (IAutomation one : getAll()) if (one.isRunning()) count++;
        return count;
    }

    public static boolean isRegistered(IAutomation automation) {

        if (automation == null) return false;
        Class<? extends IAutomation> cls = automation.getClass();

        synchronized (registeredAutomation) {
            if (!registeredAutomation.containsKey(cls)) return false;
            return automation == registeredAutomation.get(cls);
        }
    }

    public static boolean isRegistered(Worker worker) {

        if (worker == null || !IAutomation.class.isAssignableFrom(worker.getClass())) return false;

        Class<?> cls = worker.getClass();

        synchronized (registeredAutomation) {
            if (!registeredAutomation.containsKey(cls)) return false;
            return worker == registeredAutomation.get(cls);
        }
    }

    public static IAutomation get(final Class<? extends IAutomation> cls) {

        synchronized (registeredAutomation) {
            return registeredAutomation.containsKey(cls) ? registeredAutomation.get(cls) : null;
        }
    }

    public static void register(IAutomation automation) {

        if (automation == null) {
            if (Framework.debug) Log.warning(Automations.class,
                    "Invalid registration",
                    "Issue: attempt to register null as an automation");
        }
        else {

            Class<? extends IAutomation> cls = automation.getClass();

            synchronized (registeredAutomation) {
                if (registeredAutomation.containsKey(cls)) {
                    if (Framework.debug) Log.information(Automations.class,
                            "Invalid registration",
                            "Issue: duplicated registration",
                            "Class: " + cls.getName());
                }
                else {
                    registeredAutomation.put(cls, automation);

                    if (Framework.debug) Log.information(Automations.class,
                            "Automation registered",
                            "Class: " + cls.getName());
                }
            }
        }
    }
}
