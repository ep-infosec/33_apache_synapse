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

package org.apache.synapse.core.axis2;

import org.apache.axis2.client.async.AxisCallback;
import org.apache.synapse.MessageContext;
import org.apache.synapse.SynapseConstants;

/**
 * This class only "holds" the Synapse out message context for the Synapse callback message
 * receiver when a response is received or error is encountered
 */
public class AsyncCallback implements AxisCallback {

    /** The corresponding Synapse outgoing message context this instance is holding onto */
    private final MessageContext synapseOutMsgCtx;

    /** Time to timeout this callback */
    private long timeOutOn;

    /** Action to perform when timeout occurs */
    private int timeOutAction = SynapseConstants.NONE;

    public AsyncCallback(MessageContext synapseOutMsgCtx) {
        this.synapseOutMsgCtx = synapseOutMsgCtx;
    }

    @Override
    public void onMessage(org.apache.axis2.context.MessageContext messageContext) {}

    @Override
    public void onFault(org.apache.axis2.context.MessageContext messageContext) {}

    @Override
    public void onError(Exception e) {}

    @Override
    public void onComplete() {}

    public org.apache.synapse.MessageContext getSynapseOutMsgCtx() {
        return synapseOutMsgCtx;
    }

    public long getTimeOutOn() {
        return timeOutOn;
    }

    public void setTimeOutOn(long timeOutOn) {
        this.timeOutOn = timeOutOn;
    }

    public int getTimeOutAction() {
        return timeOutAction;
    }

    public void setTimeOutAction(int timeOutAction) {
        this.timeOutAction = timeOutAction;
    }
}
