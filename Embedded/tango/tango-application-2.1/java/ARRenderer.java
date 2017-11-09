/*
 * Copyright 2014 Google Inc. All Rights Reserved.
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
package ch.hefr.etu.zoutao_wen.tangoapplication;

import android.content.Context;
import android.util.Log;
import android.view.MotionEvent;

import com.google.atap.tangoservice.TangoPoseData;
import com.projecttango.tangosupport.TangoSupport;

import org.rajawali3d.Object3D;
import org.rajawali3d.lights.DirectionalLight;
import org.rajawali3d.materials.Material;
import org.rajawali3d.materials.methods.DiffuseMethod;
import org.rajawali3d.materials.textures.ATexture;
import org.rajawali3d.materials.textures.StreamingTexture;
import org.rajawali3d.materials.textures.Texture;
import org.rajawali3d.math.Matrix4;
import org.rajawali3d.math.Quaternion;
import org.rajawali3d.primitives.Cube;
import org.rajawali3d.primitives.ScreenQuad;
import org.rajawali3d.renderer.Renderer;

import javax.microedition.khronos.opengles.GL10;

/**
 * Very simple augmented reality renderer which displays two cube in place.
 * The position of the cube in the OpenGL world is updated using the {@code updateObjectPose}
 * method.
 */
public class ARRenderer extends Renderer {
    private static final float CUBE_SIDE_LENGTH = 0.2f;
    private static final String TAG = ARRenderer.class.getSimpleName();

    private float[] textureCoords0 = new float[]{0.0F, 1.0F, 0.0F, 0.0F, 1.0F, 1.0F, 1.0F, 0.0F};

    // Augmented Reality related fields.
    private ATexture mTangoCameraTexture;
    private boolean mSceneCameraConfigured;

    private Object3D mOrigin_1;
    private Object3D mOrigin_2;
    private Matrix4 mObjectTransform_1;
    private Matrix4 mObjectTransform_2;
    private boolean mObjectPoseUpdated_1 = false;
    private boolean mObjectPoseUpdated_2 = false;

    private ScreenQuad mBackgroundQuad;

    public ARRenderer(Context context) {
        super(context);
    }

    @Override
    protected void initScene() {
        // Create a quad covering the whole background and assign a texture to it where the
        // Tango color camera contents will be rendered.
        if (mBackgroundQuad == null) {
            mBackgroundQuad = new ScreenQuad();
            mBackgroundQuad.getGeometry().setTextureCoords(textureCoords0);
        }
        Material tangoCameraMaterial = new Material();
        tangoCameraMaterial.setColorInfluence(0);
        // We need to use Rajawali's {@code StreamingTexture} since it sets up the texture
        // for GL_TEXTURE_EXTERNAL_OES rendering.
        mTangoCameraTexture =
                new StreamingTexture("camera", (StreamingTexture.ISurfaceListener) null);
        try {
            tangoCameraMaterial.addTexture(mTangoCameraTexture);
            mBackgroundQuad.setMaterial(tangoCameraMaterial);
        } catch (ATexture.TextureException e) {
            Log.e(TAG, "Exception creating texture for RGB camera contents", e);
        }
        getCurrentScene().addChildAt(mBackgroundQuad, 0);

        // Add a directional light in an arbitrary direction.
        DirectionalLight light = new DirectionalLight(-1, -0.2, -1);
        light.setColor(1, 1, 1);
        light.setPower(0.8f);
        light.setPosition(3, 2, 4);
        getCurrentScene().addLight(light);

        // Set up a material: green with application of the light and
        // instructions.
        Material material_1 = new Material();
        Material material_2 = new Material();
        material_1.setColor(0xff009900);
        material_2.setColor(0xff550000);
        try {
            Texture t_1 = new Texture("origin_1", R.drawable.origin_1);
            material_1.addTexture(t_1);
            Texture t_2 = new Texture("origin_2", R.drawable.origin_2);
            material_2.addTexture(t_2);
        } catch (ATexture.TextureException e) {
            e.printStackTrace();
        }
        material_1.setColorInfluence(0.9f);
        material_1.enableLighting(true);
        material_1.setDiffuseMethod(new DiffuseMethod.Lambert());

        // Build a Cube and place it initially three meters forward from the origin.
        mOrigin_1 = new Cube(CUBE_SIDE_LENGTH);//This object can be a cube
        mOrigin_1.setMaterial(material_1);
        mOrigin_1.setPosition(0, 0, -3);
        getCurrentScene().addChild(mOrigin_1);

        material_2.setColorInfluence(0.9f);
        material_2.enableLighting(true);
        material_2.setDiffuseMethod(new DiffuseMethod.Lambert());

        // Build a Cube and place it initially three meters forward from the origin.
        mOrigin_2 = new Cube(CUBE_SIDE_LENGTH);//This object can be a cube
        mOrigin_2.setMaterial(material_2);
        mOrigin_2.setPosition(0, 0, -3);
        getCurrentScene().addChild(mOrigin_2);
    }

