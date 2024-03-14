package com.github.awsjavakit.testingutils.aws;

import java.util.ArrayList;
import java.util.List;
import software.amazon.awssdk.services.cloudwatch.CloudWatchClient;
import software.amazon.awssdk.services.cloudwatch.model.PutMetricDataRequest;
import software.amazon.awssdk.services.cloudwatch.model.PutMetricDataResponse;

public class FakeCloudWatchClient implements CloudWatchClient {

  public final List<PutMetricDataRequest> putMetricDataRequests;

  public FakeCloudWatchClient() {
    putMetricDataRequests = new ArrayList<>();
  }

  @Override
  public String serviceName() {
    return "FakeCloudWatchClient";
  }

  @Override
  public void close() {
    //NO-OP
  }

  @Override
  public PutMetricDataResponse putMetricData(PutMetricDataRequest putMetricDataRequest) {
    this.putMetricDataRequests.add(putMetricDataRequest);
    return PutMetricDataResponse.builder().build();
  }

  public List<PutMetricDataRequest> getPutMetricDataRequests() {
    return this.putMetricDataRequests;
  }
}
