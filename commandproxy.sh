#! /bin/bash
workingDir=`dirname "$0"`
java -classpath "$workingDir/jars/commandproxy-cli.jar" commandproxy.cli.Main $@

exit $?