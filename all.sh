#!/bin/bash

ant -f create_jar_script.xml
scp ./extract.jar ganeao@brutus.ethz.ch:/cluster/scratch_xl/public/ganeao/wikilinks_project/extract_thrift_from_file.jar
