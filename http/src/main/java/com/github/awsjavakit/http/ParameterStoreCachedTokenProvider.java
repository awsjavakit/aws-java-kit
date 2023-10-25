package com.github.awsjavakit.http;

import static com.github.awsjavakit.http.JsonConfig.fromJson;
import static com.github.awsjavakit.http.JsonConfig.toJson;
import static com.gtihub.awsjavakit.attempt.Try.attempt;

import com.github.awsjavakit.http.token.OAuthTokenEntry;
import com.github.awsjavakit.http.updatestrategies.TokenCacheUpdateStrategy;
import software.amazon.awssdk.services.ssm.SsmClient;
import software.amazon.awssdk.services.ssm.model.GetParameterRequest;
import software.amazon.awssdk.services.ssm.model.GetParameterResponse;
import software.amazon.awssdk.services.ssm.model.Parameter;
import software.amazon.awssdk.services.ssm.model.PutParameterRequest;

public class ParameterStoreCachedTokenProvider implements TokenProvider {

  public static final String DATATYPE = "text";
  public static final String TIER = "Standard";
  private final TokenProvider newTokenProvider;
  private final SsmClient ssmClient;
  private final String parameterName;
  private final TokenCacheUpdateStrategy<OAuthTokenEntry> updateStrategy;

  public ParameterStoreCachedTokenProvider(TokenProvider newTokenProvider,
    String ssmParameterName,
    SsmClient ssmClient,
    TokenCacheUpdateStrategy<OAuthTokenEntry> updateStrategy) {
    this.ssmClient = ssmClient;
    this.newTokenProvider = newTokenProvider;
    this.parameterName = ssmParameterName;
    this.updateStrategy = updateStrategy;
  }

  @Override
  public OAuthTokenEntry fetchToken() {
    return updateStrategy.fetchAndUpdate(this::updateToken, this::fetchTokenFromSsm);
  }

  private OAuthTokenEntry updateToken() {
    OAuthTokenEntry token;
    var newValue = newTokenProvider.fetchToken();
    token = storeNewToken(newValue);
    return token;
  }

  private OAuthTokenEntry fetchTokenFromSsm() {
    var request = creteRequest();
    return attemptToFetchToken(request);
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
      .map(json -> fromJson(json, OAuthTokenEntry.class))
      .orElse(fail -> null);
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

}
