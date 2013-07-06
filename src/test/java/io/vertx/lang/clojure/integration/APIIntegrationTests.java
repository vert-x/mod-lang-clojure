/*
 * Copyright 2013 the original author or authors.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
