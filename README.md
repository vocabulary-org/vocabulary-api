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
$ mvn clean verify
```

## starting the application locally
Replace client-id and client-secret with your own.
### from cli with maven

```shell
$ mvn spring-boot:run 
```

### with intellij
![img.png](docs/images/intellij.png)
### Accessing Swagger
[Swagger-localhost](http://localhost:9090/swagger-ui/index.html#/)


### Get the access token

```shell
TOKEN=$(curl -X POST \
http://localhost:18080/realms/your-rents/protocol/openid-connect/token \
-H 'Content-Type: application/x-www-form-urlencoded' \
-d username=user \
-d password=user \
-d grant_type=password \
-d client_id=your-rents-api \
-d client_secret=ZEeQ3Zmhnm3NX6QPGlEpPOLB2OavM3GZ \
| jq -r .access_token)
```


## Credits
Developed with the [YourRents Geodata](https://github.com/your-rents) technology stack.
