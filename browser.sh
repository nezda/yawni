#!/bin/bash
# Run the browser
VERSION=1.5.6-dev
#LEVEL_ARG=-Dedu.brandeis.cs.steele.wn.Morphy.level=SEVERE
#ASSERT_ENABLE="-ea -esa"
# system assertions cause tons of logging on OS X ([AWT-\d+] ...)
ASSERT_ENABLE="-ea"
java -Xdock:name="JWordNet Browser" $ARGS $LEVEL_ARG $ARGS -Dfile.encoding=US-ASCII $ASSERT_ENABLE -DWNHOME="$WNHOME" -cp ./build/jwordnet-core-$VERSION.jar edu.brandeis.cs.steele.wn.browser.Browser "$@"
