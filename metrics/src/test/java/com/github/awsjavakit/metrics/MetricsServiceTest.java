package com.github.awsjavakit.metrics;

import static com.github.awsjavakit.testingutils.RandomDataGenerator.randomDouble;
import static com.github.awsjavakit.testingutils.RandomDataGenerator.randomString;
import static org.assertj.core.api.Assertions.assertThat;

import com.github.awsjavakit.metrics.MetricsService.Actor;
import com.github.awsjavakit.metrics.MetricsService.Metric;
import com.github.awsjavakit.misc.SingletonCollector;
import com.github.awsjavakit.testingutils.aws.FakeCloudWatchClient;
import java.util.Collection;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.cloudwatch.model.MetricDatum;
import software.amazon.awssdk.services.cloudwatch.model.PutMetricDataRequest;
import software.amazon.awssdk.services.cloudwatch.model.StandardUnit;

class MetricsServiceTest {

  public static final String NAMESPACE = randomString();
  private FakeCloudWatchClient client;

  @BeforeEach
  public void init() {
    this.client = new FakeCloudWatchClient();
  }

  @Test
  void shouldSendMetricToCloudwatch() {
    var metricsService = MetricsService.create(client, NAMESPACE);
    var datum = randomDataPoint();
    registerMetric(metricsService, datum);
    metricsService.flush();

    var emittedDatapoint = extractEmittedData()
      .collect(SingletonCollector.collect());

    var dimension = emittedDatapoint.dimensions().stream().collect(SingletonCollector.collect());
    assertThat(dimension.name()).isEqualTo(datum.metric.metricGroup());
    assertThat(dimension.value()).isEqualTo(datum.actor.name());
    assertThat(emittedDatapoint.metricName()).isEqualTo(datum.metric.metricName());
  }

  @Test
  void shouldSendTheMetricsInBatches() {
    var metricsService = MetricsService.create(client, NAMESPACE);

    var data = IntStream.range(0, 10).boxed().map(ignored -> randomDataPoint())
      .toList();
    data.forEach(d -> registerMetric(metricsService, d));

    var emittedDataBeforeFlush = extractEmittedData().toList();
    assertThat(emittedDataBeforeFlush).isEmpty();
    metricsService.flush();
    var emittedDataAfterFlush = extractEmittedData().toList();
    assertThat(emittedDataAfterFlush).hasSize(data.size());

  }

  @Test
  void shouldFlushMetricsToCloudwatchWhenThereAreEnoughMetricPoints() {
    var metricsService = MetricsService.create(client, NAMESPACE);
    var data = IntStream.range(0, MetricsService.MAX_REQUEST_SIZE + 1).boxed()
      .map(ignored -> randomDataPoint())
      .toList();
    data.forEach(d -> registerMetric(metricsService, d));

    var emittedDatapoints = extractEmittedData().toList();
    assertThat(emittedDatapoints).hasSize(MetricsService.MAX_REQUEST_SIZE);
  }

  @Test
  void shouldNotFlushSameEntriesTwice() {
    var metricsService = MetricsService.create(client, NAMESPACE);

    var data = IntStream.range(0, 10).boxed().map(ignored -> randomDataPoint())
      .toList();
    data.forEach(d -> registerMetric(metricsService, d));

    metricsService.flush();
    var emittedDataAfterFlush = extractEmittedData().toList();
    assertThat(emittedDataAfterFlush).hasSize(data.size());
    resetFakeClient();
    metricsService.flush();
    var emittedDataAfterSecondFlush = extractEmittedData().toList();
    assertThat(emittedDataAfterSecondFlush).isEmpty();

  }

  @Test
  void shouldFlushWhenServiceCloses() {
    var metricsService = MetricsService.create(client, NAMESPACE);

    var data = IntStream.range(0, 10).boxed().map(ignored -> randomDataPoint())
      .toList();
    data.forEach(d -> registerMetric(metricsService, d));
    metricsService.close();
    var emittedDataAfterClosing = extractEmittedData().toList();
    assertThat(emittedDataAfterClosing).hasSize(data.size());
  }

  private static void registerMetric(MetricsService metricsService, Datum datum) {
    metricsService.registerMetric(datum.actor, datum.metric, datum.value);
  }

  private static Datum randomDataPoint() {
    var actor = Actor.of("Actor" + randomString());
    var metricGroup = "MetricGroup" + randomString();
    var metricName = "MetricName" + randomString();
    var metric = Metric.of(metricGroup, metricName, StandardUnit.COUNT);
    var value = randomDouble(10.0);

    return new Datum(actor, metric, value);
  }

  private void resetFakeClient() {
    client.getPutMetricDataRequests().clear();
  }

  private Stream<MetricDatum> extractEmittedData() {
    return this.client.getPutMetricDataRequests().stream().map(PutMetricDataRequest::metricData)
      .flatMap(Collection::stream);
  }

  private record Datum(Actor actor, Metric metric, double value) {

  }

}