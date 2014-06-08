package pl.edu.agh.marims.screenstreamer.lib.screen;

import android.app.Activity;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;

public class ScreenIntercepter {

    private Activity activity;
    private View rootView;

    private boolean initialized = false;

    public ScreenIntercepter(Activity activity) {
        this.activity = activity;
    }

    public void initialize() {
        try {
            rootView = activity.findViewById(android.R.id.content).getRootView();

            rootView.setDrawingCacheEnabled(true);

            initialized = true;
        } catch (NullPointerException e) {
            Toast.makeText(activity, "Wasn't able to initialize", Toast.LENGTH_SHORT).show();
            initialized = false;
            e.printStackTrace();
        }
    }

    private Bitmap takeScreenshot() {
        if (!initialized) {
            Toast.makeText(activity, "Initialize first", Toast.LENGTH_SHORT).show();
            return null;
        }
        rootView.invalidate();
        Bitmap bitmap = rootView.getDrawingCache(true);
        return bitmap;
    }

    public void intercept() {
        if (initialized) {
            Bitmap bitmap = takeScreenshot();
            new SendTask().execute(bitmap);
        }
    }

    public void stop() {
        initialized = false;
    }

    private class SendTask extends AsyncTask<Bitmap, Void, Boolean> {
        @Override
        protected Boolean doInBackground(Bitmap... params) {
            Bitmap bitmap = params[0];

            if (bitmap == null) {
                try {
                    Thread.sleep(1000);
                    return true;
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    return false;
                }
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            float scale = 0.7f;
            int quality = 50;
            Bitmap.createScaledBitmap(bitmap, (int) (bitmap.getWidth() * scale), (int) (bitmap.getHeight() * scale), false).compress(Bitmap.CompressFormat.JPEG, quality, baos);
            byte[] byteBuffer = baos.toByteArray();

            String bitmapBase64 = Base64.encodeToString(byteBuffer, Base64.NO_WRAP);

            String postString = "{ \"image\": \"" + bitmapBase64 + "\"}";

            Log.d("REQUEST", "Post length: " + postString.length());

            HttpClient httpClient = new DefaultHttpClient();
            HttpPost httpPost = new HttpPost("http://marims-backend.herokuapp.com/upload");
            try {
                httpPost.setHeader("Content-Type", "application/json");
                httpPost.setEntity(new StringEntity(postString));
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }

            try {
                httpClient.execute(httpPost);
                return true;
            } catch (IOException e) {
                e.printStackTrace();
            }

            return false;
        }

        @Override
        protected void onPostExecute(Boolean isSuccess) {
            if (isSuccess) {
                intercept();
            } else {
                Toast.makeText(activity, "Error while sending", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
