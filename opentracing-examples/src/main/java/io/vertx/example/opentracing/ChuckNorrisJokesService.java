package io.vertx.example.opentracing;

import io.jaegertracing.Configuration;
import io.jaegertracing.internal.samplers.ConstSampler;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.json.JsonArray;
import io.vertx.pgclient.PgConnectOptions;
import io.vertx.pgclient.PgPool;
import io.vertx.sqlclient.PoolOptions;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.Tuple;
import io.vertx.tracing.opentracing.OpenTracingOptions;
import org.testcontainers.containers.PostgreSQLContainer;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class ChuckNorrisJokesService extends AbstractVerticle {

  public static void main(String[] args) throws InterruptedException, ExecutionException, TimeoutException {

    PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>();
    postgres.start();
    PgConnectOptions options = new PgConnectOptions()
      .setPort(postgres.getMappedPort(5432))
      .setHost(postgres.getContainerIpAddress())
      .setDatabase(postgres.getDatabaseName())
      .setUser(postgres.getUsername())
      .setPassword(postgres.getPassword());

    Configuration.SamplerConfiguration samplerConfig = Configuration.SamplerConfiguration.fromEnv()
      .withType(ConstSampler.TYPE)
      .withParam(1);

    Configuration.ReporterConfiguration reporterConfig = new Configuration.ReporterConfiguration()
      .withLogSpans(true);

    Configuration config = new Configuration("JokeService")
      .withSampler(samplerConfig)
      .withReporter(reporterConfig);

    Vertx vertx = Vertx.vertx(new VertxOptions()
      .setTracingOptions(new OpenTracingOptions(config.getTracer())
      ));

    vertx.deployVerticle(new ChuckNorrisJokesService(options))
      .toCompletionStage()
      .toCompletableFuture()
      .get(30, TimeUnit.SECONDS);
    System.out.println("ChuckNorrisJokesService started");
  }

  private PgConnectOptions options;
  private JsonArray jokes = new JsonArray();
  private PgPool pool;

  public ChuckNorrisJokesService(PgConnectOptions options) {
    this.options = options;
  }

  @Override
  public void start(Promise<Void> startPromise) {

    pool = PgPool.pool(vertx, options, new PoolOptions().setMaxSize(5));

    pool.query("create table jokes(joke varchar(255))").execute()
      .compose(v -> vertx.fileSystem().readFile("jokes.json"))
      .compose(buffer -> {
        JsonArray array = new JsonArray(buffer);
        List<Tuple> batch = new ArrayList<>();
        for (int i = 0; i < array.size(); i++) {
          String joke = array.getJsonObject(i).getString("joke");
          batch.add(Tuple.of(joke));
        }
        return pool
          .preparedQuery("insert into jokes values ($1)")
          .executeBatch(batch);
      })
      .<Void>mapEmpty()
      .onComplete(startPromise);

    vertx.createHttpServer().requestHandler(req -> {

      pool
        .query("select joke from jokes ORDER BY random() limit 1")
        .execute().onComplete(res -> {
          if (res.succeeded() && res.result().size() > 0) {
            Row row = res.result().iterator().next();
            String joke = row.getString(0);
            req.response().putHeader("content-type", "text/plain").end(joke);
          } else {
            req.response().setStatusCode(500).end("No jokes available");
          }
      });
    }).listen(8082);
  }
}
