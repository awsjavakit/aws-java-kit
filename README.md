# AWS Java Toolkit

## A collection of small Java libraries for abstracting common operations on Amazon Web Services (AWS).

### Requirements: 
 1. Java 17 or newer
 2. AWS Java SDK version 2
 3. Jackson (https://github.com/FasterXML/jackson)

## Usage:
The library is available in Maven Central. 
Include it as a dependency in your project:  
Example:
```
    <dependency>
    <groupId>io.github.awsjavakit</groupId>
    <artifactId>misc</artifactId>
    <version>0.13.2</version>
    </dependency>
```   
**Important Note:**   
We are currently supporting only the specified versions of all the libraries that 
aws-java-kit depends on. Different versions in client code may produce build errors.


## Modules:
All functionality is described in the test classes. Below you can find a short summary 
of the implemented features.

### Testing Utils (testingutils):

#### Fake AWS clients
The fake clients implement the AWS interfaces (e.g. the FakeS3Client implements the S3Client interface),
but they do not implement all the methods of the interface. 

Most fake clients have been implemented under the assumption 
that the client code will not read and write at the same time from an AWS service. 
For example, a Lambda function will publish to an SQS queue using the `SqsClient` , 
but will not read from the SQS queue using the same client.
As a result, for most clients one can fetch the write requests that have been sent using the client, 
but one cannot use the read methods of that client to fetch the written values. 

Exceptions from this rule are the `FakeS3Client` and the `FakeSecretsManagerClient`.

##### Examples:

###### FakeSqsClient

```java

@Test
void shouldReturnSendRequestsThatWereSent() {
  FakeSqsClient client = new FakeSqsClient();
  SendMessageRequest writeRequest = validMessage();

  client.sendMessage(writeRequest);

  ReceiveMessageRequest receiveMessageRequest = createReceiveMessageRequest(writeRequest);
  assertThrows(UnsupportedOperationException.class, () -> client.receiveMessage(receiveMessageRequest));

  assertThat(client.getSendMessageRequests()).containsExactly(writeRequest);

}

```

In the above test we see that the `FakeSqsClient` 
has an own method (i.e., not inherited from the `SqsClient` interface)
that lists the  submitted `SendMessageRequest` but trying to receive the message 
throws an `UnsupportedOperationException`. The rationale behind this is that 
the same piece of code will rarely write and read messages from the same queue
and thus we are mostly interested in the write requests. 













