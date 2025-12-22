
IMAGE=ghcr.io/mmbase/commandserver:latest

.PHONY:
run:
	java  -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:8001 -jar `ls target/mmbase-commandserver-*` --maxDuration 1 --threads 1 1679

.PHONY:
test:
	java -cp `ls target/mmbase-commandserver-*` org.mmbase.util.MagickClient Makefile convert -list format
	#java -cp `ls target/mmbase-commandserver-*` org.mmbase.util.MagickClient Makefile sleep 2


docker:
	docker build . -t $(IMAGE)

docker.run: docker
	mkdir -p work data
	docker run -it -p 1679:1679 -p 8001:8001 -v $(PWD)/work:/work -v $(PWD)/data:/data $(IMAGE)

explore:
	docker run -it --entrypoint bash -v $(PWD)/work:/work -v $(PWD)/data:/data $(IMAGE)

docker.push:
	docker buildx build --platform linux/amd64,linux/arm64  -t $(IMAGE) --push .
