package utils;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.sql.Connection;
import java.sql.SQLException;


public final class DatabasePool {
    private static final HikariDataSource DATA_SOURCE = create();

    private DatabasePool() {}

    private static HikariDataSource create() {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(System.getenv().getOrDefault(
                "DB_URL",
                "jdbc:sqlserver://localhost:1433;databaseName=SocialNetworkFriendSuggestion"));
        config.setUsername(System.getenv().getOrDefault("DB_USER", "sa"));
        config.setPassword(System.getenv().getOrDefault("DB_PASSWORD", "12345"));
        config.setDriverClassName("com.microsoft.sqlserver.jdbc.SQLServerDriver");

        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        config.setConnectionTimeout(30_000);
        config.setIdleTimeout(600_000);
        config.setMaxLifetime(1_800_000);

        return new HikariDataSource(config);
    }

    public static Connection getConnection() throws SQLException {
        return DATA_SOURCE.getConnection();
    }

    public static void close() {
        DATA_SOURCE.close();
    }
}