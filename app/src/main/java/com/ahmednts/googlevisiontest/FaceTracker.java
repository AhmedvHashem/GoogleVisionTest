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
package com.ahmednts.googlevisiontest;

import android.graphics.Bitmap;
import com.ahmednts.googlevisiontest.camera.GraphicOverlay;
import com.ahmednts.googlevisiontest.stickers.StickerEyesGraphic;
import com.ahmednts.googlevisiontest.stickers.StickerFaceGraphic;
import com.ahmednts.googlevisiontest.stickers.StickerHatGraphic;
import com.google.android.gms.vision.Tracker;
import com.google.android.gms.vision.face.Face;
import com.google.android.gms.vision.face.FaceDetector;

class FaceTracker extends Tracker<Face> {
  private Bitmap[] mBitmaps;

  private GraphicOverlay mOverlay;
  private StickerEyesGraphic mEyesGraphic;
  private StickerFaceGraphic mFaceGraphic;
  private StickerHatGraphic mHatGraphic;

  FaceTracker(GraphicOverlay overlay, Bitmap[] bitmaps) {
    mOverlay = overlay;
    mBitmaps = bitmaps;
  }

  /**
   * Resets the underlying googly eyes graphic and associated physics state.
   */
  @Override
  public void onNewItem(int id, Face face) {
    mEyesGraphic = new StickerEyesGraphic(mOverlay);
    mFaceGraphic = new StickerFaceGraphic(mOverlay, mBitmaps[0]);
    mHatGraphic = new StickerHatGraphic(mOverlay, mBitmaps[1]);
  }

  /**
   * Update each graphic item info like position/sticker
   */
  @Override
  public void onUpdate(FaceDetector.Detections<Face> detectionResults, Face face) {
    mOverlay.add(mEyesGraphic);
    mOverlay.add(mFaceGraphic);
    mOverlay.add(mHatGraphic);

    mEyesGraphic.updateEyes(face);
    mFaceGraphic.updateEyes(face);
    mHatGraphic.updateEyes(face);
  }

  /**
   * Hide the graphic when the corresponding face was not detected.  This can happen for
   * intermediate frames temporarily (e.g., if the face was momentarily blocked from
   * view).
   */
  @Override
  public void onMissing(FaceDetector.Detections<Face> detectionResults) {
    mOverlay.remove(mEyesGraphic);
    mOverlay.remove(mFaceGraphic);
    mOverlay.remove(mHatGraphic);
  }

  /**
   * Called when the face is assumed to be gone for good. Remove graphic item from
   * the overlay.
   */
  @Override
  public void onDone() {
    mOverlay.remove(mEyesGraphic);
    mOverlay.remove(mFaceGraphic);
    mOverlay.remove(mHatGraphic);
  }
}