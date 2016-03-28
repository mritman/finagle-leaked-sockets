package hello;

import com.twitter.finagle.Http;
import com.twitter.finagle.Service;
import com.twitter.finagle.http.Request;
import com.twitter.finagle.http.RequestBuilder;
import com.twitter.finagle.http.Response;
import com.twitter.io.Reader$;
import com.twitter.util.Await;
import com.twitter.util.Duration;
import org.apache.coyote.AbstractProtocol;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.embedded.EmbeddedServletContainerFactory;
import org.springframework.boot.context.embedded.tomcat.TomcatEmbeddedServletContainerFactory;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class Application {

    private static final Logger logger = LoggerFactory.getLogger(Application.class);

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);

        final Service<Request, Response> service = Http.client()
                                                       .withStreaming(true)
                                                       .withSessionQualifier().noFailFast()
                                                       .newService("localhost:8080");

        int requestCounter = 1;

        while (true) {
            Request request = RequestBuilder.create()
                                            .url("http://service")
                                            .buildGet(null);

            Response response;
            try {
                response = Await.result(service.apply(request), Duration.fromMilliseconds(20));
            } catch (Exception e) {
                logger.info("{}: {}: {}", requestCounter++, e.getClass().getSimpleName(), e.getMessage());
                continue;
            }

            try {
                logger.info("{}: HTTP status code: {}. Headers: {}. Response: {}.",
                            requestCounter++,
                            response.getStatusCode(),
                            response.getHttpResponse().headers().entries(),
                            Await.result(Reader$.MODULE$.readAll(response.reader()), Duration.fromMilliseconds(20)));
            } catch (Exception exc) {

            }

            response.read().discard();
        }
    }

    @Bean
    public EmbeddedServletContainerFactory servletContainerFactory() {
        TomcatEmbeddedServletContainerFactory factory = new TomcatEmbeddedServletContainerFactory();

        factory.addConnectorCustomizers(connector -> ((AbstractProtocol) connector.getProtocolHandler()).setConnectionTimeout(5000));

        return factory;
    }

}
