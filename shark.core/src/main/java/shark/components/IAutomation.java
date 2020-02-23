package shark.components;

/**
 * Defines a Shark Automation. Any class to be registered as Shark Automation must implement this
 * interface
 */
public interface IAutomation {

    /**
     * Indicates whether the automation is running or not.
     * @return true if the automation is running; otherwise false
     */
    boolean isRunning();

    /**
     * Starts the automation
     */
    void start();

    /**
     * Stops the automation
     */
    void stop();
}