    /**
     * Update background texture's UV coordinates when device orientation is changed (i.e., change
     * between landscape and portrait mode.
     * This must be run in the OpenGL thread.
     */
    public void updateColorCameraTextureUvGlThread(int rotation) {
        if (mBackgroundQuad == null) {
            mBackgroundQuad = new ScreenQuad();
        }

        float[] textureCoords =
                TangoSupport.getVideoOverlayUVBasedOnDisplayRotation(textureCoords0, rotation);
        mBackgroundQuad.getGeometry().setTextureCoords(textureCoords, true);
        mBackgroundQuad.getGeometry().reload();
    }

    @Override
    protected void onRender(long elapsedRealTime, double deltaTime) {
        // Update the AR object if necessary.
        // Synchronize against concurrent access with the setter below.
        synchronized (this) {
            if (mObjectPoseUpdated_1) {
                // Place the 3D object in the location of the detected plane.
                mOrigin_1.setPosition(mObjectTransform_1.getTranslation());
                // Note that Rajawali uses left-hand convention for Quaternions so we need to
                // specify a quaternion with rotation in the opposite direction.
                mOrigin_1.setOrientation(new Quaternion().fromMatrix(mObjectTransform_1));
                // Move it forward by half of the size of the cube to make it
                // flush with the plane surface.
                mOrigin_1.moveForward(CUBE_SIDE_LENGTH / 2.0f);

                mObjectPoseUpdated_1 = false;
            }
            if (mObjectPoseUpdated_2) {
                // Place the 3D object in the location of the detected plane.
                mOrigin_2.setPosition(mObjectTransform_2.getTranslation());
                // Note that Rajawali uses left-hand convention for Quaternions so we need to
                // specify a quaternion with rotation in the opposite direction.
                mOrigin_2.setOrientation(new Quaternion().fromMatrix(mObjectTransform_2));
                // Move it forward by half of the size of the cube to make it
                // flush with the plane surface.
                mOrigin_2.moveForward(CUBE_SIDE_LENGTH / 2.0f);

                mObjectPoseUpdated_2 = false;
            }
        }

        super.onRender(elapsedRealTime, deltaTime);
    }

    /**
     * Save the updated plane fit pose to update the AR object on the next render pass.
     * This is synchronized against concurrent access in the render loop above.
     */
    public synchronized void updateObjectPose_1(Matrix4 origin_1) {// set the place
        //mObjectTransform = new Matrix4(planeFitTransform);
        mObjectTransform_1 = origin_1;//next problem?
        mObjectPoseUpdated_1 = true;
    }

    public void updateObjectPose_2(Matrix4 origin_2) {// set the place
        //mObjectTransform = new Matrix4(planeFitTransform);
        mObjectTransform_2 = origin_2;
        mObjectPoseUpdated_2 = true;
    }

    /**
     * Update the scene camera based on the provided pose in Tango start of service frame.
     * The camera pose should match the pose of the camera color at the time of the last rendered
     * RGB frame, which can be retrieved with this.getTimestamp();
     * <p/>
     * NOTE: This must be called from the OpenGL render thread; it is not thread safe.
     */
    public void updateRenderCameraPose(TangoPoseData cameraPose) {
        float[] rotation = cameraPose.getRotationAsFloats();
        float[] translation = cameraPose.getTranslationAsFloats();
        Quaternion quaternion = new Quaternion(rotation[3], rotation[0], rotation[1], rotation[2]);
        // Conjugating the Quaternion is needed because Rajawali uses left-handed convention for
        // quaternions.
        getCurrentCamera().setRotation(quaternion.conjugate());
        getCurrentCamera().setPosition(translation[0], translation[1], translation[2]);
    }

    /**
     * It returns the ID currently assigned to the texture where the Tango color camera contents
     * should be rendered.
     * NOTE: This must be called from the OpenGL render thread; it is not thread safe.
     */
    public int getTextureId() {
        return mTangoCameraTexture == null ? -1 : mTangoCameraTexture.getTextureId();
    }

    /**
     * We need to override this method to mark the camera for re-configuration (set proper
     * projection matrix) since it will be reset by Rajawali on surface changes.
     */
    @Override
    public void onRenderSurfaceSizeChanged(GL10 gl, int width, int height) {
        super.onRenderSurfaceSizeChanged(gl, width, height);
        mSceneCameraConfigured = false;
    }

    public boolean isSceneCameraConfigured() {
        return mSceneCameraConfigured;
    }

    /**
     * Sets the projection matrix for the scene camera to match the parameters of the color camera,
     * provided by the {@code TangoCameraIntrinsics}.
     */
    public void setProjectionMatrix(float[] matrix) {
        getCurrentCamera().setProjectionMatrix(new Matrix4(matrix));
    }

    @Override
    public void onOffsetsChanged(float xOffset, float yOffset,
                                 float xOffsetStep, float yOffsetStep,
                                 int xPixelOffset, int yPixelOffset) {
    }

    @Override
    public void onTouchEvent(MotionEvent event) {

    }
}
