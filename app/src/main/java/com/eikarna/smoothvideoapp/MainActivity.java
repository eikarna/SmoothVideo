package com.eikarna.smoothvideoapp;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import com.eikarna.smoothvideoapp.databinding.ActivityMainBinding;
import com.eikarna.smoothvideoapp.databinding.ActivitySettingsBinding;
import com.eikarna.smoothvideoapp.util.FileUtil;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public class MainActivity extends AppCompatActivity {
    private static final int REQUEST_VIDEO = 1;
    private Uri videoUri;
    private MaterialAlertDialogBuilder progressDialogBuilder;
    private AlertDialog progressDialog;
    private PowerManager.WakeLock wakeLock;
    private SharedPreferences sharedPreferences;
    private ActivityMainBinding binding;
    private ActivitySettingsBinding settingsBinding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        View view = binding.getRoot();
        setContentView(view);

        // Initialize SharedPreferences
        sharedPreferences = getSharedPreferences("SettingsPrefs", MODE_PRIVATE);

        binding.selectVideoButton.setOnClickListener(v -> selectVideo());
        binding.startProcessingButton.setOnClickListener(v -> startFilterWorker());
        binding.settingsButton.setOnClickListener(v -> showSettingsDialog());
    }

    private void showSettingsDialog() {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this);
        settingsBinding = ActivitySettingsBinding.inflate(this.getLayoutInflater());
        View dialogView = settingsBinding.getRoot();

        // Load spinner data from strings.xml
        ArrayAdapter<CharSequence> hwAccelAdapter =
                ArrayAdapter.createFromResource(
                        this, R.array.hwaccel_options, android.R.layout.simple_spinner_item);
        hwAccelAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        settingsBinding.hwaccel.setAdapter(hwAccelAdapter);

        ArrayAdapter<CharSequence> presetsAdapter =
                ArrayAdapter.createFromResource(
                        this, R.array.presets_options, android.R.layout.simple_spinner_item);
        presetsAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        settingsBinding.presets.setAdapter(presetsAdapter);

        ArrayAdapter<CharSequence> miModeAdapter =
                ArrayAdapter.createFromResource(
                        this, R.array.mi_mode_options, android.R.layout.simple_spinner_item);
        miModeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        settingsBinding.miMode.setAdapter(miModeAdapter);

        ArrayAdapter<CharSequence> mcModeAdapter =
                ArrayAdapter.createFromResource(
                        this, R.array.mc_mode_options, android.R.layout.simple_spinner_item);
        mcModeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        settingsBinding.mcMode.setAdapter(mcModeAdapter);

        ArrayAdapter<CharSequence> meAdapter =
                ArrayAdapter.createFromResource(
                        this, R.array.me_options, android.R.layout.simple_spinner_item);
        meAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        settingsBinding.me.setAdapter(meAdapter);

        ArrayAdapter<CharSequence> meModeAdapter =
                ArrayAdapter.createFromResource(
                        this, R.array.me_mode_options, android.R.layout.simple_spinner_item);
        meModeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        settingsBinding.meMode.setAdapter(meModeAdapter);

        // Listen FPS SeekBar change
        settingsBinding.fps.setOnSeekBarChangeListener(
                new OnSeekBarChangeListener() {
                    @SuppressLint("SetTextI18n")
                    @Override
                    public void onProgressChanged(SeekBar seekbar, int progress, boolean fromUser) {
                        settingsBinding.fpsText.setText(getResources().getString(R.string.settings_fps) + " (" + progress + ")");
                    }

                    @Override
                    public void onStartTrackingTouch(SeekBar seekbar) {
                        // Notify when user start tracking
                    }

                    @Override
                    public void onStopTrackingTouch(SeekBar seekbar) {
                        // Notify When User Stop Tracking
                        if (seekbar.getProgress() > 60) {
                            Toast.makeText(settingsBinding.getRoot().getContext(), "Higher FPS will take more longer time!", Toast.LENGTH_LONG).show();
                        }
                    }
                });

        // Listen MiMode item change
        settingsBinding.miMode.setOnItemSelectedListener(
                new OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(
                            AdapterView<?> parentView, View selectedItemView, int position, long id) {
                        if (getResources().getStringArray(R.array.mi_mode_options)[position].equals("mci")) {
                            settingsBinding.mcMode.setVisibility(View.VISIBLE);
                            settingsBinding.me.setVisibility(View.VISIBLE);
                            settingsBinding.meMode.setVisibility(View.VISIBLE);
                        } else {
                            settingsBinding.mcMode.setVisibility(View.INVISIBLE);
                            settingsBinding.me.setVisibility(View.INVISIBLE);
                            settingsBinding.meMode.setVisibility(View.INVISIBLE);
                        }
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> parentView) {
                        // miModeSpinner.setSelection(0);
                    }
                });

        // Load values from SharedPreferences
        loadSettings(
                settingsBinding.fps,
                settingsBinding.hwaccel,
                settingsBinding.presets,
                settingsBinding.miMode,
                settingsBinding.mcMode,
                settingsBinding.me,
                settingsBinding.meMode,
                settingsBinding.customFilters,
                settingsBinding.customParams);

        // Build and show the dialog
        builder
                .setTitle(R.string.settings_title)
                .setView(dialogView)
                .setPositiveButton(
                        getResources().getString(R.string.settings_save),
                        (dialog, id) ->
                                saveSettings(
                                        settingsBinding.fps,
                                        settingsBinding.hwaccel,
                                        settingsBinding.presets,
                                        settingsBinding.miMode,
                                        settingsBinding.mcMode,
                                        settingsBinding.me,
                                        settingsBinding.meMode,
                                        settingsBinding.customFilters,
                                        settingsBinding.customParams))
                .setNegativeButton(getResources().getString(R.string.settings_cancel), (dialog, id) -> dialog.cancel());
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
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    private void releaseWakeLock() {
        if (wakeLock != null && wakeLock.isHeld()) {
            wakeLock.release();
        }
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
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
            String inputFilePath =
                    FileUtil.copyFileToPath(this, videoUri, getFilesDir().toString(), "input.mp4");
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
                                                .getStringArray(R.array.me_options)[sharedPreferences.getInt("me", 0)])
                                .putString(
                                        "me_mode",
                                        getResources()
                                                .getStringArray(R.array.me_mode_options)[
                                                sharedPreferences.getInt("me_mode", 0)])
                                .putString("customFilters", sharedPreferences.getString("customFilters", ""))
                                .putString("customParams", sharedPreferences.getString("customParams", ""))
                                .build();

                OneTimeWorkRequest filterWork =
                        new OneTimeWorkRequest.Builder(FilterWorker.class)
                                .setInputData(filterData) // Pass the data to the worker
                                .build();

                acquireWakeLock();
                // Show progress dialog
                String wait = getResources().getString(R.string.processing_messages);
                progressDialogBuilder =
                        new MaterialAlertDialogBuilder(this)
                                .setTitle(R.string.processing_video)
                                .setMessage(wait)
                                .setCancelable(false)
                                .setPositiveButton(
                                        getResources().getString(R.string.settings_cancel),
                                        (dialog, which) -> {
                                            WorkManager.getInstance(MainActivity.this).cancelWorkById(filterWork.getId());
                                            progressDialog.dismiss();
                                            releaseWakeLock();
                                        });

                progressDialog = progressDialogBuilder.create();
                progressDialog.show();

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
                                    } else if (workInfo != null) {
                                        // Update the progress dialog message based on workInfo progress
                                        String currentMessage = workInfo.getProgress().getString("progressMessage");
                                        if (currentMessage != null) {
                                            progressDialog.setMessage(currentMessage);
                                        }
                                    }
                                });
            } else {
                showErrorDialog(getResources().getString(R.string.process_failed_copy));
            }
        }
    }

    private void showErrorDialog(String message) {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Error")
                .setMessage(message)
                .setPositiveButton(android.R.string.ok, null)
                .create()
                .show();
    }
}
