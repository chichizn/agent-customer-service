import com.anjing.Application;
import com.anjing.module.chat.LlmService;
import com.anjing.module.commerce.CommerceService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import org.aopalliance.intercept.MethodInterceptor;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.SpringApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.dao.DataAccessResourceFailureException;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.sql.*;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;

/** Test-only launcher. Never included in the backend artifact; no real model key is read. */
public class AcceptanceRuntime {
    static final String DATABASE = "agent_customer_service_acceptance_20261019";
    static final String JDBC = "jdbc:mysql://127.0.0.1:13307/" + DATABASE + "?useUnicode=true&characterEncoding=utf8&serverTimezone=GMT%2B8&useSSL=false&allowPublicKeyRetrieval=true";
    static final ObjectMapper JSON = new ObjectMapper();
    static volatile String mode = "valid", entered = "none";
    static volatile boolean queryFailure;
    static volatile CountDownLatch gate = new CountDownLatch(0);

    static Connection connection() throws SQLException {
        Connection c = DriverManager.getConnection(JDBC, "root", "");
        try (var st = c.createStatement(); var rs = st.executeQuery("select @@port, database()")) {
            if (!rs.next() || rs.getInt(1) != 13307 || !DATABASE.equals(rs.getString(2))) {
                c.close(); throw new SQLException("Wrong acceptance database");
            }
        }
        return c;
    }

    @Configuration(proxyBeanMethods=false)
    static class Hooks {
        @Bean static BeanPostProcessor acceptanceHooks() {
            return new BeanPostProcessor() {
                public Object postProcessAfterInitialization(Object bean, String name) {
                    if (bean instanceof LlmService original) return new LlmService() {
                        @Override public Map<String,String> analyzeUserInput(String text) {
                            return analyzeUserInput(text, List.of(), List.of());
                        }
                        @Override public Map<String,String> analyzeUserInput(String text,
                                List<Map<String,String>> knowledge, List<Map<String,String>> history) {
                            if (mode.equals("disabled")) return null;
                            var result = original.analyzeUserInput(text, knowledge, history);
                            if (mode.equals("hold-before-finish")) pause("before-finish");
                            return result;
                        }
                    };
                    if (bean instanceof CommerceService) {
                        ProxyFactory proxy = new ProxyFactory(bean);
                        proxy.setProxyTargetClass(true);
                        proxy.addAdvice((MethodInterceptor) call -> {
                            if (queryFailure && call.getMethod().getName().equals("getLogistics"))
                                throw new DataAccessResourceFailureException("acceptance query failure");
                            return call.proceed();
                        });
                        return proxy.getProxy();
                    }
                    return bean;
                }
            };
        }
    }

    static void pause(String point) {
        entered = point;
        try {
            if (!gate.await(15, TimeUnit.SECONDS)) throw new IllegalStateException("Acceptance gate expired");
        } catch (InterruptedException e) { Thread.currentThread().interrupt(); throw new IllegalStateException(e); }
    }

    static Map<String,Object> state() throws SQLException {
        var result = new LinkedHashMap<String,Object>();
        result.put("database", DATABASE); result.put("dbPort",13307); result.put("mode",mode);
        result.put("entered",entered); result.put("queryFailure",queryFailure); result.put("at",Instant.now().toString());
        try (var c=connection(); var st=c.createStatement()) {
            for (String table : List.of("commerce_merchant","commerce_shop","commerce_customer","commerce_order","commerce_logistics",
                    "commerce_service_ticket","commerce_ticket_submission","commerce_ticket_history","cs_chat_session","cs_chat_message","cs_chat_execution","cs_scoped_knowledge")) {
                try (var rs=st.executeQuery("select count(*) from " + table)) { rs.next(); result.put(table,rs.getLong(1)); }
            }
        }
        return result;
    }

