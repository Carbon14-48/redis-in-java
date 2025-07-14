public class ValueWithExpiry {
    String value;
    long expiryTimestamp; // set to 0 or -1 for "no expiry"

    ValueWithExpiry(String value, long expiryTimestamp) {
        this.value = value;
        this.expiryTimestamp = expiryTimestamp;
    }
    boolean isExpired() {
        return expiryTimestamp > 0 && System.currentTimeMillis() > expiryTimestamp;
    }
}
