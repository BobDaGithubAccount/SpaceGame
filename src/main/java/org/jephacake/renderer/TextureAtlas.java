package org.jephacake.renderer;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.List;

/**
 * Lightweight TextureAtlas:
 *  - packs PNG tiles into a grid (cols x rows) where tile size == max tile W/H
 *  - does CPU-only image composition (no Graphics2D draw calls)
 *  - does NOT create the GL texture until uploadToGL() is called (so you control GL timing)
 */
public final class TextureAtlas implements AutoCloseable {
    // CPU-side atlas data
    private final int atlasWidth;
    private final int atlasHeight;
    private final int tileW;
    private final int tileH;
    private final int tilesPerRow;
    private final int tilesPerColumn;
    private final float padU;
    private final float padV;
    private final List<String> tileNames;
    private final int[] pixels; // ARGB in Java int form (row-major, top-to-bottom)
    private final int padding;

    // GL texture (lazy, created by uploadToGL)
    private TextureGL texture; // null until uploadToGL()

    private TextureAtlas(int atlasWidth, int atlasHeight, int tileW, int tileH,
                         int padding, List<String> tileNames, int[] pixels) {
        this.atlasWidth = atlasWidth;
        this.atlasHeight = atlasHeight;
        this.tileW = tileW;
        this.tileH = tileH;
        this.tilesPerRow = atlasWidth / tileW;
        this.tilesPerColumn = atlasHeight / tileH;
        this.padU = padding / (float) atlasWidth;
        this.padV = padding / (float) atlasHeight;
        this.tileNames = Collections.unmodifiableList(new ArrayList<>(tileNames));
        this.pixels = pixels;
        this.padding = padding;

        System.out.println("TextureAtlas (CPU) created: " + atlasWidth + "x" + atlasHeight +
                ", tile " + tileW + "x" + tileH + ", tiles=" + tileNames.size());
    }

    /**
     * Build atlas CPU data from package resources. This does NOT create a GL texture.
     *
     * @param packagePath e.g. "org/jephacake/assets/textures"
     */
    public static TextureAtlas buildFromPackage(String packagePath) throws IOException {
        int paddingPixels = 0;
        System.out.println("Building TextureAtlas from package: " + packagePath);
        List<String> files = ResourceLoader.listResources(packagePath);
        List<String> pngs = new ArrayList<>();
        for (String f : files) {
            String lowered = f.toLowerCase(Locale.ROOT);
            if (lowered.endsWith(".png")) pngs.add(f);
        }
        Collections.sort(pngs);

        if (pngs.isEmpty()) throw new IOException("No PNG textures found in package: " + packagePath);

        List<BufferedImage> images = new ArrayList<>();
        List<String> names = new ArrayList<>();
        int maxW = 0, maxH = 0;

        // Load all images as BufferedImage (ImageIO.read)
        for (String filename : pngs) {
            String resource = packagePath + "/" + filename;
            System.out.println("Loading texture: " + resource);
            try (InputStream is = ResourceLoader.getResource(resource)) {
                if (is == null) throw new IOException("Missing texture resource: " + resource);
                BufferedImage img = ImageIO.read(is);
                if (img == null) throw new IOException("Failed to decode image: " + resource);
                // convert to TYPE_INT_ARGB if needed so we can copy pixels easily
                if (img.getType() != BufferedImage.TYPE_INT_ARGB) {
                    BufferedImage conv = new BufferedImage(img.getWidth(), img.getHeight(), BufferedImage.TYPE_INT_ARGB);
                    conv.getGraphics().drawImage(img, 0, 0, null);
                    img = conv;
                }
                images.add(img);
                String name = filename;
                if (name.endsWith(".png")) name = name.substring(0, name.length() - 4);
                names.add(name);
                maxW = Math.max(maxW, img.getWidth());
                maxH = Math.max(maxH, img.getHeight());
            }
        }

        int n = images.size();
        int cols = (int) Math.ceil(Math.sqrt(n));
        int rows = (int) Math.ceil((double) n / cols);

        int tileW = maxW;
        int tileH = maxH;
        int atlasW = cols * tileW;
        int atlasH = rows * tileH;

        System.out.println("Atlas layout: " + cols + " cols x " + rows + " rows, tile size: " + tileW + "x" + tileH);

        // Create a Java int[] pixel buffer (top-to-bottom)
        int[] atlasPixels = new int[atlasW * atlasH];
        // initialize to transparent
        Arrays.fill(atlasPixels, 0);

        // Copy each image into the atlas pixel buffer (centered in tile cell)
        for (int i = 0; i < n; i++) {
            BufferedImage img = images.get(i);
            int col = i % cols;
            int row = i / cols;
            int x0 = col * tileW + (tileW - img.getWidth()) / 2;
            int y0 = row * tileH + (tileH - img.getHeight()) / 2;
            int w = img.getWidth();
            int h = img.getHeight();

            int[] imgPixels = img.getRGB(0, 0, w, h, null, 0, w); // top-to-bottom
            for (int yy = 0; yy < h; yy++) {
                int dstOffset = (y0 + yy) * atlasW + x0;
                int srcOffset = yy * w;
                System.arraycopy(imgPixels, srcOffset, atlasPixels, dstOffset, w);
            }
        }

        return new TextureAtlas(atlasW, atlasH, tileW, tileH, paddingPixels, names, atlasPixels);
    }

