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


## Get the access token

```shell
TOKEN=$(curl -X POST \
  http://localhost:18081/realms/vocabulary/protocol/openid-connect/token \
  -H 'Content-Type: application/x-www-form-urlencoded' \
  -d username=enrico \
  -d password=enrico \
  -d grant_type=password \
  -d client_id=vocabulary-rest-api \
  | jq -r .access_token)
```
### copy to swagger
![Bearer](docs/images/swagger-token.png)

## Export the realm
```shell
docker exec -it vocabulary-api-keycloak-1 bash
##within the docker container 
mkdir opt/keycloak/data/import/
  
/opt/keycloak/bin/kc.sh export --file /opt/keycloak/data/import/vocabulary-realm.json --users same_file --realm  vocabulary --verbose \
  --db=postgres \
  --db-url=jdbc:postgresql://postgres_keycloak:5432/keycloak \
  --db-username=keycloak \
  --db-password=keycloak
exit
  
##out of the docker container 
cp vocabulary-api-keycloak-1:/opt/keycloak/data/import/vocabulary-realm.json /Users/enrico/github/vocabulary-org/vocabulary-api/keycloak/vocabulary-realm.json

```

## Credits
Developed with the [YourRents Geodata](https://github.com/your-rents) technology stack.
