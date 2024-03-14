package com.github.awsjavakit.testingutils.aws;

import static com.github.awsjavakit.testingutils.RandomDataGenerator.randomDouble;
import static com.github.awsjavakit.testingutils.RandomDataGenerator.randomString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsIterableContainingInOrder.contains;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.cloudwatch.model.Dimension;
import software.amazon.awssdk.services.cloudwatch.model.MetricDatum;
import software.amazon.awssdk.services.cloudwatch.model.PutMetricDataRequest;

class FakeCloudWatchClientTest {

  @Test
  void shouldListPutMetricRequests(){
    var client = new FakeCloudWatchClient();
    var request = somePutDataRequest();
    client.putMetricData(request);
    assertThat(client.getPutMetricDataRequests(),contains(request));
  }


  @Test
  void shouldInformThatItIsAFakeClient(){
    assertThat(new FakeCloudWatchClient().serviceName(),is(equalTo("FakeCloudWatchClient")));
  }

  @Test
  void shouldNotDoAnythingWhenClosing(){
    assertDoesNotThrow(()->new FakeCloudWatchClient().close());
  }


  private static PutMetricDataRequest somePutDataRequest() {
    var datum = MetricDatum.builder()
      .metricName(randomString())
      .dimensions(Dimension.builder()
        .name(randomString())
        .value(randomString())
        .build())
      .value(randomDouble(10))
      .counts(randomDouble(10))
      .build();
    return PutMetricDataRequest.builder()
      .metricData(datum)
      .namespace(randomString())
      .build();
  }

}