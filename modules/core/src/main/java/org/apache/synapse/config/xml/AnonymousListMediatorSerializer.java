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
package org.apache.synapse.config.xml;

import org.apache.axiom.om.OMElement;
import org.apache.synapse.Mediator;

/**
 * This is Serializer  for serialization of an anonymous list mediator(an unnamed list of mediators )
 */

public class AnonymousListMediatorSerializer extends AbstractListMediatorSerializer {

    /**
     * To serialize an anonymous list mediator
     *
     * @param m
     * @return OMElement
     */
    @Override
    public OMElement serializeSpecificMediator(Mediator m) {
        throw new UnsupportedOperationException("Anonymous list mediator has nothing specific");
    }

    @Override
    public String getMediatorClassName() {
        return AnonymousListMediator.class.getName();
    }
}
