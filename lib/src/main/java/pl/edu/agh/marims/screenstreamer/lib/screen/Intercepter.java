package pl.edu.agh.marims.screenstreamer.lib.screen;


import android.graphics.Bitmap;

public interface Intercepter {
    public void intercept();

    public Bitmap takeScreenshot();
}
