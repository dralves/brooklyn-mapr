#!/bin/bash

JAVA=`which java`

if [ ! -z "$JAVA_HOME" ] ; then 
    JAVA=$JAVA_HOME/bin/java
else
    JAVA=`which java`
fi

if [ ! -x "$JAVA" ] ; then
  echo Cannot find java. Set JAVA_HOME or add java to the path.
  exit 1
fi

if [ ! `ls brooklyn-mapr-*.jar 2> /dev/null` ] ; then
  echo This command must be run from the directory where it is installed.
  exit 1
fi

$JAVA -Xms256m -Xmx1024m -XX:MaxPermSize=1024m -classpath "conf/:patch/*:*:lib/*" io.cloudsoft.mapr.MyM3App "$@"
