package com.github.awsjavakit.testingutils.aws;

import static com.github.awsjavakit.attempt.Try.attempt;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.awsjavakit.misc.JacocoGenerated;
import com.github.awsjavakit.misc.StringUtils;
import java.net.HttpURLConnection;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.lambda.LambdaAsyncClient;
import software.amazon.awssdk.services.lambda.model.InvokeRequest;
import software.amazon.awssdk.services.lambda.model.InvokeResponse;

public  class FakeAsyncLambdaClient<I, O> implements LambdaAsyncClient {

  private final Function<I, O> function;
  private final Class<I> inputClass;
  private final ObjectMapper json;
  private final AtomicInteger invocations;

  public FakeAsyncLambdaClient(Function<I, O> lambdaFunction, Class<I> inputClass, ObjectMapper json) {
    this.function = lambdaFunction;
    this.inputClass = inputClass;
    this.json = json;
    this.invocations= new AtomicInteger(0);
  }

  @JacocoGenerated
  @Override
  public String serviceName() {
    return "FakeAsyncLambdaClient";
  }

  @JacocoGenerated
  @Override
  public void close() {
    //NO-OP
  }

  @Override
  public CompletableFuture<InvokeResponse> invoke(InvokeRequest invokeRequest) {
    invocations.incrementAndGet();
    if(StringUtils.isBlank(invokeRequest.functionName())){
      throw new IllegalArgumentException("Function name cannot be blank");
    }
    var payload = fromJson(invokeRequest.payload().asUtf8String(), inputClass);
    var response = function.apply(payload);

    return CompletableFuture.completedFuture(InvokeResponse.builder()
      .statusCode(HttpURLConnection.HTTP_OK)
      .payload(SdkBytes.fromUtf8String(toJson(response)))
      .build());
  }

  private I fromJson(String utf8String, Class<I> inputClass) {
    return attempt(()->json.readValue(utf8String,inputClass)).orElseThrow();
  }

  private String toJson(O response) {
    return attempt(()->json.writeValueAsString(response)).orElseThrow();
  }

  public int getInvocations() {
    return invocations.get();
  }
}
