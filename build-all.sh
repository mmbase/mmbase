#!/usr/bin/env bash
# exit when any command fails
set -e
#export MAVEN_OPTS="-Dorg.slf4j.simpleLogger.log.org.apache.maven.cli.transfer.Slf4jMavenTransferListener=warn"
BATCH_MODE="${BATCH_MODE:-true}"

MAVEN_ARGS="--no-transfer-progress -Pdeploy"
if [ $BATCH_MODE = 'true' ] ; then
  echo batch mode
  MAVEN_ARGS="$MAVEN_ARGS -B"
else
  echo interactive mode
fi

echo central username: ${CENTRAL_USERNAME}

DIR="$( cd -- "$( dirname -- "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )"
MVN="mvn"
#echo $OSSRH_PASSWORD | base64

#MVN="mvn -ntp -fae -Duser.home=$HOME -Dmaven.repo.local=/Users/michiel/.m2/repository_clean"
TARGET=deploy
if [ ! -z "$1" ] ; then
    TARGET=$1
fi

#cd $DIR/applications/streams && $MVN -P'deploy,!development' clean deploy
#exit
for d in  . maven-base maven maven/maven-mmbase-plugin maven-base/applications applications   ; do
    echo "========== Running with -N clean $TARGET in $d"
    (cd $DIR/$d &&  $MVN -N clean "$TARGET")
done

echo "============= Now running the rest $(pwd) $DIR"
(cd $DIR && $MVN -P'deploy,!development,default' clean "$TARGET")
