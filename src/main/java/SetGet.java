import java.util.concurrent.ConcurrentHashMap;

public class SetGet {
    ConcurrentHashMap<String, ValueWithExpiry> store = new ConcurrentHashMap<>();
    Formatter fmt = new Formatter();
    String set(String key, String value, Long pxMillis) {
        long expiry = (pxMillis != null && pxMillis > 0) ? System.currentTimeMillis() + pxMillis : 0;
        store.put(key, new ValueWithExpiry(value, expiry));
        return fmt.formatSimpleString("OK");
    }
    String get(String key) {
        ValueWithExpiry vwe = store.get(key);
        if (vwe == null) return fmt.formatBulkString(null);
        if (vwe.isExpired()) {
            store.remove(key);
            return fmt.formatBulkString(null);
        }
        return fmt.formatBulkString(vwe.value);
    }
}