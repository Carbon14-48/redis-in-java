import java.util.concurrent.ConcurrentHashMap;

public class SetGet {
    ConcurrentHashMap<String, ValueWithExpiry> store = new ConcurrentHashMap<>();

    String set(String key, String value, Long pxMillis) {
        long expiry = (pxMillis != null && pxMillis > 0) ? System.currentTimeMillis() + pxMillis : 0;
        store.put(key, new ValueWithExpiry(value, expiry));
        return "+OK\r\n";
    }

    String getValue(String key) {
        ValueWithExpiry vwe = store.get(key);
        if (vwe == null) return null;
        if (vwe.isExpired()) {
            store.remove(key);
            return null;
        }
        return vwe.value;
    }

}