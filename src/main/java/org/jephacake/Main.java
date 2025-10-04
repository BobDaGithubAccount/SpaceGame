package org.jephacake;

import org.jephacake.block.BlockRegistry;
import org.jephacake.renderer.*;
import org.jephacake.world.FlatWorldGenerator;
import org.jephacake.world.World;
import org.joml.Vector3f;

import java.io.File;

public class Main {

    //TODO s are in the todos.txt file

    public static TextureAtlas atlas;

    public static void main(String[] args) throws Exception {

        try (WindowManager wm = new WindowManager(1280, 720, "SpaceGame")) {
            Renderer renderer = new Renderer(wm.getFramebufferWidth(), wm.getFramebufferHeight());
            Camera cam = wm.getCamera();
            cam.setPosition(new org.joml.Vector3f(10, 10, 10));
            cam.setYawPitch(-120f, -20f); // point roughly at origin

            atlas = TextureAtlas.buildFromPackage("org/jephacake/assets/textures", 0); // ensure no padding as other classes expect this to be the case - TODO: remove this option as well as chunk size optionality (sticking with a 16x616x16 approach is clearly the best).
            atlas.uploadToGL();

            /// /////////////////////

            BlockRegistry.init();

            World world = new World(atlas, new FlatWorldGenerator(8), new File(ResourceLoader.getJarDirectory() + "/saves/world.dat"), 2);

            /// /////////////////////

            long last = System.nanoTime();
            while (!wm.shouldClose()) {
                long now = System.nanoTime();
                float delta = (now - last) / 1_000_000_000f;
                last = now;

                wm.pollEvents();
                wm.updateCamera(delta);

                renderer.beginFrame();
                renderer.setView(cam.getViewMatrix());
                renderer.setDirectionalLight(new Vector3f(0,0,0), new Vector3f(1,1,0.9f), true);
                world.updateAndRender(renderer, cam.getPosition().x, cam.getPosition().y, cam.getPosition().z);
                renderer.endFrame();

                wm.swapBuffers();
            }

            world.close();
            renderer.close();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

}