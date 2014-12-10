package pl.edu.agh.marims.screenstreamer.lib.network.sender;

import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

import pl.edu.agh.marims.screenstreamer.lib.convert.BitmapToBase64Converter;
import pl.edu.agh.marims.screenstreamer.lib.screen.intercepter.Intercepter;
import pl.edu.agh.marims.screenstreamer.lib.screen.intercepter.ScreenIntercepter;

/**
 * Created by Przemek on 2014-12-10.
 */
public class UdpSocketSender extends AbstractSender {

    private static final boolean DEBUG = true;

    private Handler mainLooper = new Handler(Looper.getMainLooper());
    private BitmapToBase64Converter converter;
    private SenderWorker worker;

    public UdpSocketSender(Intercepter intercepter, String serverUrl) {
        this.intercepter = intercepter;
        this.serverUrl = serverUrl;
        this.converter = new BitmapToBase64Converter();
        this.worker = new SenderWorker();
    }

    @Override
    public void startSending() {
        super.startSending();
        worker.start();
    }

    private class SenderWorker extends Thread {

        private static final int MAX_SIZE = 65000;

        private InetAddress address;
        private DatagramSocket socket;
        private Bitmap bitmap;
        private long lastScreenshotVersion;
        private long screenshotVersion = Long.MIN_VALUE;
        private boolean loadInProgress = false;

        @Override
        public void run() {
            lastScreenshotVersion = screenshotVersion;
            try {
                socket = new DatagramSocket(5000);
                address = InetAddress.getByName("192.168.0.11");
                while (runSending) {
                    if (!loadInProgress) {
                        try {
                            send();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        load();
                    }
                }
                if (socket != null && !socket.isClosed()) {
                    socket.close();
                }
            } catch (SocketException e) {
                e.printStackTrace();
            } catch (UnknownHostException e) {
                e.printStackTrace();
            }
        }

        private void send() throws IOException {
            if (lastScreenshotVersion != screenshotVersion) {
                List<byte[]> payloads = buildPayloads();

                if (DEBUG) {
                    Log.d("REQUEST", "Payloads count: " + payloads.size());
                    Log.d("REQUEST", "Payloads length: ");
                    for (int i = 0; i < payloads.size(); i++) {
                        Log.d("REQUEST", i + ":" + payloads.get(i).length);
                    }
                }

                for (int i = 0; i < payloads.size(); i++) {
                    byte[] payload = payloads.get(i);
                    DatagramPacket packet = new DatagramPacket(payload, payload.length, address, 5000);
                    socket.send(packet);
                }

                lastScreenshotVersion = screenshotVersion;
            }
        }

        private List<byte[]> buildPayloads() {
            JSONObject jsonObject = new JSONObject();
            try {
                jsonObject.put("sessionId", ScreenIntercepter.SESSION_ID);
                jsonObject.put("screenWidth", bitmap.getWidth());
                jsonObject.put("screenHeight", bitmap.getHeight());
                jsonObject.put("image", converter.convert(bitmap));
            } catch (JSONException e) {
                e.printStackTrace();
            }

            byte[] data = jsonObject.toString().getBytes();
            List<byte[]> payloads = new ArrayList<byte[]>();
            byte payloadsCount = (byte) (data.length / MAX_SIZE + 1);
            for (byte i = 0; i < payloadsCount; i++) {
                int from;
                int to;
                int length;
                if (i < payloadsCount - 1) {
                    from = i * MAX_SIZE;
                    to = i * MAX_SIZE + MAX_SIZE;
                } else {
                    from = i * MAX_SIZE;
                    to = data.length;
                }
                length = to - from;
                if (DEBUG) {
                    Log.d("REQUEST", "from: " + from + " to: " + to + " length: " + length);
                }
                byte[] payload = new byte[length + 2];
                System.arraycopy(data, from, payload, 2, length);
                payload[0] = (byte) (i + 1);
                payload[1] = payloadsCount;
                payloads.add(payload);
            }
            return payloads;
        }

        private void load() {
            loadInProgress = true;
            mainLooper.post(new Runnable() {
                @Override
                public void run() {
                    bitmap = intercepter.takeScreenshot();
                    if (bitmap != null) {
                        screenshotVersion++;
                    }
                    loadInProgress = false;
                }
            });
        }
    }

}