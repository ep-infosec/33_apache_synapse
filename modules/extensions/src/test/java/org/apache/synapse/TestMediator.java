/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *   * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

package org.apache.synapse;

import org.apache.synapse.mediators.AbstractMediator;

/**
 * Test mediator class.
 */
public class TestMediator extends AbstractMediator {

    private org.apache.synapse.TestMediateHandler handlerTest = null;

    public TestMediator() {
    }

    @Override
    public boolean mediate(MessageContext synCtx) {
        if (handlerTest != null) {
            handlerTest.handle(synCtx);
        }
        return true;
    }

    @Override
    public String getType() {
        return null;
    }

    public TestMediateHandler getHandler() {
        return handlerTest;
    }

    public void setHandler(TestMediateHandler handlerTest) {
        this.handlerTest = handlerTest;
    }
}