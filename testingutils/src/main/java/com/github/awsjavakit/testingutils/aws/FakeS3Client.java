package com.github.awsjavakit.testingutils.aws;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

import com.github.awsjavakit.misc.JacocoGenerated;
import com.github.awsjavakit.misc.paths.UnixPath;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.core.sync.ResponseTransformer;
import software.amazon.awssdk.http.AbortableInputStream;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CopyObjectRequest;
import software.amazon.awssdk.services.s3.model.CopyObjectResponse;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.GetObjectTaggingRequest;
import software.amazon.awssdk.services.s3.model.GetObjectTaggingResponse;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.ListObjectsRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsResponse;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectTaggingRequest;
import software.amazon.awssdk.services.s3.model.PutObjectTaggingResponse;
import software.amazon.awssdk.services.s3.model.S3Object;
import software.amazon.awssdk.services.s3.model.Tag;
import software.amazon.awssdk.services.s3.model.Tagging;

@JacocoGenerated
@SuppressWarnings({"PMD.CouplingBetweenObjects", "PMD.UnusedPrivateMethod"})
public class FakeS3Client implements S3Client {

  public static final String TAG_SEPARATOR_CHAR = "&";
  public static final String TAG_KEY_VALUE_SEPARATOR = "=";
  private static final int START_FROM_BEGINNING = 0;
  private final Map<String, Map<String, ByteBuffer>> filesAndContent;
  private final Map<String, Map<String, Instant>> lastModified;
  private final Map<String, Map<String, List<Tag>>> tagStore;
  private final List<CopyObjectRequest> copyRequests;
  private final Clock clock;

  public FakeS3Client() {
    this(Clock.systemDefaultZone());
  }

  public FakeS3Client(Clock clock) {
    this.filesAndContent = new LinkedHashMap<>();
    this.lastModified = new LinkedHashMap<>();
    this.copyRequests = new ArrayList<>();
    this.clock = clock;
    this.tagStore = new LinkedHashMap<>();
  }

  @Override
  public CopyObjectResponse copyObject(CopyObjectRequest copyObjectRequest) {
    createBucketEntry(copyObjectRequest.sourceBucket());
    createBucketEntry(copyObjectRequest.destinationBucket());
    this.copyRequests.add(copyObjectRequest);
    var contents = getBucketContents(copyObjectRequest.sourceBucket())
      .get(copyObjectRequest.sourceKey());
    createBucketEntry(copyObjectRequest.destinationBucket());
    getBucketContents(copyObjectRequest.destinationBucket())
      .put(copyObjectRequest.destinationKey(), contents);

    // Mirror AWS behavior loosely: destination has a new last modified timestamp.
    lastModified.get(copyObjectRequest.destinationBucket())
      .put(copyObjectRequest.destinationKey(), clock.instant());

    addTagsToCopiedObject(copyObjectRequest);

    return CopyObjectResponse.builder().build();
  }

  //TODO: fix if necessary
  @SuppressWarnings("PMD.CloseResource")
  @Override
  public <ReturnT> ReturnT getObject(GetObjectRequest getObjectRequest,
    ResponseTransformer<GetObjectResponse, ReturnT> responseTransformer) {
    var filename = getObjectRequest.key();
    var contents = extractContent(getObjectRequest.bucket(), filename).array();
    var response = GetObjectResponse.builder().contentLength((long) contents.length)
      .build();
    return transformResponse(responseTransformer, new ByteArrayInputStream(contents), response);
  }

  @Override
  public GetObjectTaggingResponse getObjectTagging(
    GetObjectTaggingRequest getObjectTaggingRequest) {
    var tags = Optional.ofNullable(tagStore.get(getObjectTaggingRequest.bucket()))
      .map(bucketTags -> bucketTags.get(getObjectTaggingRequest.key()))
      .orElse(List.of());
    return GetObjectTaggingResponse.builder()
      .tagSet(tags)
      .build();
  }

