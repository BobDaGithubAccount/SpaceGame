package org.jephacake.renderer;

import java.io.IOException;
import java.io.InputStream;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * Utility to load resources from the classpath (not the assets package which is for raw assets).
 * Use paths like "org/jephacake/renderer/shaders/voxel.vert" (no leading slash).
 */
public final class ResourceLoader {
    private ResourceLoader() {}

    public static InputStream getResource(String path) {
        InputStream is = ResourceLoader.class.getClassLoader().getResourceAsStream(path);
        if (is != null) return is;
        return ResourceLoader.class.getResourceAsStream(path);
    }

    public static String readResourceAsString(String path) throws IOException {
        try (InputStream is = getResource(path)) {
            if (is == null) throw new IOException("Resource not found: " + path);
            try (BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) sb.append(line).append('\n');
                return sb.toString();
            }
        }
    }

    public static byte[] readResourceAsBytes(String path) throws IOException {
        try (InputStream is = getResource(path)) {
            if (is == null) throw new IOException("Resource not found: " + path);
            return is.readAllBytes();
        }
    }

    public static byte[] readTexture(String texture) throws IOException {
        String path = "org/jephacake/assets/textures/" + texture + ".png";
        try (InputStream is = getResource(path)) {
            if (is == null) throw new IOException("Texture not found: " + path);
            return is.readAllBytes();
        }
    }

    public static List<String> listResources(String resourcePath) throws IOException {
        List<String> result = new ArrayList<>();
        ClassLoader cl = ResourceLoader.class.getClassLoader();
        Enumeration<URL> urls = cl.getResources(resourcePath);
        while (urls.hasMoreElements()) {
            URL url = urls.nextElement();
            String protocol = url.getProtocol();
            if ("file".equals(protocol)) {
                try {
                    Path dir = Paths.get(url.toURI());
                    if (Files.exists(dir) && Files.isDirectory(dir)) {
                        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir)) {
                            for (Path p : stream) {
                                if (Files.isRegularFile(p)) {
                                    result.add(p.getFileName().toString());
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    // fallback: try URI->Path may throw on spaces; handle by decoding path
                    String path = URLDecoder.decode(url.getPath(), StandardCharsets.UTF_8);
                    Path dir = Paths.get(path);
                    if (Files.exists(dir) && Files.isDirectory(dir)) {
                        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir)) {
                            for (Path p : stream) {
                                if (Files.isRegularFile(p)) result.add(p.getFileName().toString());
                            }
                        }
                    }
                }
            } else if ("jar".equals(protocol)) {
                // jar:file:/...!/org/jephacake/assets/textures
                JarURLConnection jconn = (JarURLConnection) url.openConnection();
                JarFile jar = jconn.getJarFile();
                String prefix = resourcePath.endsWith("/") ? resourcePath : resourcePath + "/";
                Enumeration<JarEntry> entries = jar.entries();
                while (entries.hasMoreElements()) {
                    JarEntry je = entries.nextElement();
                    String name = je.getName();
                    if (name.startsWith(prefix) && !je.isDirectory()) {
                        String rel = name.substring(prefix.length());
                        // exclude resources in subfolders - keep only top-level files
                        if (!rel.isEmpty() && !rel.contains("/")) {
                            result.add(rel);
                        }
                    }
                }
            } else {
                // other protocols are rare; try to treat as file
                try {
                    Path dir = Paths.get(url.toURI());
                    if (Files.exists(dir) && Files.isDirectory(dir)) {
                        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir)) {
                            for (Path p : stream) if (Files.isRegularFile(p)) result.add(p.getFileName().toString());
                        }
                    }
                } catch (Exception ex) {
                    // ignore - can't list
                }
            }
        }

        return result;
    }

    public static String getJarDirectory() {
        try {
            String path = ResourceLoader.class
                    .getProtectionDomain()
                    .getCodeSource()
                    .getLocation()
                    .toURI()
                    .getPath();
            Path jarPath = Paths.get(path).toAbsolutePath();
            if (Files.isDirectory(jarPath)) {
                // Running from classes directory
                return jarPath.toString();
            } else {
                // Running from JAR
                return jarPath.getParent().toString();
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to determine JAR directory", e);
        }
    }
}
