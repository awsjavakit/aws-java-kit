package com.github.awsjavakit.s3;

import static com.github.awsjavakit.s3.S3Driver.S3_SCHEME;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.collection.IsIterableContainingInOrder.contains;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.StringStartsWith.startsWith;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.github.awsjavakit.misc.ioutils.IoUtils;
import com.github.awsjavakit.misc.paths.UnixPath;
import com.github.awsjavakit.misc.paths.UriWrapper;
import com.github.awsjavakit.testingutils.aws.FakeS3Client;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.GZIPInputStream;
import net.datafaker.providers.base.BaseFaker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

class S3DriverTest {

  public static final int LARGE_NUMBER_OF_INPUTS = 10_000;
  public static final String EMPTY_STRING = "";
  public static final String ROOT = "/";
  public static final String MARKER_INDICATING_END_OF_LISTING = null;
  private static final BaseFaker FAKER = new BaseFaker();
  private static final String SAMPLE_BUCKET = "sampleBucket";
  private static final String SOME_PATH = randomString();
  private S3Driver s3Driver;
  private S3Client s3Client;

  @BeforeEach
  public void init() {
    s3Client = new FakeS3Client();
    s3Driver = new S3Driver(s3Client, SAMPLE_BUCKET);
  }

  @Test
  void shouldReturnListWithAllFilenamesInS3Folder() throws IOException {
    UnixPath firstPath = UnixPath.of(SOME_PATH).addChild(randomString()).addChild(randomString());
    UnixPath secondPath = UnixPath.of(SOME_PATH).addChild(randomString()).addChild(randomString());
    s3Driver.insertFile(firstPath, randomString());
    s3Driver.insertFile(secondPath, randomString());
    List<UnixPath> files = s3Driver.listAllFiles(UnixPath.of(SOME_PATH));
    assertThat(files, containsInAnyOrder(firstPath, secondPath));
  }

  @Test
  void shouldReturnResultContainingPartialFileListAndNewListStartingPointAndSignForTerminatingListing()
    throws IOException {
    final UnixPath firstFilePath = UnixPath.of(SOME_PATH, "a-alphabeticallyOrdered");
    final UnixPath secondFilePath = UnixPath.of(SOME_PATH, "b-alphabeticallyOrdered");
    final String firstFileContent = randomString();
    final String secondFileContent = randomString();
    s3Driver.insertFile(firstFilePath, firstFileContent);
    s3Driver.insertFile(secondFilePath, secondFileContent);

    ListingResult firstBatch = s3Driver.listFiles(UnixPath.of(SOME_PATH), null, 1);
    assertThat(firstBatch.getListingStartingPoint(), is(equalTo(firstFilePath.toString())));
    assertThat(firstBatch.isTruncated(), is(true));

    ListingResult secondBatch = s3Driver.listFiles(UnixPath.of(SOME_PATH), firstFilePath.toString(),
      1);
    assertThat(secondBatch.getListingStartingPoint(),
      is(equalTo(MARKER_INDICATING_END_OF_LISTING)));
    assertThat(secondBatch.isTruncated(), is(false));
  }

  @Test
  void shouldReturnTheContentsOfAllFilesInFolder() throws IOException {
    final UnixPath firstFilePath = UnixPath.of(SOME_PATH, randomString());
    final UnixPath secondFilePath = UnixPath.of(SOME_PATH, randomString());
    final String firstFileContent = randomString();
    final String secondFileContent = randomString();
    s3Driver.insertFile(firstFilePath, firstFileContent);
    s3Driver.insertFile(secondFilePath, secondFileContent);

    s3Driver = new S3Driver(s3Client, SAMPLE_BUCKET);
    List<String> actualContent = s3Driver.getFiles(UnixPath.of(SOME_PATH));

    assertThat(actualContent, containsInAnyOrder(firstFileContent, secondFileContent));
    assertThat(actualContent.size(), is(equalTo(2)));
  }

