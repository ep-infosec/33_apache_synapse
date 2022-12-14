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

package org.apache.synapse.mediators.builtin;

import org.apache.axiom.om.OMAbstractFactory;
import org.apache.axiom.soap.SOAP11Constants;
import org.apache.axiom.soap.SOAP12Constants;
import org.apache.axiom.soap.SOAPFactory;
import org.apache.axis2.AxisFault;
import org.apache.axis2.clustering.ClusteringFault;
import org.apache.axis2.clustering.state.Replicator;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.context.OperationContext;
import org.apache.axis2.i18n.Messages;
import org.apache.synapse.ManagedLifecycle;
import org.apache.synapse.MessageContext;
import org.apache.synapse.SynapseException;
import org.apache.synapse.SynapseLog;
import org.apache.synapse.core.SynapseEnvironment;
import org.apache.synapse.core.axis2.Axis2MessageContext;
import org.apache.synapse.core.axis2.Axis2Sender;
import org.apache.synapse.mediators.AbstractMediator;
import org.apache.synapse.mediators.base.SequenceMediator;
import org.apache.synapse.util.FixedByteArrayOutputStream;
import org.apache.synapse.util.MessageHelper;
import org.apache.axiom.soap.SOAPEnvelope;
import org.wso2.caching.*;
import org.wso2.caching.util.SOAPMessageHelper;
import org.wso2.caching.digest.DigestGenerator;

import javax.xml.soap.SOAPException;
import javax.xml.stream.XMLStreamException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * CacheMediator will cache the response messages indexed using the hash value of the
 * request message, and subsequent messages with the same request (request hash will be
 * generated and checked for the equality) within the cache expiration period will be served
 * from the stored responses in the cache
 *
 * @see org.apache.synapse.Mediator
 */
public class CacheMediator extends AbstractMediator implements ManagedLifecycle {

    private String id = null;
    private String scope = CachingConstants.SCOPE_PER_HOST;// global
    private boolean collector = false;
    private DigestGenerator digestGenerator = CachingConstants.DEFAULT_XML_IDENTIFIER;
    private int inMemoryCacheSize = CachingConstants.DEFAULT_CACHE_SIZE;
    // if this is 0 then no disk cache, and if there is no size specified in the config
    // factory will asign a default value to enable disk based caching
    private int diskCacheSize = 0;
    private long timeout = 0L;
    private SequenceMediator onCacheHitSequence = null;
    private String onCacheHitRef = null;
    private int maxMessageSize = 0;
    private static final String CACHE_KEY_PREFIX = "synapse.cache_key_";

    private String cacheKey = "synapse.cache_key";

    @Override
    public void init(SynapseEnvironment se) {
        if (onCacheHitSequence != null) {
            onCacheHitSequence.init(se);
        }
    }

    @Override
    public void destroy() {
        if (onCacheHitSequence != null) {
            onCacheHitSequence.destroy();
        }
    }

    @Override
    public boolean mediate(MessageContext synCtx) {

        SynapseLog synLog = getLog(synCtx);

        if (synLog.isTraceOrDebugEnabled()) {
            synLog.traceOrDebug("Start : Cache mediator");

            if (synLog.isTraceTraceEnabled()) {
                synLog.traceTrace("Message : " + synCtx.getEnvelope());
            }
        }

        // if maxMessageSize is specified check for the message size before processing
        if (maxMessageSize > 0) {
            FixedByteArrayOutputStream fbaos = new FixedByteArrayOutputStream(maxMessageSize);
            try {
                MessageHelper.cloneSOAPEnvelope(synCtx.getEnvelope()).serialize(fbaos);
            } catch (XMLStreamException e) {
                handleException("Error in checking the message size", e, synCtx);
            } catch (SynapseException syne) {
                synLog.traceOrDebug("Message size exceeds the upper bound for caching, " +
                            "request will not be cached");
                return true;
            }
        }

        ConfigurationContext cfgCtx =
            ((Axis2MessageContext) synCtx).getAxis2MessageContext().getConfigurationContext();
        if (cfgCtx == null) {
            handleException("Unable to perform caching, "
                + " ConfigurationContext cannot be found", synCtx);
            return false; // never executes.. but keeps IDE happy
        }

        if (synLog.isTraceOrDebugEnabled()) {
            synLog.traceOrDebug("Looking up cache at scope : " + scope + " with ID : "
                    + cacheKey);
        }

        // look up cache
        Object prop = cfgCtx.getPropertyNonReplicable(CachingConstants.CACHE_MANAGER);
        CacheManager cacheManager;
        if (prop != null && prop instanceof CacheManager) {
            cacheManager = (CacheManager) prop;
        } else {
            synchronized (cfgCtx) {
                // check again after taking the lock to make sure no one else did it before us
                prop = cfgCtx.getPropertyNonReplicable(CachingConstants.CACHE_MANAGER);
                if (prop != null && prop instanceof CacheManager) {
                    cacheManager = (CacheManager) prop;

                } else {
                    synLog.traceOrDebug("Creating/recreating the cache object");
                    cacheManager = new CacheManager();
                    cfgCtx.setProperty(CachingConstants.CACHE_MANAGER, cacheManager);
                }
            }
        }

        boolean result = true;
        try {

            if (synCtx.isResponse()) {
                processResponseMessage(synCtx, synLog);

            } else {
                result = processRequestMessage(synCtx, synLog, cacheManager);
            }

        } catch (ClusteringFault clusteringFault) {
            synLog.traceOrDebug("Unable to replicate Cache mediator state among the cluster");
        }

        synLog.traceOrDebug("End : Cache mediator");

        return result;
    }

