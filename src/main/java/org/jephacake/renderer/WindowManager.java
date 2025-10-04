package org.jephacake.renderer;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.glfw.*;
import org.lwjgl.system.MemoryStack;

import java.nio.DoubleBuffer;
import java.nio.IntBuffer;
import java.util.HashSet;
import java.util.Set;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL.createCapabilities;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.system.MemoryUtil.NULL;

/**
 * GLFW window manager with input handling and simple camera support.
 *
 * Usage:
 *  - create WindowManager(...)
 *  - get camera via getCamera(), set initial position/yaw/pitch
 *  - call pollEvents() each frame, then call updateCamera(deltaSeconds) to process movement based on keys
 *
 * Keybindings:
 *  - WASD: movement in plane relative to camera
 *  - SPACE: up
 *  - LEFT_SHIFT: down
 *  - ESC: window close (press)
 *  - RIGHT_MOUSE button toggles cursor grab (when grabbed, mouse moves camera)
 */
public class WindowManager implements AutoCloseable {
    private long window;
    private int framebufferWidth;
    private int framebufferHeight;
    private GLFWKeyCallback keyCallback;
    private GLFWFramebufferSizeCallback fbCallback;
    private GLFWCursorPosCallback cursorPosCallback;
    private GLFWMouseButtonCallback mouseButtonCallback;

    // input state
    private final Set<Integer> keysDown = new HashSet<>();
    private double lastMouseX, lastMouseY;
    private boolean firstMouse = true;
    private boolean cursorGrabbed = true;
    private boolean rightMouseDown = false;
    private float mouseDeltaX = 0f;
    private float mouseDeltaY = 0f;

    // camera
    private final Camera camera = new Camera();

