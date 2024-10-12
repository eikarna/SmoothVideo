package com.eikarna.smoothvideoapp.util;

import android.content.Context;
import android.net.Uri;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.File;

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
}
