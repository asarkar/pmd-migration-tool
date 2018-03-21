#!/bin/bash

for f in $PROJECT_DIR/src/test/resources/*.xml; do
  $PROJECT_DIR/build/install/pmd-migration-tool/bin/pmd-migration-tool $f
done