    /**
     * Upload the GPU texture. Call only after the GL context/capabilities are current.
     */
    /**
     * Upload the GPU texture. Call only after the GL context/capabilities are current.
     * Uses the TextureGL(ByteBuffer, width, height) constructor so no AWT is used here.
     */
    public void uploadToGL() {
        if (this.texture != null) return; // already uploaded

        final int w = atlasWidth;
        final int h = atlasHeight;

        // Allocate direct ByteBuffer for RGBA data (bottom-to-top row order for GL).
        java.nio.ByteBuffer buf = java.nio.ByteBuffer.allocateDirect(w * h * 4);

        // pixels[] contains ARGB ints in top-to-bottom row order.
        // We iterate rows bottom->top and push r,g,b,a
        for (int y = h - 1; y >= 0; y--) {
            int rowBase = y * w;
            for (int x = 0; x < w; x++) {
                int argb = pixels[rowBase + x];
                byte a = (byte) ((argb >> 24) & 0xFF);
                byte r = (byte) ((argb >> 16) & 0xFF);
                byte g = (byte) ((argb >> 8) & 0xFF);
                byte b = (byte) (argb & 0xFF);
                buf.put(r).put(g).put(b).put(a);
            }
        }
        buf.flip();

        // Create the GL texture via the ByteBuffer constructor
        this.texture = new TextureGL(buf, w, h);
        System.out.println("Texture uploaded to GL: id=" + this.texture.getId());
    }

    /** Bind the atlas texture to a texture unit (will upload automatically if needed). */
    public void bind(int unit) {
        if (texture == null) uploadToGL();
        texture.bind(unit);
    }

    public TextureGL getTexture() {
        if (texture == null) uploadToGL();
        return texture;
    }

    /** Get normalized UV rect {u0,v0,u1,v1} for a tile index (0..n-1). v0 = bottom, v1 = top (GL coords). */
    public float[] getUVRect(int tileIndex) {
        if (tileIndex < 0) tileIndex = 0;
        if (tileIndex >= tileNames.size()) tileIndex = tileNames.size() - 1;

        int col = tileIndex % tilesPerRow;
        int row = tileIndex / tilesPerRow;

        float u0 = col * tileW / (float) atlasWidth;
        float u1 = (col + 1) * tileW / (float) atlasWidth;

        // OpenGL: 0 = bottom, 1 = top
        float v0 = 1.0f - (row + 1) * tileH / (float) atlasHeight; // bottom
        float v1 = 1.0f - row * tileH / (float) atlasHeight;       // top

        // Apply padding
        float pu = padding / (float) atlasWidth;
        float pv = padding / (float) atlasHeight;
        u0 += pu; u1 -= pu;
        v0 += pv; v1 -= pv;

        // Ensure proper ordering: u0<u1, v0<v1
        if (u0 > u1) { float t = u0; u0 = u1; u1 = t; }
        if (v0 > v1) { float t = v0; v0 = v1; v1 = t; }

        return new float[]{ u0, v0, u1, v1 };
    }

    public int getTileIndex(String name) {
        for (int i = 0; i < tileNames.size(); i++) if (tileNames.get(i).equals(name)) return i;
        return -1;
    }

    public List<String> getTileNames() { return tileNames; }

    @Override
    public void close() {
        if (texture != null) texture.close();
    }

    public int getTilesPerRow() { return tilesPerRow; }
    public int getTilesPerColumn() { return tilesPerColumn; }
    public int getTileWidth() { return tileW; }
    public int getTileHeight() { return tileH; }
}
