package com.github.awsjavakit.misc.paths;

import static com.github.awsjavakit.misc.paths.UriWrapper.EMPTY_FRAGMENT;
import static com.github.awsjavakit.testingutils.RandomDataGenerator.randomInteger;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;

class UriWrapperTest {

  public static final String HOST = "http://www.example.org";
  public static final int MAX_PORT_NUMBER = 65_535;
  private static final String ROOT = "/";

  @Test
  void shouldRemovePathDelimiterFromTheEndOfTheUri() {
    var inputPath = "/some/folder/file.json/";
    var uriWrapper = UriWrapper.fromUri("http://www.example.org" + inputPath);
    var actualPath = uriWrapper.getPath().toString();
    var expectedPath = "/some/folder/file.json";
    assertThat(actualPath, is(equalTo(expectedPath)));
  }

  @Test
  void shouldReturnParentPathIfParentExists() {
    var uriWrapper = UriWrapper.fromUri(HOST + "/level1/level2/file.json");
    var parent = uriWrapper.getParent().orElseThrow();
    assertThat(parent.getPath().toString(), is(equalTo("/level1/level2")));
    var grandParent = parent.getParent().orElseThrow();
    assertThat(grandParent.getPath().toString(), is(equalTo("/level1")));
  }

  @Test
  void shouldReturnEmptyWhenPathIsRoot() {
    var uriWrapper = UriWrapper.fromUri(HOST + "/");
    Optional<UriWrapper> parent = uriWrapper.getParent();
    assertThat(parent.isEmpty(), is(true));
  }

  @Test
  void shouldReturnHostUri() {
    var uriWrapper = UriWrapper.fromUri(HOST + "/some/path/is.here");
    var expectedUri = URI.create(HOST);
    assertThat(uriWrapper.getHost().getUri(), is(equalTo(expectedUri)));
  }

  @Test
  void shouldAddChildToPath() {
    var originalPath = "/some/path";
    var parent = UriWrapper.fromUri(HOST + originalPath);
    var child = parent.addChild("level1", "level2", "level3");
    var expectedChildUri = URI.create(HOST + originalPath + "/level1/level2/level3");
    assertThat(child.getUri(), is(equalTo(expectedChildUri)));

    var anotherChild = parent.addChild("level4").addChild("level5");
    var expectedAnotherChildUri = URI.create(HOST + originalPath + "/level4/level5");
    assertThat(anotherChild.getUri(), is(equalTo(expectedAnotherChildUri)));
  }

  @Test
  void shouldReturnPathWithChildWhenChildDoesNotStartWithDelimiter() {
    var parentPath = UriWrapper.fromUri(HOST);
    var inputChildPath = "some/path";
    var expectedResult = URI.create(HOST + ROOT + inputChildPath);
    var actualResult = parentPath.addChild(inputChildPath);
    assertThat(actualResult.getUri(), is(equalTo(expectedResult)));
  }

  @Test
  void shouldReturnS3BucketPathWithoutRoot() {
    var expectedPath = "parent1/parent2/filename.txt";
    var s3Uri = URI.create("s3://somebucket" + ROOT + expectedPath);
    var wrapper = UriWrapper.fromUri(s3Uri);
    var s3Path = wrapper.toS3bucketPath();
    assertThat(s3Path.toString(), is(equalTo(expectedPath)));
  }

  @Test
  void shouldReturnFilenameOfUri() {
    var expectedFilename = "filename.txt";
    var filePath = String.join(UnixPath.PATH_DELIMITER, "parent1", "parent2", expectedFilename);
    var s3Uri = URI.create("s3://somebucket" + ROOT + filePath);
    var wrapper = UriWrapper.fromUri(s3Uri);
    assertThat(wrapper.getLastPathElement(), is(equalTo(expectedFilename)));
  }

  @Test
  void shouldReturnUriWithSchemeAndHostWhenCalledWithSchemeAndHost() {
    var uri = new UriWrapper("https", "example.org");
    assertThat(uri.getUri(), is(equalTo(URI.create("https://example.org"))));
  }

  @Test
  void shouldReturnUriWithQueryParametersWhenSingleQueryParameterIsPresent() {
    var expectedUri = URI.create("https://www.example.org/path1/path2?key1=value1");
    var uri = URI.create("https://www.example.org/");
    var actualUri = UriWrapper.fromUri(uri)
      .addChild("path1")
      .addQueryParameter("key1", "value1")
      .addChild("path2")
      .getUri();
    assertThat(actualUri, is(equalTo(expectedUri)));
  }

