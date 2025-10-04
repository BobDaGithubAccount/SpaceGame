package org.jephacake.renderer;

import org.joml.Matrix4f;
import org.joml.Vector3f;

/**
 * Simple fly camera (position + Euler yaw/pitch). Returns a view matrix compatible with Renderer.
 */
public class Camera {
    private final Vector3f position = new Vector3f(0, 0, 0);
    private float yaw = -90.0f;   // degrees: -90 so forward is -Z initially
    private float pitch = 0.0f;   // degrees
    private final Vector3f front = new Vector3f(0, 0, -1);
    private final Vector3f up = new Vector3f(0, 1, 0);
    private final Vector3f right = new Vector3f();
    private final Vector3f worldUp = new Vector3f(0, 1, 0);

    private float movementSpeed = 5.0f; // units/sec
    private float mouseSensitivity = 0.1f; // degrees per pixel

    public Camera() {
        updateVectors();
    }

    public Matrix4f getViewMatrix() {
        Vector3f center = new Vector3f(position).add(front);
        return new Matrix4f().lookAt(position, center, up);
    }

    public Vector3f getPosition() { return position; }
    public void setPosition(Vector3f p) { position.set(p); }

    public void setYawPitch(float yawDeg, float pitchDeg) {
        this.yaw = yawDeg;
        this.pitch = pitchDeg;
        clampPitch();
        updateVectors();
    }

    public float getYaw() { return yaw; }
    public float getPitch() { return pitch; }

    public void processMouseMovement(float deltaX, float deltaY) {
        this.yaw += deltaX * mouseSensitivity;
        this.pitch += -deltaY * mouseSensitivity; // invert Y to typical FPS behavior
        clampPitch();
        updateVectors();
    }

    private void clampPitch() {
        if (pitch > 89.9f) pitch = 89.9f;
        if (pitch < -89.9f) pitch = -89.9f;
    }

    public void processKeyboard(boolean forward, boolean back, boolean left, boolean rightKey, boolean upKey, boolean downKey, float deltaSeconds) {
        float velocity = movementSpeed * deltaSeconds;
        if (forward) position.fma(velocity, front);
        if (back) position.fma(-velocity, front);
        if (left) position.fma(-velocity, this.right);
        if (rightKey) position.fma(velocity, this.right);
        if (upKey) position.fma(velocity, worldUp);
        if (downKey) position.fma(-velocity, worldUp);
    }

    public void setMovementSpeed(float speed) { this.movementSpeed = speed; }
    public void setMouseSensitivity(float sens) { this.mouseSensitivity = sens; }

    private void updateVectors() {
        // Convert yaw/pitch degrees to a direction vector
        float yawRad = (float) Math.toRadians(yaw);
        float pitchRad = (float) Math.toRadians(pitch);
        front.x = (float) (Math.cos(yawRad) * Math.cos(pitchRad));
        front.y = (float) Math.sin(pitchRad);
        front.z = (float) (Math.sin(yawRad) * Math.cos(pitchRad));
        front.normalize();

        // Recalculate right and up
        front.cross(worldUp, right).normalize();
        right.cross(front, up).normalize();
    }
}
