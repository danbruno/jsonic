package net.jsonic;

import lombok.Data;

@Data
public class SonicSettings {
    public static final long DEFAULT_CONNECT_TIMEOUT = 3000;
    public static final long DEFAULT_READ_TIMEOUT = 60000;
    public static final long DEFAULT_IDLE_TIMEOUT = 60000;

    public static final int DEFAULT_MAX_THREADS = 0;
    public static final int DEFAULT_MAX_CONN_PER_HOST = 100;

    private long connectTimeout = DEFAULT_CONNECT_TIMEOUT;
    private long readTimeout = DEFAULT_READ_TIMEOUT;
    private long idleTimeout = DEFAULT_IDLE_TIMEOUT;
    private int maxThreads = DEFAULT_MAX_THREADS;
    private int maxConnPerHost = DEFAULT_MAX_CONN_PER_HOST;

    private String host = "localhost";
    private int port = 1491;
    private String password = "SecretPassword";
}
