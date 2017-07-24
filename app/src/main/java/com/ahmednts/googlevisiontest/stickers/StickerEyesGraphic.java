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

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;
import com.ahmednts.googlevisiontest.camera.GraphicOverlay;
import com.google.android.gms.vision.face.Face;
import com.google.android.gms.vision.face.Landmark;
import java.util.HashMap;
import java.util.Map;

public class StickerEyesGraphic extends GraphicOverlay.Graphic {
  private static final float EYE_RADIUS_PROPORTION = 0.45f;

  private volatile Face mFace;

  private Paint mEyeWhitesPaint;
  private Paint mEyeOutlinePaint;

  private volatile PointF mLeftPosition;
  private volatile PointF mRightPosition;

  // Record the previously seen proportions of the landmark locations relative to the bounding box
  // of the face.  These proportions can be used to approximate where the landmarks are within the
  // face bounding box if the eye landmark is missing in a future update.
  private Map<Integer, PointF> mPreviousProportions = new HashMap<>();

  public StickerEyesGraphic(GraphicOverlay overlay) {
    super(overlay);

    mEyeWhitesPaint = new Paint();
    mEyeWhitesPaint.setColor(Color.WHITE);
    mEyeWhitesPaint.setStyle(Paint.Style.FILL);

    mEyeOutlinePaint = new Paint();
    mEyeOutlinePaint.setColor(Color.BLACK);
    mEyeOutlinePaint.setStyle(Paint.Style.STROKE);
    mEyeOutlinePaint.setStrokeWidth(5);
  }

  /**
   * Updates the eye positions and state from the detection of the most recent frame.  Invalidates
   * the relevant portions of the overlay to trigger a redraw.
   */
  public void updateEyes(Face face) {
    mFace = face;

    updatePreviousProportions(face);

    mLeftPosition = getLandmarkPosition(face, Landmark.LEFT_EYE);
    mRightPosition = getLandmarkPosition(face, Landmark.RIGHT_EYE);

    postInvalidate();
  }

  private void updatePreviousProportions(Face face) {
    for (Landmark landmark : face.getLandmarks()) {
      PointF position = landmark.getPosition();
      float xProp = (position.x - face.getPosition().x) / face.getWidth();
      float yProp = (position.y - face.getPosition().y) / face.getHeight();
      mPreviousProportions.put(landmark.getType(), new PointF(xProp, yProp));
    }
  }

  /**
   * Finds a specific landmark position, or approximates the position based on past observations
   * if it is not present.
   */
  private PointF getLandmarkPosition(Face face, int landmarkId) {
    for (Landmark landmark : face.getLandmarks()) {
      if (landmark.getType() == landmarkId) {
        return landmark.getPosition();
      }
    }

    PointF prop = mPreviousProportions.get(landmarkId);
    if (prop == null) {
      return null;
    }

    float x = face.getPosition().x + (prop.x * face.getWidth());
    float y = face.getPosition().y + (prop.y * face.getHeight());
    return new PointF(x, y);
  }

  /**
   * Draws the current eye state to the supplied canvas.  This will draw the eyes at the last
   * reported position from the tracker, and the iris positions according to the physics
   * simulations for each iris given motion and other forces.
   */
  @Override
  public void draw(Canvas canvas) {
    Face face = mFace;
    if (face == null) {
      return;
    }

    PointF detectLeftPosition = mLeftPosition;
    PointF detectRightPosition = mRightPosition;
    if ((detectLeftPosition == null) || (detectRightPosition == null)) {
      return;
    }

    PointF leftPosition =
        new PointF(translateX(detectLeftPosition.x), translateY(detectLeftPosition.y));
    PointF rightPosition =
        new PointF(translateX(detectRightPosition.x), translateY(detectRightPosition.y));

    // Use the inter-eye distance to set the size of the eyes.
    float distance = (float) Math.sqrt(Math.pow(rightPosition.x - leftPosition.x, 2) +
        Math.pow(rightPosition.y - leftPosition.y, 2));
    float eyeRadius = EYE_RADIUS_PROPORTION * distance;

    //drawEye(canvas, leftPosition, eyeRadius);
    //drawEye(canvas, rightPosition, eyeRadius);
  }

  /**
   * Draws the eye, either closed or open with the iris in the current position.
   */
  private void drawEye(Canvas canvas, PointF eyePosition, float eyeRadius) {

    canvas.drawCircle(eyePosition.x, eyePosition.y, eyeRadius, mEyeWhitesPaint);
    canvas.drawCircle(eyePosition.x, eyePosition.y, eyeRadius, mEyeOutlinePaint);
  }
}
