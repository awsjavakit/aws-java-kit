package com.github.awsjavakit.testingutils.aws;

import static com.github.awsjavakit.attempt.Try.attempt;
import static java.util.Objects.isNull;

import com.github.awsjavakit.misc.JacocoGenerated;
import java.util.HashSet;
import java.util.Set;
import software.amazon.awssdk.services.ssm.SsmClient;
import software.amazon.awssdk.services.ssm.model.GetParameterRequest;
import software.amazon.awssdk.services.ssm.model.GetParameterResponse;
import software.amazon.awssdk.services.ssm.model.Parameter;
import software.amazon.awssdk.services.ssm.model.ParameterAlreadyExistsException;
import software.amazon.awssdk.services.ssm.model.ParameterNotFoundException;
import software.amazon.awssdk.services.ssm.model.PutParameterRequest;
import software.amazon.awssdk.services.ssm.model.PutParameterResponse;

public class FakeSsmClient implements SsmClient {

  public static final long HARDCODED_VERSION = 1L;
  public static final long ABSENT = 0;
  private final Set<Parameter> parameters;

  public FakeSsmClient() {
    this.parameters = new HashSet<>();
  }

  @Override
  public PutParameterResponse putParameter(PutParameterRequest putParameterRequest) {
    var lastVersion = fetchLatestVersion(putParameterRequest);
    validateRequest(putParameterRequest, lastVersion);
    persistParameter(putParameterRequest, lastVersion);
    return PutParameterResponse.builder()
      .version(HARDCODED_VERSION)
      .build();
  }

  @Override
  public GetParameterResponse getParameter(GetParameterRequest getParameterRequest) {
    return getLatestVersion(getParameterRequest.name());
  }

  @Override
  @JacocoGenerated
  public String serviceName() {
    return "ssm";
  }

  @Override
  @JacocoGenerated
  public void close() {
    //NO-OP
  }

  private static void validateRequest(PutParameterRequest putParameterRequest, Long lastVersion) {
    if (lastVersion != ABSENT && overwriteNotEnabled(putParameterRequest)) {
      throw ParameterAlreadyExistsException.builder().message(putParameterRequest.name()).build();
    }
  }

  private static boolean overwriteNotEnabled(PutParameterRequest putParameterRequest) {
    return isNull(putParameterRequest.overwrite()) || !putParameterRequest.overwrite();
  }

  private static Parameter createParameter(PutParameterRequest putParameterRequest,
    Long lastVersion) {
    return Parameter.builder()
      .name(putParameterRequest.name())
      .value(putParameterRequest.value())
      .version(lastVersion + 1)
      .build();
  }

  private static ParameterNotFoundException notFoundException(String parameterName) {
    return ParameterNotFoundException.builder()
      .message("Parameter does not exist:" + parameterName).build();
  }

  private void persistParameter(PutParameterRequest putParameterRequest, Long lastVersion) {
    var parameter = createParameter(putParameterRequest, lastVersion);
    parameters.add(parameter);
  }

  private GetParameterResponse getLatestVersion(String parameterName) {
    return parameters.stream()
      .filter(parameter -> parameter.name().equals(parameterName))
      .reduce(this::maxVersion)
      .map(this::createResponse)
      .orElseThrow(() -> notFoundException(parameterName));
  }

  private Long fetchLatestVersion(PutParameterRequest putParameterRequest) {
    return attempt(() -> getLatestVersion(putParameterRequest.name()))
      .map(GetParameterResponse::parameter)
      .map(Parameter::version)
      .orElse(fail -> ABSENT);
  }

  private GetParameterResponse createResponse(Parameter parameter) {
    return GetParameterResponse.builder()
      .parameter(parameter)
      .build();
  }

  private Parameter maxVersion(Parameter p1, Parameter p2) {
    return p1.version() > p2.version() ? p1 : p2;
  }

}
