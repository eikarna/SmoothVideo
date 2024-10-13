package com.eikarna.smoothvideoapp.util;

import android.content.Context;
import android.net.Uri;
import android.os.Environment;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class FileUtil {
    public static String copyFileToPath(Context ctx, Uri uri, String path, String fileName) {
        try {
            // Open the input stream from the URI
            InputStream inputStream = ctx.getContentResolver().openInputStream(uri);
            if (inputStream == null) return null;

            // Create the destination file in the internal storage
            File internalFile = new File(path, fileName);
            OutputStream outputStream = new FileOutputStream(internalFile);

            // Copy the file content to the internal storage
            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }

            // Close streams
            inputStream.close();
            outputStream.close();

            // Return the internal file path
            return internalFile.getAbsolutePath();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static void saveFileToPath(Context ctx, Uri uri, String fileName) {
        File dir = Environment.getExternalStorageDirectory();
        File saveDir = new File(dir, "Movies/SmoothVideo");
        if (!saveDir.exists()) {
            saveDir.mkdirs();
        }
        File outputFile = new File(saveDir, fileName);
        try (InputStream is = new FileInputStream(uri.toString());
             OutputStream os = new FileOutputStream(outputFile.getAbsolutePath())) {
            // Copy the file content to the internal storage
            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = is.read(buffer)) != -1) {
                os.write(buffer, 0, bytesRead);
            }
        } catch (IOException e) {
            // Handle error
            e.printStackTrace();
        }
        Toast.makeText(ctx, "File saved successfully at Movies/SmoothVideo/" + fileName, Toast.LENGTH_LONG).show();
    }
}