  @Override
  public HeadObjectResponse headObject(HeadObjectRequest headObjectRequest) {
    var bucket = headObjectRequest.bucket();
    var key = headObjectRequest.key();
    failIfFileDoesNotExist(bucket, key);

    var lastModified = Optional.ofNullable(this.lastModified.get(bucket))
      .map(lastMod -> lastMod.get(key))
      .orElse(null);

    return HeadObjectResponse.builder()
      .lastModified(lastModified)
      .build();
  }

  /**
   * Lists objects paginated one by one.
   *
   * @param listObjectsRequest the request
   * @return Response containing only one object.
   */
  @Override
  public ListObjectsResponse listObjects(ListObjectsRequest listObjectsRequest) {
    var fileKeys = new ArrayList<>(getBucketContents(listObjectsRequest.bucket()).keySet());

    var startIndex = calculateStartIndex(fileKeys, listObjectsRequest.marker());
    var excludedEndIndex = calculateEndIndex(fileKeys, listObjectsRequest.marker(),
      listObjectsRequest.maxKeys());

    var files = fileKeys.subList(startIndex, excludedEndIndex).stream()
      .filter(filePath -> filePathIsInSpecifiedParentFolder(filePath, listObjectsRequest))
      .map(filename -> S3Object.builder().key(filename).build())
      .collect(Collectors.toList());
    var nextStartListingPoint = calculateNestStartListingPoint(fileKeys, excludedEndIndex);

    return ListObjectsResponse.builder().contents(files)
      .nextMarker(nextStartListingPoint)
      .isTruncated(nonNull(nextStartListingPoint)).build();
  }

  @Override
  public ListObjectsV2Response listObjectsV2(ListObjectsV2Request v2Request) {
    var oldRequest = ListObjectsRequest.builder()
      .bucket(v2Request.bucket())
      .marker(v2Request.continuationToken())
      .maxKeys(v2Request.maxKeys())
      .prefix(v2Request.prefix())
      .build();
    var oldResponse = listObjects(oldRequest);
    return ListObjectsV2Response
      .builder()
      .contents(oldResponse.contents())
      .isTruncated(oldResponse.isTruncated())
      .continuationToken(v2Request.continuationToken())
      .nextContinuationToken(oldResponse.nextMarker())
      .build();
  }

  //TODO: fix if necessary
  @SuppressWarnings("PMD.CloseResource")
  @Override
  public PutObjectResponse putObject(PutObjectRequest putObjectRequest, RequestBody requestBody) {
    var bucketName = putObjectRequest.bucket();
    var path = putObjectRequest.key();
    var content = requestBody.contentStreamProvider().newStream();
    createBucketEntry(bucketName);
    this.filesAndContent.get(bucketName).put(path, inputSteamToByteBuffer(content));
    this.lastModified.get(bucketName).put(path, clock.instant());
    return PutObjectResponse.builder().build();
  }

  @Override
  public PutObjectTaggingResponse putObjectTagging(
    PutObjectTaggingRequest putObjectTaggingRequest) {
    var bucket = putObjectTaggingRequest.bucket();
    var key = putObjectTaggingRequest.key();
    var tags = putObjectTaggingRequest.tagging().tagSet();
    tagStore.computeIfAbsent(bucket, b -> new HashMap<>())
      .put(key, tags);
    return PutObjectTaggingResponse.builder()
      .versionId(putObjectTaggingRequest.versionId())
      .build();
  }

  @Override
  public String serviceName() {
    return "FakeS3Client";
  }

  @Override
  public void close() {
    //NO-OP;
  }

  public List<CopyObjectRequest> getCopyRequests() {
    return copyRequests;
  }

  private static List<Tag> extractTagsList(String stringTags) {
    return Optional.ofNullable(stringTags)
      .map(tagsString -> Arrays.stream(tagsString.split(TAG_SEPARATOR_CHAR))
        .filter(tag -> !tag.isEmpty())
        .map(s -> s.split(TAG_KEY_VALUE_SEPARATOR))
        .map(arr -> Tag.builder().key(arr[0]).value(arr[1]).build())
        .toList())
      .orElse(Collections.emptyList());
  }

