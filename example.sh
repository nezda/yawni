#!/bin/bash
# Run the browser
VERSION=1.5.6
LOG_LEVEL=SEVERE
java -Xmx192m -Dedu.brandeis.cs.steele.wn.Morphy.level=$LOG_LEVEL -ea -esa -DWNHOME="$WNHOME" -cp ./lib/jwordnet-$VERSION.jar:./lib/jwordnet-example-$VERSION.jar example.Example "$@"
