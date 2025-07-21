package pro.finstream.sso.support.test;

import io.restassured.RestAssured;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.boot.test.autoconfigure.orm.jpa.AutoConfigureTestEntityManager;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ContextConfiguration;
import pro.finstream.sso.App;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestEntityManager
@ContextConfiguration(classes = App.class)
public class WebIntegrationTestBase {
    @LocalServerPort
    private Integer port;

    @BeforeEach
    public void setUp() {
        RestAssured.port = port;
    }
}
