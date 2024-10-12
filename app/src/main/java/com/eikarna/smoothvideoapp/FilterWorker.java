package com.eikarna.smoothvideoapp;

import android.content.Context;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.work.Data;
import androidx.work.ForegroundInfo;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import com.arthenica.ffmpegkit.FFmpegKit;
import com.arthenica.ffmpegkit.FFmpegSession;

import java.io.File;

public class FilterWorker extends Worker {
    private static final String TAG = "FilterWorker";

    public FilterWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        String inputFilePath = getInputData().getString("inputFilePath");
        String outputFilePath = getInputData().getString("outputFilePath");
        int fps = getInputData().getInt("fps", 60);
        String hwaccel = getInputData().getString("hwaccel");
        String preset = getInputData().getString("preset");
        String miMode = getInputData().getString("mi_mode");
        String mcMode = getInputData().getString("mc_mode");
        String me = getInputData().getString("me");
        String meMode = getInputData().getString("me_mode");
        String customFilters = getInputData().getString("customFilters");
        String customParams = getInputData().getString("customParams");

        // Validate inputs
        if (inputFilePath == null || outputFilePath == null) {
            Log.e(TAG, "Input or output file path is null.");
            return Result.failure();
        }

        // Ensure output directory exists
        File outputFile = new File(outputFilePath);
        File outputDir = outputFile.getParentFile();
        if (!outputDir.exists() && !outputDir.mkdirs()) {
            Log.e(TAG, "Failed to create output directory.");
            return Result.failure();
        }

        // Build the FFmpeg command dynamically
        StringBuilder ffmpegCommand = new StringBuilder();
        ffmpegCommand.append("-hwaccel ").append(hwaccel).append(" -y -i ")
                .append(inputFilePath)
                .append(" -preset ").append(preset)
                .append(" -filter:v \"minterpolate='mi_mode=").append(miMode);
                if (meMode == "mci") {
                    ffmpegCommand.append(":mc_mode=").append(mcMode)
                    .append(":me=").append(me)
                    .append(":me_mode=").append(meMode)
                    .append(":mb_size=4");
                }
                ffmpegCommand.append(":fps=").append(fps)
                .append(customFilters).append("'\" ").append(customParams)
                .append(" -r ").append(fps).append(" ").append(outputFilePath);

        // Log the command for debugging
        Log.d(TAG, "FFmpeg command: " + ffmpegCommand);

        // Optionally, set foreground service with a progress notification
        // setForegroundAsync(createForegroundInfo());

        // Execute the FFmpeg command
        FFmpegSession session = FFmpegKit.execute(ffmpegCommand.toString());
        Log.d(TAG, "FFmpeg output: " + session.getOutput());
        Log.e(TAG, "FFmpeg error: " + session.getFailStackTrace());

        // Check if processing succeeded
        if (session.getReturnCode().isValueSuccess()) {
            Data outputData = new Data.Builder().putString("outputFilePath", outputFilePath).build();
            return Result.success(outputData);
        } else {
            Log.e(TAG, "FFmpeg process failed");
            return Result.failure();
        }
    }
}