#!/bin/bash
# Run the browser
#set -v
VERSION=2.0.0-SNAPSHOT
#LEVEL_ARG=-Dedu.brandeis.cs.steele.wn.Morphy.level=SEVERE
# system assertions cause tons of logging on OS X ([AWT-\d+] ...)
#ASSERT_ENABLE="-ea"
#ASSERT_ENABLE="-ea -esa"
#ASSERT_ENABLE="-ea -Xrunhprof:cpu=samples,depth=20,interval=20"
# the OS X brushMetal* stuff only seems to work if provided as a system property at the command line
ASSERT_ENABLE=" -ea -Dswing.aatext=true -Dawt.useSystemAAFontSettings=on -Dapple.awt.textantialiasing=on -Dapple.laf.useScreenMenuBar=true -Dapple.awt.brushMetalLook=true -Dapple.awt.brushMetalRounded=true -Dapple.awt.showGrowBox=false"
#java='/System/Library/Frameworks/JavaVM.framework/Versions/1.6/Home/bin/java -ea'
java='java'
# read data from jar
#unset WNHOME
wnhome=$WNHOME
unset WNHOME
#CLASSPATH=./core/target/jwordnet-core-$VERSION.jar:./data/target/jwordnet-data-$VERSION.jar
echo "WNHOME: $WNHOME wnhome: $wnhome";
MVN_REPO=$HOME/.m2/repository/
CLASSPATH=$MVN_REPO/org/yawni/wn/yawni-core/$VERSION/yawni-core-$VERSION.jar:$MVN_REPO/org/slf4j/slf4j-api/1.5.6/slf4j-api-1.5.6.jar:$MVN_REPO/org/slf4j/slf4j-jdk14/1.5.6/slf4j-jdk14-1.5.6.jar
#CLASSPATH=$CLASSPATH:$MVN_REPO/org/yawni/wn/yawni-data/$VERSION/yawni-data-$VERSION.jar
JVM_ARGS="-Xdock:name=\"Yawni Browser\" -Dfile.encoding=US-ASCII"
$java -Xdock:name="Yawni Browser" -Dfile.encoding=US-ASCII $ASSERT_ENABLE -DWNHOME="$wnhome" -cp "$CLASSPATH" org.yawni.wn.browser.Browser "$@"
#$java -Xdock:name="Yawni Browser" -Dfile.encoding=US-ASCII $ASSERT_ENABLE -cp "$CLASSPATH" org.yawni.wn.browser.Browser "$@"
