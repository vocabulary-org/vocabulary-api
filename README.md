# Vocabulary API
Vocabulary Builder: Embrace the essence of traditional paper vocabularies in a digital format.    

Effortlessly organize, learn, and reinforce new words with the added power of interactive flashcards.

[![Build](https://github.com/egch/vocabulary/actions/workflows/maven.yml/badge.svg)](https://github.com/egch/vocabulary/actions/workflows/maven.yml)

## Prerequisites

- Java 21+
- A Docker environment supported by Testcontainers: <https://www.testcontainers.org/supported_docker_environment/> (a recent local Docker installation is enough)

## Getting Started

Clone the repository and run the following command to build the project:

```shell
./mvnw clean verify
```

## Running the service in development

Run the following command:

```shell
./mvnw spring-boot:run
```
### Accessing Swagger
[Swagger-localhost](http://localhost:8080/swagger-ui/index.html#/)

## Credits
Developed with the [YourRents Geodata](https://github.com/your-rents) technology stack.
