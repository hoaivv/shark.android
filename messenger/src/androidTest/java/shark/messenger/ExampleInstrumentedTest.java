package shark.messenger;

import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import shark.runtime.Action;

import static org.junit.Assert.*;

/**
 * Instrumented test, which will execute on an Android device.
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
@RunWith(AndroidJUnit4.class)
public class ExampleInstrumentedTest {
    @Test
    public void useAppContext() {
        // Context of the app under test.
        Context appContext = InstrumentationRegistry.getTargetContext();

        assertEquals("shark.messenger.test", appContext.getPackageName());

        final MessengerClient client = new MessengerClient("api.ahacafe.vn");

                client.register("test").then(new Action<Boolean>() {
                    @Override
                    public void process(Boolean data) {
                        if (data) {

                            client.addOnDataEventListener(new Action<MessengerPackage>() {
                                @Override
                                public void process(MessengerPackage data) {
                                    // do something
                                }
                            });

                            new Thread(new Runnable() {
                                @Override
                                public void run() {
                                    try {
                                        while (client.isReady()) {
                                            client.push(client, "text", "test message");
                                            Thread.sleep(1000);
                                        }
                                    } catch (InterruptedException e) {
                                    }
                                }
                            }).start();
                        }
                    }
                });
    }
}
