#!/bin/bash
# Run the browser
VERSION=1.5.6
LOG_LEVEL=SEVERE
java -Dedu.brandeis.cs.steele.wn.Morphy.level=$LOG_LEVEL -Dfile.encoding=US-ASCII -ea -esa -DWNHOME="$WNHOME" -cp ./lib/jwordnet-$VERSION.jar:./lib/jwordnet-browser-$VERSION.jar:. browser.Browser "$@"