    /**
     * Process a response message through this cache mediator. This finds the Cache used, and
     * updates it for the corresponding request hash
     *
     * @param synLog         the Synapse log to use
     * @param synCtx         the current message (response)
     * @throws ClusteringFault is there is an error in replicating the cfgCtx
     */
    private void processResponseMessage(MessageContext synCtx,
                                        SynapseLog synLog) throws ClusteringFault {

        if (!collector) {
            handleException("Response messages cannot be handled in a non collector cache", synCtx);
        }

        org.apache.axis2.context.MessageContext msgCtx =
                ((Axis2MessageContext)synCtx).getAxis2MessageContext();
        OperationContext operationContext = msgCtx.getOperationContext();

        CachableResponse response =
                (CachableResponse) operationContext.getPropertyNonReplicable(CachingConstants.CACHED_OBJECT);
        if (response != null) {
            if (synLog.isTraceOrDebugEnabled()) {
                synLog.traceOrDebug("Storing the response message into the cache at scope : " +
                        scope + " with ID : " + cacheKey + " for request hash : " + response.getRequestHash());
            }
            if (synLog.isTraceOrDebugEnabled()) {
                synLog.traceOrDebug("Storing the response for the message with ID : " +
                        synCtx.getMessageID() + " with request hash ID : " +
                        response.getRequestHash() + " in the cache : " + cacheKey);
            }

            ByteArrayOutputStream outStream = new ByteArrayOutputStream();
            try {
                synCtx.getEnvelope().serialize(outStream);
                response.setResponseEnvelope(outStream.toByteArray());
            } catch (XMLStreamException e) {
                handleException("Unable to set the response to the Cache", e, synCtx);
            }

            // this is not required yet, can commented this for perf improvements
            // in the future there can be a situation where user sends the request
            // with the response hash (if client side caching is on) in which case
            // we can compare that response hash with the given response hash and
            // respond with not-modified http header */
            // cachedObj.setResponseHash(cache.getGenerator().getDigest(
            //     ((Axis2MessageContext) synCtx).getAxis2MessageContext()));

            if (response.getTimeout() > 0) {
                response.setExpireTimeMillis(System.currentTimeMillis() + response.getTimeout());
            }

            // Finally, we may need to replicate the changes in the cache
            CacheReplicationCommand cacheReplicationCommand
                    = (CacheReplicationCommand) msgCtx.getPropertyNonReplicable(
                    CachingConstants.STATE_REPLICATION_OBJECT);
            if (cacheReplicationCommand != null) {
                try {
                    Replicator.replicateState(cacheReplicationCommand,
                            msgCtx.getRootContext().getAxisConfiguration());
                } catch (ClusteringFault clusteringFault) {
                    log.error("Cannot replicate cache changes");
                }
            }
        } else {
            synLog.auditWarn("A response message without a valid mapping to the " +
                    "request hash found. Unable to store the response in cache");
        }


    }

