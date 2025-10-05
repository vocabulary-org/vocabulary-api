# Development Notes
## Docker - Export the realm
```shell
docker exec -it vocabulary-api-keycloak-1 bash
##within the docker container 
mkdir opt/keycloak/data/import/
  
/opt/keycloak/bin/kc.sh export --file /opt/keycloak/data/import/vocabulary-realm.json --users same_file --realm  vocabulary --verbose \
  --db=postgres \
  --db-url=jdbc:postgresql://postgres_keycloak:5432/keycloak-vocabulary-db \
  --db-username=keycloak \
  --db-password=keycloak
exit
  
##out of the docker container 
docker cp vocabulary-api-keycloak-1:/opt/keycloak/data/import/vocabulary-realm.json /Users/enrico/github/vocabulary-org/vocabulary-api/keycloak/vocabulary-realm.json

```

## Docker
### Build the image
Build and upload a docker image on github.

```shell
mvn spring-boot:build-image
docker login
docker tag vocabulary/vocabulary-api egch/vocabulary-api:latest
docker push egch/vocabulary-api:latest
```

### Nginx as a docker container
[docker-compose-nginx.yaml](../docker-compose-nginx.yaml)
```shell
 docker cp vocabulary-nginx:/etc/nginx/conf.d/default.conf ngnix/default.conf
 docker exec -it vocabulary-nginx bash
```


## Troubleshooting
### Keycloak logs
```shell
docker logs -f vocabulary-api-keycloak-1
```
### SSL issues

<img src="docs/images/KC-SSL.png" alt="HTTPS required" width="400">

```shell
enrico@Mac-mini-3 ~ % docker exec -it vocabulary-api-keycloak-1 bash
bash-5.1$ cd /opt/keycloak/bin/
bash-5.1$ ./kcadm.sh config credentials \
  --server http://127.0.0.1:8080 \
  --realm master \
  --user admin \
  --password Pa55w0rd
Logging into http://127.0.0.1:8080 as user admin of realm master

bash-5.1$ ./kcadm.sh update realms/vocabulary -s sslRequired=NONE

```
## Spin up just the keycloak
```shell
 docker compose -f docker-compose-vocabulary-api.yaml up keycloak 
```
