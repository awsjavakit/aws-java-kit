package com.github.awsjavakit.testingutils.aws;

import static com.github.awsjavakit.testingutils.RandomDataGenerator.randomInstant;
import static com.github.awsjavakit.testingutils.RandomDataGenerator.randomString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.collection.IsIterableContainingInOrder.contains;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsNot.not;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import com.github.awsjavakit.misc.SingletonCollector;
import com.github.awsjavakit.misc.paths.UnixPath;
import com.github.awsjavakit.misc.paths.UriWrapper;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.core.sync.ResponseTransformer;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CopyObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.GetObjectTaggingRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsResponse;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectTaggingRequest;
import software.amazon.awssdk.services.s3.model.S3Object;
import software.amazon.awssdk.services.s3.model.Tag;
import software.amazon.awssdk.services.s3.model.Tagging;

class FakeS3ClientTest {

  public static final URI SOME_URI = URI.create("s3://bucket/some/path/file.txt");
  public static final String SOME_BUCKET = "somebucket";
  public static final String SOME_BUCKET_URI = "s3://" + SOME_BUCKET;
  public static final int SOME_PAGE_SIZE = 10;
  public static final String LIST_FROM_BEGINNING = null;

  @Test
  void putObjectMakesContentAvailableForGetting() {
    FakeS3Client fakeS3Client = new FakeS3Client();

    String expectedContent = randomString();

    putObject(fakeS3Client, SOME_URI, expectedContent);
    ResponseBytes<GetObjectResponse> result = getObject(fakeS3Client, SOME_URI);
    String actualContent = result.asUtf8String();
    assertThat(actualContent, is(equalTo(expectedContent)));
  }

  @Test
  void putObjectDoesNotRemoveExistingData() {
    var s3Client = new FakeS3Client();
    var content = Map.of(URI.create("s3://someBucket/" + randomString()), randomString(),
      URI.create("s3://someBucket/" + randomString()), randomString());
    content.keySet().forEach(key -> putObject(s3Client, key, content.get(key)));
    for (var entry : content.entrySet()) {
      var actualContent = getObject(s3Client, entry.getKey())
        .asUtf8String();
      assertThat(actualContent, is(equalTo(entry.getValue())));
    }
  }

  @Test
  void shouldListInsertedFilesByInsertionOrder() {
    var sampleFilenames = createLargeSetOfRandomFilenames();
    var s3Client = new FakeS3Client();
    var listObjectsRequest = insertFilesToBucketInOrder(sampleFilenames, s3Client);
    var response = s3Client.listObjects(listObjectsRequest);
    var actualFilenames = extractFilenamesFromResponse(response);

    assertThat(actualFilenames, contains(sampleFilenames.toArray(String[]::new)));
  }

  @Test
  void shouldListFilesForSpecifiedFolder() {
    var s3Client = new FakeS3Client();
    var bucket = randomString();
    var expectedFile = insertRandomFileToS3(s3Client, bucket);
    var unexpectedFile = insertRandomFileToS3(s3Client, bucket);

    var listObjectRequest = createListObjectsRequest(bucket,
      expectedFile.getParent().orElseThrow());
    var result = s3Client.listObjects(listObjectRequest);
    var actualFilename = extractListedKeys(result);
    assertThat(actualFilename, contains(expectedFile.toString()));
    assertThat(actualFilename, not(contains(unexpectedFile.toString())));
  }

  @Test
  void shouldReturnNextListingStartPointWhenReturningAPageOfResults() {
    var s3Client = new FakeS3Client();
    var bucket = randomString();
    var expectedFile1 = insertRandomFileToS3(s3Client, bucket);
    insertRandomFileToS3(s3Client, bucket);

    var listObjectRequest = createListObjectsRequest(bucket,
      expectedFile1.getParent().orElseThrow(), 1,
      LIST_FROM_BEGINNING);
    var result = s3Client.listObjects(listObjectRequest);
    assertThat(result.nextMarker(), is(equalTo(expectedFile1.toString())));
  }

