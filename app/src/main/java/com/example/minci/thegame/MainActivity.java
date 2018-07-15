package com.example.minci.thegame;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    public static final String LOG_KEY = MainActivity.class.getSimpleName();
    public static final String LOG_THREAD_ONE = "GeneralDownloadThread";
    public static final String REQUEST = "https://api.flickr.com/services/rest/";
    public static final int IMAGE_CROP_SIZE = 30;

    private ArrayList<Button> buttons = new ArrayList<>();
    private ArrayList<Photo> flickrImages = new ArrayList<>();
    private TableLayout tableLayout;
    private TextView tv_score;
    private TextView tv_change;
    private ArrayList<Photo> randomImages = new ArrayList<>();

    private Button btnStart;
    private Button click2Btn;
    private Button click1Btn;

    private int trueNum;
    private int falseNum;
    private int currentGameLevel;
    private int clickNum;
    private int changeToNextGame;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btnStart = findViewById(R.id.btn_start);
        tableLayout = findViewById(R.id.btnArea);


        currentGameLevel = 4;
        btnStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                btnStart.setEnabled(false);
                startGame();
            }
        });

        tv_score = findViewById(R.id.tv_score);
        tv_score.setText(R.string.score_board_init);

        tv_change = findViewById(R.id.tv_change);
    }

    private void startGame() {
        trueNum = 0;
        falseNum = 0;
        clickNum = 0;
        try {
            flickrImages.clear();
            buttons.clear();
            randomImages.clear();

            Toast.makeText(MainActivity.this, "Downloading images", Toast.LENGTH_SHORT).show();

            Thread imageListDownThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    URL url;
                    HttpURLConnection urlConnection = null;
                    ArrayList<ImageDownThread> imageDownThreads = new ArrayList<>();

                    try {
                        int imageNumber;
                        StringBuilder requestSB = new StringBuilder(REQUEST);
                        imageNumber = currentGameLevel * currentGameLevel / 2;
                        requestSB.append("?method=flickr.photos.search")
                                .append("&per_page=")
                                .append(imageNumber)
                                .append("&api_key=3f44e9fba4d2afdc714c73a5caee3b4a")
                                .append("&tags=dogs")
                                .append("&format=json")
                                .append("&nojsoncallback=1");
                        url = new URL(requestSB.toString());
                        urlConnection = (HttpURLConnection) url.openConnection();

                        Log.d(LOG_THREAD_ONE, "Request url:" + requestSB.toString());

                        StringBuilder jsonMsg = new StringBuilder();

                        BufferedReader bfr = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));

                        String line;
                        while ((line = bfr.readLine()) != null) {
                            jsonMsg.append(line);
                        }

                        flickrImages = parseFlickrJSON(jsonMsg.toString());

                        for (int i = 0; i < imageNumber * 2; ++i) {
                            ImageDownThread imageDownThread = new ImageDownThread(flickrImages.get(i / 2), randomImages);
                            imageDownThreads.add(imageDownThread);
                            imageDownThread.start();
                            Log.d(LOG_THREAD_ONE, flickrImages.get(i / 2).toString());
                        }
                        Log.d(LOG_THREAD_ONE, "Download :" + imageNumber + " image address");

                        for (int i = 0; i < imageNumber * 2; ++i) {
                            imageDownThreads.get(i).join();
                        }
                    } catch (Exception e) {
                        Log.e(LOG_THREAD_ONE, e.getMessage());
                        e.printStackTrace();
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(MainActivity.this, "Downloading Images Failed", Toast.LENGTH_SHORT).show();
                            }
                        });
                    } finally {
                        if (urlConnection != null) {
                            urlConnection.disconnect();

                            runOnUiThread(new Runnable() {
                                @SuppressLint("SetTextI18n")
                                @Override
                                public void run() {
                                    btnStart.setText("RESTART");

                                    btnStart.setEnabled(true);
                                }
                            });
                        }
                    }
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            setButtonBackgrounds();
                        }
                    });
                }
            });
            initBtnArea(currentGameLevel);
            imageListDownThread.start();
            tv_score.setText(R.string.score_board_init);
            changeToNextGame = currentGameLevel * 10;
            tv_change.setText("Change:" + changeToNextGame);
        } catch (Exception e) {
            Log.d("ImageListDownThread", e.getMessage());
        }
    }

    private void initBtnArea(int n) {
        tableLayout.removeAllViews();
        for (int i = 0; i < n; ++i) {
            TableRow tableRow = new TableRow(MainActivity.this);
            tableRow.setLayoutParams(new TableLayout.LayoutParams(
                    TableLayout.LayoutParams.MATCH_PARENT,
                    TableLayout.LayoutParams.MATCH_PARENT,
                    1.0f
            ));
            for (int j = 0; j < n; ++j) {
                Button btn = new Button(MainActivity.this);
                btn.setId(i * n + j);
                btn.setLayoutParams(new TableRow.LayoutParams(
                        TableRow.LayoutParams.MATCH_PARENT,
                        TableRow.LayoutParams.MATCH_PARENT,
                        1.0f
                ));
                //btn.setBackground(null);
                btn.setEnabled(false);
                btn.setPadding(5, 5, 5, 5);
                btn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Button clickedBtn = (Button) v;
                        gameLogic(clickedBtn);
                    }
                });

                buttons.add(btn);
                tableRow.addView(btn);
            }
            tableLayout.addView(tableRow);
        }
    }

    private void setButtonBackgrounds() {
        Bitmap scaledQuestion = BitmapFactory.decodeResource(getResources(), R.drawable.question_mark);
        scaledQuestion = Bitmap.createScaledBitmap(scaledQuestion, buttons.get(0).getWidth() - IMAGE_CROP_SIZE, buttons.get(0).getHeight() - IMAGE_CROP_SIZE, true);
        BitmapDrawable bitmapDrawable = new BitmapDrawable(getResources(), scaledQuestion);
        for (int i = 0; i < buttons.size(); ++i) {
            buttons.get(i).setBackground(bitmapDrawable);
            buttons.get(i).setEnabled(true);
        }
        Toast.makeText(MainActivity.this, "Images Downloaded.", Toast.LENGTH_SHORT).show();
    }


    private void gameLogic(Button imgBtn) {

        if (changeToNextGame != 0) {
            if (clickNum == 0) {
                click1Btn = imgBtn;
                Bitmap scaledBitmap = Bitmap.createScaledBitmap(randomImages.get(click1Btn.getId()).getBitmap(), imgBtn.getWidth() - IMAGE_CROP_SIZE, imgBtn.getHeight() - IMAGE_CROP_SIZE, true);

                ++clickNum;

                click1Btn.setBackground(new BitmapDrawable(getResources(), scaledBitmap));

                click1Btn.setEnabled(false);

            } else if (clickNum == 1) {
                click2Btn = imgBtn;
                final Photo image1 = randomImages.get(click1Btn.getId());
                final Photo image2 = randomImages.get(click2Btn.getId());

                click2Btn.setBackground(new BitmapDrawable(getResources(), image2.getBitmap()));

                if (image1.equals(image2)) {
                    click1Btn.setEnabled(false);
                    click2Btn.setEnabled(false);
                    ++trueNum;
                } else {
                    Handler handler = new Handler();
                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            Bitmap scaledQuestion = BitmapFactory.decodeResource(getResources(), R.drawable.question_mark);
                            scaledQuestion = Bitmap.createScaledBitmap(scaledQuestion, click1Btn.getWidth() - IMAGE_CROP_SIZE, click1Btn.getHeight() - IMAGE_CROP_SIZE, true);
                            BitmapDrawable bitmapDrawable = new BitmapDrawable(getResources(), scaledQuestion);

                            click1Btn.setBackground(bitmapDrawable);
                            click1Btn.setEnabled(true);
                            click2Btn.setBackground(bitmapDrawable);
                            click2Btn.setEnabled(true);
                        }
                    }, 300);
                    ++falseNum;
                    --changeToNextGame;
                    tv_change.setText("Change:" + changeToNextGame);
                }
                --clickNum;
            }
            tv_score.setText("SCORE ->"+trueNum);

            if (trueNum == (currentGameLevel * 2) && (falseNum < (currentGameLevel * 10))) {
                currentGameLevel += 2;
                startGame();
            } else {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(MainActivity.this, "You Failed! Try Again!", Toast.LENGTH_LONG).show();
                    }
                });
            }
        }
    }

    private ArrayList<Photo> parseFlickrJSON(String json){
        ArrayList<Photo> images = new ArrayList<>();
        if (json.isEmpty()) {
            Log.e(LOG_KEY, "EMPTY JSON STRING");
            return null;
        }
        try{
            JSONObject jsonObject = new JSONObject(json);
            JSONObject photoJsonObj = jsonObject.getJSONObject("photos");
            JSONArray jsonArray = photoJsonObj.getJSONArray("photo");
            for (int i = 0; i < jsonArray.length(); ++i){
                Photo photo = new Photo();
                photo.setFarm(jsonArray.getJSONObject(i).getString("farm"));
                photo.setServer(jsonArray.getJSONObject(i).getString("server"));
                photo.setId(jsonArray.getJSONObject(i).getString("id"));
                photo.setSecret(jsonArray.getJSONObject(i).getString("secret"));
                images.add(photo);
            }
        }catch (Exception e){
            Log.e(LOG_KEY, e.getMessage());
        }

        return images;
    }
}