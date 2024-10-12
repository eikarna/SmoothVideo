package com.eikarna.smoothvideoapp;

import android.content.Context;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import com.arthenica.ffmpegkit.FFmpegKit;
import com.arthenica.ffmpegkit.FFmpegSession;
import com.arthenica.ffmpegkit.FFmpegKitConfig;
import androidx.work.Data;

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
    
    if (inputFilePath == null || outputFilePath == null) {
      return Result.failure();
    }

    // Ensure output directory exists
    File outputFile = new File(outputFilePath);
    File outputDir = outputFile.getParentFile();
    if (!outputDir.exists()) {
      outputDir.mkdirs();
    }

    // FFmpeg command to apply the interpolation filter
    /*String ffmpegCommand =
        "-hwaccel mediacodec -y -i "
            + inputFilePath
            + " -preset veryfast -tune film "
            + "-filter:v \"minterpolate='mi_mode=mci:mc_mode=aobmc:me_mode=bidir:me=umh:mb_size=8:fps=60'\" -r 60 "
            + outputFilePath;*/
    
    String ffmpegCommand = "-hwaccel " + hwaccel + " -y -i " + inputFilePath + 
        " -preset " + preset + 
        " -filter:v \"minterpolate='mi_mode=" + miMode + ":mc_mode=" + mcMode +
        ":me=" + me + ":me_mode=" + meMode + ":mb_size=8" +
        ":fps=" + fps + customFilters + "'\"" + customParams + " -r " + fps + " " + outputFilePath;

    // Execute FFmpeg command
    FFmpegSession session = FFmpegKit.execute(ffmpegCommand);
    Log.d(TAG, "FFmpeg output: " + session.getOutput());
    Log.e(TAG, "FFmpeg error: " + session.getFailStackTrace());

    // Check if processing succeeded
    if (session.getReturnCode().isSuccess()) {
      Data outputData = new Data.Builder().putString("outputFilePath", outputFilePath).build();
      return Result.success(outputData);
    } else {
      Log.e(TAG, "FFmpeg process failed");
      return Result.failure();
    }
  }
}