    public WindowManager(int width, int height, String title) {
        GLFWErrorCallback.createPrint(System.err).set();
        if (!glfwInit()) throw new IllegalStateException("Unable to initialize GLFW");

        // Configure window
        glfwDefaultWindowHints();
        glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 3);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 3);
        glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);

        window = glfwCreateWindow(width, height, title, NULL, NULL);
        if (window == NULL) throw new RuntimeException("Failed to create GLFW window");

        // center
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer pW = stack.mallocInt(1);
            IntBuffer pH = stack.mallocInt(1);
            glfwGetWindowSize(window, pW, pH);
            long primary = glfwGetPrimaryMonitor();
            var vidMode = glfwGetVideoMode(primary);
            if (vidMode != null) {
                glfwSetWindowPos(window,
                        (vidMode.width() - pW.get(0)) / 2,
                        (vidMode.height() - pH.get(0)) / 2);
            }
        }

        glfwMakeContextCurrent(window);
        glfwSwapInterval(1);
        glfwShowWindow(window);

        createCapabilities();

        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer fbW = stack.mallocInt(1);
            IntBuffer fbH = stack.mallocInt(1);
            glfwGetFramebufferSize(window, fbW, fbH);
            framebufferWidth = fbW.get(0);
            framebufferHeight = fbH.get(0);
            glViewport(0, 0, framebufferWidth, framebufferHeight);
        }

        // framebuffer resize
        fbCallback = new GLFWFramebufferSizeCallback() {
            @Override
            public void invoke(long win, int w, int h) {
                framebufferWidth = w;
                framebufferHeight = h;
                glViewport(0, 0, w, h);
            }
        };
        glfwSetFramebufferSizeCallback(window, fbCallback);

        // key callback
        keyCallback = new GLFWKeyCallback() {
            @Override
            public void invoke(long win, int key, int scancode, int action, int mods) {
                if (action == GLFW_PRESS) {
                    keysDown.add(key);
                    if (key == GLFW_KEY_ESCAPE) {
                        glfwSetWindowShouldClose(win, true);
                    }
                } else if (action == GLFW_RELEASE) {
                    keysDown.remove(key);
                }
            }
        };
        glfwSetKeyCallback(window, keyCallback);

        // mouse position callback
        cursorPosCallback = new GLFWCursorPosCallback() {
            @Override
            public void invoke(long win, double xpos, double ypos) {
                if (firstMouse) {
                    lastMouseX = xpos;
                    lastMouseY = ypos;
                    firstMouse = false;
                }
                mouseDeltaX += (float) (xpos - lastMouseX);
                mouseDeltaY += (float) (ypos - lastMouseY);
                lastMouseX = xpos;
                lastMouseY = ypos;
            }
        };
        glfwSetCursorPosCallback(window, cursorPosCallback);

        // mouse button callback (toggle grab on RMB)
        mouseButtonCallback = new GLFWMouseButtonCallback() {
            @Override
            public void invoke(long win, int button, int action, int mods) {
                if (button == GLFW_MOUSE_BUTTON_RIGHT) {
                    if (action == GLFW_PRESS) {
                        rightMouseDown = true;
                        setCursorGrabbed(true);
                    } else if (action == GLFW_RELEASE) {
                        rightMouseDown = false;
                        // optionally keep grabbed — we keep grabbed while RMB down
                        setCursorGrabbed(false);
                    }
                }
            }
        };
        glfwSetMouseButtonCallback(window, mouseButtonCallback);

        // initial cursor state
        setCursorGrabbed(true);
    }

    public Camera getCamera() { return camera; }

    public boolean isKeyDown(int key) {
        return keysDown.contains(key);
    }

    public void pollEvents() {
        glfwPollEvents();
    }

    public void swapBuffers() {
        glfwSwapBuffers(window);
    }

    public boolean shouldClose() {
        return glfwWindowShouldClose(window);
    }

    public int getFramebufferWidth() { return framebufferWidth; }
    public int getFramebufferHeight() { return framebufferHeight; }
    public long getWindowHandle() { return window; }

    /**
     * Call once per frame to apply input to camera. deltaSeconds = frame time.
     */
    public void updateCamera(float deltaSeconds) {
        // Keyboard movement: WASD (forward/back/left/right), space up, left shift down
        boolean forward = isKeyDown(GLFW_KEY_W);
        boolean back = isKeyDown(GLFW_KEY_S);
        boolean left = isKeyDown(GLFW_KEY_A);
        boolean right = isKeyDown(GLFW_KEY_D);
        boolean up = isKeyDown(GLFW_KEY_SPACE);
        boolean down = isKeyDown(GLFW_KEY_LEFT_SHIFT) || isKeyDown(GLFW_KEY_RIGHT_SHIFT);

        camera.processKeyboard(forward, back, left, right, up, down, deltaSeconds);

        // Mouse look (only when cursor grabbed)
        if (cursorGrabbed) {
            float dx = mouseDeltaX;
            float dy = mouseDeltaY;
            if (dx != 0f || dy != 0f) {
                camera.processMouseMovement(dx, dy);
            }
        }

        // reset deltas
        mouseDeltaX = 0f;
        mouseDeltaY = 0f;
    }

    public void setCursorGrabbed(boolean grabbed) {
        cursorGrabbed = grabbed;
        if (grabbed) {
            glfwSetInputMode(window, GLFW_CURSOR, GLFW_CURSOR_DISABLED);
            // reset mouse tracker
            firstMouse = true;
        } else {
            glfwSetInputMode(window, GLFW_CURSOR, GLFW_CURSOR_NORMAL);
        }
    }

    @Override
    public void close() {
        if (keyCallback != null) keyCallback.free();
        if (fbCallback != null) fbCallback.free();
        if (cursorPosCallback != null) cursorPosCallback.free();
        if (mouseButtonCallback != null) mouseButtonCallback.free();
        if (window != NULL) glfwDestroyWindow(window);
        glfwTerminate();
        var cb = GLFWErrorCallback.get(window);
        System.out.println(cb.toString());
    }
}
