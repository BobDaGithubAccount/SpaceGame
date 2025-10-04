package org.jephacake.world;

        import org.jephacake.renderer.Renderer;
        import org.jephacake.renderer.TextureAtlas;

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

            public World(TextureAtlas atlas, WorldGenerator generator, File saveFile, int renderDistance) {
                this.atlas = atlas;
                this.generator = generator;
                this.saveFile = saveFile;
                this.renderDistance = renderDistance;
                loadFromDisk();
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
                    pendingUnload.add(c); // defer destruction
                }
            }

            public void updateAndRender(Renderer renderer, float camX, float camY, float camZ) {
                int cx = (int)Math.floor(camX / Chunk.SIZE);
                int cy = (int)Math.floor(camY / Chunk.SIZE);
                int cz = (int)Math.floor(camZ / Chunk.SIZE);

                // Load chunks in range
                for (int x = cx - renderDistance; x <= cx + renderDistance; x++) {
                    for (int y = cy - renderDistance; y <= cy + renderDistance; y++) {
                        for (int z = cz - renderDistance; z <= cz + renderDistance; z++) {
                            loadOrGenerateChunk(x, y, z);
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
                for (Chunk c : toUnload) unloadChunk(c.getCX(), c.getCY(), c.getCZ());

                // Rebuild dirty
                for (Chunk c : dirtyChunks) c.rebuildMesh(atlas);
                dirtyChunks.clear();

                // Render live chunks
                for (Chunk c : chunks.values()) c.render(renderer);

                // Now it’s safe to free GL stuff
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
        }