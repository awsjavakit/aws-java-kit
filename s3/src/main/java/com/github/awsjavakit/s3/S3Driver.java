package com.github.awsjavakit.s3;

import static com.github.awsjavakit.attempt.Try.attempt;
import static java.util.Objects.isNull;

import com.github.awsjavakit.misc.JacocoGenerated;
import com.github.awsjavakit.misc.StringUtils;
import com.github.awsjavakit.misc.ioutils.IoUtils;
import com.github.awsjavakit.misc.paths.UnixPath;
import com.github.awsjavakit.misc.paths.UriWrapper;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.core.sync.ResponseTransformer;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CopyObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.GetObjectTaggingRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Object;
import software.amazon.awssdk.services.s3.model.Tag;
import software.amazon.awssdk.services.s3.model.Tagging;
import software.amazon.awssdk.services.s3.model.TaggingDirective;

//TODO: Address God Class issue
@SuppressWarnings({"PMD.GodClass", "PMD.CouplingBetweenObjects"})
public class S3Driver {

  public static final String GZIP_ENDING = ".gz";
  public static final String LINE_SEPARATOR = System.lineSeparator();

  public static final String DOUBLE_BACKSLASH = "\\\\\\\\";
  public static final String SINGLE_BACKSLASH = "\\\\";
  public static final String UNIX_SEPARATOR = "/";
  public static final int REMOVE_ROOT = 1;
  public static final int MAX_RESPONSE_SIZE_FOR_S3_LISTING = 1000;
  public static final String S3_SCHEME = "s3";
  private final S3Client client;
  private final String bucketName;

  public S3Driver(S3Client s3Client, String bucketName) {
    this.client = s3Client;
    this.bucketName = bucketName;
  }

  /**
   * Inserts the content of the string in the specified location.If the filename is gz, it
   * compresses the contents.
   *
   * @param fullPath the Location path of the item (without the bucket name)
   * @param content  The data we want to store
   * @return URI for the S3 object
   * @throws IOException when compression fails.
   */
  public URI insertFile(UnixPath fullPath, String content) throws IOException {
    if (fullPath.getLastPathElement().endsWith(GZIP_ENDING)) {
      insertCompressedFile(fullPath, content);
    } else {
      insertUncompressedFile(fullPath, content);
    }
    return s3BucketUri().addChild(fullPath).getUri();
  }

  public URI insertFile(UnixPath fullPath, InputStream content) throws IOException {
    client.putObject(newPutObjectRequest(fullPath), createRequestBody(content));
    return s3BucketUri().addChild(fullPath).getUri();
  }

  public URI insertFile(UnixPath fullPath, File content) {
    client.putObject(newPutObjectRequest(fullPath), createRequestBody(content));
    return s3BucketUri().addChild(fullPath).getUri();
  }

  /**
   * Method for creating event bodies in S3 bucket.
   *
   * @param folder  The folder where the event will be stored
   * @param content the event body
   * @return S3 uri to the event file.
   * @throws IOException when IO fails
   */
  public URI insertEvent(UnixPath folder, String content) throws IOException {
    UnixPath filePath = folder.addChild(UUID.randomUUID() + GZIP_ENDING);
    insertCompressedFile(filePath, content);
    return s3BucketUri().addChild(filePath).getUri();
  }

  /**
   * Method for reading event bodies from S3 bucket.
   *
   * @param uri the S3 URI to the file. The host must be equal to the bucket name of the S3 driver
   * @return the file contents uncompressed.
   */
  public String readEvent(URI uri) {
    return readFile(uri);
  }

  /**
   * Method for reading files from S3 bucket.
   *
   * @param uri the S3 URI to the file. The host must be equal to the bucket name of the S3 driver
   * @return the file contents uncompressed.
   */
  public String readFile(URI uri) {
    UnixPath filePath = UriWrapper.fromUri(uri).toS3bucketPath();
    return getFile(filePath);
  }

  public InputStream readFileAsStream(URI uri) {
    var filePath = UriWrapper.fromUri(uri).toS3bucketPath();
    return getFileAsStream(filePath);
  }

  /**
   * Returns the last modified timestamp of the given S3 object.
   *
   * @param fileUri the S3 URI to the file. The host must be equal to the bucket name of the S3
   *                driver
   * @return the object's last modified timestamp.
   */
  public Instant lastModified(URI fileUri) {
    var filePath = UriWrapper.fromUri(fileUri).toS3bucketPath();
    var response = client.headObject(createHeadObjectRequest(filePath));
    return response.lastModified();
  }

  /**
   * Returns the tags associated with the given S3 object.
   *
   * @param fileUri the S3 URI to the file. The host must be equal to the bucket name of the S3
   *                driver
   * @return a map of tag keys to tag values.
   */
  public Map<String, String> getTags(URI fileUri) {
    var filePath = UriWrapper.fromUri(fileUri).toS3bucketPath();
    var request = GetObjectTaggingRequest.builder()
        .bucket(bucketName)
        .key(filePath.toString())
        .build();
    var response = client.getObjectTagging(request);
    return response.tagSet().stream()
        .collect(Collectors.toMap(Tag::key,Tag::value));
  }

