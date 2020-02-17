package shark.test;

import android.app.Activity;
import android.os.Bundle;
import android.webkit.WebView;

import shark.messenger.MessengerClient;
import shark.messenger.MessengerPackage;
import shark.runtime.Action;

public class Main extends Activity {

    WebView view;

    MessengerClient client = new MessengerClient("api.ahacafe.vn", 500);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        /*
        view = new WebView(this);
        view.getSettings().setJavaScriptEnabled(true);
        view.getSettings().setDomStorageEnabled(true);
        view.loadUrl("http://api.ahacafe.vn/rok.html");

        setContentView(view);
        */

        client.onPackageReceived = new Action<MessengerPackage>() {
            @Override
            public void process(MessengerPackage data) {

                System.out.println(data.Data);
            }
        };

        client.register("HVV RoK Bot").then(new Action<Boolean>() {
            @Override
            public void process(Boolean data) {
                if (data) {
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            while(true){
                                client.push(client, "text", "test");
                            }

                        }
                    }).start();
                }
            }
        });
    }
}
