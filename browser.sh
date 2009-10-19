#!/bin/bash
# Run the browser
#set -v
VERSION=2.0.0-SNAPSHOT
ASSERT_ENABLE="-ea"
# system assertions (-esa) cause tons of logging on OS X ([AWT-\d+] ...)
#ASSERT_ENABLE="-ea -esa"
#ASSERT_ENABLE="-ea -Xrunhprof:cpu=samples,depth=20,interval=20"
# the OS X brushMetal* stuff only seems to work if provided as a system property at the command line
#ASSERT_ENABLE="-Dswing.aatext=true -Dawt.useSystemAAFontSettings=on -Dapple.awt.textantialiasing=on -Dapple.laf.useScreenMenuBar=true -Dapple.awt.brushMetalLook=true -Dapple.awt.brushMetalRounded=true -Dapple.awt.showGrowBox=false"
#java='/System/Library/Frameworks/JavaVM.framework/Versions/1.6/Home/bin/java -ea'
java='java'
# read data from jar (unset any local WNHOME system property)
#unset WNHOME
wnhome=$WNHOME
unset WNHOME
#CLASSPATH=./core/target/jwordnet-core-$VERSION.jar:./data/target/jwordnet-data-$VERSION.jar
echo "WNHOME: $WNHOME wnhome: $wnhome";
MVN_REPO=$HOME/.m2/repository/
# classpath when slf4j is not "shaded" into jar
#CLASSPATH=$MVN_REPO/org/yawni/wn/yawni-core/$VERSION/yawni-core-$VERSION.jar:$MVN_REPO/org/slf4j/slf4j-api/1.5.8/slf4j-api-1.5.8.jar:$MVN_REPO/org/slf4j/slf4j-jdk14/1.5.8/slf4j-jdk14-1.5.8.jar
# simple single jar classpath
CLASSPATH=$MVN_REPO/org/yawni/wn/yawni-browser/$VERSION/yawni-browser-$VERSION.jar
#CLASSPATH=$CLASSPATH:$MVN_REPO/org/yawni/wn/yawni-data/$VERSION/yawni-data-$VERSION.jar
# outter parens make this a Bash array of space separated elements
JVM_ARGS=(-Xdock:name="Yawni Browser")
#JVM_ARGS+=(-Dlog4j.configuration=org/yawni/wn/log4j.properties)
JVM_ARGS+=(-Dfile.encoding=UTF-8)
JVM_ARGS+=(-d32)
# need lots RAM for huge searches (e.g., all hyponyms of "person")
#JVM_ARGS+=(-Xmx96m)
#JVM_ARGS+=(-Dfile.encoding=US-ASCII)
#JVM_ARGS+=(-Dlog4j.debug)
# expand all array elements of JVM_ARGS Bash array
$java "${JVM_ARGS[@]}" $ASSERT_ENABLE -DWNHOME="$wnhome" -cp "$CLASSPATH" org.yawni.wn.browser.Browser "$@"
