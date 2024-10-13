package com.eikarna.smoothvideoapp;

import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.media3.common.MediaItem;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.ui.PlayerView;

import com.eikarna.smoothvideoapp.util.FileUtil;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class VideoPlayerActivity extends AppCompatActivity {
    private ExoPlayer player;
    private Uri videoUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_player);

        PlayerView playerView = findViewById(R.id.videoPlayerView);
        player = new ExoPlayer.Builder(this).build();
        playerView.setPlayer(player);

        videoUri = getIntent().getData();
        if (videoUri != null) {
            player.setMediaItem(MediaItem.fromUri(videoUri));
            player.prepare();
            player.play();
        }
        setupListener();
    }

    private void setupListener() {
        Button showInfo = findViewById(R.id.show_info);
        Button saveVideo = findViewById(R.id.save_video);

        showInfo.setOnClickListener(v -> Toast.makeText(this, "Coming Soon!", Toast.LENGTH_LONG).show());
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            saveVideo.setOnClickListener(v -> saveVideo());
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.Q)
    private void saveVideo() {
        Toast.makeText(this, getResources().getString(R.string.saving_file), Toast.LENGTH_SHORT).show();
        SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault());
        String formattedDate = dateFormatter.format(new Date());
        String outputFileStr = "SmoothVideo-" + formattedDate + ".mp4";
        if (FileUtil.saveFileToPath(this, videoUri, outputFileStr)) {
          Toast.makeText(this,  getResources().getString(R.string.success_save) + "Movies/SmoothVideo/" + outputFileStr, Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (player != null) {
            player.release();
        }
    }
}