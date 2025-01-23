# Tutorial Build RAG on AWS

This is a simple tutorial to create a Rag System on AWS.

Link to post on Medium: [Tutorial Build RAG on AWS](https://medium.com/@giuseppetrisciuoglio/building-a-rag-system-on-aws-with-langchain4j-and-chatgpt-77d934606ced)

## Architecture

The architecture of the system is composed of the following components:
- KnowledgeBaseHandler: Lambda function that receives a document and stores it in the knowledge base (Vector DB).
- AssistantHandler: Lambda function that receives a question and returns the anwser generate from LLMs with most similar document from the knowledge base.

## Requirements
- Java 21
- Maven 3.9.x
- AWS CLI
- SAM CLI
- Enable AI Model on AWS Bedrock or OpenAI API Key

## Steps
- Build the project:
```shell
mvn clean install
```
- Test the project:
```shell
mvn clean test
```
