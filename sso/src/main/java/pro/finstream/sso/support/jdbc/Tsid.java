package pro.finstream.sso.support.jdbc;

import java.security.SecureRandom;
import java.time.Instant;

public final class Tsid {
    private static final int RANDOM_BITS = 22;
    private static final long TSID_EPOCH = Instant.parse("2024-01-01T00:00:00.000Z").toEpochMilli();
    private static final SecureRandom generator = new SecureRandom();

    public static long generate() {
        return (System.currentTimeMillis() - TSID_EPOCH  << RANDOM_BITS)
            + generator.nextLong((1 << RANDOM_BITS) - 1L);
    }

    private Tsid() {}
}
