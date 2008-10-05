#!/bin/bash
# Run the browser
VERSION=2.0.0-SNAPSHOT
#LEVEL_ARG=-Dedu.brandeis.cs.steele.wn.Morphy.level=SEVERE
#ASSERT_ENABLE="-ea -esa"
# system assertions cause tons of logging on OS X ([AWT-\d+] ...)
#ASSERT_ENABLE="-ea -Xrunhprof:cpu=samples,depth=20,interval=20"
#ASSERT_ENABLE="-ea"
#-Dswing.aatext=true
# the OS X brushMetal* stuff only seems to work if provided as a system property at the command line
ASSERT_ENABLE=" -ea -Dswing.aatext=true -Dawt.useSystemAAFontSettings=on -Dapple.awt.textantialiasing=on -Dapple.laf.useScreenMenuBar=true -Dapple.awt.brushMetalLook=true -Dapple.awt.brushMetalRounded=true -Dapple.awt.showGrowBox=false"
#java='/System/Library/Frameworks/JavaVM.framework/Versions/1.6/Home/bin/java -ea'
java='java'
$java -Xdock:name="JWordNet Browser" $LEVEL_ARG $ARGS -Dfile.encoding=US-ASCII $ASSERT_ENABLE -DWNHOME="$WNHOME" -cp ./dict:./target/jwordnet-$VERSION.jar edu.brandeis.cs.steele.wn.browser.Browser "$@"
