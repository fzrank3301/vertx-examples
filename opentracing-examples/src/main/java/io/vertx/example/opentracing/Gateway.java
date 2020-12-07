package io.vertx.example.opentracing;

import io.jaegertracing.Configuration;
import io.jaegertracing.internal.samplers.ConstSampler;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.predicate.ResponsePredicate;
import io.vertx.tracing.opentracing.OpenTracingOptions;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class Gateway extends AbstractVerticle {

  public static void main(String[] args) throws InterruptedException, ExecutionException, TimeoutException {

    Configuration.SamplerConfiguration samplerConfig = Configuration.SamplerConfiguration.fromEnv()
      .withType(ConstSampler.TYPE)
      .withParam(1);

    Configuration.ReporterConfiguration reporterConfig = new Configuration.ReporterConfiguration()
      .withLogSpans(true);

    Configuration config = new Configuration("Gateway")
      .withSampler(samplerConfig)
      .withReporter(reporterConfig);

    Vertx vertx = Vertx.vertx(new VertxOptions()
      .setTracingOptions(new OpenTracingOptions(config.getTracer())
      ));

    vertx.deployVerticle(new Gateway())
      .toCompletionStage()
      .toCompletableFuture()
      .get(30, TimeUnit.SECONDS);
  }

  @Override
  public void start() {
    WebClient client = WebClient.create(vertx);
    vertx.createHttpServer().requestHandler(req -> {
      switch (req.path()) {
        case "/hello":
          client.get(8081, "localhost", "/")
            .expect(ResponsePredicate.SC_OK).send()
            .onSuccess(resp -> req.response().end(resp.body()))
            .onFailure(failure -> {
              failure.printStackTrace();
              req.response().setStatusCode(500).end();
            });
          break;
        case "/jokes":
          client.get(8082, "localhost", "/")
            .expect(ResponsePredicate.SC_OK).send()
            .onSuccess(resp -> req.response().end(resp.body()))
            .onFailure(failure -> {
              failure.printStackTrace();
              req.response().setStatusCode(500).end();
            });
          break;
        default:
          req.response().setStatusCode(404).end();
          break;
      }
    }).listen(8080);
  }
}
