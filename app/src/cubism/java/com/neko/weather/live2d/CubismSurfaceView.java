/*
 * NekoWeather Cubism runtime adapter.
 * Cubism SDK source/API usage is governed by the licenses shipped with the SDK.
 */
package com.neko.weather.live2d;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.PixelFormat;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.GLUtils;
import android.util.Log;
import android.view.MotionEvent;

import com.live2d.sdk.cubism.framework.CubismDefaultParameterId.ParameterId;
import com.live2d.sdk.cubism.framework.CubismFramework;
import com.live2d.sdk.cubism.framework.CubismFrameworkConfig;
import com.live2d.sdk.cubism.framework.CubismModelSettingJson;
import com.live2d.sdk.cubism.framework.effect.CubismBreath;
import com.live2d.sdk.cubism.framework.effect.CubismEyeBlink;
import com.live2d.sdk.cubism.framework.math.CubismMatrix44;
import com.live2d.sdk.cubism.framework.id.CubismId;
import com.live2d.sdk.cubism.framework.model.CubismUserModel;
import com.live2d.sdk.cubism.framework.motion.ACubismMotion;
import com.live2d.sdk.cubism.framework.motion.CubismBreathUpdater;
import com.live2d.sdk.cubism.framework.motion.CubismEyeBlinkUpdater;
import com.live2d.sdk.cubism.framework.motion.CubismExpressionMotion;
import com.live2d.sdk.cubism.framework.motion.CubismExpressionUpdater;
import com.live2d.sdk.cubism.framework.motion.CubismMotion;
import com.live2d.sdk.cubism.framework.motion.CubismPhysicsUpdater;
import com.live2d.sdk.cubism.framework.motion.CubismPoseUpdater;
import com.live2d.sdk.cubism.framework.rendering.android.CubismOffscreenManagerAndroid;
import com.live2d.sdk.cubism.framework.rendering.android.CubismRendererAndroid;
import com.live2d.sdk.cubism.framework.rendering.android.CubismShaderAndroid;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/** A transparent, lifecycle-aware GLSurfaceView backed by the official Cubism SDK. */
public final class CubismSurfaceView extends GLSurfaceView {
    private static final long FRAME_INTERVAL_MILLIS = 16L;
    private final RuntimeRenderer runtimeRenderer;
    private final boolean interactionEnabled;
    private float downX;
    private float downY;
    private long downAt;
    private boolean renderLoopActive;
    private final Runnable renderTick = new Runnable() {
        @Override public void run() {
            if (!renderLoopActive) return;
            // Attachment can happen before the Compose window becomes visible. Keep
            // the ticker alive so the first visible frame starts without a touch.
            if (getWindowVisibility() == VISIBLE && isShown()) requestRender();
            postOnAnimationDelayed(this, FRAME_INTERVAL_MILLIS);
        }
    };

    public CubismSurfaceView(Context context, String modelJsonPath, boolean interactionEnabled, Runnable onFirstFrame) {
        super(context);
        this.interactionEnabled = interactionEnabled;
        setEGLContextClientVersion(2);
        setEGLConfigChooser(8, 8, 8, 8, 16, 0);
        getHolder().setFormat(PixelFormat.TRANSLUCENT);
        setZOrderOnTop(true);
        setPreserveEGLContextOnPause(true);
        runtimeRenderer = new RuntimeRenderer(context.getApplicationContext(), modelJsonPath, () -> post(onFirstFrame));
        setRenderer(runtimeRenderer);
        // 设备即使是 120/144 Hz，也只提交约 60 帧，避免一个很小的主页模型
        // 独占 GL 线程和 GPU；按需渲染还能在窗口不可见时完全停下来。
        setRenderMode(RENDERMODE_WHEN_DIRTY);
    }

