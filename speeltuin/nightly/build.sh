#!/bin/bash

source env.sh

export MAILADDRESS="developers@lists.mmbase.org"
#export MAILADDRESS="michiel.meeuwissen@gmail.com"
export BUILD_MAILADDRESS=$MAILADDRESS

source version.sh

# UNSTABLE branch

if [ 1 == 1 ] ; then
    cd ${BUILD_HOME}/nightly-build/cvs/mmbase

    echo cwd: `pwd`, build dir: ${builddir}

    echo Cleaning
    echo >  ${builddir}/messages.log 2> ${builddir}/errors.log
    # removes all 'target' directories
    # the same as ${MAVEN} multiproject:clean >>  ${builddir}/messages.log 2>> ${builddir}/errors.log
    find . -type d -name target -print | xargs rm -rf  >> ${builddir}/messages.log

    pwd
    echo "CVS" | tee -a ${builddir}/messages.log
    echo ${CVS} -q  update -d -P  ${cvsversionoption} ${cvsversion} ${revision} | tee -a ${builddir}/messages.log

    # I realy don't get the deal with the quotes around ${cvsversion}.
    # undoubtly to do with some bash detail. If $cvsversion contains no space, then it seems essential that these quotes are _not_ there
    # otherwise it seems essential _that_ they are. It's maddening.
    ${CVS} -q update -d -P  ${cvsversionoption} "${cvsversion}"  ${revision} | tee -a ${builddir}/messages.log


    echo Starting nightly build | tee -a ${builddir}/messages.log
    echo all:install
    ((${MAVEN} all:install | tee -a ${builddir}/messages.log) 3>&1 1>&2 2>&3 | tee -a ${builddir}/errors.log) 3>&1 1>&2 2>&3

    echo ====================================================================== |  tee -a ${builddir}/messages.log
    echo creating RECENTCHANGES |  tee -a ${builddir}/messages.log
    ${CVS} log -N -d"last week<now" 2> /dev/null | ${FILTER} > ${builddir}/RECENTCHANGES.txt
fi

if [ 1 == 1 ] ; then
    cd maven-site
    echo Creating site `pwd`. | tee -a ${builddir}/messages.log
    ((${MAVEN} multiproject:site | tee -a ${builddir}/messages.log) 3>&1 1>&2 2>&3 | tee -a ${builddir}/errors.log) 3>&1 1>&2 2>&3
fi


$HOME/bin/copy-artifacts.sh


if [ 1 == 1 ] ; then
    echo Now executing tests. Results in ${builder}/test-results. | tee -a ${builddir}/messages.log
    cd ${BUILD_HOME}/nightly-build/cvs/mmbase/tests
    ${antcommand} -quiet -listener org.apache.tools.ant.listener.Log4jListener -lib lib:.  run.all  2>&1 | tee  ${builddir}/tests-results.log
fi


echo Creating symlink for latest build | tee -a ${builddir}/messages.log
rm /home/nightly/builds/latest
cd /home/nightly/builds
ln -s ${builddir} latest

 # Using one thread for all mail about failures
parent="<20080906100002.GA1861@james.mmbase.org>";
mutthdr="my_hdr In-Reply-To: $parent";


showtests=1
if [ 1 == 1 ] ; then
    if [ -f latest/messages.log ] ; then
        if (( `cat latest/messages.log  | grep -P '\[javac\]\s+[0-9]+\s+errors' | wc -l` > 0 )) ; then
	    echo Build failed, sending mail to ${BUILD_MAILADDRESS} | tee -a ${builddir}/messages.log
	    echo -e "Build on ${version} failed:\n\n" | \
		cat latest/messages.log latest/errors.log | grep -B 10 "\[javac\]" | \
		mutt -e $mutthdr -s "Build failed ${version}" ${BUILD_MAILADDRESS}
	    showtests=0;
        fi
    else
        echo Build failed, sending mail to ${BUILD_MAILADDRESS} | tee -a ${builddir}/messages.log
        echo -e "No build created on ${version}\n\n" | \
            tail -q -n 20 - latest/errors.log | \
            mutt -e $mutthdr -s "Build failed ${version}" ${BUILD_MAILADDRESS}
	showtests=0;
    fi
fi



if [ 1 == $showtests ] ; then
    cd /home/nightly/builds
    echo Test results | tee -a ${builddir}/messages.log

    if [ -f latest/tests-results.log ] ; then
	if (( `cat latest/tests-results.log  | grep 'FAILURES' | wc -l` > 0 )) ; then
	    echo Failures, sending mail to ${MAILADDRESS}  | tee -a ${builddir}/messages.log
	    (echo "Failures on build ${version}" ; echo "See also http://www.mmbase.org/download/builds/latest/tests-results.log" ; \
                cat latest/tests-results.log  | grep -P  '(^Tests run:|^[0-9]+\)|^\tat org\.mmbase|FAILURES|========================|OK)' ) | \
		mutt -e $mutthdr -s "Test cases failures" ${MAILADDRESS}
	fi
    fi
fi

