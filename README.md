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
**Description:**
The `testingutils` module contains: 
1. A set of simple fake clients for use in testing.
   Most clients implement only a limited subset of API methods, however, it is simple to extend the clients to add missing functionality.
   All implemented functionality is described in the associated test class
2. An HttpClient that works with Wiremock and the Https protocol.
3. A Hamcrest matcher that checks recursively whether a class has empty fields.
4. A random data generator for different types of data.







