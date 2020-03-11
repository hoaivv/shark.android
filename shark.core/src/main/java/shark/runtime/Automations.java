package shark.runtime;

import java.util.ArrayList;
import java.util.HashMap;

import shark.Framework;
import shark.components.IAutomation;
import shark.utils.Log;

/**
 * Shark Automation Manager
 */
@SuppressWarnings({"SpellCheckingInspection", "unused"})
public final class Automations {

    @SuppressWarnings("SpellCheckingInspection")
    private Automations(){
    }

    private static final HashMap<Class<?>, IAutomation> registeredAutomation = new HashMap<>();

    public static IAutomation[] getAll() {

        synchronized (registeredAutomation) {
            return registeredAutomation.values().toArray(new IAutomation[0]);
        }
    }

    /**
     * Gets all running automation
     * @return all running automation
     */
    @SuppressWarnings("SpellCheckingInspection")
    public static IAutomation[] getRunningAutomations() {

        ArrayList<IAutomation> results = new ArrayList<>();
        for (IAutomation one : getAll()) if (one.isRunning()) results.add(one);

        return results.toArray(new IAutomation[0]);
    }

    /**
     * Indicates whether at least one automation is running or not
     * @return true if at least one automation is running; otherwise false
     */
    public static boolean hasRunningAutomation() {

        for (IAutomation one : getAll()) if (one.isRunning()) return  true;
        return false;
    }

    /**
     * Gets number of running automation
     * @return number of running automation
     */
    @SuppressWarnings("unused")
    public static int countRunning() {

        int count = 0;
        for (IAutomation one : getAll()) if (one.isRunning()) count++;
        return count;
    }

    /**
     * Indicates whether the specified automation is registered or not
     * @param automation automation to be checked
     * @return true if the automation is registered; otherwise false
     */
    public static boolean isRegistered(IAutomation automation) {

        if (automation == null) return false;
        Class<? extends IAutomation> cls = automation.getClass();

        synchronized (registeredAutomation) {
            if (!registeredAutomation.containsKey(cls)) return false;
            return automation == registeredAutomation.get(cls);
        }
    }

    /**
     * Indicates whether the specified worker is registered as an automation or not
     * @param worker worker to be checked
     * @return true if the worker is registered; otherwise false
     */
    public static boolean isRegistered(Worker worker) {

        if (worker == null || !IAutomation.class.isAssignableFrom(worker.getClass())) return false;

        Class<?> cls = worker.getClass();

        synchronized (registeredAutomation) {
            if (!registeredAutomation.containsKey(cls)) return false;
            //noinspection EqualsBetweenInconvertibleTypes
            return worker == registeredAutomation.get(cls);
        }
    }

    /**
     * Gets a registered automation of a specified type
     * @param cls type of automation
     * @return a registered automation of the specified type if it is registered; otherwise null
     */
    @SuppressWarnings("unused")
    public static IAutomation get(final Class<? extends IAutomation> cls) {

        synchronized (registeredAutomation) {
            return registeredAutomation.containsKey(cls) ? registeredAutomation.get(cls) : null;
        }
    }

    /**
     * Registers an automation
     * @param automation automation to be registered
     */
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
