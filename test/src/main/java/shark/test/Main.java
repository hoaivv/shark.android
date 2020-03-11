package shark.test;

import android.app.Activity;
import android.os.Bundle;
import android.webkit.WebView;

import shark.Framework;

@SuppressWarnings("ALL")
public class Main extends Activity {


    WebView view;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Framework.initialise(this).then(isSucceed -> {

            if(isSucceed) {
                Framework.start(new TestService());
            }
        });
    }
}