  @Test
  void shouldReturnNextListingStartPointWhenReturningAPageOfResultsWithVersion2Request() {
    var s3Client = new FakeS3Client();
    var bucket = randomString();
    var expectedFile1 = insertRandomFileToS3(s3Client, bucket);
    insertRandomFileToS3(s3Client, bucket);

    var listObjectRequest = ListObjectsV2Request.builder()
      .bucket(bucket)
      .prefix(expectedFile1.getParent().orElseThrow().toString())
      .maxKeys(1)
      .build();
    var result = s3Client.listObjectsV2(listObjectRequest);
    assertThat(result.nextContinuationToken(), is(equalTo(expectedFile1.toString())));
  }

  @Test
  void shouldBeAbleToListALlFilesUsingPaginationWhenAlsoSpecifyingAPrefix() {
    var s3Client = new FakeS3Client();
    var bucket = randomString();
    var expectedFolder = UnixPath.of(randomString());
    var unexpectedFolder = UnixPath.of(randomString());
    int numberOfExpectedFiles = 10;
    createAMixOfExpectedAndUnexpectedFiles(s3Client, bucket, expectedFolder, unexpectedFolder,
      numberOfExpectedFiles);
    var listedFiles = fetchAllExpectedFilesUsingPagination(s3Client, bucket, expectedFolder);

    assertThat(listedFiles, everyItem(containsString(expectedFolder.toString())));
    assertThat(listedFiles.size(), is(equalTo(numberOfExpectedFiles)));
  }

  @Test
  void shouldThrowExceptionWhenNextMarkerDoesNotExist() {
    var s3Client = new FakeS3Client();
    var bucket = randomString();
    var expectedFile1 = insertRandomFileToS3(s3Client, bucket);

    var listObjectRequest = ListObjectsV2Request.builder()
      .bucket(bucket)
      .prefix(expectedFile1.getParent().orElseThrow().toString())
      .continuationToken(randomString())
      .maxKeys(1)
      .build();
    assertThrows(IllegalArgumentException.class, () -> s3Client.listObjectsV2(listObjectRequest));
  }

  @Test
  void shouldAcceptCopyRequests() {
    var client = new FakeS3Client();
    var request = CopyObjectRequest.builder()
      .sourceBucket(randomString())
      .sourceKey(randomString())
      .sourceVersionId(randomString())
      .destinationBucket(randomString())
      .destinationKey(randomString())
      .build();
    client.copyObject(request);
    var sentRequest = client.getCopyRequests().stream().collect(SingletonCollector.collect());
    assertThat(sentRequest, is(equalTo(request)));
  }

  @Test
  void shouldCopyTheFileContentsFromSourceToDestination() {
    var client = new FakeS3Client();
    var bucket = randomString();
    var bucketUri = UriWrapper.fromUri("s3://" + bucket);
    var sourceFile = insertRandomFileToS3(client, bucket);

    var destination = UnixPath.of(randomString()).addChild(randomString());
    var request = CopyObjectRequest.builder()
      .sourceBucket(bucket)
      .sourceKey(sourceFile.toString())
      .destinationBucket(bucket)
      .destinationKey(destination.toString())
      .build();
    client.copyObject(request);

    var sourceUri = bucketUri.addChild(sourceFile).getUri();
    var sourceContent = getObject(client, sourceUri).asUtf8String();
    var destinationUri = bucketUri.addChild(destination).getUri();
    var destinationContent = getObject(client, destinationUri).asUtf8String();

    assertThat(sourceContent, is(not(nullValue())));
    assertThat(destinationContent, is(equalTo(sourceContent)));

  }

  @Test
  void shouldStoreSeparatelyFileOfSamePathStoredInDifferentBuckets() {
    var path = UnixPath.of("some", "path");
    var firstFileContent = "firstFileContents";
    var secondFileContent = "secondFileContents";
    var client = new FakeS3Client();
    var firstBucket = "bucket1";
    var secondBucket = "bucket2";
    var firstUri = new UriWrapper("s3", firstBucket).addChild(path).getUri();
    var secondUri = new UriWrapper("s3", secondBucket).addChild(path).getUri();

    putObject(client, firstUri, firstFileContent);
    putObject(client, secondUri, secondFileContent);

    var firstActualContent = getObject(client, firstUri).asUtf8String();
    var secondActualContent = getObject(client, secondUri).asUtf8String();
    assertThat(firstActualContent, is(equalTo(firstFileContent)));
    assertThat(secondActualContent, is(equalTo(secondFileContent)));

  }

