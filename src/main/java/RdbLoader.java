import java.io.*;
import java.util.*;

public class RdbLoader {
    public static Map<String, String> loadRdbData(String filePath) {
        Map<String, String> data = new HashMap<>();
        try (InputStream in = new FileInputStream(filePath)) {
            in.skip(9); // skip header
            int b;
            while ((b = in.read()) != -1) {
                System.out.println("RDB byte: " + String.format("0x%02X", b));
                if (b == 0xFE) { 
                    readSize(in); readSize(in); readSize(in);
                } else if (b == 0xFC || b == 0xFD) { 
                    int len = (b == 0xFC) ? 8 : 4;
                    in.skip(len);
                } else if (b == 0x00) { // string type
                    String key = readString(in);
                    String value = readString(in);
                    // Fix: if key is empty, treat value as key and next string as value
                    if (key != null && !key.isEmpty()) {
                        System.out.println("Loaded key='" + key + "', value='" + value + "'");
                        data.put(key, value);
                    } else {
                        // key is empty, value is actual key, so read another string for actual value
                        String realKey = value;
                        String realValue = readString(in);
                        System.out.println("Loaded key='" + realKey + "', value='" + realValue + "'");
                        data.put(realKey, realValue);
                    }
                } else if (b == 0xFF) {
                    break;
                }
            }
        } catch (IOException e) {
            System.out.println("Exception while loading RDB data: " + e.getMessage());
        }
        System.out.println("Final loaded RDB data: " + data);
        return data;
    }

    private static int readSize(InputStream in) throws IOException {
        int first = in.read();
        int type = (first & 0xC0) >> 6;
        if (type == 0) return first & 0x3F;
        else if (type == 1) {
            int second = in.read();
            return ((first & 0x3F) << 8) | second;
        } else if (type == 2) {
            int[] bytes = new int[4];
            for (int i = 0; i < 4; i++) bytes[i] = in.read();
            return (bytes[0] << 24) | (bytes[1] << 16) | (bytes[2] << 8) | bytes[3];
        } else return 0;
    }

    private static String readString(InputStream in) throws IOException {
        int first = in.read();
        int type = (first & 0xC0) >> 6;
        int length = 0;
        if (type == 0) length = first & 0x3F;
        else if (type == 1) {
            int second = in.read();
            length = ((first & 0x3F) << 8) | second;
        } else if (type == 2) {
            int[] bytes = new int[4];
            for (int i = 0; i < 4; i++) bytes[i] = in.read();
            length = (bytes[0] << 24) | (bytes[1] << 16) | (bytes[2] << 8) | bytes[3];
        } else if (type == 3) {
            throw new IOException("Unsupported string encoding");
        }
        byte[] buf = new byte[length];
        int off = 0;
        while (off < length) {
            int n = in.read(buf, off, length - off);
            if (n == -1) throw new IOException("Unexpected end of stream");
            off += n;
        }
        String s = new String(buf, "UTF-8");
        System.out.println("RDB readString: length=" + length + ", value='" + s + "'");
        return s;
    }
}