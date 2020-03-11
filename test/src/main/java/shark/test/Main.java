package shark.test;

import android.app.Activity;
import android.os.Bundle;
import android.webkit.WebView;

import shark.Framework;
import shark.utils.http;

@SuppressWarnings("ALL")
public class Main extends Activity {

    class SongDTO {
        public String ID;
        public String FileName;
        public long InfoStamp;
        public long DataStamp;
    }

    WebView view;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        view = new WebView(this);
        view.getSettings().setJavaScriptEnabled(true);
        view.getSettings().setDomStorageEnabled(true);
        view.loadUrl("http://api.ahacafe.vn/rok.html");

        setContentView(view);

        TestService test = new TestService();

        Framework.start(this);


        System.out.println("test");


        try {

            SongDTO[] songs = http.at("api.ahacafe.vn/music/songs").method("S").expect(SongDTO[].class).result();
        } catch (Exception e) {
        }
    }
}