  @Test
  void shouldInformOnBucketNotExistingWhenNoEntryHasBeenInsertedInTheBucket() {
    var client = new FakeS3Client();
    var exception = assertThrows(IllegalStateException.class,
      () -> getObject(client, URI.create(SOME_BUCKET_URI)));
    assertThat(exception.getMessage(), containsString(SOME_BUCKET));
  }

  @Test
  void shouldIncludeBucketNameWhenFileIsNotFound() {
    var client = new FakeS3Client();
    var existingFile = UriWrapper.fromUri(SOME_BUCKET_URI).addChild(randomString()).getUri();
    var nonExistingFile = UriWrapper.fromUri(SOME_BUCKET_URI).addChild(randomString()).getUri();
    putObject(client, existingFile, randomString());
    var exception = assertThrows(NoSuchKeyException.class,
      () -> getObject(client, nonExistingFile));
    assertThat(exception.getMessage(), containsString(SOME_BUCKET));
  }

  @Test
  void shouldReturnLastModifiedTimestampWhenFileIsInserted() {
    var fixedTime = randomInstant();
    var fixedClock = Clock.fixed(fixedTime, ZoneId.systemDefault());
    var client = new FakeS3Client(fixedClock);

    var uri = UriWrapper.fromUri(SOME_BUCKET_URI).addChild(randomString()).getUri();
    putObject(client, uri, randomString());

    var headRequest = HeadObjectRequest.builder()
      .bucket(uri.getHost())
      .key(extractKey(uri))
      .build();
    var response = client.headObject(headRequest);

    assertThat(response.lastModified(), is(equalTo(fixedTime)));
  }

  @Test
  void shouldUpdateLastModifiedTimestampWhenFileIsOverwritten() {
    var firstTime = randomInstant();
    var secondTime = randomInstant(firstTime);
    var clock = mock(Clock.class);
    when(clock.instant()).thenReturn(firstTime).thenReturn(secondTime);
    var client = new FakeS3Client(clock);

    var uri = UriWrapper.fromUri(SOME_BUCKET_URI).addChild(randomString()).getUri();
    putObject(client, uri, randomString());

    var headRequest = HeadObjectRequest.builder()
      .bucket(uri.getHost())
      .key(extractKey(uri))
      .build();
    var firstResponse = client.headObject(headRequest);
    assertThat(firstResponse.lastModified(), is(equalTo(firstTime)));

    putObject(client, uri, randomString());
    var secondResponse = client.headObject(headRequest);
    assertThat(secondResponse.lastModified(), is(equalTo(secondTime)));
  }

  @Test
  void shouldSetLastModifiedOnCopiedFile() {
    var firstTime = randomInstant();
    var secondTime = randomInstant(firstTime);
    var clock = mock(Clock.class);
    when(clock.instant()).thenReturn(firstTime).thenReturn(secondTime);
    var client = new FakeS3Client(clock);

    var bucket = SOME_BUCKET;
    var sourceUri = UriWrapper.fromUri(SOME_BUCKET_URI).addChild(randomString()).getUri();
    var destUri = UriWrapper.fromUri(SOME_BUCKET_URI).addChild(randomString()).getUri();

    putObject(client, sourceUri, randomString());

    var copyRequest = CopyObjectRequest.builder()
      .sourceBucket(bucket)
      .sourceKey(extractKey(sourceUri))
      .destinationBucket(bucket)
      .destinationKey(extractKey(destUri))
      .build();
    client.copyObject(copyRequest);

    var headRequest = HeadObjectRequest.builder()
      .bucket(bucket)
      .key(extractKey(destUri))
      .build();
    var response = client.headObject(headRequest);

    assertThat(response.lastModified(), is(equalTo(secondTime)));
  }