  @Test
  void shouldReturnTheContentsOfAllFilesInFolderWhenInputIsAFolderAsAnS3Uri() throws IOException {

    var folder = SOME_PATH;
    final var firstFilePath = UnixPath.of(folder, randomFileName());
    final var secondFilePath = UnixPath.of(folder, randomFileName());
    s3Driver.insertFile(firstFilePath, randomString());
    s3Driver.insertFile(secondFilePath, randomString());


    s3Driver = new S3Driver(s3Client, SAMPLE_BUCKET);
    var folderUri = URI.create(String.format("s3://%s/%s", SAMPLE_BUCKET, folder));

    var filePaths = s3Driver.listAllFiles(folderUri);

    assertThat(filePaths.size(), is(equalTo(2)));
    assertThat(filePaths, containsInAnyOrder(firstFilePath, secondFilePath));
  }

  @Test
  void shouldReturnFileWhenFileExists() throws IOException {
    UnixPath somePath = UnixPath.of(SOME_PATH);
    String expectedContent = randomString();
    URI fileLocation = s3Driver.insertFile(somePath, expectedContent);
    String actualContent = s3Driver.getUncompressedFile(toS3Path(fileLocation));
    assertThat(actualContent, is(equalTo(expectedContent)));
  }

  @Test
  void shouldInsertObjectEncodedInUtf8() throws IOException {
    UnixPath filePath = constructNestedPath();
    String expectedContent = randomString();
    s3Driver.insertFile(filePath, expectedContent);
    String actualContent = s3Driver.getFile(filePath);
    assertThat(actualContent, is(equalTo(expectedContent)));
  }

  @Test
  void shouldReturnContentsAsCompressedStreamWhenInputIsCompressed() throws IOException {
    String compressedFilename = "compressed.gz";
    String expectedContents = randomString();
    s3Driver.insertFile(UnixPath.of(compressedFilename), expectedContents);
    GZIPInputStream contents = s3Driver.getCompressedFile(UnixPath.of(compressedFilename));
    String result = readCompressedContents(contents);
    assertThat(result, is(equalTo(expectedContents)));
  }

  @Test
  void shouldSendDataToS3WhenInputIsInputStream() throws IOException {
    String expectedContent = longText();
    InputStream inputStream = IoUtils.stringToStream(expectedContent);

    UnixPath somePath = UnixPath.of(randomString());
    s3Driver.insertFile(somePath, inputStream);
    String actualContent = s3Driver.getFile(somePath);
    assertThat(actualContent, is(equalTo(expectedContent)));
  }

  @Test
  void shouldCompressAndStoreFileWhenInputFilenameEndsWithGzAndContentIsString()
    throws IOException {
    String expectedContent = longText();
    UnixPath filePath = UnixPath.of("input.gz");
    s3Driver.insertFile(filePath, expectedContent);
    GZIPInputStream actualContentStream = s3Driver.getCompressedFile(filePath);
    String actualContent = readCompressedContents(actualContentStream);

    assertThat(actualContent, is(equalTo(expectedContent)));
  }

  @Test
  void shouldReturnUriToS3FileLocationWhenSavingEvent() throws IOException {
    String content = randomString();
    UnixPath someFolder = UnixPath.of("parent", "child1", "child2");
    URI fileLocation = s3Driver.insertEvent(someFolder, content);
    String randomFilename = UriWrapper.fromUri(fileLocation).getLastPathElement();
    assertThat(fileLocation.getScheme(), is(equalTo(S3_SCHEME)));
    assertThat(fileLocation.getHost(), is(equalTo(SAMPLE_BUCKET)));
    assertThat(fileLocation.getPath(), is(equalTo("/parent/child1/child2/" + randomFilename)));
  }

