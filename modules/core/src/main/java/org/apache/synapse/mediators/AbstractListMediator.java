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

package org.apache.synapse.mediators;

import org.apache.synapse.*;
import org.apache.synapse.core.SynapseEnvironment;
import org.apache.synapse.core.axis2.Axis2MessageContext;
import org.apache.synapse.transport.passthru.util.RelayUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * This is the base class for all List mediators
 *
 * @see ListMediator
 */
public abstract class AbstractListMediator extends AbstractMediator
        implements ListMediator {

    /** the list of child mediators held. These are executed sequentially */
    protected final List<Mediator> mediators = new ArrayList<Mediator>();

    private boolean contentAware = false;

    @Override
    public boolean mediate(MessageContext synCtx) {

        int parentsEffectiveTraceState = synCtx.getTracingState();
        // if I have been explicitly asked to enable or disable tracing, set it to the message
        // to pass it on; else, do nothing -> i.e. let the parents state flow
        setEffectiveTraceState(synCtx);
        int myEffectiveTraceState = synCtx.getTracingState();

        try {
            SynapseLog synLog = getLog(synCtx);
            if (synLog.isTraceOrDebugEnabled()) {
                synLog.traceOrDebug("Sequence <" + getType() + "> :: mediate()");
            }

            if (contentAware) {
                try {
                    RelayUtils.buildMessage(((Axis2MessageContext) synCtx).getAxis2MessageContext(), false);
                } catch (Exception e) {
                    handleException("Error while building message", e, synCtx);
                }
            }

            for (Mediator mediator : mediators) {
                // ensure correct trace state after each invocation of a mediator
                synCtx.setTracingState(myEffectiveTraceState);
                if (!mediator.mediate(synCtx)) {
                    return false;
                }
            }
        } catch (SynapseException e) {
            throw e;
        } catch (Exception e) {
            handleException("Runtime error occurred while mediating the message", e, synCtx);
        } finally {
            synCtx.setTracingState(parentsEffectiveTraceState);
        }
        return true;
    }

    @Override
    public List<Mediator> getList() {
        return mediators;
    }

    @Override
    public boolean addChild(Mediator m) {
        return mediators.add(m);
    }

    @Override
    public boolean addAll(List<Mediator> c) {
        return mediators.addAll(c);
    }

    @Override
    public Mediator getChild(int pos) {
        return mediators.get(pos);
    }

    @Override
    public boolean removeChild(Mediator m) {
        return mediators.remove(m);
    }

    @Override
    public Mediator removeChild(int pos) {
        return mediators.remove(pos);
    }

    /**
     * Initialize child mediators recursively
     * @param se synapse environment
     */
    @Override
    public void init(SynapseEnvironment se) {
        if (log.isDebugEnabled()) {
            log.debug("Initializing child mediators of mediator : " + getType());
        }

        for (Mediator mediator : mediators) {
            if (mediator instanceof ManagedLifecycle) {
                ((ManagedLifecycle) mediator).init(se);
            }

            if (mediator.isContentAware()) {
                contentAware = true;
            }
        }
    }

    /**
     * Destroy child mediators recursively
     */
    @Override
    public void destroy() {
        if (log.isDebugEnabled()) {
            log.debug("Destroying child mediators of mediator : " + getType());
        }

        for (Mediator mediator : mediators) {
            if (mediator instanceof ManagedLifecycle) {
                ((ManagedLifecycle) mediator).destroy();
            }
        }
    }

    @Override
    public boolean isContentAware() {
        return contentAware;
    }
}
