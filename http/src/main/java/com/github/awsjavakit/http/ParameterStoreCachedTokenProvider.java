package com.github.awsjavakit.http;

import static com.gtihub.awsjavakit.attempt.Try.attempt;
import static java.time.Instant.now;
import static java.util.Objects.isNull;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.awsjavakit.http.token.OAuthTokenEntry;
import java.time.Duration;
import java.time.Instant;
import java.util.Random;
import java.util.UUID;
import software.amazon.awssdk.services.ssm.SsmClient;
import software.amazon.awssdk.services.ssm.model.GetParameterRequest;
import software.amazon.awssdk.services.ssm.model.GetParameterResponse;
import software.amazon.awssdk.services.ssm.model.Parameter;
import software.amazon.awssdk.services.ssm.model.PutParameterRequest;

public class ParameterStoreCachedTokenProvider implements TokenProvider {

  public static final String DATATYPE = "text";
  public static final String TIER = "Standard";
  public static final int MINIMUM_SLEEP_AMOUNT = 100;
  public static final int SOME_LONG_ENOUGH_PERIOD = 400;
  private final TokenProvider newTokenProvider;
  private final Duration maxTokenAge;
  private final SsmClient ssmClient;
  private final ObjectMapper objectMapper;
  private final String parameterName;

  public ParameterStoreCachedTokenProvider(TokenProvider newTokenProvider,
    String ssmParameterName,
    SsmClient ssmClient,
    Duration maxTokenAge,
    ObjectMapper objectMapper) {
    this.ssmClient = ssmClient;
    this.newTokenProvider = newTokenProvider;
    this.maxTokenAge = maxTokenAge;
    this.objectMapper = objectMapper;
    this.parameterName = ssmParameterName;
  }

  @Override
  public OAuthTokenEntry fetchToken() {
    var now = now();
    var token = fetchTokenFromSsm();
    if (thereIsNoValidToken(token, now)) {
      var newValue = newTokenProvider.fetchToken();
      token = storeNewToken(newValue);
    }
    return token;
  }

  private static void someOtherProcessMightBeGeneratingAToken() {
    long seed = UUID.randomUUID().hashCode();
    try {
      Thread.sleep(MINIMUM_SLEEP_AMOUNT +new Random(seed).nextLong(SOME_LONG_ENOUGH_PERIOD));
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
  }

  private Boolean thereIsNoValidToken(OAuthTokenEntry token, Instant now) {
    return isNull(token) || now.isAfter(token.timestamp().plus(maxTokenAge));
  }

  private OAuthTokenEntry fetchTokenFromSsm() {
    var request = creteRequest();
    var token = attemptToFetchToken(request);
    if (thereIsNoValidToken(token, now())) {
      someOtherProcessMightBeGeneratingAToken();
      token = attemptToFetchToken(request);
    }
    return token;

  }

  private GetParameterRequest creteRequest() {
    return GetParameterRequest.builder()
      .name(parameterName)
      .build();
  }

  private OAuthTokenEntry attemptToFetchToken(GetParameterRequest request) {
    return attempt(() -> ssmClient.getParameter(request))
      .map(GetParameterResponse::parameter)
      .map(Parameter::value)
      .map(this::fromJson)
      .orElse(fail -> null);
  }

  private OAuthTokenEntry fromJson(String json) {
    return attempt(() -> objectMapper.readValue(json, OAuthTokenEntry.class)).orElseThrow();
  }

  private OAuthTokenEntry storeNewToken(OAuthTokenEntry token) {
    ssmClient.putParameter(PutParameterRequest.builder()
      .name(parameterName)
      .dataType(DATATYPE)
      .tier(TIER)
      .value(toJson(token))
      .overwrite(true)
      .build());
    return token;
  }

  private String toJson(OAuthTokenEntry tokenEntry) {
    return attempt(() -> objectMapper.writeValueAsString(tokenEntry)).orElseThrow();
  }
}
