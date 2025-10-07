package org.jephacake.world;

import org.jephacake.configuration.Options;
import org.jephacake.renderer.*;
import org.joml.Vector3f;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;

public class World implements AutoCloseable {

    private final Map<String, Chunk> chunks = new ConcurrentHashMap<>();
    private final Map<String, int[]> savedChunkData = new ConcurrentHashMap<>();
    private final TextureAtlas atlas;
    private final WorldGenerator generator;
    private final File saveFile;
    private final int renderDistance;

    private final ChunkRenderer chunkRenderer = new ChunkRenderer();
    public Vector3f position;

    // async meshing
    private final ExecutorService meshingPool =
            Executors.newFixedThreadPool(Math.max(2, 4));
    private final ConcurrentLinkedQueue<MeshJobResult> completedMeshes = new ConcurrentLinkedQueue<>();

    private record MeshJobResult(Chunk chunk, ChunkMesher.MeshData data) {}

    public World(TextureAtlas atlas, WorldGenerator generator, File saveFile, int renderDistance) {
        this(atlas, generator, saveFile, renderDistance, new Vector3f(0, 0, 0));
    }

    public World(TextureAtlas atlas, WorldGenerator generator, File saveFile,
                 int renderDistance, Vector3f position) {
        this.atlas = atlas;
        this.generator = generator;
        this.saveFile = saveFile;
        this.renderDistance = renderDistance;
        this.position = position;
        loadFromDisk();
        System.out.println("World save file: " + saveFile.getAbsolutePath());
    }

    private static String key(int cx, int cy, int cz) {
        return cx + "," + cy + "," + cz;
    }

