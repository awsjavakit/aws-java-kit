package com.github.awsjavakit.http;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import software.amazon.awssdk.services.ssm.SsmClient;
import software.amazon.awssdk.services.ssm.model.GetParameterRequest;
import software.amazon.awssdk.services.ssm.model.GetParameterResponse;
import software.amazon.awssdk.services.ssm.model.Parameter;
import software.amazon.awssdk.services.ssm.model.ParameterTier;
import software.amazon.awssdk.services.ssm.model.PutParameterRequest;
import software.amazon.awssdk.services.ssm.model.PutParameterResponse;

public class FakeSsmClient implements SsmClient {

  Map<String,PutParameterRequest> parameters;

  public FakeSsmClient(){
    this.parameters = new ConcurrentHashMap<>();
  }

  @Override
  public String serviceName() {
    return null;
  }

  @Override
  public void close() {

  }

  @Override
  public PutParameterResponse putParameter(PutParameterRequest putParameterRequest){
     parameters.put(putParameterRequest.name(),putParameterRequest);
     return PutParameterResponse.builder()
       .tier(putParameterRequest.tier())

       .build();
  }

  @Override
  public GetParameterResponse getParameter(GetParameterRequest request){
      var value = parameters.get(request.name());
      return GetParameterResponse.builder()
        .parameter(Parameter.builder().)
        .build()
  }
}