    static void faults(Map<String,Object> body) throws Exception {
        String fault = (String)body.getOrDefault("fault","clear");
        String session = (String)body.getOrDefault("sessionId", "");
        if (!Set.of("clear","query","create","update").contains(fault)) throw new IllegalArgumentException("fault");
        if (Set.of("create","update").contains(fault) && !session.matches("[a-f0-9-]{36}")) throw new IllegalArgumentException("sessionId");
        try (var c=connection(); var st=c.createStatement()) {
            st.execute("drop trigger if exists acceptance_execution_failure");
            if (Set.of("create","update").contains(fault)) {
                String action = fault.equals("create")?"create_service_ticket":"update_service_ticket";
                st.execute("create trigger acceptance_execution_failure before insert on cs_chat_execution for each row begin if NEW.session_id = '" + session +
                    "' and NEW.action = '" + action + "' and NEW.status = 'SUCCESS' then signal sqlstate '45000' set message_text = 'acceptance injected failure'; end if; end");
            }
        }
        queryFailure = fault.equals("query");
    }

    static Object backup() throws Exception {
        Path folder = Path.of("backend/target/acceptance-backup-" + System.currentTimeMillis());
        Files.createDirectories(folder);
        try (var c=connection(); var tables=c.getMetaData().getTables(DATABASE,null,"%",new String[]{"TABLE"})) {
            c.setAutoCommit(false); c.setTransactionIsolation(Connection.TRANSACTION_REPEATABLE_READ);
            while(tables.next()) {
                String table=tables.getString("TABLE_NAME");
                if (!table.matches("[a-z0-9_]+")) throw new IllegalStateException("Unexpected table name");
                var rows=new ArrayList<Map<String,Object>>();
                try (var st=c.createStatement();var rs=st.executeQuery("select * from `"+table+"`")) {
                    while(rs.next()) {
                        var row=new LinkedHashMap<String,Object>();
                        for(int col=1;col<=rs.getMetaData().getColumnCount();col++) {
                            Object value=rs.getObject(col);
                            row.put(rs.getMetaData().getColumnLabel(col),value==null?null:value.toString());
                        }
                        rows.add(row);
                    }
                }
                Files.writeString(folder.resolve(table+".json"),JSON.writeValueAsString(rows));
            }
            c.commit();
        }
        return Map.of("backupPath",folder.toAbsolutePath().toString());
    }

    static Object sentinel() throws Exception {
        try (var c=connection();var st=c.createStatement()) {
            st.executeUpdate("insert ignore into cs_chat_session (session_id,user_id,user_name,status,merchant_id,shop_id,customer_id,demo_data,created_at,updated_at) values ('acceptance-nondemo-session','acceptance-sentinel','验收保留哨兵','active','demo-merchant','demo-shop-tb','demo-customer-tb-1',false,now(),now())");
            st.executeUpdate("insert ignore into cs_scoped_knowledge (id,merchant_id,shop_id,scope,topic,title,content,source,status,kind,demo_data) values ('acceptance-nondemo-knowledge','sentinel-merchant',null,'MERCHANT','sentinel','非演示保留验证','immutable acceptance sentinel','local acceptance','ACTIVE','RULE',false)");
            var result=new LinkedHashMap<String,Object>();
            for (String table:List.of("cs_chat_session","cs_scoped_knowledge")) {
                String key=table.equals("cs_chat_session")?"session_id":"id";
                String id=table.equals("cs_chat_session")?"acceptance-nondemo-session":"acceptance-nondemo-knowledge";
                try(var rs=st.executeQuery("select * from "+table+" where "+key+"='"+id+"'")) {
                    var row=new LinkedHashMap<String,Object>();
                    if(!rs.next()) throw new IllegalStateException("missing sentinel");
                    for(int col=1;col<=rs.getMetaData().getColumnCount();col++) {
                        Object value=rs.getObject(col); row.put(rs.getMetaData().getColumnLabel(col),value==null?null:value.toString());
                    }
                    result.put(table,row);
                }
            }
            return result;
        }
    }

