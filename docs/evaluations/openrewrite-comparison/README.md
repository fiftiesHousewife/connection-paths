# Comparison with OpenRewrite's call graph

The evidence for [connection-paths is built, not adopted, after checking the existing tools](../../decisions/0005-connection-paths-is-built-not-adopted-after-checking-the-existing-tools.md),
in a form anyone can run again. connection-paths needs none of this; it is run by hand.

| File | What |
|---|---|
| [`rewrite.yml`](rewrite.yml) | A search-only OpenRewrite recipe: `org.openrewrite.FindCallGraph` (JDK calls included) and `org.openrewrite.java.search.FindClassHierarchy`, written out as data tables |
| [`run-rewrite.sh`](run-rewrite.sh) | Runs it on one Maven module through the repo's wrapper, as a dryRun: no file is changed |
| [`compare_rewrite.py`](compare_rewrite.py) | Walks OpenRewrite's calls with connection-paths' rules and prints the figures beside connection-paths' facts |

To run it on Openfire 4.6.0's `xmppserver` module:

1. Clone Openfire at its 4.6.0 release and copy `rewrite.yml` to its root.
2. `run-rewrite.sh <openfire folder> xmppserver` (about two minutes; Maven must reach its repositories).
3. Write connection-paths' facts for the same module with the build's classpath:
   `sh mvnw -q -pl xmppserver -am -DskipTests compile dependency:build-classpath -Dmdep.outputFile=build/classpath.txt`, then
   `java -jar connection-paths.jar --module xmppserver --classpath @xmppserver/build/classpath.txt --out facts.json`.
4. `python3 compare_rewrite.py xmppserver/target/rewrite/datatables/<run> facts.json`

The data tables (about 13 MB) and the facts file are not kept here: they are made again by the steps above.
