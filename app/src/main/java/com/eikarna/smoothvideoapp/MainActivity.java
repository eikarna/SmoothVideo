package com.eikarna.smoothvideoapp;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.net.Uri;
import android.util.Log;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import androidx.work.WorkInfo;
import com.eikarna.smoothvideoapp.FilterWorker;
import com.eikarna.smoothvideoapp.VideoPlayerActivity;
import com.google.android.material.button.MaterialButton;
import android.database.Cursor;
import android.content.Intent;
import android.Manifest;
import android.content.pm.PackageManager;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import java.io.IOException;
import androidx.annotation.NonNull;
import android.widget.Toast;
import android.os.PowerManager;
import android.content.SharedPreferences;
import android.widget.Spinner;
import android.widget.SeekBar;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.view.LayoutInflater;

import com.eikarna.smoothvideoapp.util.FileUtil;

public class MainActivity extends AppCompatActivity {
  private static final int REQUEST_VIDEO = 1;
  private Uri videoUri;
  private AlertDialog progressDialog;
  private PowerManager.WakeLock wakeLock;
  private SharedPreferences sharedPreferences;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);

    if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
        != PackageManager.PERMISSION_GRANTED) {
      ActivityCompat.requestPermissions(
          this, new String[] {Manifest.permission.READ_EXTERNAL_STORAGE}, 1);
    }

    // Initialize SharedPreferences
    sharedPreferences = getSharedPreferences("SettingsPrefs", MODE_PRIVATE);

    MaterialButton selectVideoButton = findViewById(R.id.selectVideoButton);
    selectVideoButton.setOnClickListener(v -> selectVideo());

    MaterialButton startProcessingButton = findViewById(R.id.startProcessingButton);
    startProcessingButton.setOnClickListener(v -> startFilterWorker());
    
    MaterialButton settingsButton = findViewById(R.id.settingsButton);
    settingsButton.setOnClickListener(v -> showSettingsDialog());
  }

  private void showSettingsDialog() {
    AlertDialog.Builder builder = new AlertDialog.Builder(this);
    LayoutInflater inflater = this.getLayoutInflater();
    View dialogView = inflater.inflate(R.layout.activity_settings, null);

    // Get all UI elements (SeekBar and Spinners)
    SeekBar fpsSeekBar = dialogView.findViewById(R.id.fps);
    Spinner hwAccelSpinner = dialogView.findViewById(R.id.hwaccel);
    Spinner presetsSpinner = dialogView.findViewById(R.id.presets);
    Spinner miModeSpinner = dialogView.findViewById(R.id.mi_mode);
    Spinner mcModeSpinner = dialogView.findViewById(R.id.mc_mode);
    Spinner meSpinner = dialogView.findViewById(R.id.me);
    Spinner meModeSpinner = dialogView.findViewById(R.id.me_mode);
    EditText customFilters = dialogView.findViewById(R.id.customFilters);
    EditText customParams = dialogView.findViewById(R.id.customParams);

    // Load spinner data from strings.xml
    ArrayAdapter<CharSequence> hwAccelAdapter =
        ArrayAdapter.createFromResource(
            this, R.array.hwaccel_options, android.R.layout.simple_spinner_item);
    hwAccelAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
    hwAccelSpinner.setAdapter(hwAccelAdapter);

    ArrayAdapter<CharSequence> presetsAdapter =
        ArrayAdapter.createFromResource(
            this, R.array.presets_options, android.R.layout.simple_spinner_item);
    presetsAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
    presetsSpinner.setAdapter(presetsAdapter);

    ArrayAdapter<CharSequence> miModeAdapter =
        ArrayAdapter.createFromResource(
            this, R.array.mi_mode_options, android.R.layout.simple_spinner_item);
    miModeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
    miModeSpinner.setAdapter(miModeAdapter);

    ArrayAdapter<CharSequence> mcModeAdapter =
        ArrayAdapter.createFromResource(
            this, R.array.mc_mode_options, android.R.layout.simple_spinner_item);
    mcModeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
    mcModeSpinner.setAdapter(mcModeAdapter);
    
    ArrayAdapter<CharSequence> meAdapter =
        ArrayAdapter.createFromResource(
            this, R.array.me_options, android.R.layout.simple_spinner_item);
    meAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
    meSpinner.setAdapter(meAdapter);

    ArrayAdapter<CharSequence> meModeAdapter =
        ArrayAdapter.createFromResource(
            this, R.array.me_mode_options, android.R.layout.simple_spinner_item);
    meModeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
    meModeSpinner.setAdapter(meModeAdapter);

    // Load values from SharedPreferences
    loadSettings(fpsSeekBar, hwAccelSpinner, presetsSpinner, miModeSpinner, mcModeSpinner, meSpinner, meModeSpinner, customFilters, customParams);

    // Build and show the dialog
    builder
        .setTitle("FFmpeg Settings")
        .setView(dialogView)
        .setPositiveButton(
            "Save",
            (dialog, id) ->
                saveSettings(
                    fpsSeekBar, hwAccelSpinner, presetsSpinner, miModeSpinner, mcModeSpinner, meSpinner, meModeSpinner, customFilters, customParams))
        .setNegativeButton("Cancel", (dialog, id) -> dialog.cancel());
    builder.create().show();
  }

  private void loadSettings(
      SeekBar fpsSeekBar,
      Spinner hwAccelSpinner,
      Spinner presetsSpinner,
      Spinner miModeSpinner,
      Spinner mcModeSpinner,
      Spinner meSpinner,
      Spinner meModeSpinner,
      EditText customFilters,
      EditText customParams) {
    // Load FPS setting
    int fps = sharedPreferences.getInt("fps", 60); // default 60
    fpsSeekBar.setProgress(fps);

    // Load spinner settings
    hwAccelSpinner.setSelection(sharedPreferences.getInt("hwaccel", 0)); // default index 0
    presetsSpinner.setSelection(sharedPreferences.getInt("presets", 0));
    miModeSpinner.setSelection(sharedPreferences.getInt("mi_mode", 0));
    mcModeSpinner.setSelection(sharedPreferences.getInt("mc_mode", 0));
    meSpinner.setSelection(sharedPreferences.getInt("me", 0));
    meModeSpinner.setSelection(sharedPreferences.getInt("me_mode", 0));
    customFilters.setText(sharedPreferences.getString("customFilters", ""));
    customParams.setText(sharedPreferences.getString("customParams", ""));
  }

  private void saveSettings(
      SeekBar fpsSeekBar,
      Spinner hwAccelSpinner,
      Spinner presetsSpinner,
      Spinner miModeSpinner,
      Spinner mcModeSpinner,
      Spinner meSpinner,
      Spinner meModeSpinner,
      EditText customFilters,
      EditText customParams) {
    SharedPreferences.Editor editor = sharedPreferences.edit();

    // Save FPS setting
    editor.putInt("fps", fpsSeekBar.getProgress());

    // Save spinner selections
    editor.putInt("hwaccel", hwAccelSpinner.getSelectedItemPosition());
    editor.putInt("presets", presetsSpinner.getSelectedItemPosition());
    editor.putInt("mi_mode", miModeSpinner.getSelectedItemPosition());
    editor.putInt("mc_mode", mcModeSpinner.getSelectedItemPosition());
    editor.putInt("me", meSpinner.getSelectedItemPosition());
    editor.putInt("me_mode", meModeSpinner.getSelectedItemPosition());
    editor.putString("customFilters", customFilters.getText().toString());
    editor.putString("customParams", customParams.getText().toString());

    // Commit the changes
    editor.apply();
  }

  private void acquireWakeLock() {
    PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
    wakeLock =
        powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK, "SmoothVideoApp::VideoProcessingLock");
    wakeLock.acquire();
  }

  private void releaseWakeLock() {
    if (wakeLock != null && wakeLock.isHeld()) {
      wakeLock.release();
    }
  }

  private void selectVideo() {
    Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Video.Media.EXTERNAL_CONTENT_URI);
    startActivityForResult(intent, REQUEST_VIDEO);
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
    super.onActivityResult(requestCode, resultCode, data);
    if (requestCode == REQUEST_VIDEO && resultCode == RESULT_OK && data != null) {
      videoUri = data.getData();
    }
  }

  private void startFilterWorker() {
    if (videoUri != null) {
      // Copy the video to internal storage
      String inputFilePath = FileUtil.copyFileToPath(this, videoUri, getFilesDir().toString(), "input.mp4");
      String outputFilePath = getFilesDir() + "/output.mp4";

      if (inputFilePath != null) {
        // Create a Data object to pass video path to the worker

        Data filterData =
            new Data.Builder()
                .putString("inputFilePath", inputFilePath)
                .putString("outputFilePath", outputFilePath)
                .putInt("fps", sharedPreferences.getInt("fps", 60))
                .putString(
                    "hwaccel",
                    getResources()
                        .getStringArray(R.array.hwaccel_options)[
                        sharedPreferences.getInt("hwaccel", 0)])
                .putString(
                    "preset",
                    getResources()
                        .getStringArray(R.array.presets_options)[
                        sharedPreferences.getInt("presets", 0)])
                .putString(
                    "mi_mode",
                    getResources()
                        .getStringArray(R.array.mi_mode_options)[
                        sharedPreferences.getInt("mi_mode", 0)])
                .putString(
                    "mc_mode",
                    getResources()
                        .getStringArray(R.array.mc_mode_options)[
                        sharedPreferences.getInt("mc_mode", 0)])
                .putString(
                    "me",
                    getResources()
                        .getStringArray(R.array.me_options)[
                        sharedPreferences.getInt("me", 0)])
                .putString(
                    "me_mode",
                    getResources()
                        .getStringArray(R.array.me_mode_options)[
                        sharedPreferences.getInt("me_mode", 0)])
                .putString(
                    "customFilters", sharedPreferences.getString("customFilters", ""))
                .putString(
                    "customParams", sharedPreferences.getString("customParams", ""))
                .build();

        OneTimeWorkRequest filterWork =
            new OneTimeWorkRequest.Builder(FilterWorker.class)
                .setInputData(filterData) // Pass the data to the worker
                .build();

        acquireWakeLock();
        // Show progress dialog
        progressDialog =
            new AlertDialog.Builder(this)
                .setTitle("Processing Video")
                .setMessage("Please wait while we apply the filter...")
                .setCancelable(false)
                .setPositiveButton(
                    "Cancel",
                    (dialog, which) -> {
                      WorkManager.getInstance(MainActivity.this).cancelWorkById(filterWork.getId());
                      progressDialog.dismiss();
                      releaseWakeLock();
                    })
                .show();

        WorkManager.getInstance(this).enqueue(filterWork);

        // Observe the work to see when it finishes
        WorkManager.getInstance(this)
            .getWorkInfoByIdLiveData(filterWork.getId())
            .observe(
                this,
                workInfo -> {
                  if (workInfo != null && workInfo.getState().isFinished()) {
                    progressDialog.dismiss(); // Dismiss the progress dialog
                    releaseWakeLock();

                    // Check if the output data is not null
                    String outputPath = workInfo.getOutputData().getString("outputFilePath");
                    if (outputPath != null) {
                      Intent intent = new Intent(MainActivity.this, VideoPlayerActivity.class);
                      intent.setData(Uri.parse(outputPath));
                      startActivity(intent);
                    } else {
                      Log.e(
                          "MainActivity", "Output path is null. Video processing may have failed.");
                      // Handle the error case, possibly show an error dialog
                      showErrorDialog("Error processing video. Please try again.");
                    }
                  }
                });
      } else {
        showErrorDialog("Failed to copy video to internal storage.");
      }
    }
  }

  private void showErrorDialog(String message) {
    new AlertDialog.Builder(this)
        .setTitle("Error")
        .setMessage(message)
        .setPositiveButton(android.R.string.ok, null)
        .show();
  }
}
