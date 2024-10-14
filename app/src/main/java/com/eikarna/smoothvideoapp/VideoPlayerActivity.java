package com.eikarna.smoothvideoapp;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.media3.common.MediaItem;
import androidx.media3.exoplayer.ExoPlayer;

import com.arthenica.ffmpegkit.FFprobeKit;
import com.arthenica.ffmpegkit.FFprobeSession;
import com.eikarna.smoothvideoapp.databinding.ActivityVideoPlayerBinding;
import com.eikarna.smoothvideoapp.util.FileUtil;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class VideoPlayerActivity extends AppCompatActivity {
    private ExoPlayer player;
    private Uri videoUri;
    private ActivityVideoPlayerBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityVideoPlayerBinding.inflate(getLayoutInflater());
        View view = binding.getRoot();
        setContentView(view);

        player = new ExoPlayer.Builder(this).build();
        binding.videoPlayerView.setPlayer(player);

        videoUri = getIntent().getData();
        if (videoUri != null) {
            player.setMediaItem(MediaItem.fromUri(videoUri));
            player.prepare();
            player.play();
        }
        setupListener();
    }

    private void setupListener() {
        binding.showInfo.setOnClickListener(v -> showVideoInfo());
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            binding.saveVideo.setOnClickListener(v -> saveVideo());
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.Q)
    private void saveVideo() {
        Toast.makeText(this, getResources().getString(R.string.saving_file), Toast.LENGTH_SHORT).show();
        SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault());
        String formattedDate = dateFormatter.format(new Date());
        String outputFileStr = "SmoothVideo-" + formattedDate + ".mp4";
        if (FileUtil.saveFileToPath(this, videoUri, outputFileStr)) {
            Toast.makeText(this, getResources().getString(R.string.success_save) + "Movies/SmoothVideo/" + outputFileStr, Toast.LENGTH_LONG).show();
        }
    }

    private void showVideoInfo() {
        if (videoUri == null) {
            Toast.makeText(this, "No video selected.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Build and execute ffprobe command
        String ffprobeCommand = String.format("-v quiet -print_format json -show_format \"%s\"", videoUri.getPath());

        FFprobeSession session = FFprobeKit.execute(ffprobeCommand);

        // Build dialog
        MaterialAlertDialogBuilder dialog = new MaterialAlertDialogBuilder(this)
                .setTitle("FFprobe Output")
                .setMessage(session.getOutput())
                .setPositiveButton("COPY", (dialogInterface, i) -> {
                    ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                    ClipData clip = ClipData.newPlainText("FFprobe Output", session.getOutput());
                    clipboard.setPrimaryClip(clip);
                });

        if (session.getReturnCode().isValueSuccess()) {
            String output = session.getOutput();
            parseAndDisplayVideoInfo(output, dialog);
        }
        dialog.create().show();
    }

    private void parseAndDisplayVideoInfo(String ffprobeOutput, MaterialAlertDialogBuilder dialog) {
        // Parse the JSON ffprobe output and extract the needed fields
        try {
            JSONObject jsonObject = new JSONObject(ffprobeOutput);
            JSONObject format = jsonObject.getJSONObject("format");
            String duration = format.getString("duration");
            String bitrate = format.getString("bit_rate");
            String formatName = format.getString("format_name");

            // You can add more details here based on what ffprobe returns
            String info = String.format("Duration: %s seconds\nBitrate: %s bps\nFormat: %s", duration, bitrate, formatName);

            // Display the info in a Toast or a custom dialog
            dialog.setPositiveButton("COPY", (dialogInterface, i) -> {
                ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("FFprobe Output", info);
                clipboard.setPrimaryClip(clip);
            }).setMessage(info);
        } catch (Exception e) {
            dialog.setPositiveButton("COPY", (dialogInterface, i) -> {
                ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("FFprobe Output", e.getMessage());
                clipboard.setPrimaryClip(clip);
            }).setMessage(e.getMessage());
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
