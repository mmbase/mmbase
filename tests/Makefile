
#export JAVA_HOME=/home/nightly/jdk

#mvn=/home/nightly/maven2/bin/mvn
mvn=mvn
ant=ant -quiet -listener org.apache.tools.ant.listener.Log4jListener -lib target/dependency:. -Dnoconnection=true
#ant=ant -lib lib:. -Dnoconnection=true

.PHONY: all bridge rmmci dbsq  datatypes clustering convert typerel richtext cloudcontext transactions releasestrategy


help:     ## Show this help.
	sed -n 's/^##//p' $(MAKEFILE_LIST)
	@grep -E '^[/%a-zA-Z0-9._-]+:.*?## .*$$' $(MAKEFILE_LIST) | awk 'BEGIN {FS = ":.*?## "}; {printf "\033[36m%-30s\033[0m %s\n", $$1, $$2}'

all: compile dependencies   ## run all tests
	$(ant) run.all | tee work/all.log

#ok
bridge: dependencies   ## bridge tests
	$(ant) run.bridge | tee work/bridge.log


#1 or 2 failures
rmmci:  dependencies  ## rmmci tests
	$(ant) run.bridge.rmmci | tee work/rmmci.log

#ok
richtext: dependencies ## richtext tests
	$(ant) run.richtext | tee work/richtext.log

#ok
dbsq: dependencies ## dbsq tests
	$(ant) run.dbsq | tee work/dbsq.log


# Sigh, don't know how to this via rmmci.
rmmciutil: dependencies
	$(ant) run.rmmciutil | tee work/rmmciutil.log

#ok
datatypes: dependencies ## datatypes tests
	$(ant) run.datatypes | tee work/datatypes.log

#
clustering:  dependencies ## clustering tests
	$(ant) run.clustering | tee work/clustering.log

#ok
cloudcontext: dependencies  ## cloudcontext tests
	$(ant) run.cloudcontext | tee work/cloudcontext.log

#ok
convert: dependencies  ## convert tests
	$(ant) run.convertimage | tee work/convertimage.log

#ok
typerel: dependencies  ## typerel tests
	$(ant) run.typerel | tee work/typerel.log

#ok
releasestrategy: dependencies ## releasestrategy tests
	$(ant) run.releasestrategy | tee work/releastrategy.log

# not yet checked in
transactions:  ## transactions tests
	$(ant) run.transactions | tee work/transactions.log


dependencies:
	mkdir -p work
	$(mvn) validate

.PHONY: compile
compile:
	$(mvn) compile jar:jar
