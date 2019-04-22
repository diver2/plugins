// Copyright 2017 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package io.flutter.plugins.imagepicker;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.provider.MediaStore;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

class ImageResizer {
  private final File externalFilesDirectory;
  private final ExifDataCopier exifDataCopier;

  ImageResizer(File externalFilesDirectory, ExifDataCopier exifDataCopier) {
    this.externalFilesDirectory = externalFilesDirectory;
    this.exifDataCopier = exifDataCopier;
  }

  /**
   * If necessary, resizes the image located in imagePath and then returns the path for the scaled
   * image.
   *
   * <p>If no resizing is needed, returns the path for the original image.
   */
//  String resizeImageIfNeeded(String imagePath, Double maxWidth, Double maxHeight) {
  byte[] resizeAndRotateImage(String imagePath, Double maxWidth, Double maxHeight) {
    boolean shouldScale = maxWidth != null || maxHeight != null;
    try {
      return resizedImage(imagePath, maxWidth, maxHeight, shouldScale);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  //private File resizedImage(String path, Double maxWidth, Double maxHeight) throws IOException {
  private byte[] resizedImage(String path, Double maxWidth, Double maxHeight,
                              boolean shouldScale) throws IOException {
    //get orientation
    int orientation = getOrientation(path);

    Bitmap finalBmp;
    BitmapFactory.Options options = new BitmapFactory.Options();
    options.inPreferredConfig = Bitmap.Config.ARGB_8888;

    //scale
    if (shouldScale) {
      Bitmap bmp = BitmapFactory.decodeFile(path, options);
      double originalWidth = bmp.getWidth() * 1.0;
      double originalHeight = bmp.getHeight() * 1.0;

      boolean hasMaxWidth = maxWidth != null;
      boolean hasMaxHeight = maxHeight != null;

      Double width = hasMaxWidth ? Math.min(originalWidth, maxWidth) : originalWidth;
      Double height = hasMaxHeight ? Math.min(originalHeight, maxHeight) : originalHeight;

      boolean shouldDownscaleWidth = hasMaxWidth && maxWidth < originalWidth;
      boolean shouldDownscaleHeight = hasMaxHeight && maxHeight < originalHeight;
      boolean shouldDownscale = shouldDownscaleWidth || shouldDownscaleHeight;

      if (shouldDownscale) {
        double downscaledWidth = (height / originalHeight) * originalWidth;
        double downscaledHeight = (width / originalWidth) * originalHeight;

        if (width < height) {
          if (!hasMaxWidth) {
            width = downscaledWidth;
          } else {
            height = downscaledHeight;
          }
        } else if (height < width) {
          if (!hasMaxHeight) {
            height = downscaledHeight;
          } else {
            width = downscaledWidth;
          }
        } else {
          if (originalWidth < originalHeight) {
            width = downscaledWidth;
          } else if (originalHeight < originalWidth) {
            height = downscaledHeight;
          }
        }
      }
      finalBmp = Bitmap.createScaledBitmap(bmp, width.intValue(), height.intValue(), false);
    } else {
      finalBmp = BitmapFactory.decodeFile(path, options);
    }
    // rotate whatever we need (according to path orientation)
    Bitmap rotatedBitmap = rotateImage(finalBmp,orientation);

    //put bitmap into byte []
    ByteArrayOutputStream outStream = new ByteArrayOutputStream();
    rotatedBitmap.compress(Bitmap.CompressFormat.PNG, 100, outStream);
    byte[] byteArray = outStream.toByteArray();

    //end stuff
    rotatedBitmap.recycle();
    finalBmp.recycle();
    outStream.close();
    return byteArray;
  }

  private Bitmap performRotation(Bitmap source, float angle) {
    Matrix matrix = new Matrix();
    matrix.postRotate(angle);
    return Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(),
        matrix, true);
  }

  private int getOrientation(String path) throws IOException {
    ExifInterface ei = new ExifInterface(path);
    return ei.getAttributeInt(ExifInterface.TAG_ORIENTATION,
        ExifInterface.ORIENTATION_UNDEFINED);
  }

  private Bitmap rotateImage(Bitmap bitmap, int orientation) {
    switch (orientation) {
      case ExifInterface.ORIENTATION_ROTATE_90:
        return performRotation(bitmap, 90);
      case ExifInterface.ORIENTATION_ROTATE_180:
        return performRotation(bitmap, 180);
      case ExifInterface.ORIENTATION_ROTATE_270:
        return performRotation(bitmap, 270);
      case ExifInterface.ORIENTATION_NORMAL:
      default:
        return bitmap;
    }
  }
}