  @Test
  void shouldPreservePortWhenAddingPathAndQueryPapametersInUri() {
    var expectedUri = URI.create("https://www.example.org:1234/path1/path2?key1=value1");
    var host = URI.create("https://www.example.org:1234");
    var actualUri = UriWrapper.fromUri(host)
      .addChild("path1")
      .addQueryParameter("key1", "value1")
      .addChild("path2")
      .getUri();
    assertThat(actualUri, is(equalTo(expectedUri)));
  }

  @Test
  void shouldReturnUriWithQueryParametersWhenManyQueryParametersArePresent() {
    var expectedUri = URI.create("https://www.example.org/path1/path2?key1=value1&key2=value2");
    var uri = URI.create("https://www.example.org/");
    var actualUri = UriWrapper.fromUri(uri)
      .addChild("path1")
      .addQueryParameter("key1", "value1")
      .addQueryParameter("key2", "value2")
      .addChild("path2")
      .getUri();
    assertThat(actualUri, is(equalTo(expectedUri)));
  }

  @Test
  void shouldReturnUriWithQueryParametersWhenQueryParametersAreMap() {
    var expectedUri = URI.create(
      "https://www.example.org/path1/path2?key1=value1&key2=value2&key3=value3");
    var uri = URI.create("https://www.example.org/");
    final Map<String, String> parameters = getOrderedParametersMap();
    var actualUri = UriWrapper.fromUri(uri)
      .addChild("path1")
      .addQueryParameters(parameters)
      .addChild("path2")
      .addQueryParameter("key3", "value3")
      .getUri();
    assertThat(actualUri, is(equalTo(expectedUri)));
  }

  @Test
  void shouldReturnStringRepresentationOfUri() {
    var expectedUri = URI.create(
      "https://www.example.org/path1/path2?key1=value1&key2=value2&key3=value3");
    var uri = new UriWrapper("https", "www.example.org")
      .addChild("path1")
      .addChild("path2")
      .addQueryParameter("key1", "value1")
      .addQueryParameter("key2", "value2")
      .addQueryParameter("key3", "value3");

    assertThat(uri.toString(), is(equalTo(expectedUri.toString())));
  }

  @ParameterizedTest(name = "should throw exception when either host is empty")
  @NullAndEmptySource
  void shouldThrowExceptionWhenHostIsEmpty(String emptyInput) {
    assertThrows(IllegalArgumentException.class, () -> new UriWrapper("https", emptyInput));
  }

  @Test
  void shouldCreateAnHttpsUriByDefaultWhenInputIsAHostDomain() {
    var constructedUri = UriWrapper.fromHost("example.org").getUri();
    assertThat(constructedUri.getScheme(), is(equalTo(UriWrapper.HTTPS)));
  }

  @Test
  void shouldATolerateInputAsUriWhenCreatingUriFromHost() {
    var hostAsUri = "http://example.com/hello/world";
    var actualHostUri = UriWrapper.fromHost(hostAsUri).getUri();
    var expectedHostUri = URI.create("https://example.com");
    assertThat(actualHostUri, is(expectedHostUri));
  }

  @Test
  void shouldReturnUriWithCustomPort() {
    var expectedPort = randomInteger(MAX_PORT_NUMBER);
    var actualUri = UriWrapper.fromHost("example.org", expectedPort).getUri();
    assertThat(actualUri, is(equalTo(URI.create("https://example.org:" + expectedPort))));
  }

  @Test
  void shouldReturnQueryParameters() throws URISyntaxException {
    var queryString = "key1=withoutSpace&key2=with space&key3=with\\&ampersand";
    var uri = new URI("https", "www.example.com", "/some/path", queryString, EMPTY_FRAGMENT);

    var wrapped = UriWrapper.fromUri(uri);
    var queryParameters = wrapped.getQueryParameters();
    assertThat(queryParameters.keySet(), containsInAnyOrder("key1", "key2", "key3"));
    assertThat(queryParameters.values(),
      containsInAnyOrder("withoutSpace", "with space", "with\\&ampersand"));
  }

  private Map<String, String> getOrderedParametersMap() {
    final Map<String, String> parameters = new ConcurrentHashMap<>();
    parameters.put("key1", "value1");
    parameters.put("key2", "value2");
    return parameters;
  }
}
