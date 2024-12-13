#!/bin/bash

SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"
cd $SCRIPT_DIR/..

rm -rf files
gradle run
bin/epubcheck docs/download/iliaden.epub && git commit -a -m updated && git push


