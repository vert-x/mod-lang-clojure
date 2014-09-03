Since I forget this every time, I'm documenting it here.

First, commit and push all changes.

## SNAPSHOT

If this is a SNAPSHOT:

    mvn clean deploy

You're done!

## Versioned Release

1. Update the ChangeLog.
2. `mvn release:clean`
3. `mvn release:prepare -DautoVersionSubmodules` -
   Use the version number (ex: '0.3.0') as the tag, and use the next
   minor version + SNAPSHOT for the next dev version (ex:
   '0.4.0-SNAPSHOT' instead of '0.3.1-SNAPSHOT')
4. `mvn release:perform`
5. Log into <http://oss.sonatype.org>
6. Browse to 'Staging Repositories', find the correct 'io.vertx' repo,
   and select it
7. Close the repo, then release it (you may need to refresh the list
   for the release button to become active).
8. Publish the API docs:
   * Switch to release tag
   * `mvn install` in api to generate docs
   * `mv api/target/html-docs /tmp`
   * `git checkout gh-pages`
   * `mv /tmp/html-docs docs/<version>/`
   * edit index.html to refer to new docs
   * commit
   * push the gh-pages branch
   * `git checkout master`
9. Update README with link to ^ and update the version number
10. Wait for several hours until sonatype syncs to central. You can
    check <http://search.maven.org> to know when this has completed.
11. Announce it: twitter, clojure@, vertx@
12. Register it in the [module registry](http://modulereg.vertx.io/)

For reference:

<https://docs.sonatype.org/display/Repository/Sonatype+OSS+Maven+Repository+Usage+Guide>
<http://wickedsource.org/2013/09/23/releasing-your-project-to-maven-central-guide/>
