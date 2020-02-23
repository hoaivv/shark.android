package shark.test;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.webkit.WebView;

import shark.Framework;
import shark.runtime.Action;
import shark.runtime.Cache;
import shark.runtime.Parallel;
import shark.utils.LogData;

public class Main extends Activity {

    WebView view;

   // MessengerClient client = new MessengerClient("api.ahacafe.vn", 500);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        view = new WebView(this);
        view.getSettings().setJavaScriptEnabled(true);
        view.getSettings().setDomStorageEnabled(true);
        view.loadUrl("http://api.ahacafe.vn/rok.html");

        setContentView(view);

        Framework.onLogged.add(new Action.One<LogData>() {
            @Override
            public void run(LogData eventArgs) {

                switch (eventArgs.getType()) {
                    case Error: Log.e(eventArgs.getOwner().getName(), eventArgs.getTrace() + ": " + eventArgs.getMessage()); break;
                    case Warning: Log.w(eventArgs.getOwner().getName(), eventArgs.getTrace() + ": " + eventArgs.getMessage()); break;
                    case Information: Log.i(eventArgs.getOwner().getName(), eventArgs.getTrace() + ": " + eventArgs.getMessage()); break;
                }
            }
        });

        Framework.onStarted.add(new Action() {
            @Override
            public void run() {

                Parallel.start(new Runnable() {
                    @Override
                    public void run() {

                        try {

                            Cache<String, String> cache = Cache.of(String.class, String.class);
                            //Cache.of(String.class, String.class).clear();
                            //Cache.of(String.class, String.class).update("t1", "aaa");
                            //Cache.of(String.class, String.class).update("t2", "aaa");
                            //Log.i("!!!!!!", cache.retrive("test"));
                        }
                        catch (InterruptedException e) {
                        }
                    }
                });


            }
        });

        Framework.log = true;
        Framework.debug = true;
        Framework.traceLogCaller = true;
        Framework.writeLogsToFiles = true;

        final Context context = this;

        new Thread(new Runnable() {
            @Override
            public void run() {

                Framework.start(context);

                //try {
                    //Services.get("test", String.class).process(new ServiceRequestInfo<String>("Hoai"));
                //}
                //catch (InterruptedException e) {
                //}
            }
        }).start();
    }
}
