#!/bin/sh
# Runs OpenRewrite's call graph and class hierarchy on one Maven module, search only (dryRun: no file is changed).
# Usage: run-rewrite.sh <repo folder> <module>   (copy rewrite.yml to the repo's root first)
# Maven sends proxy credentials only after the proxy asks for them; behind a proxy that wants them up front, run
# this where the proxy isn't needed.
set -e
cd "$1"
time sh mvnw -q -pl "$2" -am \
  org.openrewrite.maven:rewrite-maven-plugin:6.46.1:dryRun \
  -Drewrite.recipeArtifactCoordinates=org.openrewrite.recipe:rewrite-all:1.28.1 \
  -Drewrite.activeRecipes=com.example.CallGraphComparison \
  -Drewrite.exportDatatables=true
ls -d "$2"/target/rewrite/datatables/*
