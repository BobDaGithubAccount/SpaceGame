package org.jephacake.world;

import org.jephacake.renderer.*;
import org.joml.Vector3f;

import java.io.*;
import java.util.*;

public class World implements AutoCloseable {
    private final Map<String, Chunk> chunks = new HashMap<>();
    private final Set<Chunk> dirtyChunks = new HashSet<>();
    private final TextureAtlas atlas;
    private final WorldGenerator generator;
    private final File saveFile;
    private final Map<String, int[]> savedChunkData = new HashMap<>();
    private final int renderDistance;
    private final Queue<Chunk> pendingUnload = new ArrayDeque<>();

    private final ChunkRenderer chunkRenderer = new ChunkRenderer();

    public Vector3f position;

    public World(TextureAtlas atlas, WorldGenerator generator, File saveFile, int renderDistance) {
        this(atlas, generator, saveFile, renderDistance, new Vector3f(0f, 0f, 0f));
    }

    public World(TextureAtlas atlas, WorldGenerator generator, File saveFile, int renderDistance,
                 Vector3f position) {
        this.atlas = atlas;
        this.generator = generator;
        this.saveFile = saveFile;
        this.renderDistance = renderDistance;
        this.position = position;
        loadFromDisk();
        System.out.println("Using world save file: " + saveFile.getAbsolutePath());
    }

    private static String key(int cx, int cy, int cz) {
        return cx + "," + cy + "," + cz;
    }

