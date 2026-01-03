# This file just is for me to remember how to start a standalone hsqldb

# Actually the complete recipe is:

# - Start 3 terminals
# - In terminal 1 type: make
# - In terminal 2 type: mvn jetty:run -DjdbcUrl=jdbc:hsqldb:hsql://localhost/mmbase
# - In terminal 3 type: mvn jetty:run -DjdbcUrl=jdbc:hsqldb:hsql://localhost/mmbase -Djetty.port=8081 -Dmmbaseroot.machinename=webapp2

# And then you have the 'example webapp' running in a mini mmbase cluster.

REGISTRY?=ghcr.io/
NAME?=mmbase/example-webapp
TAG=latest

help:     ## Show this help.
	@grep -h -E '^[/%a-zA-Z0-9._-]+:.*?## .*$$' $(MAKEFILE_LIST) | awk 'BEGIN {FS = ":.*?## "}; {printf "\033[36m%-30s\033[0m %s\n", $$1, $$2}'


data/mmbase.lck: ## start a hsldb server
	java org.hsqldb.Server -database.0 data/mmbase -dbname.0 mmbase


docker_push:  ## build and push example webapp docker image (multiplatform build) You can set REGISTRY="" to push to hub.docker
		docker buildx build --platform=linux/amd64,linux/arm64 $(ARGS) -t $(REGISTRY)$(NAME):$(TAG) . --push

docker:  ## build docker image, no push, current platform. Handy for local testing
		docker build -t $(REGISTRY)$(NAME):$(TAG) .

docker_run:   ## run the docker image
		mkdir -p work data
		docker run -it --user $(shell id -u):$(shell id -g) -p 8080:8080 -p 8000:8000 -v $(PWD)/data:/data -v $(PWD)/work:/work $(REGISTRY)$(NAME):$(TAG)


docker_explore:   ## run the docker image
		mkdir -p work data
		docker run -it --user $(shell id -u):$(shell id -g) -p 8080:8080 -p 8000:8000 -v $(PWD)/data:/data -v $(PWD)/work:/work --entrypoint /bin/bash $(REGISTRY)$(NAME):$(TAG)
