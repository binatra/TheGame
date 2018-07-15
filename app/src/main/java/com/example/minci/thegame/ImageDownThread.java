package com.example.minci.thegame;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;

class ImageDownThread extends Thread {

    private static final String LOG_KEY = "DownloadWorker";

    private Photo image;
    private URL url;
    private HttpURLConnection httpURLConnection = null;
    private ArrayList<Photo> randomList;
    private static Object lock = new Object();
    private StringBuilder sb = new StringBuilder();
    private long execTime;

    public ImageDownThread(Photo image, ArrayList<Photo> randomList) {
        this.randomList = randomList;
        this.image = image;
    }

    @SuppressLint("SimpleDateFormat")
    public void run() {

        try {
            execTime = System.currentTimeMillis();
            sb.append(" Start:").append(new SimpleDateFormat("HH:mm:ss:SSS ").format(Calendar.getInstance().getTime()));
            url = new URL(image.getPreviewURL());
            //Log.d(LOG_KEY,"D:"+image.getPreviewURL());
            httpURLConnection = (HttpURLConnection) url.openConnection();
            InputStream inputStream = httpURLConnection.getInputStream();
            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);

            synchronized (lock){
                image.setBitmap(bitmap);
                randomList.add(image);
            }

        } catch (Exception e) {
            Log.d(LOG_KEY,e.getMessage());
        } finally {
            if (httpURLConnection != null)
                httpURLConnection.disconnect();

            execTime = System.currentTimeMillis() -execTime;
            sb.append("-- End:").append(new SimpleDateFormat("HH:mm:ss:SSS ").format(Calendar.getInstance().getTime()));
            sb.append("-- Execution Time(ms):").append(String.valueOf(execTime));
            Log.d(LOG_KEY,sb.toString());
        }

    }
}