  @JacocoGenerated
  @Deprecated
  public URI insertAndCompressFiles(UnixPath s3Folder, List<String> content) throws IOException {
    return insertAndCompressObjects(s3Folder, content);
  }

  @JacocoGenerated
  @Deprecated
  public URI insertAndCompressFiles(List<String> content) throws IOException {
    return insertAndCompressObjects(content);
  }

  public URI insertAndCompressObjects(UnixPath s3Folder, List<String> content) throws IOException {
    UnixPath path = filenameForZippedFile(s3Folder);
    PutObjectRequest putObjectRequest = newPutObjectRequest(path);
    try (InputStream compressedContent = compressContent(content)) {
      RequestBody requestBody = createRequestBody(compressedContent);
      client.putObject(putObjectRequest, requestBody);
    }
    return s3BucketUri().addChild(path).getUri();
  }

  public URI insertAndCompressObjects(List<String> content) throws IOException {
    return insertAndCompressObjects(UnixPath.EMPTY_PATH, content);
  }

  public List<String> getFiles(UnixPath folder) {
    return listAllFiles(folder)
      .stream()
      .map(this::getFile)
      .collect(Collectors.toList());
  }

  public List<UnixPath> listAllFiles(URI s3Uri) {
    return listAllFiles(UriWrapper.fromUri(s3Uri).toS3bucketPath());
  }

  public List<UnixPath> listAllFiles(UnixPath folder) {

    ListingResult result = ListingResult.emptyResult();
    do {
      var currentStartingPoint = result.getListingStartingPoint();
      var newBatch = listFiles(calculateListingFolder(folder),
        currentStartingPoint,
        MAX_RESPONSE_SIZE_FOR_S3_LISTING);

      result = result.add(newBatch);
    } while (result.isTruncated());

    return result.getFiles();
  }

  /**
   * Returns a partial result of the files contained in the specified folder. The listing starts
   * from the {@code listingStartingPoint}  if is not null or from the beginning if it is null.
   * After a call the next starting point can be acquired by the {@link ListingResult}
   *
   * @param folder               The folder that we wish to list its files.
   * @param listingStartingPoint The starting point for the listing, can be {@code null} to indicate
   *                             that the beginning of the listing.
   * @param responseSize         The number of filenames returned in each batch. Max size determined
   *                             by S3 is 1000.
   * @return a result containing the returned filenames, the next {@code listingStartingPoint} and
   * whether there are more files to list.
   */
  public ListingResult listFiles(UnixPath folder, String listingStartingPoint, int responseSize) {
    var listingResult = fetchNewResultsBatch(folder, listingStartingPoint, responseSize);
    List<UnixPath> files = extractResultsFromResponse(listingResult);
    return new ListingResult(files, listingResult.nextContinuationToken(),
      listingResult.isTruncated());
  }

  public String getUncompressedFile(UnixPath file) {
    return getUncompressedFile(file, StandardCharsets.UTF_8);
  }

  public String getUncompressedFile(UnixPath file, Charset charset) {
    GetObjectRequest getObjectRequest = createGetObjectRequest(file);
    ResponseBytes<GetObjectResponse> response = fetchObject(getObjectRequest);
    return response.asString(charset);
  }

  public InputStream getUncompressedFileAsStream(UnixPath file) {
    return client.getObject(createGetObjectRequest(file), ResponseTransformer.toInputStream());
  }

  public GZIPInputStream getCompressedFile(UnixPath file) throws IOException {
    GetObjectRequest getObjectRequest = createGetObjectRequest(file);
    ResponseInputStream<GetObjectResponse> response = client.getObject(getObjectRequest);
    return new GZIPInputStream(response);
  }

  public InputStream getCompressedFileAsStream(UnixPath file) throws IOException {
    return new GZIPInputStream(getUncompressedFileAsStream(file));
  }

  public String getFile(UnixPath filename, Charset charset) {
    if (isCompressed(filename.getLastPathElement())) {
      return attempt(() -> getCompressedFile(filename))
        .map(stream -> readCompressedStream(stream, charset))
        .orElseThrow();
    } else {
      return getUncompressedFile(filename, charset);
    }
  }

  public InputStream getFileAsStream(UnixPath filename) {
    if (isCompressed(filename.getLastPathElement())) {
      return attempt(() -> getCompressedFileAsStream(filename))
        .orElseThrow();
    } else {
      return getUncompressedFileAsStream(filename);
    }
  }

  public String getFile(UnixPath filename) {
    return getFile(filename, StandardCharsets.UTF_8);
  }

  /**
   * Copies a file from source to destination. Both URIs must be S3 URIs.
   *
   * @param sourceUri      the uri of the source file
   * @param destinationUri the uri of the destination file
   * @param tags           optional tags to replace existing tags to the copied file, may be null or empty.
   */
  public void copyFile(URI sourceUri, URI destinationUri, Tag... tags) {
    var requestBuilder = createBasicCopyRequest(sourceUri, destinationUri);
    var validTags = validTagSet(tags);
    var request = addTagsInCopyRequest(requestBuilder, validTags).build();
    client.copyObject(request);
  }

