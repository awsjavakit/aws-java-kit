package com.github.awsjavakit.metrics;

import static java.util.Objects.nonNull;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;
import software.amazon.awssdk.services.cloudwatch.CloudWatchClient;
import software.amazon.awssdk.services.cloudwatch.model.Dimension;
import software.amazon.awssdk.services.cloudwatch.model.MetricDatum;
import software.amazon.awssdk.services.cloudwatch.model.PutMetricDataRequest;
import software.amazon.awssdk.services.cloudwatch.model.StandardUnit;

public class MetricsService implements AutoCloseable {

  public static final int HIGH_RESOLUTION = 1;

  public static final int MAX_REQUEST_SIZE = 90;

  private final ReentrantLock instanceLock = new ReentrantLock();
  private final Clock clock;
  private final CloudWatchClient cloudwatchClient;
  private final String namespace;
  private List<MetricDatum> metricData;

  public MetricsService(Clock clock, CloudWatchClient cloudwatchClient, String namespace) {
    this.clock = clock;
    this.cloudwatchClient = cloudwatchClient;
    this.metricData = new ArrayList<>();
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
    try {
      instanceLock.lock();
      var datum = createDatapoint(actor, metric, value);
      metricData.add(datum);
      if (metricData.size() >= MAX_REQUEST_SIZE) {
        flush();
      }

    } finally {
      instanceLock.unlock();
    }
  }

  public void flush() {
    try {
      instanceLock.lock();
      if (isNotEmpty()) {
        var request = createRequest();
        cloudwatchClient.putMetricData(request);
        metricData = new ArrayList<>();
      }
    } finally {
      instanceLock.unlock();
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

  private boolean isNotEmpty() {
    return nonNull(metricData) && !metricData.isEmpty();
  }

  private MetricDatum createDatapoint(Actor actor, Metric metric,
                                      double metricValue) {
    var dimension = createDimension(actor.name(), metric);
    return crateDatapoint(metricValue, metric, dimension);
  }

  private PutMetricDataRequest createRequest() {
    return PutMetricDataRequest.builder()
      .namespace(namespace)
      .metricData(metricData)
      .build();
  }

  private MetricDatum crateDatapoint(double metricValue, Metric metric, Dimension dimension) {
    return MetricDatum.builder()
      .metricName(metric.metricName())
      .dimensions(dimension)
      .value(metricValue)
      .storageResolution(HIGH_RESOLUTION)
      .timestamp(clock.instant())
      .unit(metric.unit)

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

}
