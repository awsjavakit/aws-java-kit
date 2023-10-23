package com.github.awsjavakit.http;

import static com.gtihub.awsjavakit.attempt.Try.attempt;
import static java.util.Objects.isNull;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.awsjavakit.http.token.OAuthTokenEntry;
import java.time.Duration;
import java.time.Instant;
import software.amazon.awssdk.services.ssm.SsmClient;
import software.amazon.awssdk.services.ssm.model.GetParameterRequest;
import software.amazon.awssdk.services.ssm.model.GetParameterResponse;
import software.amazon.awssdk.services.ssm.model.Parameter;
import software.amazon.awssdk.services.ssm.model.PutParameterRequest;

public class ParameterStoreCachedTokenProvider implements TokenProvider {

  public static final String DATATYPE = "text";
  public static final String TIER = "Standard";
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
  public String fetchToken() {
    var now = Instant.now();
    var token = fetchTokenFromSsm();
    if (thereIsNoValidToken(token, now)) {
      var newValue = newTokenProvider.fetchToken();
      token = storeNewToken(newValue);
    }
    return token.value();
  }

  private Boolean thereIsNoValidToken(OAuthTokenEntry token, Instant now) {
    return isNull(token) || now.isAfter(token.timestamp().plus(maxTokenAge));
  }

  private OAuthTokenEntry fetchTokenFromSsm() {
    GetParameterRequest request = GetParameterRequest.builder()
      .name(parameterName)
      .build();
    return attempt(() -> ssmClient.getParameter(request))
      .map(GetParameterResponse::parameter)
      .map(Parameter::value)
      .map(this::fromJson)
      .orElse(fail -> null);

  }

  private OAuthTokenEntry fromJson(String json) {
    return attempt(() -> objectMapper.readValue(json, OAuthTokenEntry.class)).orElseThrow();
  }

  private OAuthTokenEntry storeNewToken(String token) {
    var tokenEntry = new OAuthTokenEntry(token, Instant.now());
    ssmClient.putParameter(PutParameterRequest.builder()
      .name(parameterName)
      .dataType(DATATYPE)
      .tier(TIER)
      .value(toJson(tokenEntry))
      .build());
    return tokenEntry;
  }

  private String toJson(OAuthTokenEntry tokenEntry) {
    return attempt(() -> objectMapper.writeValueAsString(tokenEntry)).orElseThrow();
  }
}
