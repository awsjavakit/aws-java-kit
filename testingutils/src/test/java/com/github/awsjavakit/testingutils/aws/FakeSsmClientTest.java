package com.github.awsjavakit.testingutils.aws;

import static com.github.awsjavakit.testingutils.RandomDataGenerator.randomString;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import software.amazon.awssdk.services.ssm.model.GetParameterRequest;
import software.amazon.awssdk.services.ssm.model.ParameterAlreadyExistsException;
import software.amazon.awssdk.services.ssm.model.ParameterNotFoundException;
import software.amazon.awssdk.services.ssm.model.PutParameterRequest;

class FakeSsmClientTest {

  public static final String TEXT_DATATYPE = "text";
  private FakeSsmClient client;

  @BeforeEach
  public void init() {
    this.client = new FakeSsmClient();
  }

  @Test
  void shouldStoreParameterInParameterStore() {
    var parameterName = randomString();
    var parameterValue = randomString();
    var putRequest = createPutRequest(parameterName, parameterValue, null);
    client.putParameter(putRequest);
    var response = client.getParameter(createGetRequest(parameterName));
    var parameter = response.parameter();
    assertThat(parameter.name()).isEqualTo(parameterName);
    assertThat(parameter.value()).isEqualTo(parameterValue);
  }

  @Test
  void shouldThrowParameterNotFoundExceptionWhenParameterDoesNotExist() {
    var parameterName = randomString();
    Executable action = () -> client.getParameter(createGetRequest(parameterName));
    var exception = assertThrows(ParameterNotFoundException.class, action);
    assertThat(exception.getMessage()).contains(parameterName);
  }

  @Test
  void shouldReturnTheLatestVersionOfTheParameter() {
    var parameterName = randomString();
    var firstValue = randomString();
    var secondValue = randomString();
    client.putParameter(createPutRequest(parameterName, firstValue, null));
    var firstFetch = client.getParameter(createGetRequest(parameterName))
      .parameter().value();
    assertThat(firstFetch).isEqualTo(firstValue);

    client.putParameter(createPutRequest(parameterName, secondValue, true));
    var secondFetch = client.getParameter(createGetRequest(parameterName))
      .parameter().value();
    assertThat(secondFetch).isEqualTo(secondValue);
  }

  @Test
  void shouldThrowExceptionWhenTryingToOverwriteValueAndOverwriteOptionHasNotBeenSet() {
    var parameterName = randomString();
    var value = randomString();
    var request = createPutRequest(parameterName, value, null);
    client.putParameter(request);
    assertThrows(ParameterAlreadyExistsException.class, () -> client.putParameter(request));
  }

  @Test
  void shouldThrowExceptionWhenTryingToOverwriteValueAndOverwriteOptionHasBeenDisabled() {
    var parameterName = randomString();
    var value = randomString();
    var request = createPutRequest(parameterName, value, null);
    client.putParameter(request);
    assertThrows(ParameterAlreadyExistsException.class, () -> client.putParameter(request));
  }

  private static PutParameterRequest createPutRequest(String parameterName, String parameterValue,
    Boolean ovewrite) {
    return PutParameterRequest.builder()
      .name(parameterName)
      .value(parameterValue)
      .dataType(TEXT_DATATYPE)
      .overwrite(ovewrite)
      .build();
  }

  private GetParameterRequest createGetRequest(String parameterName) {
    return GetParameterRequest.builder()
      .name(parameterName)
      .build();
  }

}