  @Test
  void shouldReadFileContentBasedOnUri() throws IOException {
    s3Driver = new S3Driver(new FakeS3Client(), "ignoredBucketName");
    String content = randomString();
    UnixPath someFolder = UnixPath.of("parent", "child1", "child2");
    URI fileLocation = s3Driver.insertEvent(someFolder, content);
    String retrievedContent = s3Driver.readFile(fileLocation);
    assertThat(retrievedContent, is(equalTo(content)));
  }

  @ParameterizedTest
  @ValueSource(strings = { "uncompressedFileName", "compressedFileName.gz"})
  void shouldReadFileContentAsStreamBasedOnUri(String path) throws IOException {
    var fileName = UnixPath.of(path);
    var expectedContents = randomString();
    var fileLocation = s3Driver.insertFile(fileName, expectedContents);
    var streamContent = s3Driver.readFileAsStream(fileLocation);
    try (var reader = new BufferedReader(new InputStreamReader(streamContent, StandardCharsets.UTF_8))) {
      var retrievedContent = reader.readLine();
      assertThat(retrievedContent, is(equalTo(expectedContents)));
    }
  }

  @Test
  void shouldReadFileWhenReadingEvent() throws IOException {
    s3Driver = new S3Driver(new FakeS3Client(), "ignoredBucketName");
    String content = randomString();
    UnixPath someFolder = UnixPath.of("parent", "child1", "child2");
    URI fileLocation = s3Driver.insertEvent(someFolder, content);
    String retrievedContentAsEvent = s3Driver.readEvent(fileLocation);
    String retrievedContentAsFile = s3Driver.readFile(fileLocation);
    assertThat(retrievedContentAsEvent, is(equalTo(retrievedContentAsFile)));
  }

  @Test
  void shouldCompressCollectionUnderSpecifiedPath() throws IOException {
    List<String> input = new ArrayList<>();
    for (int i = 0; i < LARGE_NUMBER_OF_INPUTS; i++) {
      input.add(longText());
    }
    UnixPath somePath = UnixPath.of(randomString());
    URI fileLocation = s3Driver.insertAndCompressObjects(somePath, input);
    GZIPInputStream compressedData = s3Driver.getCompressedFile(toS3Path(fileLocation));
    BufferedReader inputReader = new BufferedReader(new InputStreamReader(compressedData));
    List<String> actualContent = inputReader.lines().collect(Collectors.toList());
    assertThat(actualContent, is(equalTo(input)));
  }

  @ParameterizedTest(name = "should store all content under specified path removing root")
  @ValueSource(strings = {EMPTY_STRING, ROOT})
  void shouldStoreAllContentUnderSpecifiedPathRemovingRoot(String pathPrefix)
    throws IOException {
    List<String> input = new ArrayList<>();
    for (int i = 0; i < LARGE_NUMBER_OF_INPUTS; i++) {
      input.add(longText());
    }
    String expectedFolderNeverContainsRootFolder = "parent/child/";
    URI fileLocation =
      s3Driver.insertAndCompressObjects(
        UnixPath.of(pathPrefix + expectedFolderNeverContainsRootFolder), input);
    GZIPInputStream compressedContent = s3Driver.getCompressedFile(toS3Path(fileLocation));
    List<String> actualContent = new BufferedReader(new InputStreamReader(compressedContent))
      .lines()
      .collect(Collectors.toList());
    assertThat(actualContent, is(equalTo(input)));
    assertThat(toS3Path(fileLocation).toString(),
      startsWith(expectedFolderNeverContainsRootFolder));
  }

  @Test
  void shouldStoreAllFilesDirectlyUnderBucketWhenCalledWithoutPath() throws IOException {
    String input = longText();
    URI fileLocation = s3Driver.insertAndCompressObjects(List.of(input));
    String actualContent = s3Driver.getFile(toS3Path(fileLocation));
    assertThat(actualContent, is(equalTo(input)));
  }

