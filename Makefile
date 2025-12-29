ARGS:=install


help:     ## Show this help.
	@echo "These commands can be used to build MMBase in docker (but without act)"
	@echo "'make mvn'"
	@sed -n 's/^##//p' $(MAKEFILE_LIST)
	@grep -h -E '^[/%a-zA-Z0-9._-]+:.*?## .*$$' $(MAKEFILE_LIST) | awk 'BEGIN {FS = ":.*?## "}; {printf "\033[36m%-30s\033[0m %s\n", $$1, $$2}'

mvn:  ## run mvn build (actually build-all.sh) via docker
	docker run -v $(shell pwd):/mmbase -v ${HOME}/.m2:/root/.m2 -w /mmbase  -it --entrypoint bash  -e PROFILES=default ghcr.io/mmbase/build:latest-jdk8 build-all.sh $(ARGS)

bash: ## gives a shell on build build, which current directory mounted
	docker run -v $(shell pwd):/mmbase -v ${HOME}/.m2:/root/.m2 -w /mmbase  -it --entrypoint bash ghcr.io/mmbase/build:latest-jdk8
