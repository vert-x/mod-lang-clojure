/*
 * Copyright 2011-2012 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.vertx.java.platform.impl;

import org.vertx.java.core.Vertx;
import org.vertx.java.core.logging.impl.LoggerFactory;
import org.vertx.java.core.logging.Logger;
import org.vertx.java.platform.Container;
import org.vertx.java.platform.Verticle;
import org.vertx.java.platform.VerticleFactory;

import clojure.lang.RT;

public class ClojureVerticleFactory implements VerticleFactory {

    private static final Logger log = LoggerFactory.getLogger(ClojureVerticleFactory.class);
    private ClassLoader cl;
    private Vertx vertx;
    private Container container;

    public ClojureVerticleFactory() {
        super();
    }

    @Override
    public void init(Vertx vertx, Container container, ClassLoader cl) {
        this.cl = cl;
        this.vertx = vertx;
        this.container = container;
    }

    @Override
    public Verticle createVerticle(String scriptName) throws Exception {
        return new ClojureVerticle(scriptName);
    }

    @Override
    public void reportException(Logger logger, Throwable t) {
        logger.error("Unexpected exception in Clojure verticle", t);
    }

    @Override
    public void close() {}

    private class ClojureVerticle extends Verticle {
        private final String scriptName;

        ClojureVerticle(String scriptName) {
            this.scriptName = scriptName;
        }

        public void start() {
            try {
                RT.load("clojure/core");
                clojure.lang.Var.pushThreadBindings(RT.map(clojure.lang.Compiler.LOADER, cl));
                RT.var("vertx.core", "vertx", getVertx());
                RT.var("vertx.core", "container", getContainer());
                RT.loadResourceScript(scriptName);
            } catch(Exception e) {
                log.info(e.getMessage(), e);
            }
            log.info("Started clojure verticle: " + scriptName);
        }

        public void stop() {
            //TODO: setting a stop handler
            log.info("Stop verticle: " + scriptName);
        }
    }
}