  private static ByteBuffer inputSteamToByteBuffer(InputStream inputStream) {
    return ByteBuffer.wrap(readAllBytes(inputStream));
  }

  private static byte[] readAllBytes(InputStream inputStream) {
    try {
      return inputStream.readAllBytes();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private static int indexOfLastReadFile(List<String> fileKeys, String marker) {
    int indexOfLastFileRead = fileKeys.indexOf(marker);
    if (indexOfLastFileRead < 0) {
      throw new IllegalArgumentException("Marker/ContinuationToken is not valid");
    }
    return indexOfLastFileRead;
  }

  private static String noSuchKeyErrorMessage(String bucket, String filename) {
    return String.format("Bucket %s does not contain key %s", bucket, filename);
  }

  private void addTagsToCopiedObject(CopyObjectRequest copyObjectRequest) {
    var tagList = extractTagsList(copyObjectRequest.tagging());
    putObjectTagging(PutObjectTaggingRequest.builder()
      .bucket(copyObjectRequest.destinationBucket())
      .key(copyObjectRequest.destinationKey())
      .tagging(Tagging.builder().tagSet(tagList).build())
      .build());
  }

  private Map<String, ByteBuffer> getBucketContents(String bucketName) {
    if (!filesAndContent.containsKey(bucketName)) {
      throw new IllegalStateException(String.format("Bucket %s is empty", bucketName));
    }
    return filesAndContent.get(bucketName);
  }

  private void failIfFileDoesNotExist(String bucket, String key) {
    extractContent(bucket, key);
  }

  private void createBucketEntry(String bucketName) {
    if (!filesAndContent.containsKey(bucketName)) {
      filesAndContent.put(bucketName, new LinkedHashMap<>());
    }
    if (!lastModified.containsKey(bucketName)) {
      lastModified.put(bucketName, new LinkedHashMap<>());
    }
  }

  private String calculateNestStartListingPoint(List<String> fileKeys,
    int excludedEndIndex) {
    return excludedEndIndex >= fileKeys.size()
      ? null
      : fileKeys.get(excludedEndIndex - 1);
  }

  private boolean filePathIsInSpecifiedParentFolder(String filePathString,
    ListObjectsRequest listObjectsRequest) {
    var filePath = UnixPath.of(filePathString).removeRoot();
    var parentFolder = Optional.of(listObjectsRequest)
      .map(ListObjectsRequest::prefix)
      .map(UnixPath::of)
      .map(UnixPath::removeRoot)
      .orElse(UnixPath.EMPTY_PATH);

    return parentFolder.isEmptyPath()
      || parentFolder.isRoot()
      || filePath.toString().startsWith(parentFolder.toString());
  }

  private int calculateEndIndex(List<String> fileKeys, String marker, Integer pageSize) {
    int startIndex = calculateStartIndex(fileKeys, marker);
    return Math.min(startIndex + pageSize, fileKeys.size());
  }

  private int calculateStartIndex(List<String> fileKeys, String marker) {
    if (isNull(marker)) {
      return START_FROM_BEGINNING;
    } else {
      var calculatedStartIndex = indexOfLastReadFile(fileKeys, marker) + 1;
      if (calculatedStartIndex < fileKeys.size()) {
        return calculatedStartIndex;
      }
    }
    throw new IllegalStateException("Start index is out of bounds in FakeS3Client");
  }

  private ByteBuffer extractContent(String bucket, String filename) {
    if (getBucketContents(bucket).containsKey(filename)) {
      return getBucketContents(bucket).get(filename);
    } else {
      throw NoSuchKeyException.builder()
        .message(noSuchKeyErrorMessage(bucket, filename))
        .build();
    }
  }

  private <ReturnT> ReturnT transformResponse(
    ResponseTransformer<GetObjectResponse, ReturnT> responseTransformer,
    InputStream inputStream, GetObjectResponse response) {
    try {
      return responseTransformer.transform(response, AbortableInputStream.create(inputStream));
    } catch (Exception exception) {
      throw new RuntimeException(exception);
    }
  }
}
