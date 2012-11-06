#!/bin/bash
# Run the browser
#set -v
YAWNI_VERSION=2.0.0-SNAPSHOT
SLF4J_VERSION=1.6.6
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
#wnhome=$WNHOME
#unset WNHOME
echo "WNHOME: $WNHOME wnhome: $wnhome";
MVN_REPO=$HOME/.m2/repository/
# classpath when slf4j is not "shaded" into jar
#CLASSPATH=$MVN_REPO/org/yawni/yawni-wordnet-api/$YAWNI_VERSION/yawni-wordnet-api-$YAWNI_VERSION.jar:$MVN_REPO/org/slf4j/slf4j-api/1.5.8/slf4j-api-1.5.8.jar:$MVN_REPO/org/slf4j/slf4j-jdk14/1.5.8/slf4j-jdk14-1.5.8.jar
# simple single jar classpath without data jar
#CLASSPATH=$MVN_REPO/org/yawni/yawni-wordnet-browser/$YAWNI_VERSION/yawni-wordnet-browser-$YAWNI_VERSION.jar:$MVN_REPO/org/slf4j/slf4j-api/$SLF4J_VERSION/slf4j-api-$SLF4J_VERSION.jar:$MVN_REPO/org/slf4j/slf4j-log4j12/$SLF4J_VERSION/slf4j-log4j12-$SLF4J_VERSION.jar:$MVN_REPO/log4j/log4j/1.2.14/log4j-1.2.14.jar
# rigged up to test missing jar failure mode
# note there's mvn dependency:build-classpath
CLASSPATH=/Users/nezda/cvs/yawni.git/browser/target/yawni-wordnet-browser-2.0.0-SNAPSHOT.jar
# append data jar to classpath
#CLASSPATH=$CLASSPATH:$MVN_REPO/org/yawni/wordnet/yawni-data/$VERSION/yawni-wordnet-data-$VERSION.jar
# outter parens make this a Bash array of space separated elements
#JVM_ARGS+=()
JVM_ARGS=(-Xdock:name="Yawni Browser")
#JVM_ARGS+=(-Xdock:icon=jar://org/yawni/wordnet/browser/yawni_115x128_icon.png)
#JVM_ARGS+=(-Xdock:icon=./browser/src/main/resources/org/yawni/wordnet/browser/yawni_115x128_icon.png)
#JVM_ARGS+=(-Dlog4j.configuration=org/yawni/wordnet/log4j.properties)
JVM_ARGS+=(-Dlog4j.configuration=org/yawni/wordnet/browser/log4j.properties)
JVM_ARGS+=(-Dfile.encoding=UTF-8)
JVM_ARGS+=(-d32)
# no longer works
JVM_ARGS+=(-Dapple.awt.brushMetalLook="true")
#JVM_ARGS+=(-XX:+AggressiveOpts)
#JVM_ARGS+=(-XX:+UseFastAccessorMethods)
# need lots RAM for huge searches (e.g., all hyponyms of "person")
#JVM_ARGS+=(-Xmx96m)
#JVM_ARGS+=(-Dfile.encoding=US-ASCII)
#JVM_ARGS+=(-Dlog4j.debug)
# expand all array elements of JVM_ARGS Bash array
#XXX incomplete CLASSPATH! $java "${JVM_ARGS[@]}" $ASSERT_ENABLE -DWNHOME="$wnhome" -cp "$CLASSPATH" org.yawni.wordnet.browser.Browser "$@"
# !! command below must be run from browser sub-project !!
mvn -P useLog4j exec:java -Dlog4j.debug -Dlog4j.configuration=org/yawni/wordnet/log4j.properties -Dexec.mainClass="org.yawni.wordnet.browser.Browser" -Dexec.classpathScope="test"

# use the maven-exec-plugin (from api module):
# mvn -P useWNHOMEDataForTest -DWNHOME=/Users/nezda/code/c/wordnets/WordNet-2.0 exec:java -Dexec.mainClass="org.yawni.util.cache.BloomFilters" -Dexec.classpathScope="test"
