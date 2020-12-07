package io.vertx.example.opentracing;

import io.jaegertracing.Configuration;
import io.jaegertracing.internal.samplers.ConstSampler;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpMethod;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.predicate.ResponsePredicate;
import io.vertx.tracing.opentracing.OpenTracingOptions;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class HelloService extends AbstractVerticle {

  public static void main(String[] args) throws InterruptedException, ExecutionException, TimeoutException {

    Configuration.SamplerConfiguration samplerConfig = Configuration.SamplerConfiguration.fromEnv()
      .withType(ConstSampler.TYPE)
      .withParam(1);

    Configuration.ReporterConfiguration reporterConfig = new Configuration.ReporterConfiguration()
      .withLogSpans(true);

    Configuration config = new Configuration("HelloService")
      .withSampler(samplerConfig)
      .withReporter(reporterConfig);

    Vertx vertx = Vertx.vertx(new VertxOptions()
      .setTracingOptions(new OpenTracingOptions(config.getTracer())
      ));

    vertx.deployVerticle(new HelloService())
      .toCompletionStage()
      .toCompletableFuture()
      .get(30, TimeUnit.SECONDS);
  }

  private WebClient client;

  @Override
  public void start() {
    client = WebClient.create(vertx);
    vertx.createHttpServer().requestHandler(req -> {
      client
        .get(8082, "localhost", "/")
        .expect(ResponsePredicate.SC_OK)
        .expect(ResponsePredicate.contentType("text/plain"))
        .send()
        .onSuccess(resp -> {
          req.response().end("Hello, here is a joke for you \"" + resp.bodyAsString() + "\"");
        })
        .onFailure(failure -> {
          failure.printStackTrace();
          req.response().end("Hello, sorry no joke for you today");
        });
    }).listen(8081);
  }
}
