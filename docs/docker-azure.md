# Running on Azure
## Installing docker on Ubuntu

````shell
#!/bin/bash
set -e

# Update system
sudo apt-get update
sudo apt-get install -y ca-certificates curl gnupg lsb-release

# Add Docker’s official GPG key
sudo install -m 0755 -d /etc/apt/keyrings
curl -fsSL https://download.docker.com/linux/ubuntu/gpg | \
  sudo gpg --dearmor -o /etc/apt/keyrings/docker.gpg
sudo chmod a+r /etc/apt/keyrings/docker.gpg

# Add Docker repo
echo \
  "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.gpg] \
  https://download.docker.com/linux/ubuntu $(lsb_release -cs) stable" | \
  sudo tee /etc/apt/sources.list.d/docker.list > /dev/null

# Update again with Docker repo
sudo apt-get update

# Install Docker Engine + Compose v2 plugin
sudo apt-get install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin

# Enable Docker at startup
sudo systemctl enable docker
sudo systemctl start docker

````
### Checks
````shell
docker --version
docker compose version

````
You should see something like:
````shell
Docker version 27.0.3, build abc123
Docker Compose version v2.29.2
````
### Add your user to docker
````shell
sudo usermod -aG docker $USER
newgrp docker
````

## Running docker compose
````shell
sudo docker compose -f docker-compose-vocabulary-api.yaml up

````