    private void loadFromDisk() {
        if (!saveFile.exists()) return;
        try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(saveFile))) {
            Object obj = in.readObject();
            if (obj instanceof Map<?,?> m) {
                savedChunkData.clear();
                for (var e : m.entrySet()) {
                    savedChunkData.put((String) e.getKey(), (int[]) e.getValue());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void saveToDisk() {
        try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(saveFile))) {
            out.writeObject(savedChunkData);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Chunk loadOrGenerateChunk(int cx, int cy, int cz) {
        String k = key(cx, cy, cz);
        if (chunks.containsKey(k)) return chunks.get(k);

        int[] saved = savedChunkData.get(k);
        Chunk c = new Chunk(cx, cy, cz);

        if (saved != null) {
            int[] vox = saved;
            System.arraycopy(vox, 0, c.getVoxelData(), 0, vox.length);
        } else {
            c = generator.generateChunk(cx, cy, cz);
        }

        c.markDirty();
        chunks.put(k, c);
        dirtyChunks.add(c);
        return c;
    }

    public void unloadChunk(int cx, int cy, int cz) {
        String k = key(cx, cy, cz);
        Chunk c = chunks.remove(k);
        if (c != null) {
            savedChunkData.put(k, c.getVoxelData().clone());
            pendingUnload.add(c); // defer destruction (GL close happens in pending unload processing)
        }
    }

    /**
     * Return the block id at world coordinates. Missing chunks -> 0.
     */
    public int getBlockGlobal(int wx, int wy, int wz) {
        int cx = Math.floorDiv(wx, Chunk.SIZE);
        int cy = Math.floorDiv(wy, Chunk.SIZE);
        int cz = Math.floorDiv(wz, Chunk.SIZE);
        String k = key(cx, cy, cz);
        Chunk c = chunks.get(k);
        if (c == null) return 0;
        int lx = Math.floorMod(wx, Chunk.SIZE);
        int ly = Math.floorMod(wy, Chunk.SIZE);
        int lz = Math.floorMod(wz, Chunk.SIZE);
        return c.getBlock(lx, ly, lz);
    }

    public void updateAndRender(Renderer renderer, float camX, float camY, float camZ) {
        int cx = (int)Math.floor(camX / Chunk.SIZE);
        int cy = (int)Math.floor(camY / Chunk.SIZE);
        int cz = (int)Math.floor(camZ / Chunk.SIZE);

        boolean chunksChanged = false;

        // Load chunks in range
        for (int x = cx - renderDistance; x <= cx + renderDistance; x++) {
            for (int y = cy - renderDistance; y <= cy + renderDistance; y++) {
                for (int z = cz - renderDistance; z <= cz + renderDistance; z++) {
                    String k = key(x,y,z);
                    if (!chunks.containsKey(k)) {
                        loadOrGenerateChunk(x, y, z);
                        chunksChanged = true;
                    }
                }
            }
        }

        // Collect to unload (but don't destroy yet)
        List<Chunk> toUnload = new ArrayList<>();
        for (Chunk c : chunks.values()) {
            int dx = c.getCX() - cx;
            int dy = c.getCY() - cy;
            int dz = c.getCZ() - cz;
            if (Math.abs(dx) > renderDistance || Math.abs(dy) > renderDistance || Math.abs(dz) > renderDistance) {
                toUnload.add(c);
            }
        }
        for (Chunk c : toUnload) {
            unloadChunk(c.getCX(), c.getCY(), c.getCZ());
            chunksChanged = true;
        }

        // Rebuild only dirty chunks (synchronous)
        if (!dirtyChunks.isEmpty() || chunksChanged) {
            for (Chunk c : dirtyChunks) {
                c.rebuildMesh(atlas, this);
                System.out.println("Built mesh for chunk " + c.getCX() + "," + c.getCY() + "," + c.getCZ());
            }
            dirtyChunks.clear();
        }

        // Render - ChunkRenderer will apply per-chunk translation using chunk coords + world.position
        chunkRenderer.renderChunks(chunks.values(), renderer, this);

        // Now it’s safe to free chunk GL stuff that were pending unload
        while (!pendingUnload.isEmpty()) {
            pendingUnload.poll().close();
        }
    }

    public void setBlock(int wx, int wy, int wz, int blockId) {
        int cx = Math.floorDiv(wx, Chunk.SIZE);
        int cy = Math.floorDiv(wy, Chunk.SIZE);
        int cz = Math.floorDiv(wz, Chunk.SIZE);

        int lx = Math.floorMod(wx, Chunk.SIZE);
        int ly = Math.floorMod(wy, Chunk.SIZE);
        int lz = Math.floorMod(wz, Chunk.SIZE);

        Chunk c = loadOrGenerateChunk(cx, cy, cz);
        c.setBlock(lx, ly, lz, blockId);
        dirtyChunks.add(c);

        // Mark neighbors dirty if change touches a boundary; only mark loaded neighbors.
        if (lx == 0) markNeighborDirtyIfLoaded(cx - 1, cy, cz);
        if (lx == Chunk.SIZE - 1) markNeighborDirtyIfLoaded(cx + 1, cy, cz);
        if (ly == 0) markNeighborDirtyIfLoaded(cx, cy - 1, cz);
        if (ly == Chunk.SIZE - 1) markNeighborDirtyIfLoaded(cx, cy + 1, cz);
        if (lz == 0) markNeighborDirtyIfLoaded(cx, cy, cz - 1);
        if (lz == Chunk.SIZE - 1) markNeighborDirtyIfLoaded(cx, cy, cz + 1);
    }

    private void markNeighborDirtyIfLoaded(int ncx, int ncy, int ncz) {
        String nk = key(ncx, ncy, ncz);
        Chunk neighbor = chunks.get(nk);
        if (neighbor != null) {
            neighbor.markDirty();
            dirtyChunks.add(neighbor);
        }
    }

    @Override
    public void close() {
        for (Chunk c : chunks.values()) {
            savedChunkData.put(key(c.getCX(), c.getCY(), c.getCZ()), c.getVoxelData().clone());
            c.close();
        }
        saveToDisk();
        chunks.clear();
        while (!pendingUnload.isEmpty()) {
            pendingUnload.poll().close();
        }
    }

    public ChunkRenderer getChunkRenderer() {
        return chunkRenderer;
    }
}
