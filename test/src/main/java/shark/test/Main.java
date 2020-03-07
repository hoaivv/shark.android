package shark.test;

import android.app.Activity;
import android.os.Bundle;
import android.webkit.WebView;

import shark.utils.http;

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

        SongDTO[] songs = http.at("api.ahacafe.vn/music/songs").expect(SongDTO[].class).result();
    }
}
