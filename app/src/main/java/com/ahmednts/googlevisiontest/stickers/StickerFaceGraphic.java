/*
 * Copyright (C) The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.ahmednts.googlevisiontest.stickers;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import com.ahmednts.googlevisiontest.camera.GraphicOverlay;
import com.google.android.gms.vision.face.Face;

public class StickerFaceGraphic extends GraphicOverlay.Graphic {
  private static final float FACE_POSITION_RADIUS = 10.0f;
  private static final float BOX_STROKE_WIDTH = 5.0f;

  private volatile Face mFace;

  private Paint mFacePositionPaint;
  private Paint mBoxPaint;

  private Bitmap mBitmap;

  public StickerFaceGraphic(GraphicOverlay overlay, Bitmap bitmap) {
    super(overlay);

    mBitmap = bitmap;

    mFacePositionPaint = new Paint();
    mFacePositionPaint.setColor(Color.RED);

    mBoxPaint = new Paint();
    mBoxPaint.setColor(Color.RED);
    mBoxPaint.setStyle(Paint.Style.STROKE);
    mBoxPaint.setStrokeWidth(BOX_STROKE_WIDTH);
  }

  public void updateEyes(Face face) {
    mFace = face;

    postInvalidate();
  }

  @Override
  public void draw(Canvas canvas) {
    Face face = mFace;
    if (face == null) {
      return;
    }

    // Draws a circle at the position of the detected face, with the face's track id below.
    float x = translateX(face.getPosition().x + face.getWidth() / 2);
    float y = translateY(face.getPosition().y + face.getHeight() / 2);
    //canvas.drawCircle(x, y, FACE_POSITION_RADIUS, mFacePositionPaint);

    // Draws a bounding box around the face.
    float xOffset = scaleX(face.getWidth() / 2.0f);
    float yOffset = scaleY(face.getHeight() / 2.0f);
    float left = x - xOffset;
    float top = y - yOffset;
    float right = x + xOffset;
    float bottom = y + yOffset;
    //canvas.drawRect(left, top, right, bottom, mBoxPaint);

    double viewWidth = canvas.getWidth();
    double viewHeight = canvas.getHeight();
    double imageWidth = mBitmap.getWidth();
    double imageHeight = mBitmap.getHeight();
    double scale = Math.min(viewWidth / imageWidth, viewHeight / imageHeight);

    double finalWidth = imageWidth * scale / 2;
    double finalHeight = imageHeight * scale / 2;

    //Rect destBounds =
    //    new Rect((int) (x - finalWidth / 2)
    //        , (int) (y - finalHeight / 2)
    //        , (int) (x + finalWidth / 2)
    //        , (int) (y + finalHeight / 2));

    Rect destBounds = new Rect((int) left, (int) top, (int) right, (int) bottom);
    canvas.drawBitmap(mBitmap, null, destBounds, null);
  }
}
