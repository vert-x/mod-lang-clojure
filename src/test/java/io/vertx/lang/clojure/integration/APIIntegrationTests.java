package io.vertx.lang.clojure.integration;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.vertx.testtools.ScriptClassRunner;
import org.vertx.testtools.TestVerticleInfo;

/**
 * This is dummy JUnit test class which runs clojure files
 * as JUnit tests.
 *
 * The scripts go in src/test/resources.
 */
@RunWith(ScriptClassRunner.class)
@TestVerticleInfo(filenameFilter = ".+_test\\.clj", funcRegex = "defn[\\s]+(test[^\\s]+)")
public class APIIntegrationTests {

    @Test
    public void __vertxDummy() {
    }
}
