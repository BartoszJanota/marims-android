package pl.edu.agh.marims.screenstreamer.lib.network.sender;

import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.util.Log;

import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

import pl.edu.agh.marims.screenstreamer.lib.convert.BitmapToBase64Converter;
import pl.edu.agh.marims.screenstreamer.lib.screen.intercepter.Intercepter;
import pl.edu.agh.marims.screenstreamer.lib.screen.intercepter.ScreenIntercepter;

public class AsyncTaskSender extends AbstractSender {

    private BitmapToBase64Converter converter;

    public AsyncTaskSender(Intercepter intercepter, String serverUrl) {
        this.intercepter = intercepter;
        this.serverUrl = serverUrl;
        this.converter = new BitmapToBase64Converter();
    }

    public AsyncTaskSender(Intercepter intercepter, String serverUrl, SenderCallback senderCallback) {
        this(intercepter, serverUrl);
        this.senderCallback = senderCallback;
    }

    @Override
    public void startSending() {
        super.startSending();
        send();
    }

    private void send() {
        new Sender().execute(intercepter.takeScreenshot());
    }

    private class Sender extends AsyncTask<Bitmap, Void, Boolean> {

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

            JSONObject jsonObject = new JSONObject();
            try {
                jsonObject.put("sessionId", ScreenIntercepter.SESSION_ID);
                jsonObject.put("screenWidth", bitmap.getWidth());
                jsonObject.put("screenHeight", bitmap.getHeight());
                jsonObject.put("image", converter.convert(bitmap));
            } catch (JSONException e) {
                e.printStackTrace();
            }

            String postString = jsonObject.toString();
//            String postString = "{ \"sessionId\": \"" + ScreenIntercepter.SESSION_ID + "\", \"image\": \"" + converter.convert(bitmap) + "\"}";

            Log.d("REQUEST", "Post length: " + postString.length());

            HttpClient httpClient = new DefaultHttpClient();
            HttpPost httpPost = new HttpPost(serverUrl);
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
                if (senderCallback != null) {
                    senderCallback.onSuccess();
                }
                if (runSending) {
                    send();
                }
            } else {
                if (senderCallback != null) {
                    senderCallback.onFailure();
                }
            }
        }
    }
}
