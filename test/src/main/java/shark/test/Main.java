package shark.test;

import android.app.Activity;
import android.os.Bundle;
import android.webkit.WebView;

import shark.utils.http;

@SuppressWarnings("ALL")
public class Main extends Activity {


    WebView view;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        http.at("http://api.ahacafe.vn/music/song/677a8f72-3232-4f93-95e4-149472168299").download().then(data -> {
            System.out.print("a");
        });

    }
}