    @Override protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        renderLoopActive = true;
        onResume();
        removeCallbacks(renderTick);
        requestRender();
        post(renderTick);
    }

    @Override protected void onDetachedFromWindow() {
        renderLoopActive = false;
        removeCallbacks(renderTick);
        super.onDetachedFromWindow();
    }

    @Override protected void onWindowVisibilityChanged(int visibility) {
        super.onWindowVisibilityChanged(visibility);
        if (visibility == VISIBLE && renderLoopActive) {
            removeCallbacks(renderTick);
            post(renderTick);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!interactionEnabled || event.getPointerCount() > 1) return false;
        if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
            downX = event.getX();
            downY = event.getY();
            downAt = event.getEventTime();
        }
        if (event.getActionMasked() == MotionEvent.ACTION_DOWN || event.getActionMasked() == MotionEvent.ACTION_MOVE) {
            float x = getWidth() == 0 ? 0f : event.getX() / getWidth() * 2f - 1f;
            float y = getHeight() == 0 ? 0f : 1f - event.getY() / getHeight() * 2f;
            queueEvent(() -> runtimeRenderer.setDragging(x, y));
            return true;
        }
        if (event.getActionMasked() == MotionEvent.ACTION_UP ||
            event.getActionMasked() == MotionEvent.ACTION_CANCEL) {
            float x = getWidth() == 0 ? 0f : event.getX() / getWidth() * 2f - 1f;
            float y = getHeight() == 0 ? 0f : 1f - event.getY() / getHeight() * 2f;
            boolean isTap = event.getActionMasked() == MotionEvent.ACTION_UP &&
                event.getEventTime() - downAt < 450L &&
                Math.hypot(event.getX() - downX, event.getY() - downY) < 36f;
            queueEvent(() -> runtimeRenderer.setDragging(0f, 0f));
            if (isTap) queueEvent(() -> runtimeRenderer.onTap(x, y));
            return true;
        }
        return super.onTouchEvent(event);
    }

    public void release() {
        renderLoopActive = false;
        removeCallbacks(renderTick);
        queueEvent(runtimeRenderer::release);
        onPause();
    }

    private static final class RuntimeRenderer implements Renderer {
        private final Context context;
        private final String modelJsonPath;
        private final CubismMatrix44 projection = CubismMatrix44.create();
        private RuntimeModel model;
        private int width;
        private int height;
        private long previousNanos;
        private long nextRetryNanos;
        private boolean released;
        private final Runnable onFirstFrame;
        private boolean firstFrameReported;
        private int renderedFrames;

        RuntimeRenderer(Context context, String modelJsonPath, Runnable onFirstFrame) {
            this.context = context;
            this.modelJsonPath = modelJsonPath;
            this.onFirstFrame = onFirstFrame;
        }

        @Override
        public void onSurfaceCreated(GL10 ignored, EGLConfig config) {
            GLES20.glEnable(GLES20.GL_BLEND);
            GLES20.glBlendFunc(GLES20.GL_ONE, GLES20.GL_ONE_MINUS_SRC_ALPHA);
            ensureFramework(context);
            // Shader programs belong to the current EGL context. Navigating between
            // Compose pages can recreate that context, so stale global shader IDs
            // must be discarded before a model renderer is constructed.
            CubismShaderAndroid.getInstance().releaseInvalidShaderProgram();
            CubismShaderAndroid.deleteInstance();
            previousNanos = System.nanoTime();
        }

        @Override
        public void onSurfaceChanged(GL10 ignored, int width, int height) {
            this.width = width;
            this.height = height;
            GLES20.glViewport(0, 0, width, height);
            released = false;
            loadModel();
        }

        @Override
        public void onDrawFrame(GL10 ignored) {
            GLES20.glClearColor(0f, 0f, 0f, 0f);
            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
            long now = System.nanoTime();
            if (model == null && !released && width > 0 && height > 0 && now >= nextRetryNanos) loadModel();
            RuntimeModel current = model;
            if (current == null || width <= 0 || height <= 0) return;
            float delta = Math.min(0.05f, Math.max(0.001f, (now - previousNanos) / 1_000_000_000f));
            previousNanos = now;

            CubismOffscreenManagerAndroid.getInstance().beginFrameProcess();
            current.update(delta);
            projection.loadIdentity();
            float aspect = (float) width / height;
            float canvasRatio = current.getModel().getCanvasHeight() / current.getModel().getCanvasWidth();
            float displayRatio = (float) height / width;
            if (canvasRatio < displayRatio) {
                current.getModelMatrix().setWidth(2f);
                projection.scale(1f, aspect);
            } else {
                current.getModelMatrix().setHeight(2f);
                projection.scale(1f / aspect, 1f);
            }
            current.draw(projection);
            if (!firstFrameReported) {
                firstFrameReported = true;
                onFirstFrame.run();
            }
            CubismOffscreenManagerAndroid.getInstance().endFrameProcess();
            // 清理并不需要每帧执行；降低 Java/GL 同步和临时对象开销。
            if (++renderedFrames % 120 == 0) {
                CubismOffscreenManagerAndroid.getInstance().releaseStaleRenderTextures();
            }
        }

        void setDragging(float x, float y) {
            if (model != null) model.setDragging(x, y);
        }

        void onTap(float x, float y) {
            if (model != null) model.playInteraction(x, y);
        }

        void release() {
            released = true;
            if (model != null) {
                model.close();
                model = null;
            }
        }

        private void loadModel() {
            try {
                if (model != null) model.close();
                model = new RuntimeModel(modelJsonPath, width, height);
                nextRetryNanos = 0L;
                previousNanos = System.nanoTime();
                firstFrameReported = false;
            } catch (Throwable error) {
                Log.e("NekoCubism", "Unable to load model; retrying", error);
                model = null;
                nextRetryNanos = System.nanoTime() + 1_500_000_000L;
            }
        }
    }

    private static synchronized void ensureFramework(Context context) {
        if (!CubismFramework.isStarted()) {
            CubismFramework.Option option = new CubismFramework.Option();
            option.loggingLevel = CubismFrameworkConfig.LogLevel.WARNING;
            option.logFunction = message -> Log.d("NekoCubism", message);
            option.loadFileFunction = path -> {
                try {
                    return context.getAssets().open(path).readAllBytes();
                } catch (IOException error) {
                    Log.e("NekoCubism", "Asset not found: " + path, error);
                    return new byte[0];
                }
            };
            CubismFramework.startUp(option);
        }
        if (!CubismFramework.isInitialized()) CubismFramework.initialize();
    }

    private static final class RuntimeModel extends CubismUserModel implements AutoCloseable {
        private final String home;
        private final CubismModelSettingJson setting;
        private final Map<String, ACubismMotion> motions = new HashMap<>();
        private final List<String> interactionMotions = new ArrayList<>();
        private final List<CubismExpressionMotion> expressions = new ArrayList<>();
        private boolean motionUpdated;
        private String idleGroup;
        private int idleIndex;
        private int interactionIndex;
        private int expressionIndex;
        private final CubismId mouthOpenId;
        private float elapsedSeconds;

        RuntimeModel(String jsonPath, int width, int height) throws IOException {
            File json = new File(jsonPath);
            home = json.getParentFile().getAbsolutePath() + File.separator;
            setting = new CubismModelSettingJson(read(json));
            loadModel(read(resolve(setting.getModelFileName())), true);
            mouthOpenId = CubismFramework.getIdManager().getId(ParameterId.MOUTH_OPEN_Y.getId());

            for (int index = 0; index < setting.getExpressionCount(); index++) {
                String expressionPath = setting.getExpressionFileName(index);
                if (!expressionPath.isEmpty()) {
                    CubismExpressionMotion expression = loadExpression(read(resolve(expressionPath)));
                    if (expression != null) expressions.add(expression);
                }
            }
            if (!expressions.isEmpty()) updateScheduler.addUpdatableList(new CubismExpressionUpdater(expressionManager));

            String physicsPath = setting.getPhysicsFileName();
            if (!physicsPath.isEmpty()) loadPhysics(read(resolve(physicsPath)));
            if (physics != null) updateScheduler.addUpdatableList(new CubismPhysicsUpdater(physics));

            String posePath = setting.getPoseFileName();
            if (!posePath.isEmpty()) loadPose(read(resolve(posePath)));
            if (pose != null) updateScheduler.addUpdatableList(new CubismPoseUpdater(pose));

            String userDataPath = setting.getUserDataFile();
            if (!userDataPath.isEmpty()) loadUserData(read(resolve(userDataPath)));

            if (setting.getEyeBlinkParameterCount() > 0) {
                eyeBlink = CubismEyeBlink.create(setting);
                updateScheduler.addUpdatableList(new CubismEyeBlinkUpdater(() -> motionUpdated, eyeBlink));
            }

            breath = CubismBreath.create();
            List<CubismBreath.BreathParameterData> breathData = new ArrayList<>();
            breathData.add(breath(ParameterId.ANGLE_X, 0f, 10f, 6.5f));
            breathData.add(breath(ParameterId.ANGLE_Y, 0f, 5f, 3.5f));
            breathData.add(breath(ParameterId.ANGLE_Z, 0f, 6f, 5.5f));
            breathData.add(breath(ParameterId.BODY_ANGLE_X, 0f, 3f, 15.5f));
            breathData.add(breath(ParameterId.BREATH, 0.5f, 0.5f, 3.2f));
            breath.setParameters(breathData);
            updateScheduler.addUpdatableList(new CubismBreathUpdater(breath));

            Map<String, Float> layout = new HashMap<>();
            if (setting.getLayoutMap(layout)) modelMatrix.setupFromLayout(layout);
            model.saveParameters();
            preloadMotions();
            updateScheduler.sortUpdatableList();

            setupRenderer(CubismRendererAndroid.create(width, height));
            setupTextures();
        }

        private CubismBreath.BreathParameterData breath(ParameterId id, float offset, float peak, float cycle) {
            return new CubismBreath.BreathParameterData(
                CubismFramework.getIdManager().getId(id.getId()), offset, peak, cycle, 0.5f
            );
        }

        private void preloadMotions() throws IOException {
            for (int groupIndex = 0; groupIndex < setting.getMotionGroupCount(); groupIndex++) {
                String group = setting.getMotionGroupName(groupIndex);
                if (idleGroup == null || "idle".equalsIgnoreCase(group)) idleGroup = group;
                for (int index = 0; index < setting.getMotionCount(group); index++) {
                    String fileName = setting.getMotionFileName(group, index);
                    if (fileName.isEmpty()) continue;
                    CubismMotion motion = loadMotion(read(resolve(fileName)));
                    if (motion == null) continue;
                    float fadeIn = setting.getMotionFadeInTimeValue(group, index);
                    float fadeOut = setting.getMotionFadeOutTimeValue(group, index);
                    if (fadeIn >= 0f) motion.setFadeInTime(fadeIn);
                    if (fadeOut >= 0f) motion.setFadeOutTime(fadeOut);
                    List<com.live2d.sdk.cubism.framework.id.CubismId> eyeIds = new ArrayList<>();
                    for (int eye = 0; eye < setting.getEyeBlinkParameterCount(); eye++) eyeIds.add(setting.getEyeBlinkParameterId(eye));
                    List<com.live2d.sdk.cubism.framework.id.CubismId> lipIds = new ArrayList<>();
                    for (int lip = 0; lip < setting.getLipSyncParameterCount(); lip++) lipIds.add(setting.getLipSyncParameterId(lip));
                    motion.setEffectIds(eyeIds, lipIds);
                    motions.put(group + "_" + index, motion);
                    if (!"idle".equalsIgnoreCase(group)) interactionMotions.add(group + "_" + index);
                }
            }
        }

        private void setupTextures() {
            for (int index = 0; index < setting.getTextureCount(); index++) {
                String name = setting.getTextureFileName(index);
                if (name.isEmpty()) continue;
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inPremultiplied = true;
                Bitmap bitmap = BitmapFactory.decodeFile(resolve(name).getAbsolutePath(), options);
                if (bitmap == null) throw new IllegalArgumentException("Texture cannot be decoded: " + name);
                int[] id = new int[1];
                GLES20.glGenTextures(1, id, 0);
                GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, id[0]);
                GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0);
                GLES20.glGenerateMipmap(GLES20.GL_TEXTURE_2D);
                GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR_MIPMAP_LINEAR);
                GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
                this.<CubismRendererAndroid>getRenderer().bindTexture(index, id[0]);
                bitmap.recycle();
            }
            this.<CubismRendererAndroid>getRenderer().isPremultipliedAlpha(true);
        }

        void update(float delta) {
            elapsedSeconds += delta;
            motionUpdated = false;
            model.loadParameters();
            if (motionManager.isFinished()) startIdle();
            else motionUpdated = motionManager.updateMotion(model, delta);
            model.saveParameters();
            updateScheduler.onLateUpdate(model, delta);
            if (CubismSignals.isSpeaking()) {
                float mouth = 0.25f + 0.65f * Math.abs((float) Math.sin(elapsedSeconds * 12.5f));
                model.addParameterValue(mouthOpenId, mouth, 0.85f);
            }
            model.update();
        }

        private void startIdle() {
            if (idleGroup == null) return;
            int count = Math.max(1, setting.getMotionCount(idleGroup));
            ACubismMotion motion = motions.get(idleGroup + "_" + (idleIndex++ % count));
            if (motion != null) motionManager.startMotionPriority(motion, 1);
        }

        void playInteraction(float x, float y) {
            if (!interactionMotions.isEmpty()) {
                String preferredGroup = y >= 0f ? "taphead_" : "tapbody_";
                List<String> preferred = new ArrayList<>();
                for (String key : interactionMotions) {
                    if (key.toLowerCase(java.util.Locale.ROOT).startsWith(preferredGroup)) preferred.add(key);
                }
                List<String> candidates = preferred.isEmpty() ? interactionMotions : preferred;
                String key = candidates.get(interactionIndex++ % candidates.size());
                ACubismMotion motion = motions.get(key);
                if (motion != null) motionManager.startMotionPriority(motion, 3);
            }
            if (!expressions.isEmpty()) {
                CubismExpressionMotion expression = expressions.get(expressionIndex++ % expressions.size());
                expressionManager.startMotionPriority(expression, 3);
            }
        }

        void draw(CubismMatrix44 matrix) {
            CubismMatrix44.multiply(modelMatrix.getArray(), matrix.getArray(), matrix.getArray());
            this.<CubismRendererAndroid>getRenderer().setMvpMatrix(matrix);
            this.<CubismRendererAndroid>getRenderer().drawModel();
        }

        private File resolve(String relative) {
            return new File(home, relative.replace('/', File.separatorChar));
        }

        private static byte[] read(File file) throws IOException {
            return Files.readAllBytes(file.toPath());
        }

        @Override
        public void close() {
            delete();
        }
    }
}