    /**
     * Processes a request message through the cache mediator. Generates the request hash and looks
     * up for a hit, if found; then the specified named or anonymous sequence is executed or marks
     * this message as a response and sends back directly to client.
     *
     * @param synCtx         incoming request message
     * @param synLog         the Synapse log to use
     * @param cacheManager   the cache manager
     * @return should this mediator terminate further processing?
     * @throws ClusteringFault if there is an error in replicating the cfgCtx
     */
    private boolean processRequestMessage(MessageContext synCtx,
        SynapseLog synLog, CacheManager cacheManager) throws ClusteringFault {

        if (collector) {
            handleException("Request messages cannot be handled in a collector cache", synCtx);
        }
        OperationContext opCtx = ((Axis2MessageContext)synCtx).getAxis2MessageContext().
                getOperationContext();

        String requestHash = null;
        try {
            requestHash = digestGenerator.getDigest(
                ((Axis2MessageContext) synCtx).getAxis2MessageContext());
            synCtx.setProperty(CachingConstants.REQUEST_HASH, requestHash);
        } catch (CachingException e) {
            handleException("Error in calculating the hash value of the request", e, synCtx);
        }

        if (synLog.isTraceOrDebugEnabled()) {
            synLog.traceOrDebug("Generated request hash : " + requestHash);
        }

        ServiceName service;
        if (id != null) {
            service = new ServiceName(id);
        } else {
            service = new ServiceName(cacheKey);
        }

        RequestHash hash = new RequestHash(requestHash);
        CachableResponse cachedResponse =
                cacheManager.getCachedResponse(service, hash);

        org.apache.axis2.context.MessageContext msgCtx = ((Axis2MessageContext)synCtx).getAxis2MessageContext();
        opCtx.setNonReplicableProperty(CachingConstants.REQUEST_HASH, requestHash);
        CacheReplicationCommand cacheReplicationCommand = new CacheReplicationCommand();

        if (cachedResponse != null) {
            // get the response from the cache and attach to the context and change the
            // direction of the message
            if (!cachedResponse.isExpired()) {
                if (synLog.isTraceOrDebugEnabled()) {
                    synLog.traceOrDebug("Cache-hit for message ID : " + synCtx.getMessageID());
                }
                cachedResponse.setInUse(true);
                // mark as a response and replace envelope from cache
                synCtx.setResponse(true);
                opCtx.setNonReplicableProperty(CachingConstants.CACHED_OBJECT, cachedResponse);

                SOAPEnvelope omSOAPEnv;
                try {
                    omSOAPEnv = SOAPMessageHelper.buildSOAPEnvelopeFromBytes(
                            cachedResponse.getResponseEnvelope());
                    if (omSOAPEnv != null) {
                        synCtx.setEnvelope(omSOAPEnv);
                    }
                } catch (AxisFault axisFault) {
                    handleException("Error setting response envelope from cache : "
                            + cacheKey, synCtx);
                } catch (IOException ioe) {
                    handleException("Error setting response envelope from cache : "
                            + cacheKey, ioe, synCtx);
                } catch (SOAPException soape) {
                    handleException("Error setting response envelope from cache : "
                            + cacheKey, soape, synCtx);
                }

                // take specified action on cache hit
                if (onCacheHitSequence != null) {
                    // if there is an onCacheHit use that for the mediation
                    synLog.traceOrDebug("Delegating message to the onCachingHit "
                            + "Anonymous sequence");
                    onCacheHitSequence.mediate(synCtx);

                } else if (onCacheHitRef != null) {

                    if (synLog.isTraceOrDebugEnabled()) {
                        synLog.traceOrDebug("Delegating message to the onCachingHit " +
                                "sequence : " + onCacheHitRef);
                    }
                    synCtx.getSequence(onCacheHitRef).mediate(synCtx);

                } else {

                    if (synLog.isTraceOrDebugEnabled()) {
                        synLog.traceOrDebug("Request message " + synCtx.getMessageID() +
                                " was served from the cache : " + cacheKey);
                    }
                    // send the response back if there is not onCacheHit is specified
                    synCtx.setTo(null);
                    Axis2Sender.sendBack(synCtx);
                }
                // stop any following mediators from executing
                return false;

            } else {
                cachedResponse.reincarnate(timeout);
                if (synLog.isTraceOrDebugEnabled()) {
                    synLog.traceOrDebug("Existing cached response has expired. Reset cache element");
                }
                cacheManager.cacheResponse(service, hash, cachedResponse, cacheReplicationCommand);
                opCtx.setNonReplicableProperty(CachingConstants.CACHED_OBJECT, cachedResponse);
                opCtx.setNonReplicableProperty(CachingConstants.STATE_REPLICATION_OBJECT,
                        cacheReplicationCommand);

                Replicator.replicate(opCtx);
            }
        } else {
            // if not found in cache, check if we can cache this request
            if (cacheManager.getCacheSize(service) >= inMemoryCacheSize) { // If cache is full
                cacheManager.removeExpiredResponses(service, cacheReplicationCommand); // try to remove expired responses
                if (cacheManager.getCacheSize(service) >= inMemoryCacheSize) { // recheck if there is space
                    if (log.isDebugEnabled()) {
                        log.debug("In-memory cache is full. Unable to cache");
                    }
                } else { // if we managed to free up some space in the cache. Need state replication
                    cacheNewResponse(msgCtx, service, hash, cacheManager,
                                     cacheReplicationCommand);
                }
            } else { // if there is more space in the cache. Need state replication
                cacheNewResponse(msgCtx, service, hash, cacheManager,
                                 cacheReplicationCommand);
            }
        }

        return true;
    }