    public static void main(String[] args) throws Exception {
        if (args.length!=1 || !args[0].equals("--isolated-acceptance")) throw new IllegalArgumentException("Explicit --isolated-acceptance required");
        try (var c=connection()) { }
        HttpServer stub=HttpServer.create(new InetSocketAddress("127.0.0.1",10004),0);
        stub.setExecutor(Executors.newCachedThreadPool());
        stub.createContext("/", exchange -> {
            int status=200; Object response;
            try {
                String path=exchange.getRequestURI().getPath();
                Map<String,Object> body=exchange.getRequestMethod().equals("POST")?
                    JSON.readValue(exchange.getRequestBody(),Map.class):Map.of();
                switch(path) {
                    case "/state" -> response=state();
                    case "/backup" -> response=backup();
                    case "/sentinel" -> response=sentinel();
                    case "/control" -> {
                        String next=(String)body.getOrDefault("mode","valid");
                        if (!Set.of("valid","disabled","500","timeout","bad-json","unknown-intent","hold-model","hold-before-finish").contains(next))
                            throw new IllegalArgumentException("mode");
                        gate.countDown(); gate=new CountDownLatch(next.startsWith("hold-")?1:0); entered="none"; mode=next; response=state();
                    }
                    case "/release" -> { gate.countDown(); response=state(); }
                    case "/fault" -> { faults(body); response=state(); }
                    case "/v1/chat/completions" -> {
                        String current=mode;
                        if (current.equals("hold-model")) pause("model-request");
                        if (current.equals("timeout")) Thread.sleep(1800);
                        if (current.equals("500")) { status=500; response=Map.of("error","acceptance failure"); }
                        else {
                            String content=current.equals("bad-json")?"{broken":current.equals("unknown-intent")?
                                "{\"intentCode\":\"REFUND_IMMEDIATELY\"}":"{\"intentCode\":\"GENERAL_QUERY\",\"scene\":\"通用咨询\"}";
                            response=Map.of("choices",List.of(Map.of("message",Map.of("content",content))));
                        }
                    }
                    default -> { status=404;response=Map.of("error","not found"); }
                }
            } catch (Exception e) { status=500;response=Map.of("errorType",e.getClass().getSimpleName()); }
            byte[] bytes=JSON.writeValueAsString(response).getBytes(StandardCharsets.UTF_8);
            try { exchange.getResponseHeaders().set("Content-Type","application/json; charset=utf-8"); exchange.sendResponseHeaders(status,bytes.length); exchange.getResponseBody().write(bytes); }
            finally { exchange.close(); }
        });
        stub.start();
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            gate.countDown(); stub.stop(0);
            try { faults(Map.of("fault","clear")); } catch(Exception ignored) { }
        }));
        var app=new SpringApplication(Application.class,Hooks.class);
        app.run("--spring.profiles.active=acceptance","--server.address=127.0.0.1","--server.port=10003",
            "--spring.datasource.url="+JDBC,"--spring.datasource.username=root","--spring.datasource.password=",
            "--spring.datasource.type=org.springframework.jdbc.datasource.DriverManagerDataSource",
            "--spring.jpa.hibernate.ddl-auto=update","--spring.jpa.open-in-view=false",
            "--app.features.redis.enabled=false","--app.features.distributed-lock.provider=local","--management.health.redis.enabled=false",
            "--spring.datasource.druid.web-stat-filter.enabled=false","--spring.datasource.druid.stat-view-servlet.enabled=false",
            "--logging.level.root=WARN","--logging.level.com.anjing.module.chat.LlmService=OFF","--spring.main.banner-mode=off",
            "--llm.enabled=true","--llm.api-key=acceptance-only","--llm.model=local-fixture",
            "--llm.api-url=http://127.0.0.1:10004/v1/chat/completions","--llm.connect-timeout-ms=500","--llm.read-timeout-ms=1000");
        System.out.println("ACCEPTANCE_RUNTIME_READY api=10003 control=10004 database="+DATABASE);
    }
}
