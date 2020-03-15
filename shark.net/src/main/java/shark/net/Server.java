package shark.net;

import java.net.ServerSocket;

import shark.components.IAutomation;
import shark.runtime.Worker;

public class Server extends Worker implements IAutomation {

    private ServerSocket listener;
    private int[] ports;

    private int backlog;

    @Override
    protected void initialise() {
        registerTask(this::maintainTcpListenerTask, null, true);
    }

    private void maintainTcpListenerTask(Object state) {

        if (listener == null) {
            if (ports != null && ports.length > 0) {
                Exception exception = null;

                try {
                    listener = new ServerSocket(getPort(), backlog);
                    listener.
                }
            }
        }
    }
}
