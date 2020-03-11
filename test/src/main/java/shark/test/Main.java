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

        Framework.initialise(this).then(succeed -> {
            if(succeed) {
                Framework.start(new TestService());
            }
        });
    }
}
