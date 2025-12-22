#!/usr/bin/env bash
# exit when any command fails
set -e
java -version
#export MAVEN_OPTS="-Dorg.slf4j.simpleLogger.log.org.apache.maven.cli.transfer.Slf4jMavenTransferListener=warn"
BATCH_MODE="${BATCH_MODE:-false}"
PROFILES="${PROFILES:-default}"

#MVN="mvn -ntp -fae -Duser.home=$HOME -Dmaven.repo.local=/Users/michiel/.m2/repository_clean"
TARGET=deploy
if [ ! -z "$1" ] ; then
    TARGET=$1
fi

if [ -z "$PROFILES" ]; then
  echo "No PROFILES set, using default based on target=$TARGET"
  if [ "$TARGET" == "deploy" ] ; then
    PROFILE_ARG="deploy"
  else
    PROFILE_ARG="default"
  fi
else
  PROFILE_ARG="${PROFILES}"
fi

export MAVEN_ARGS="--no-transfer-progress -P${PROFILE_ARG}"
echo "MAVEN_ARGS=${MAVEN_ARGS}"

if [ $BATCH_MODE = 'true' ] ; then
  echo batch mode
  export MAVEN_ARGS="$MAVEN_ARGS -B"
else
  echo interactive mode
fi

echo central username: ${CENTRAL_USERNAME}

DIR="$( cd -- "$( dirname -- "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )"
MVN="mvn"
#echo $OSSRH_PASSWORD | base64



#cd $DIR/applications/streams && $MVN -P'deploy,!development' clean deploy
#exit
for d in  . maven-base maven maven/maven-mmbase-plugin maven-base/applications applications   ; do
    echo "========== Running with -N clean $TARGET in $d"
    (cd $DIR/$d &&  $MVN -N clean "$TARGET")
done

echo "============= Now running the rest $(pwd) $DIR"
(cd $DIR && $MVN -P'!development,${PROFILE_ARG}' clean "$TARGET")

