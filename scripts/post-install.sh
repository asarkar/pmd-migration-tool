#!/bin/bash

projectDir=$1
for f in $projectDir/src/test/resources/*.xml; do
  printf "Migrating %s\n" "$f"
  $projectDir/build/install/pmd-migration-tool/bin/pmd-migration-tool "$f"
done