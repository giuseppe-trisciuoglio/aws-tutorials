# Tutorial AWS Lambda with Spring Boot

This is a simple tutorial to create a AWS Lambda function with Spring Boot.

## Requirements
- Java 21
- Maven 3.9.x
- AWS CLI
- SAM CLI

## Steps
- Build the project:
```shell
mvn clean install
```
- Run the project:
```shell
mvn spring-boot:run
```
- Test the project:
```shell
mvn clean test
```
- Build the project with SAM:
```shell
sam build
```
- Deploy the project:
```shell
sam deploy --guided
```
- Clean & Delete the project:
```shell
sam delete
```