  private static CopyObjectRequest.Builder addTagsInCopyRequest(
    CopyObjectRequest.Builder requestBuilder, Collection<Tag> tags) {
    if (tags.isEmpty()) {
      return requestBuilder;
    }
    return requestBuilder.tagging(Tagging.builder().tagSet(tags).build())
      .taggingDirective(TaggingDirective.REPLACE);
  }

  private static List<Tag> validTagSet(Tag... tags) {
    return
      Optional.ofNullable(tags)
        .stream()
        .flatMap(tag -> Arrays.stream(tag).sequential())
        .map(S3Driver::validTag)
        .toList();
  }

  private static Tag validTag(Tag tag) {
    if (StringUtils.isBlank(tag.key()) || StringUtils.isBlank(tag.value())) {
      throw new IllegalTagException(tag.toString());
    }
    return tag;
  }

  private static CopyObjectRequest.Builder createBasicCopyRequest(URI sourceUri,
    URI destinationUri) {
    return CopyObjectRequest.builder()
      .sourceKey(UriWrapper.fromUri(sourceUri).toS3bucketPath().toString())
      .sourceBucket(sourceUri.getHost())
      .destinationKey(UriWrapper.fromUri(destinationUri).toS3bucketPath().toString())
      .destinationBucket(destinationUri.getHost());
  }

  private UnixPath calculateListingFolder(UnixPath folder) {
    return isNull(folder) || folder.isEmptyPath() || folder.isRoot()
           ? UnixPath.EMPTY_PATH
           : folder;
  }

  private UriWrapper s3BucketUri() {
    return new UriWrapper(S3_SCHEME, bucketName);
  }

  private void insertUncompressedFile(UnixPath fullPath, String content) throws IOException {
    try (InputStream inputStream = IoUtils.stringToStream(content)) {
      client.putObject(newPutObjectRequest(fullPath), createRequestBody(inputStream));
    }
  }

  private void insertCompressedFile(UnixPath fullPath, String content) throws IOException {
    try (InputStream inputStream = compressContent(List.of(content))) {
      insertFile(fullPath, inputStream);
    }
  }

  private UnixPath filenameForZippedFile(UnixPath s3Folder) {
    String folderPath = processPath(s3Folder);
    return UnixPath.of(folderPath, UUID.randomUUID() + GZIP_ENDING);
  }

  private String processPath(UnixPath s3Folder) {
    String unixPath = s3Folder.toString()
      .replaceAll(DOUBLE_BACKSLASH, UNIX_SEPARATOR)
      .replaceAll(SINGLE_BACKSLASH, UNIX_SEPARATOR);
    return unixPath.startsWith(UNIX_SEPARATOR)
           ? unixPath.substring(REMOVE_ROOT)
           : unixPath;
  }

  private RequestBody createRequestBody(File input) {
    return RequestBody.fromFile(input);

  }

  private RequestBody createRequestBody(InputStream input) throws IOException {
    var bytes = IoUtils.inputStreamToBytes(input);
    return RequestBody.fromBytes(bytes);
  }

  private InputStream compressContent(List<String> content) throws IOException {
    return new StringCompressor(content).gzippedData();
  }

  private ListObjectsV2Response fetchNewResultsBatch(UnixPath folder, String listingStartingPoint,
    int responseSize) {
    var request = requestForListingFiles(folder, listingStartingPoint, responseSize);
    return client.listObjectsV2(request);
  }

  private ResponseBytes<GetObjectResponse> fetchObject(GetObjectRequest getObjectRequest) {
    return client.getObject(getObjectRequest, ResponseTransformer.toBytes());
  }

  private String readCompressedStream(GZIPInputStream gzipInputStream, Charset charset)
    throws IOException {
    try (BufferedReader reader = new BufferedReader(
      new InputStreamReader(gzipInputStream, charset))) {
      return reader.lines().collect(Collectors.joining(LINE_SEPARATOR));
    }
  }

  private boolean isCompressed(String filename) {
    return filename.endsWith(GZIP_ENDING);
  }

  private GetObjectRequest createGetObjectRequest(UnixPath file) {
    return GetObjectRequest.builder()
      .bucket(bucketName)
      .key(file.toString())
      .build();
  }

  private HeadObjectRequest createHeadObjectRequest(UnixPath file) {
    return HeadObjectRequest.builder()
      .bucket(bucketName)
      .key(file.toString())
      .build();
  }

  private List<UnixPath> extractResultsFromResponse(ListObjectsV2Response result) {
    return result.contents().stream()
      .map(S3Object::key)
      .map(UnixPath::of)
      .collect(Collectors.toList());
  }

  private ListObjectsV2Request requestForListingFiles(UnixPath folder, String startingPoint,
    int responseSize) {
    return ListObjectsV2Request.builder()
      .bucket(bucketName)
      .prefix(folder.toString())
      .continuationToken(startingPoint)
      .maxKeys(responseSize)
      .build();
  }

  private PutObjectRequest newPutObjectRequest(UnixPath fullPath) {
    return PutObjectRequest.builder()
      .bucket(bucketName)
      .key(fullPath.toString())
      .build();
  }
}