  @Test
  void shouldUseDifferentTimestampsForFilesPutAtDifferentTimes() {
    var firstTime = Instant.parse("2026-01-11T10:00:00Z");
    var secondTime = Instant.parse("2026-01-11T11:00:00Z");

    var firstClock = Clock.fixed(firstTime, ZoneId.of("UTC"));
    var firstClient = new FakeS3Client(firstClock);

    var firstUri = UriWrapper.fromUri(SOME_BUCKET_URI).addChild("file1.txt").getUri();
    putObject(firstClient, firstUri, "content1");

    var secondClock = Clock.fixed(secondTime, ZoneId.of("UTC"));
    var secondClient = new FakeS3Client(secondClock);

    var secondUri = UriWrapper.fromUri(SOME_BUCKET_URI).addChild("file2.txt").getUri();
    putObject(secondClient, secondUri, "content2");

    var firstHeadRequest = HeadObjectRequest.builder()
      .bucket(SOME_BUCKET)
      .key(extractKey(firstUri))
      .build();
    var firstResponse = firstClient.headObject(firstHeadRequest);

    var secondHeadRequest = HeadObjectRequest.builder()
      .bucket(SOME_BUCKET)
      .key(extractKey(secondUri))
      .build();
    var secondResponse = secondClient.headObject(secondHeadRequest);

    assertThat(firstResponse.lastModified(), is(equalTo(firstTime)));
    assertThat(secondResponse.lastModified(), is(equalTo(secondTime)));
  }

  @Test
  void shouldBeAbleToAddAndProvideObjectTags() {
    var client = new FakeS3Client();
    var uri = UriWrapper.fromUri(SOME_BUCKET_URI).addChild(randomString()).getUri();
    putObject(client, uri, randomString());
    var tag = Tag.builder().key(randomString()).value(randomString()).build();
    var expectedTags = List.of(tag);
    client.putObjectTagging(PutObjectTaggingRequest.builder()
        .bucket(uri.getHost())
        .key(extractKey(uri))
        .tagging(Tagging.builder().tagSet(tag).build())
      .build());

    var retrievedTags = client.getObjectTagging(GetObjectTaggingRequest.builder()
      .bucket(uri.getHost())
      .key(extractKey(uri))
      .build()).tagSet();
    assertThat(retrievedTags, containsInAnyOrder(expectedTags.toArray()));
  }

  @Test
  void shouldUseSystemClockByDefaultWhenNoClockProvided() {
    var client = new FakeS3Client();
    var beforeInsertion = Instant.now();

    var uri = UriWrapper.fromUri(SOME_BUCKET_URI).addChild(randomString()).getUri();
    putObject(client, uri, randomString());

    var afterInsertion = Instant.now();

    var headRequest = HeadObjectRequest.builder()
      .bucket(uri.getHost())
      .key(extractKey(uri))
      .build();
    var response = client.headObject(headRequest);

    assertThat(response.lastModified(), is(not(nullValue())));
    assertThat(response.lastModified().isAfter(beforeInsertion.minusSeconds(1)), is(true));
    assertThat(response.lastModified().isBefore(afterInsertion.plusSeconds(1)), is(true));
  }

  @Test
  void shouldThrowExceptionWhenHeadObjectCalledOnNonExistentFile() {
    var client = new FakeS3Client();
    var fileJustForTheBucketNotToBeEmpty = UriWrapper.fromUri(SOME_BUCKET_URI)
      .addChild(randomString()).getUri();
    putObject(client, fileJustForTheBucketNotToBeEmpty, randomString());
    var nonExistentUri = UriWrapper.fromUri(SOME_BUCKET_URI).addChild(randomString()).getUri();

    var headRequest = HeadObjectRequest.builder()
      .bucket(nonExistentUri.getHost())
      .key(extractKey(nonExistentUri))
      .build();
    assertThrows(NoSuchKeyException.class, () -> client.headObject(headRequest));
  }

  private static ListObjectsRequest createListObjectsRequest(String bucket,
                                                             UnixPath folder,
                                                             int pageSize,
                                                             String listingStartPoint) {
    return ListObjectsRequest.builder()
      .bucket(bucket)
      .prefix(folder.toString())
      .maxKeys(pageSize)
      .marker(listingStartPoint)
      .build();
  }

  private static ListObjectsRequest createListObjectsRequest(String bucket, UnixPath folder) {
    return createListObjectsRequest(bucket, folder, SOME_PAGE_SIZE, LIST_FROM_BEGINNING);
  }

