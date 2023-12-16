package com.gnfosst.laba4;

import androidx.appcompat.app.AppCompatActivity;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.view.View;
import android.widget.Button;
import android.widget.Chronometer;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;

public class MainActivity extends AppCompatActivity {
    String uri = "http://77.234.212.71:8002/live";
    Button btnSongsHist;
    private TextView song;
    private Chronometer chronometer;
    private ImageView imageControl;
    private MediaPlayer mediaPlayer;
    DBHelper dbHelper;
    SQLiteDatabase db;
    Cursor userCursor;
    private long PauseOffSet = 0;

    private Handler handler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        dbHelper = new DBHelper(getApplicationContext());
        db = dbHelper.getReadableDatabase();

        chronometer = findViewById(R.id.chronometer);
        song = findViewById(R.id.song);
        imageControl = findViewById(R.id.imageControl);
        mediaPlayer = new MediaPlayer();
        btnSongsHist = findViewById(R.id.btnSongsHist);

        handler.removeCallbacks(timeUpdaterRunnable);
        handler.postDelayed(timeUpdaterRunnable, 100);
        imageControl.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mediaPlayer.isPlaying())
                {
                    chronometer.stop();
                    PauseOffSet = SystemClock.elapsedRealtime() - chronometer.getBase();
                    mediaPlayer.pause();
                    imageControl.setImageResource(R.drawable.pause);
                } else
                {
                    chronometer.setBase(SystemClock.elapsedRealtime() - PauseOffSet);
                    chronometer.start();

                    mediaPlayer.start();
                    imageControl.setImageResource(R.drawable.play);
                }
            }
        });

        prepareMediaPlayer();

        btnSongsHist.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, AudioActivity.class);
                startActivity(intent);
            }
        });


    }
    private  void prepareMediaPlayer(){
        try {
            mediaPlayer.setDataSource(uri);
            mediaPlayer.prepare();
        } catch (Exception exception){
            Toast.makeText(this, "Нет подключения к интернету", Toast.LENGTH_SHORT).show();
        }
    }

    private Runnable timeUpdaterRunnable = new Runnable() {

        public void run() {
            SongTask songTask = new SongTask();
            songTask.execute();

            handler.postDelayed(this, 5000);
        }
    };

    @Override
    protected void onPause() {
        handler.removeCallbacks(timeUpdaterRunnable);
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        handler.postDelayed(timeUpdaterRunnable, 5000);
    }

    class SongTask extends AsyncTask<Void, String, String> {
        private Document doc;
        private String songInfo;
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected String doInBackground(Void... url) {

            try {
                doc = Jsoup.connect("https://media.itmo.ru/includes/get_song.php").get();
                songInfo = doc.text();
            } catch (IOException e) {
                e.printStackTrace();
            }

            return songInfo;
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);

            song.setText(result);

            Calendar c = Calendar.getInstance();
            SimpleDateFormat df = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");
            String formattedDate = df.format(c.getTime());

            String[] songInfos = song.getText().toString().split(" - ");

            if(songInfos.length != 0){
                db = dbHelper.getReadableDatabase();
                userCursor = db.rawQuery("SELECT * FROM songs WHERE name = ?" , new String[] {songInfos[1].toString()});
                int value = userCursor.getCount() + 1;
                userCursor.close();
                db.close();

                if(value == 1){
                    db = dbHelper.getReadableDatabase();

                    ContentValues cv = new ContentValues();
                    cv.put(DBHelper.COLUMN_MUSICIAN, songInfos[0]);
                    cv.put(DBHelper.COLUMN_NAME, songInfos[1]);
                    cv.put(DBHelper.COLUMN_TIMEADD, formattedDate);

                    db.insert(DBHelper.TABLE, null, cv);
                    db.close();
                }
            }
        }
    }
}