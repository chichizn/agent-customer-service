import com.anjing.Application;
import com.anjing.module.chat.LlmService;
import org.springframework.boot.SpringApplication;
import java.sql.DriverManager;

/** Real LlmService with the configuration switch off; only the isolated demo database is allowed. */
public class AcceptanceDisabledRuntime {
    public static void main(String[] args) throws Exception {
        if (args.length != 1 || !args[0].equals("--isolated-acceptance"))
            throw new IllegalArgumentException("Explicit --isolated-acceptance required");
        String database = "agent_customer_service_acceptance_20261019";
        String jdbc = "jdbc:mysql://127.0.0.1:13307/" + database
            + "?useUnicode=true&characterEncoding=utf8&serverTimezone=GMT%2B8&useSSL=false&allowPublicKeyRetrieval=true";
        try (var connection = DriverManager.getConnection(jdbc, "root", "");
             var statement = connection.createStatement();
             var result = statement.executeQuery("SELECT @@port, DATABASE()")) {
            if (!result.next() || result.getInt(1) != 13307 || !database.equals(result.getString(2)))
                throw new IllegalStateException("Wrong acceptance database");
        }
        var app = new SpringApplication(Application.class);
        var context = app.run("--spring.profiles.active=acceptance", "--server.address=127.0.0.1", "--server.port=10005",
            "--spring.datasource.url=" + jdbc, "--spring.datasource.username=root", "--spring.datasource.password=",
            "--spring.datasource.type=org.springframework.jdbc.datasource.DriverManagerDataSource",
            "--spring.jpa.hibernate.ddl-auto=update", "--spring.jpa.open-in-view=false",
            "--app.features.redis.enabled=false", "--app.features.distributed-lock.provider=local",
            "--management.health.redis.enabled=false", "--spring.datasource.druid.web-stat-filter.enabled=false",
            "--spring.datasource.druid.stat-view-servlet.enabled=false", "--logging.level.root=WARN",
            "--logging.level.com.anjing.module.chat.LlmService=OFF", "--spring.main.banner-mode=off",
            "--llm.enabled=false", "--llm.api-key=acceptance-only", "--llm.model=unused",
            "--llm.api-url=http://127.0.0.1:10006/should-not-be-called",
            "--llm.connect-timeout-ms=500", "--llm.read-timeout-ms=1000");
        if (context.getEnvironment().getProperty("llm.enabled", Boolean.class, true)
            || context.getBean(LlmService.class).getClass() != LlmService.class)
            throw new IllegalStateException("Expected original LlmService and llm.enabled=false");
        System.out.println("DISABLED_ACCEPTANCE_READY api=10005 llm.enabled=false originalLlmService=true database=" + database);
    }
}
