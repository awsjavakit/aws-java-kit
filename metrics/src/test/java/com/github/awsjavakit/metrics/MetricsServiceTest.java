package com.github.awsjavakit.metrics;

import static com.github.awsjavakit.testingutils.RandomDataGenerator.randomDouble;
import static com.github.awsjavakit.testingutils.RandomDataGenerator.randomInteger;
import static com.github.awsjavakit.testingutils.RandomDataGenerator.randomString;
import static org.assertj.core.api.Assertions.assertThat;
import com.github.awsjavakit.metrics.MetricsService.Actor;
import com.github.awsjavakit.metrics.MetricsService.Measurement;
import com.github.awsjavakit.metrics.MetricsService.Metric;
import com.github.awsjavakit.misc.SingletonCollector;
import com.github.awsjavakit.testingutils.aws.FakeCloudWatchClient;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Collection;
import java.util.function.Function;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.assertj.core.data.Percentage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.cloudwatch.model.MetricDatum;
import software.amazon.awssdk.services.cloudwatch.model.PutMetricDataRequest;
import software.amazon.awssdk.services.cloudwatch.model.StandardUnit;

class MetricsServiceTest {

  public static final String NAMESPACE = randomString();
  public static final Clock CLOCK = Clock.fixed(Instant.now(), ZoneId.systemDefault());
  private FakeCloudWatchClient client;
  private MetricsService metricsService;

  @BeforeEach
  public void init() {
    this.client = new FakeCloudWatchClient();
    this.metricsService = MetricsService.create(CLOCK, client, NAMESPACE);
  }

  @Test
  void shouldSendMetricToCloudwatch() {

    var datum = randomDataPoint();
    registerMetric(metricsService, datum);
    metricsService.flush();

    var emittedDatapoint = extractEmittedData()
      .collect(SingletonCollector.collect());

    var dimension = emittedDatapoint.dimensions().stream().collect(SingletonCollector.collect());
    assertThat(dimension.name()).isEqualTo(datum.metric().metricGroup());
    assertThat(dimension.value()).isEqualTo(datum.actor().name());
    assertThat(emittedDatapoint.metricName()).isEqualTo(datum.metric().metricName());
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

    var data = IntStream.range(0, 10).boxed().map(ignored -> randomDataPoint())
      .toList();
    data.forEach(d -> registerMetric(metricsService, d));
    metricsService.close();
    var emittedDataAfterClosing = extractEmittedData().toList();
    assertThat(emittedDataAfterClosing).hasSize(data.size());
  }

  @Test
  void shouldAggregateResultsOfMeasurementsOfSameMetricAndActor() {
    var dataPoint = randomDataPoint(numberAllowingPredictableAggregation());
    var anotherDataPoint = randomDataPoint(numberAllowingPredictableAggregation());

    var repetitions = 10;
    for (var i = 0; i < repetitions; i++) {
      registerMetric(metricsService, dataPoint);
      registerMetric(metricsService, anotherDataPoint);
    }
    metricsService.flush();
    var values = extractEmittedData().toList();
    var expectedValues = Stream.of(
        new Measurement(dataPoint.actor(), dataPoint.metric(), repetitions * dataPoint.value()),
        new Measurement(anotherDataPoint.actor(), anotherDataPoint.metric(),
          repetitions * anotherDataPoint.value())
      ).map(m -> m.toDataPoint(CLOCK))
      .toList();

    assertThat(values).containsExactlyInAnyOrder(expectedValues.toArray(MetricDatum[]::new));
  }

  @Test
  void shouldUseAggregateFunctionFromMetric() {
    double aggregatedValue = randomDouble(8.0);
    Function<Collection<Double>, Double> aggregateFunction = doubles -> aggregatedValue;
    var data = IntStream.range(0, 10).boxed()
      .map(ignored -> randomDataPoint(aggregateFunction))
      .toList();
    data.forEach(d -> registerMetric(metricsService, d));
    metricsService.close();
    var emittedDataAfterClosing = extractEmittedData().collect(SingletonCollector.collect());
    assertThat(emittedDataAfterClosing.value()).isEqualTo(aggregatedValue);
  }

  @Test
  void shouldReturnAggregatedAverageFromMetrics() {
    var data = IntStream.range(0, 10).boxed()
      .map(ignored -> randomDataPoint(MetricsService.AVERAGE))
      .toList();
    data.forEach(d -> registerMetric(metricsService, d));
    metricsService.flush();
    var emittedDataAfterClosing = extractEmittedData().collect(SingletonCollector.collect());
    var expectedSum = data.stream().map(Measurement::value).reduce(Double::sum).orElseThrow();
    var expectedAverage = expectedSum / data.size();
    assertThat(emittedDataAfterClosing.value()).isCloseTo(expectedAverage, Percentage.withPercentage(0.01));
  }

  private int numberAllowingPredictableAggregation() {
    return 1+randomInteger(10);
  }

  private static void registerMetric(MetricsService metricsService, Measurement datum) {
    metricsService.registerMetric(datum.actor(), datum.metric(), datum.value());
  }

  private static Measurement randomDataPoint() {
    var actor = Actor.of("Actor" + randomString());
    var metricGroup = "MetricGroup" + randomString();
    var metricName = "MetricName" + randomString();
    var metric = Metric.of(metricGroup, metricName, StandardUnit.COUNT,
      MetricsService.SUM);
    var value = randomDouble(10.0);

    return new Measurement(actor, metric, value);
  }

  private static Measurement randomDataPoint(Function<Collection<Double>, Double> aggregateFunction) {
    var actor = Actor.of("Actor");
    var metricGroup = "MetricGroup";
    var metricName = "MetricName";
    var metric = Metric.of(metricGroup, metricName, StandardUnit.COUNT, aggregateFunction);
    var value = randomDouble(10.0);
    return new Measurement(actor, metric, value);
  }

  private static Measurement randomDataPoint(int value) {
    var actor = Actor.of("Actor" + randomString());
    var metricGroup = "MetricGroup" + randomString();
    var metricName = "MetricName" + randomString();
    var metric = Metric.of(metricGroup, metricName, StandardUnit.COUNT, MetricsService.SUM);


    return new Measurement(actor, metric, value);
  }

  private void resetFakeClient() {
    client.getPutMetricDataRequests().clear();
  }

  private Stream<MetricDatum> extractEmittedData() {
    return this.client.getPutMetricDataRequests().stream().map(PutMetricDataRequest::metricData)
      .flatMap(Collection::stream);
  }

}