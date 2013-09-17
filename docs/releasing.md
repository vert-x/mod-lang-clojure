Since I forget this every time, I'm documenting it here.

First, commit and push all changes.

## SNAPSHOT

If this is a SNAPSHOT:

    mvn clean deploy
    
You're done!

## Versioned Release 

1. `mvn release:clean`
2. `mvn release:prepare` - Use the version number (ex: '0.3.0') as the
   tag, and use the next minor version + SNAPSHOT for the next dev
   version (ex: '0.4.0-SNAPSHOT' instead of '0.3.1-SNAPSHOT')
3. `mvn release:perform`
4. Log into <http://oss.sonatype.org>
5. Browse to 'Staging Repositories`, find the correct 'io.vertx' repo,
   and select it 
6. Close the repo, then release it (you may need to refresh the list 
   for the release button to become active).
7. Update the ChangeLog.
8. Switch to release tag, build docs, and upload to tcrawley.org (for now)
9. Update README with link to ^
10. Wait for several hours until sonatype syncs to central.
11. Announce it: twitter, clojure@, vertx@

For reference:

<https://docs.sonatype.org/display/Repository/Sonatype+OSS+Maven+Repository+Usage+Guide>
