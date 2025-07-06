#!/usr/bin/env bash
# exit when any command fails
set -e
export MAVEN_OPTS="-Dorg.slf4j.simpleLogger.log.org.apache.maven.cli.transfer.Slf4jMavenTransferListener=warn"

DIR="$( cd -- "$( dirname -- "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )"
#MVN="mvn -Dcom.sun.net.ssl.checkRevocation=false -Dhttps.protocols=TLSv1.2 -Duser.home=$DIR"
#echo $OSSRH_PASSWORD | base64

MVN="mvn -B -ntp -fae -Duser.home=$HOME"
TARGET=deploy
if [ ! -z "$1" ] ; then
    TARGET=$1
fi

#cd $DIR/applications/streams && $MVN -P'deploy,!development' clean deploy
#exit
for d in  . maven-base maven maven/maven-mmbase-plugin maven-base/applications applications   ; do
    echo "========== Running with -N in $d"
    (cd $DIR/$d &&  $MVN -N clean "$TARGET")
done

echo "============= Now running the rest $(pwd) $DIR"
(cd $DIR && $MVN -P'deploy,!development,default' clean "$TARGET")
