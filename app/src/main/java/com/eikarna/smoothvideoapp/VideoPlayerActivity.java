package com.eikarna.smoothvideoapp;

import android.net.Uri;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import com.eikarna.smoothvideoapp.util.FileUtil;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.ui.PlayerView;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.material.button.MaterialButton;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class VideoPlayerActivity extends AppCompatActivity {
    private ExoPlayer player;
    private PlayerView playerView;
    private Uri videoUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_player);

        playerView = findViewById(R.id.videoPlayerView);
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
      MaterialButton showInfo = findViewById(R.id.show_info);
      MaterialButton saveVideo = findViewById(R.id.save_video);
    
      showInfo.setOnClickListener(null);
      saveVideo.setOnClickListener(v -> saveVideo());
    }
    
    private void saveVideo() {
      SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyyMMdd", Locale.getDefault());
      String formattedDate = dateFormatter.format(new Date());
      FileUtil.copyFileToPath(this, videoUri, getExternalFilesDir(null).toString(), "SmoothVideo-"+formattedDate);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (player != null) {
            player.release();
        }
    }
}