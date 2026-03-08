# Nginx SSL Migration to Cloudflare Origin Certificate (Hetzner + Docker)

## Context

The Vocabulary application runs on a Hetzner server with:

- Docker
- nginx container: `vocabulary-nginx-ssl`
- Cloudflare in front of the server
- Subdomains:
    - `api.myvocabulary.net`
    - `auth.myvocabulary.net`

The original nginx certificates expired. Since Cloudflare already terminates TLS for users, the origin certificates were replaced with a Cloudflare Origin Certificate.

## Final Architecture

- User -> HTTPS -> Cloudflare public certificate
- Cloudflare -> HTTPS -> nginx with Cloudflare Origin Certificate
- nginx -> Docker services:
    - vocabulary-api
    - keycloak

Benefits:

- End-to-end TLS encryption
- No browser SSL warnings
- No frequent renewal on the origin
- Compatible with Cloudflare `Full (strict)`

## Step 1 - Generate Cloudflare Origin Certificate

In Cloudflare dashboard:

`SSL/TLS -> Origin Server -> Create Certificate`

Configuration used:

- Hostnames:
    - `*.myvocabulary.net`
    - `myvocabulary.net`
- Validity:
    - `15 years`

Cloudflare generated:

- Origin Certificate
- Private Key

## Step 2 - Install Certificate on Hetzner

The same wildcard certificate was installed for both:

- `api.myvocabulary.net`
- `auth.myvocabulary.net`

Both subdomains share a single certificate directory:

- `nginx-conf/ssl/cloudflare/a.pem` — Cloudflare Origin Certificate
- `nginx-conf/ssl/cloudflare/pk.pem` — Private Key

## Step 3 - Mount Certificates in Docker

In `docker-compose-nginx-ssl.yaml`, the nginx container mounts the shared cloudflare folder for both subdomains:

- `./nginx-conf/ssl/cloudflare:/etc/nginx/ssl/auth:ro`
- `./nginx-conf/ssl/cloudflare:/etc/nginx/ssl/api:ro`

This allows nginx inside Docker to read the certificates for both `auth` and `api`.

## Step 4 - Verify Certificate Inside the Running Container

Commands used:

```shell
docker exec -it vocabulary-nginx-ssl openssl x509 -enddate -noout -in /etc/nginx/ssl/auth/a.pem
docker exec -it vocabulary-nginx-ssl openssl x509 -enddate -noout -in /etc/nginx/ssl/api/a.pem
```

Result for both:

`notAfter=Mar  4 21:47:00 2041 GMT`

This confirmed that the new certificate was correctly mounted inside the running container.

## Step 5 - Restart nginx

After replacing the certificates, nginx was restarted:

```shell
docker restart vocabulary-nginx-ssl
```

Optional validation:

```shell
docker exec -it vocabulary-nginx-ssl nginx -t
```

## Step 6 - Verify nginx Is Actually Serving the New Certificate

Origin test run on the Hetzner server:

```shell
openssl s_client -connect localhost:443 -servername api.myvocabulary.net 2>/dev/null | openssl x509 -noout -dates -subject -issuer
```

Observed result:

- `notBefore=Mar  8 21:47:00 2026 GMT`
- `notAfter=Mar  4 21:47:00 2041 GMT`
- subject = `CloudFlare Origin Certificate`
- issuer = `CloudFlare Origin SSL Certificate Authority`

This proved that nginx was not only seeing the new files, but was actively serving the new Cloudflare Origin Certificate.

The same kind of test can also be executed for auth:

```shell
openssl s_client -connect localhost:443 -servername auth.myvocabulary.net 2>/dev/null | openssl x509 -noout -dates -subject -issuer
```

## Step 7 - Enable Strict SSL Mode in Cloudflare

In Cloudflare dashboard:

`SSL/TLS -> Overview`

Set encryption mode to:

`Full (strict)`

This is important because:

- `Full` allows Cloudflare to connect to the origin even if the origin certificate is expired or invalid
- `Full (strict)` forces Cloudflare to validate the origin certificate

## Step 8 - Final External Test

External checks from a client machine:

```shell
curl -v https://api.myvocabulary.net
curl -v https://auth.myvocabulary.net
```

Expected behavior:

- TLS connection succeeds
- client still sees Cloudflare public certificate
- API may return `401` if unauthenticated
- auth may redirect to login

Important note:

Seeing Cloudflare's public certificate from the outside is normal. The origin certificate is only used on the connection between Cloudflare and nginx.

## Key Finding During Troubleshooting

The old certificates on nginx were expired, but the application still worked because users were seeing Cloudflare's public certificate, not the origin certificate on Hetzner.

That meant:

- browser -> Cloudflare certificate was valid
- Cloudflare -> origin certificate could still be expired if Cloudflare was in `Full` mode

After installing the Cloudflare Origin Certificate and switching to `Full (strict)`, the connection became properly validated end-to-end.

## Final Result

The infrastructure now uses:

- Cloudflare public TLS for users
- Cloudflare Origin Certificate on nginx
- strict validation between Cloudflare and Hetzner
- certificate validity until 2041

This removes the previous expiry issue on the origin and avoids frequent certificate renewal on the Hetzner server.
