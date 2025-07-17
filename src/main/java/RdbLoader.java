import java.io.*;
import java.util.*;

public class RdbLoader {
    public static Map<String, ValueWithExpiry> loadRdbData(String filePath) {
        Map<String, ValueWithExpiry> data = new HashMap<>();
        try (DataInputStream in = new DataInputStream(new FileInputStream(filePath))) {
            // Check magic
            byte[] magic = new byte[5];
            in.readFully(magic);
            if (!"REDIS".equals(new String(magic))) throw new IOException("Invalid RDB header");
            in.skipBytes(4);

            Long expiry = null;
            while (true) {
                int opcode;
                try {
                    opcode = in.readUnsignedByte();
                } catch (EOFException e) {
                    break;
                }
                if (opcode == 0xFF) break;

                switch (opcode) {
                    case 0xFB: // RESIZEDB
                        readLength(in);
                        readLength(in); 
                        break;
                    case 0xFA: // AUX
                        readString(in); readString(in);
                        break;
                    case 0xFC: // Expiry ms
                        expiry = Long.reverseBytes(in.readLong());
                        break;
                    case 0xFD: // Expiry s
                        expiry = Integer.reverseBytes(in.readInt()) * 1000L;
                        break;
                    case 0x00: { // String key-value
                        String key = readString(in);
                        String value = readString(in);
                        if (key != null && !key.isEmpty() && value != null) {
                            data.put(key, new ValueWithExpiry(value, expiry != null ? expiry : 0));
                        }
                        expiry = null;
                        break;
                    }
                    case 0xFE: // Select DB
                        in.readUnsignedByte(); 
                        break;
                    default:
                        return data;
                }
            }
        } catch (IOException e) {
            System.err.println("[RDB] Error loading: " + e.getMessage());
        }
        return data;
    }

    private static String readString(DataInputStream in) throws IOException {
        int firstByte = in.readUnsignedByte();
        int encType = (firstByte & 0xC0) >> 6;
        int length;
        switch (encType) {
            case 0: // 6-bit length
                length = firstByte & 0x3F;
                break;
            case 1: // 14-bit length
                length = ((firstByte & 0x3F) << 8) | in.readUnsignedByte();
                break;
            case 2: // 32-bit length
                length = in.readInt();
                break;
            case 3:
                int encVal = firstByte & 0x3F;
                if (encVal == 0) { in.readUnsignedByte(); return ""; } // int8
                if (encVal == 1) { in.readUnsignedByte(); in.readUnsignedByte(); return ""; } // int16
                if (encVal == 2) { in.readInt(); return ""; } // int32
                return "";
            default:
                return "";
        }
        byte[] bytes = new byte[length];
        in.readFully(bytes);
        return new String(bytes, "UTF-8");
    }

    private static int readLength(DataInputStream in) throws IOException {
        int first = in.readUnsignedByte();
        int type = (first & 0xC0) >> 6;
        if (type == 0) return first & 0x3F;
        if (type == 1) return ((first & 0x3F) << 8) | in.readUnsignedByte();
        if (type == 2) return in.readInt();
        throw new IOException("Unsupported length encoding");
    }
}