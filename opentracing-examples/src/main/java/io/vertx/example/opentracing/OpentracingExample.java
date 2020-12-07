package io.vertx.example.opentracing;

import io.jaegertracing.Configuration;
import io.jaegertracing.internal.samplers.ConstSampler;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientResponse;
import io.vertx.core.http.HttpMethod;
import io.vertx.tracing.opentracing.OpenTracingOptions;

public class OpentracingExample {

  public static void main(String[] args) throws Exception {
    ChuckNorrisJokesService.main(args);
    HelloService.main(args);
    Gateway.main(args);
    Client.main(args);
  }
}