  private static UnixPath insertFileToS3UnderSubfolder(S3Client s3Client,
                                                       String bucket,
                                                       UnixPath subfolder) {
    var filePath = subfolder.addChild(randomString()).addChild(randomString());
    var putRequest = insertFileToS3(bucket, filePath);
    s3Client.putObject(putRequest, RequestBody.fromBytes(randomString().getBytes()));
    return filePath;
  }

  private static UnixPath insertRandomFileToS3(S3Client s3Client, String bucket) {
    var filePath = UnixPath.of(randomString(), randomString(), randomString());
    var putRequest = insertFileToS3(bucket, filePath);
    s3Client.putObject(putRequest, RequestBody.fromBytes(randomString().getBytes()));
    return filePath;
  }

  private static PutObjectRequest insertFileToS3(String bucket, UnixPath filePath) {
    return PutObjectRequest.builder()
      .bucket(bucket)
      .key(filePath.toString())
      .build();
  }

  private static List<String> fetchAllExpectedFilesUsingPagination(FakeS3Client s3Client,
                                                                   String bucket,
                                                                   UnixPath expectedFolder) {
    final int smallPage = 3;

    var listObjectRequest =
      createListObjectsRequest(bucket, expectedFolder, smallPage, LIST_FROM_BEGINNING);
    var listingResult = s3Client.listObjects(listObjectRequest);
    var allListedKeys = new ArrayList<>(extractListedKeys(listingResult));

    while (listingResult.isTruncated()) {
      listObjectRequest =
        createListObjectsRequest(bucket, expectedFolder, smallPage, listingResult.nextMarker());
      listingResult = s3Client.listObjects(listObjectRequest);
      allListedKeys.addAll(extractListedKeys(listingResult));
    }
    return allListedKeys;
  }

  private static List<String> extractListedKeys(ListObjectsResponse listingResult) {
    return listingResult.contents()
      .stream()
      .map(S3Object::key)
      .collect(Collectors.toList());
  }

  private static String extractKey(URI s3Uri) {
    return UriWrapper.fromUri(s3Uri.getPath()).toS3bucketPath().toString();
  }

  private void createAMixOfExpectedAndUnexpectedFiles(FakeS3Client s3Client,
                                                      String bucket,
                                                      UnixPath expectedFolder,
                                                      UnixPath unexpectedFolder,
                                                      int numberOfExpectedFiles) {

    for (int counter = 0; counter < numberOfExpectedFiles; counter++) {
      insertFileToS3UnderSubfolder(s3Client, bucket, expectedFolder);
      insertFileToS3UnderSubfolder(s3Client, bucket, unexpectedFolder);
    }
  }

  private List<String> extractFilenamesFromResponse(ListObjectsResponse response) {
    return response.contents().stream()
      .map(S3Object::key)
      .map(UnixPath::of)
      .map(UnixPath::removeRoot)
      .map(UnixPath::toString)
      .collect(Collectors.toList());
  }

  private ListObjectsRequest insertFilesToBucketInOrder(List<String> sampleFilenames,
                                                        FakeS3Client s3Client) {
    for (var filename : sampleFilenames) {
      putObject(s3Client, URI.create(SOME_BUCKET_URI + "/" + filename), randomString());
    }
    return ListObjectsRequest.builder()
      .bucket(SOME_BUCKET)
      .maxKeys(sampleFilenames.size())
      .build();
  }

  private List<String> createLargeSetOfRandomFilenames() {
    return IntStream.range(0, 100)
      .boxed()
      .map(i -> randomString())
      .collect(Collectors.toList());
  }

  private ResponseBytes<GetObjectResponse> getObject(FakeS3Client fakeS3Client, URI s3Uri) {
    var getObjectRequest = GetObjectRequest.builder()
      .bucket(s3Uri.getHost())
      .key(extractKey(s3Uri))
      .build();
    return fakeS3Client.getObject(getObjectRequest, ResponseTransformer.toBytes());
  }

  private void putObject(FakeS3Client fakeS3Client, URI s3Uri, String expectedContent) {
    var putObjectRequest = PutObjectRequest.builder()
      .bucket(s3Uri.getHost())
      .key(extractKey(s3Uri))
      .tagging("asuhfoari")
      .build();

    fakeS3Client.putObject(putObjectRequest,
      RequestBody.fromBytes(expectedContent.getBytes(StandardCharsets.UTF_8)));
  }
}