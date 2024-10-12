package com.eikarna.smoothvideoapp;

import com.arthenica.ffmpegkit.FFmpegKit;

public class FFmpegHelper {
    public static void applySmoothFilter(String inputFilePath, String outputFilePath) {
        String command = "-i " + inputFilePath + " -vf fps=60,format=yuv420p " + outputFilePath;
        FFmpegKit.execute(command);
    }
}