    private void loadFromDisk() {
        if (!saveFile.exists()) return;
        try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(saveFile))) {
            Object obj = in.readObject();
            if (obj instanceof Map<?,?> m) {
                for (var e : m.entrySet()) {
                    savedChunkData.put((String) e.getKey(), (int[]) e.getValue());
                }
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void saveToDisk() {
        try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(saveFile))) {
            out.writeObject(savedChunkData);
        } catch (IOException e) { e.printStackTrace(); }
    }

    public Chunk loadOrGenerateChunk(int cx, int cy, int cz) {
        String k = key(cx, cy, cz);
        Chunk existing = chunks.get(k);
        if (existing != null) return existing;

        int[] saved = savedChunkData.get(k);
        Chunk c = new Chunk(cx, cy, cz);

        if (saved != null) System.arraycopy(saved, 0, c.getVoxelData(), 0, saved.length);
        else c = generator.generateChunk(cx, cy, cz);

        chunks.put(k, c);
        queueMeshBuild(c);
        return c;
    }

    private void unloadChunk(int cx, int cy, int cz) {
        String k = key(cx, cy, cz);
        Chunk c = chunks.remove(k);
        if (c != null) {
            savedChunkData.put(k, c.getVoxelData().clone());
            c.close();
        }
    }

    private void queueMeshBuild(Chunk chunk) {
        meshingPool.submit(() -> {
            try {
                ChunkMesher.MeshData data = chunk.generateMeshData(this, atlas);
                completedMeshes.add(new MeshJobResult(chunk, data));
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    private void applyCompletedMeshes() {
        int applied = 0;
        MeshJobResult res;

        // Process completed meshes gradually to avoid frame spikes
        while (applied < Options.MAX_MESH_UPLOADS_PER_FRAME && (res = completedMeshes.poll()) != null) {
            try {
                Chunk chunk = res.chunk();

                if (!chunks.containsValue(chunk))
                    continue;

                chunk.applyMeshData(res.data(), atlas);
                applied++;

            } catch (Exception e) {
                System.err.println("[World] Failed to apply mesh data:");
                e.printStackTrace();
            }
        }
    }

    /** Retrieve a block globally, across chunks (used by mesher). */
    public int getBlockGlobal(int wx, int wy, int wz) {
        int cx = Math.floorDiv(wx, Chunk.SIZE);
        int cy = Math.floorDiv(wy, Chunk.SIZE);
        int cz = Math.floorDiv(wz, Chunk.SIZE);
        Chunk c = chunks.get(key(cx, cy, cz));
        if (c == null) return 0;
        int lx = Math.floorMod(wx, Chunk.SIZE);
        int ly = Math.floorMod(wy, Chunk.SIZE);
        int lz = Math.floorMod(wz, Chunk.SIZE);
        return c.getBlock(lx, ly, lz);
    }

    /** Sets a block and queues remeshing for this and neighbor chunks if edge touched. */
    public void setBlock(int wx, int wy, int wz, int blockId) {
        int cx = Math.floorDiv(wx, Chunk.SIZE);
        int cy = Math.floorDiv(wy, Chunk.SIZE);
        int cz = Math.floorDiv(wz, Chunk.SIZE);

        int lx = Math.floorMod(wx, Chunk.SIZE);
        int ly = Math.floorMod(wy, Chunk.SIZE);
        int lz = Math.floorMod(wz, Chunk.SIZE);

        Chunk c = loadOrGenerateChunk(cx, cy, cz);
        c.setBlock(lx, ly, lz, blockId);
        queueMeshBuild(c);

        // Regenerate neighbors if this block touches a chunk boundary
        if (lx == 0) queueIfLoaded(cx - 1, cy, cz);
        if (lx == Chunk.SIZE - 1) queueIfLoaded(cx + 1, cy, cz);
        if (ly == 0) queueIfLoaded(cx, cy - 1, cz);
        if (ly == Chunk.SIZE - 1) queueIfLoaded(cx, cy + 1, cz);
        if (lz == 0) queueIfLoaded(cx, cy, cz - 1);
        if (lz == Chunk.SIZE - 1) queueIfLoaded(cx, cy, cz + 1);
    }

    private void queueIfLoaded(int cx, int cy, int cz) {
        Chunk neighbor = chunks.get(key(cx, cy, cz));
        if (neighbor != null) queueMeshBuild(neighbor);
    }

    public void updateAndRender(Renderer renderer, float camX, float camY, float camZ) {
        int cx = (int) Math.floor(camX / Chunk.SIZE);
        int cy = (int) Math.floor(camY / Chunk.SIZE);
        int cz = (int) Math.floor(camZ / Chunk.SIZE);

        // unload chunks out of range immediately
        for (Iterator<Map.Entry<String, Chunk>> it = chunks.entrySet().iterator(); it.hasNext();) {
            Chunk c = it.next().getValue();
            int dx = c.getCX() - cx;
            int dy = c.getCY() - cy;
            int dz = c.getCZ() - cz;
            if (Math.abs(dx) > renderDistance || Math.abs(dy) > renderDistance || Math.abs(dz) > renderDistance) {
                unloadChunk(c.getCX(), c.getCY(), c.getCZ());
                it.remove();
            }
        }

        // load chunks in range (meshing async)
        for (int x = cx - renderDistance; x <= cx + renderDistance; x++) {
            for (int y = cy - renderDistance; y <= cy + renderDistance; y++) {
                for (int z = cz - renderDistance; z <= cz + renderDistance; z++) {
                    loadOrGenerateChunk(x, y, z);
                }
            }
        }

        // apply finished meshes
        applyCompletedMeshes();

        // render
        chunkRenderer.renderChunks(chunks.values(), renderer, this);
    }

    @Override
    public void close() {
        meshingPool.shutdownNow();
        for (Chunk c : chunks.values()) {
            savedChunkData.put(key(c.getCX(), c.getCY(), c.getCZ()), c.getVoxelData().clone());
            c.close();
        }
        chunks.clear();
        saveToDisk();
    }

    public ChunkRenderer getChunkRenderer() {
        return chunkRenderer;
    }
}
