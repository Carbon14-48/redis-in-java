public class RedisConfig {
    private final String dir;
    private final String dbfilename;

    public RedisConfig(String dir, String dbfilename) {
        this.dir = dir;
        this.dbfilename = dbfilename;
    }

    public String getDir() {
        return dir;
    }

    public String getDbfilename() {
        return dbfilename;
    }
}