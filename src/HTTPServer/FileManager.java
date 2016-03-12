package HTTPServer;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.concurrent.ConcurrentHashMap;

public class FileManager {
    private String path;
    private static ConcurrentHashMap<String, byte[]> map = new ConcurrentHashMap<String, byte[]>();

    public FileManager(String path) {
        // "c:\folder\" --> "c:\folder"
        if (path.endsWith("/") || path.endsWith("\\"))
            path = path.substring(0, path.length() - 1);

        this.path = path;
    }

    public byte[] get(String url) {
        try {
            byte[] buf = map.get(url);
            if (buf != null) // in cache
                return buf;

            // "c:\folder" + "/index.html" -> "c:/folder/index.html"
            String fullPath = path.replace('\\', '/') + url;

            RandomAccessFile f = new RandomAccessFile(fullPath, "r");
            try {
                buf = new byte[(int) f.length()];
                f.read(buf, 0, buf.length);
            } finally {
                f.close();
            }

            map.put(url, buf); // put to cache
            return buf;
        } catch (IOException ex) {
            ex.printStackTrace();
            return null;
        }
    }
}