    private void cacheNewResponse(org.apache.axis2.context.MessageContext msgContext,
                                  ServiceName serviceName, RequestHash requestHash,
                                  CacheManager cacheManager,
                                  CacheReplicationCommand cacheReplicationCommand) throws ClusteringFault {
        OperationContext opCtx = msgContext.getOperationContext();
        CachableResponse response = new CachableResponse();
        response.setRequestHash(requestHash.getRequestHash());
        response.setTimeout(timeout);
        cacheManager.cacheResponse(serviceName, requestHash, response, cacheReplicationCommand);
        opCtx.setNonReplicableProperty(CachingConstants.CACHED_OBJECT, response);
        opCtx.setNonReplicableProperty(CachingConstants.STATE_REPLICATION_OBJECT,
                                       cacheReplicationCommand);

        Replicator.replicate(opCtx);
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getScope() {
        return scope;
    }

    public void setScope(String scope) {
        this.scope = scope;
        if (CachingConstants.SCOPE_PER_MEDIATOR.equals(scope)) {
            cacheKey = CACHE_KEY_PREFIX + id;
        }
    }

    public boolean isCollector() {
        return collector;
    }

    public void setCollector(boolean collector) {
        this.collector = collector;
    }

    public DigestGenerator getDigestGenerator() {
        return digestGenerator;
    }

    public void setDigestGenerator(DigestGenerator digestGenerator) {
        this.digestGenerator = digestGenerator;
    }

    public int getInMemoryCacheSize() {
        return inMemoryCacheSize;
    }

    public void setInMemoryCacheSize(int inMemoryCacheSize) {
        this.inMemoryCacheSize = inMemoryCacheSize;
    }

    public int getDiskCacheSize() {
        return diskCacheSize;
    }

    public void setDiskCacheSize(int diskCacheSize) {
        this.diskCacheSize = diskCacheSize;
    }

    // change the variable to Timeout milis seconds
    public long getTimeout() {
        return timeout / 1000;
    }

    public void setTimeout(long timeout) {
        this.timeout = timeout * 1000;
    }

    public SequenceMediator getOnCacheHitSequence() {
        return onCacheHitSequence;
    }

    public void setOnCacheHitSequence(SequenceMediator onCacheHitSequence) {
        this.onCacheHitSequence = onCacheHitSequence;
    }

    public String getOnCacheHitRef() {
        return onCacheHitRef;
    }

    public void setOnCacheHitRef(String onCacheHitRef) {
        this.onCacheHitRef = onCacheHitRef;
    }

    public int getMaxMessageSize() {
        return maxMessageSize;
    }

    public void setMaxMessageSize(int maxMessageSize) {
        this.maxMessageSize = maxMessageSize;
    }

    public SOAPFactory getSOAPFactory(org.apache.axis2.context.MessageContext msgContext) throws AxisFault {
        String nsURI = msgContext.getEnvelope().getNamespace().getNamespaceURI();
        if (SOAP12Constants.SOAP_ENVELOPE_NAMESPACE_URI.equals(nsURI)) {
            return OMAbstractFactory.getSOAP12Factory();
        } else if (SOAP11Constants.SOAP_ENVELOPE_NAMESPACE_URI.equals(nsURI)) {
            return OMAbstractFactory.getSOAP11Factory();
        } else {
            throw new AxisFault(Messages.getMessage("invalidSOAPversion"));
        }
    }
}