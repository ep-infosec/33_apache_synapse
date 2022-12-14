/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.synapse.samples.n2n;

import org.apache.synapse.SynapseConstants;
import org.apache.axiom.om.OMElement;
import samples.userguide.MTOMSwAClient;

/**
 *
 */
public class SynapseSample_51_Integration extends AbstractAutomationTestCase {

    @Override
    protected void setUp() throws Exception {
        System.setProperty(SynapseConstants.SYNAPSE_XML, SAMPLE_CONFIG_ROOT_PATH + "synapse_sample_51.xml");
        super.setUp();
    }

    public void testSample() throws Exception {
        System.setProperty("opt_mode", "mtom");
        OMElement response = MTOMSwAClient.sendUsingMTOM(
                "./../../repository/conf/sample/resources/mtom/asf-logo.gif", "http://localhost:8280/services/MTOMSwASampleService");
//        assertXpathExists("ns:getQuoteResponse", resultString);
//        assertXpathExists("ns:getQuoteResponse/ns:return", resultString);
    }
}