  @Test
  void shouldListFilesForFolder() throws IOException {
    var expectedFile = s3Driver.insertFile(randomPath(), randomString());
    var unexpectedFile = s3Driver.insertFile(randomPath(), randomString());

    var expectedFilepath = UriWrapper.fromUri(expectedFile).getPath().removeRoot();
    var unexpectedFilepath = UriWrapper.fromUri(unexpectedFile).getPath().removeRoot();
    var parentFolder = expectedFilepath.getParent().orElseThrow();
    var files = s3Driver.listAllFiles(parentFolder);
    assertThat(files, contains(expectedFilepath));
    assertThat(files, not(contains(unexpectedFilepath)));
  }

  @Test
  void shouldAcceptEmptyPathAndListAllBucketFilesWhenInputIsEmptyPath() throws IOException {
    var files = Stream.of(
        s3Driver.insertFile(randomPath(), randomString()),
        s3Driver.insertFile(randomPath(), randomString()))
      .map(UriWrapper::fromUri)
      .map(UriWrapper::getPath)
      .map(UnixPath::removeRoot)
      .collect(Collectors.toList());

    var actualFiles = s3Driver.listAllFiles(UnixPath.of(""));
    assertThat(actualFiles, containsInAnyOrder(files.toArray(UnixPath[]::new)));
  }

  @Test
  void shouldBeAbleToDecodeStringInEncodingOtherThanUtf8() {
    var expectedContent = randomString();
    var filename = randomFileName();
    var utf16 = expectedContent.getBytes(StandardCharsets.UTF_16);
    var request = PutObjectRequest.builder()
      .bucket(SAMPLE_BUCKET)
      .key(filename)
      .build();
    s3Client.putObject(request, RequestBody.fromBytes(utf16));
    var actualContent = s3Driver.getFile(UnixPath.of(filename), StandardCharsets.UTF_16);
    assertThat(actualContent, is(equalTo(expectedContent)));
  }

  @Test
  void shouldCopyFileFromS3UriToS3Uri() throws IOException {
    var sourceContent = randomString();
    var sourceUri = s3Driver.insertFile(randomPath(), sourceContent);
    var destinationUri =
      UriWrapper.fromUri("s3://" + SAMPLE_BUCKET).addChild(randomPath()).getUri();
    s3Driver.copyFile(sourceUri, destinationUri);
    var destinationContent = s3Driver.readFile(destinationUri);
    assertThat(destinationContent, is(equalTo(sourceContent)));
  }

  @Test
  void shouldThrowExceptionWhenWrongEncodingHasBeenUsed() {
    var expectedContent = randomString();
    var filename = randomFileName();
    var utf16 = expectedContent.getBytes(StandardCharsets.UTF_16);
    var request = PutObjectRequest.builder()
      .bucket(SAMPLE_BUCKET)
      .key(filename)
      .build();
    s3Client.putObject(request, RequestBody.fromBytes(utf16));

    Executable action = () -> s3Driver.getFile(UnixPath.of(filename), StandardCharsets.UTF_8);
    assertThrows(UncheckedIOException.class, action);
  }

  private static String randomFileName() {
    return FAKER.file().fileName();
  }

  private static String randomString() {
    return FAKER.lorem().word();
  }

  private static UnixPath constructNestedPath() {
    UnixPath expectedFileName = UnixPath.of(randomFileName());

    UnixPath parentFolder = UnixPath.of("some", "nested", "path");
    return parentFolder.addChild(expectedFileName);
  }

  private UnixPath randomPath() {
    return UnixPath.of(randomString(), randomString());
  }

  private String readCompressedContents(GZIPInputStream contents) {
    BufferedReader reader = new BufferedReader(new InputStreamReader(contents));
    return reader.lines().collect(Collectors.joining());
  }

  private UnixPath toS3Path(URI fileLocation) {
    return UriWrapper.fromUri(fileLocation).toS3bucketPath();
  }

  private String longText() {
    return FAKER.lorem().paragraph(10);
  }
}