public class ValueWithExpiry {
    public final String value;
    public final long expiryTimestamp; 

    public ValueWithExpiry(String value, long expiryTimestamp) {
        this.value = value;
        this.expiryTimestamp = expiryTimestamp;
    }

    public boolean isExpired() {
        return expiryTimestamp > 0 && System.currentTimeMillis() > expiryTimestamp;
    }
}