package com.github.awsjavakit.testingutils.aws;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import com.github.awsjavakit.misc.JacocoGenerated;
import com.github.awsjavakit.misc.paths.UnixPath;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
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
import software.amazon.awssdk.services.s3.model.ListObjectsRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsResponse;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import software.amazon.awssdk.services.s3.model.S3Object;

@JacocoGenerated
@SuppressWarnings({"PMD.CouplingBetweenObjects", "PMD.UnusedPrivateMethod"})
public class FakeS3Client implements S3Client {

  private static final int START_FROM_BEGINNING = 0;
  private final Map<String, Map<String, ByteBuffer>> filesAndContent;
  private final List<CopyObjectRequest> copyRequests;

  public FakeS3Client() {
    this.filesAndContent = new LinkedHashMap<>();
    this.copyRequests = new ArrayList<>();
  }

  //TODO: fix if necessary
  @SuppressWarnings("PMD.CloseResource")
  @Override
  public <ReturnT> ReturnT getObject(GetObjectRequest getObjectRequest,
                                     ResponseTransformer<GetObjectResponse, ReturnT> responseTransformer) {
    var filename = getObjectRequest.key();
    var contents = extractContent(getObjectRequest.bucket(),filename).array();
    var response = GetObjectResponse.builder().contentLength((long) contents.length)
      .build();
    return transformResponse(responseTransformer, new ByteArrayInputStream(contents), response);
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

  private Map<String, ByteBuffer> getBucketContents(String bucketName) {
    if(!filesAndContent.containsKey(bucketName)){
      throw new IllegalStateException(String.format("Bucket %s is empty",bucketName));
    }
    return filesAndContent.get(bucketName);
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
    return PutObjectResponse.builder().build();
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
    return CopyObjectResponse.builder().build();
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

  private void createBucketEntry(String bucketName) {
    if (!filesAndContent.containsKey(bucketName)) {
      filesAndContent.put(bucketName, new LinkedHashMap<>());
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

  private static String noSuchKeyErrorMessage(String bucket, String filename) {
    return String.format("Bucket %s does not contain key %s", bucket, filename);
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
