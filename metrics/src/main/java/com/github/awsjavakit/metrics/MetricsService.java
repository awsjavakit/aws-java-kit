package com.github.awsjavakit.metrics;

import static java.util.Objects.nonNull;
import java.time.Clock;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import software.amazon.awssdk.services.cloudwatch.CloudWatchClient;
import software.amazon.awssdk.services.cloudwatch.model.Dimension;
import software.amazon.awssdk.services.cloudwatch.model.MetricDatum;
import software.amazon.awssdk.services.cloudwatch.model.PutMetricDataRequest;
import software.amazon.awssdk.services.cloudwatch.model.StandardUnit;

public class MetricsService implements AutoCloseable {

  public static final int NORMAL_RESOLUTION = 60;

  public static final int MAX_REQUEST_SIZE = 1000;

  private final Clock clock;
  private final CloudWatchClient cloudwatchClient;
  private final String namespace;
  private final AtomicInteger listSize;
  private Deque<Measurement> measurements;

  public MetricsService(Clock clock, CloudWatchClient cloudwatchClient, String namespace) {
    this.clock = clock;
    this.cloudwatchClient = cloudwatchClient;
    this.measurements = new ConcurrentLinkedDeque<>();
    this.listSize = new AtomicInteger(0);
    this.namespace = namespace;
  }

  public static MetricsService create(CloudWatchClient cloudWatchClient, String namespace) {
    return create(Clock.systemDefaultZone(), cloudWatchClient, namespace);
  }

  public static MetricsService create(Clock clock,
                                      CloudWatchClient cloudWatchClient,
                                      String namespace) {
    return new MetricsService(clock, cloudWatchClient, namespace);
  }

  public void registerMetric(Actor actor, Metric metric, double value) {

    measurements.add(new Measurement(actor, metric, value));
    listSize.incrementAndGet();
    if (listSize.get() >= MAX_REQUEST_SIZE) {
      flush();
    }

  }

  public void flush() {
    if (isNotEmpty()) {
      var aggregateMeasurements = aggregateMeasurements();
      var metricData = aggregateMeasurements
        .map(measurement -> measurement.toDataPoint(clock))
        .toList();
      var request = createRequest(metricData);
      cloudwatchClient.putMetricData(request);
      measurements = new ConcurrentLinkedDeque<>();
      listSize.set(0);
    }

  }

  @Override
  public void close() {
    flush();
  }

  private static Dimension createDimension(String actor, Metric metric) {
    return Dimension.builder()
      .name(metric.metricGroup())
      .value(actor)
      .build();
  }

  private Stream<Measurement> aggregateMeasurements() {
    return
      measurements.stream()
        .collect(Collectors.groupingBy(AggregatingKey::from))
        .values().stream().map(this::aggregateMeasurementsOfSameMetric);

  }

  private Measurement aggregateMeasurementsOfSameMetric(List<Measurement> list) {
    return list.stream()
      .reduce((l, r) -> new Measurement(l.actor(), l.metric(), l.value + r.value))
      .orElseThrow();
  }

  private boolean isNotEmpty() {
    return nonNull(measurements) && listSize.intValue() > 0;
  }

  private PutMetricDataRequest createRequest(List<MetricDatum> metricData) {
    return PutMetricDataRequest.builder()
      .namespace(namespace)
      .metricData(metricData)
      .build();
  }

  public record Actor(String name) {

    public static Actor of(String name) {
      return new Actor(name);
    }
  }

  public record Metric(String metricGroup, String metricName, StandardUnit unit) {

    public static Metric of(String metricGroup, String metricName, StandardUnit unit) {
      return new Metric(metricGroup, metricName, unit);
    }

  }

  record Measurement(Actor actor, Metric metric, double value) {

    public MetricDatum toDataPoint(Clock clock) {
      var dimension = createDimension(actor.name(), metric);
      return createDatapoint(value, metric, dimension, clock);
    }

    private MetricDatum createDatapoint(double metricValue,
                                        Metric metric,
                                        Dimension dimension,
                                        Clock clock) {
      return MetricDatum.builder()
        .metricName(metric.metricName())
        .dimensions(dimension)
        .value(metricValue)
        .storageResolution(NORMAL_RESOLUTION)
        .timestamp(clock.instant())
        .unit(metric.unit)
        .build();
    }

  }

  private record AggregatingKey(Metric metric, Actor actor) {

    public static AggregatingKey from(Measurement measurement) {
      return new AggregatingKey(measurement.metric, measurement.actor);
    }
  }

}
