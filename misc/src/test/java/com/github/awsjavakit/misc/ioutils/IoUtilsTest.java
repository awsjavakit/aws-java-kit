package com.github.awsjavakit.misc.ioutils;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.StringContains.containsString;
import static org.hamcrest.text.IsEmptyString.emptyOrNullString;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.github.awsjavakit.misc.ioutils.exceptions.FileNotFoundUncheckedException;
import com.github.awsjavakit.misc.ioutils.exceptions.ResourceNotFoundException;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class IoUtilsTest {

    private static final Path NON_EXISTING_RESOURCE = Path.of("sampleFolder", "nonExisting.txt");
    private static final Path RESOURCE = Path.of("sampleFolder", "sampleResource.txt");

    @Test
    @DisplayName("inputStreamFromResources returns a non empty stream for existing resources")
    void inputStreamFromResourcesReturnANonEmptyStreamForExistingResource()
            throws IOException {
        InputStream stream = IoUtils.inputStreamFromResources(RESOURCE);
        String line = new BufferedReader(new InputStreamReader(stream)).readLine();
        assertThat(line, is(not(emptyOrNullString())));

    }

    @Test
    @DisplayName("inputStreamFromResources throws a ResourceNotFoundException for non existing resources")
    void inputStreamFromResourcesReturnsAResourceNotFoundExceptionForNonExistingResources() {
        ResourceNotFoundException exc = assertThrows(ResourceNotFoundException.class,
                () -> IoUtils.inputStreamFromResources(NON_EXISTING_RESOURCE));
        assertThat(exc.getMessage(), containsString(ResourceNotFoundException.ERROR_MESSAGE));
        assertThat(exc.getMessage(), containsString(NON_EXISTING_RESOURCE.toString()));
    }

    @Test
    @DisplayName("stringFromResources returns the whole content of a resource file in a string")
    void stringFromResourcesReturnsTheWholeContentOfAResourceFileInAString() {
        String actual = IoUtils.stringFromResources(RESOURCE);
        String expected = new BufferedReader(
                new InputStreamReader(IoUtils.inputStreamFromResources(RESOURCE)))
                .lines().collect(Collectors.joining(System.lineSeparator()));
        assertThat(actual, is(equalTo(expected)));
    }

    @Test
    @DisplayName("stringFromResources throws a ResourceNotFoundException for non existing resources")
    void stringFromResourcesReturnsAResourceNotFoundExceptionForNonExistingResources() {
        ResourceNotFoundException exc = assertThrows(ResourceNotFoundException.class,
                () -> IoUtils.stringFromResources(NON_EXISTING_RESOURCE));
        assertThat(exc.getMessage(), containsString(ResourceNotFoundException.ERROR_MESSAGE));
        assertThat(exc.getMessage(), containsString(NON_EXISTING_RESOURCE.toString()));
    }

    @Test
    @DisplayName("stringFromFile return file contents a string")
    void fileAsStringReturnsFileContentsAsString() throws URISyntaxException {
        URL file = Thread.currentThread().getContextClassLoader().getResource(RESOURCE.toString());
        Path filePath = Path.of(file.toURI());
        String actual = IoUtils.stringFromFile(filePath);
        String expected = IoUtils.stringFromResources(RESOURCE);
        assertThat(actual, is(equalTo(expected)));
    }

    @Test
    @DisplayName("stringFromFile throws an FileNotFoundUncheckedException when file does not exist")
    void stringFromFileThrowsAFileNotFoundUncheckedExceptionWhenFileDoesNotExist() {
        FileNotFoundUncheckedException exception = assertThrows(FileNotFoundUncheckedException.class,
                () -> IoUtils.stringFromFile(NON_EXISTING_RESOURCE));
        assertThat(exception.getMessage(), containsString(NON_EXISTING_RESOURCE.toString()));
        assertThat(exception.getMessage(),
                containsString(FileNotFoundUncheckedException.FILE_NOT_FOUND_MESSAGE));
    }

    @Test
    @DisplayName("streamToString throws UncheckedIOException when there is an error reading from a stream")
    void streamToStringThrowsUncheckedIoExceptionWhenThereIsAnErrorReadingFromAStream()
            throws IOException {
        InputStream errorWhenClosingStream = streamThrowingIoException();
        Exception e = assertThrows(UncheckedIOException.class,
                () -> IoUtils.streamToString(errorWhenClosingStream));
        assertThat(e.getMessage(), is(not(emptyOrNullString())));
    }

    @Test
    @DisplayName("streamToString throws UncheckedIOException when there is an error closing the stream")
    void streamToStringThrowsUncheckedIoExceptionWhenThereIsAnErrorClosingTheStream()
            throws IOException {
        InputStream unavailableStream = streamThrowingIoException();
        Exception e = assertThrows(UncheckedIOException.class,
                () -> IoUtils.streamToString(unavailableStream));
        assertThat(e.getMessage(), is(not(emptyOrNullString())));
    }

    @Test
    @DisplayName("linesFromResource returns the resource contents one line per list element")
    void linesFromResourceReturnsTheContentsOfResourceOnelinePerListElement() {
        List<String> lines = IoUtils.linesfromResource(RESOURCE);
        int linesOfResourceFile = 3;
        assertThat(lines.size(), is(equalTo(linesOfResourceFile)));
    }

    @Test
    void stringToStreamReturnsAnInputStreamWithTheStringContents() {
        String sample = createSampleString();
        InputStream stream = IoUtils.stringToStream(sample);
        BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
        String actualString = reader.lines().collect(Collectors.joining(System.lineSeparator()));
        assertThat(actualString, is(equalTo(sample)));
    }

    @Test
    void inputStreamToBytesReturnsBytesOfInputStreamWithoutLossOfInformation()
            throws IOException {
        String sample = createSampleString();
        InputStream stream = IoUtils.stringToStream(sample);
        byte[] bytes = IoUtils.inputStreamToBytes(stream);
        String regeneratedSample = IoUtils.streamToString(new ByteArrayInputStream(bytes));
        assertThat(regeneratedSample, is(equalTo(sample)));
    }


    @Test
    void shouldReturnFilePathOfResourceFile() {
        var expectedContent = IoUtils.stringFromResources(RESOURCE);
        var file = IoUtils.fileFromResource(RESOURCE);
        var actualContent = IoUtils.stringFromFile(file.toPath());
        assertThat(actualContent, is(equalTo(expectedContent)));
    }

    @Test
    void shouldThrowFileNotFoundExceptionWhenFileDoesNotExist() {
     assertThrows(ResourceNotFoundException.class, () -> IoUtils.fileFromResource(NON_EXISTING_RESOURCE));
    }

    private String createSampleString() {
        return "First line"
                + System.lineSeparator()
                + "Second line";
    }

    private InputStream streamThrowingIoException() throws IOException {
        InputStream stream = InputStream.nullInputStream();
        stream.close();
        return stream;
    }
}
