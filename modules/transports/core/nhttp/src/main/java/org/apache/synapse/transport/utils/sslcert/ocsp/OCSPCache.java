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

package org.apache.synapse.transport.utils.sslcert.ocsp;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.commons.jmx.MBeanRegistrar;
import org.apache.synapse.transport.utils.sslcert.CertificateVerificationException;
import org.apache.synapse.transport.utils.sslcert.cache.CacheController;
import org.apache.synapse.transport.utils.sslcert.cache.CacheManager;
import org.apache.synapse.transport.utils.sslcert.cache.ManageableCache;
import org.apache.synapse.transport.utils.sslcert.cache.ManageableCacheValue;
import org.bouncycastle.cert.ocsp.BasicOCSPResp;
import org.bouncycastle.cert.ocsp.OCSPReq;
import org.bouncycastle.cert.ocsp.OCSPResp;
import org.bouncycastle.cert.ocsp.SingleResp;

import java.math.BigInteger;
import java.util.Date;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * This is a cache to store OCSP responses against Certificate Serial Number since an OCSP
 * response depends on the certificate. This is a singleton since more than one cache of this
 * kind should not be allowed. This cache can be shared by many transports which need SSL
 * validation through OCSP.
 */
public class OCSPCache implements ManageableCache {

    private static final Log log = LogFactory.getLog(OCSPCache.class);

    private static final OCSPCache cache = new OCSPCache();
    private final Map<BigInteger,OCSPCacheValue> hashMap;
    private Iterator<Map.Entry<BigInteger,OCSPCacheValue>> iterator;
    private volatile CacheManager cacheManager;
    private final OCSPVerifier ocspVerifier;

    private OCSPCache() {
        this.hashMap = new ConcurrentHashMap<BigInteger, OCSPCacheValue>();
        this.iterator = hashMap.entrySet().iterator();
        this.ocspVerifier = new OCSPVerifier(null);
    }

    public static OCSPCache getCache() {
        return cache;
    }

    /**
     * This lazy initializes the Cache with a CacheManager. If this method is not called, a cache manager will not be used.
     * @param size max size of the cache
     * @param delay defines how frequently the CacheManager will be started
     */
    public void init(int size, int delay) {
        if (cacheManager == null) {
            synchronized (OCSPCache.class) {
                if (cacheManager == null) {
                    cacheManager = new CacheManager(cache, size, delay);
                    CacheController mbean = new CacheController(cache,cacheManager);
                    MBeanRegistrar.getInstance().registerMBean(mbean, "CacheController", "OCSPCacheController");
                }
            }
        }
    }

    /**
     * This method is needed by the cache Manager to go through the cache entries to remove invalid values or
     * to remove LRU cache values if the cache has reached its max size.
     * Todo: Can move to an abstract class.
     * @return next cache value of the cache.
     */
    @Override
    public ManageableCacheValue getNextCacheValue() {
        //Changes to the hash map are reflected on the keySet. And its iterator is weakly consistent. so will never
        //throw concurrent modification exception.
        if (iterator.hasNext()) {
            return hashMap.get(iterator.next().getKey());
        } else {
            resetIterator();
            return null;
        }
    }

    /**
     * @return the current cache size (size of the hash map)
     */
    @Override
    public int getCacheSize() {
        return hashMap.size();
    }

    @Override
    public synchronized void resetIterator(){
        iterator = hashMap.entrySet().iterator();
    }

    // This has to be synchronized coz several threads will try to replace cache value
    // (cacheManager and Reactor thread)
    private synchronized void replaceNewCacheValue(OCSPCacheValue cacheValue){
        //If someone has updated with the new value before current Thread.
        if (cacheValue.isValid()) {
            return;
        }

        try {
            String serviceUrl = cacheValue.serviceUrl;
            OCSPReq request = cacheValue.request;
            OCSPResp response= ocspVerifier.getOCSPResponse(serviceUrl, request);

            if (OCSPResp.SUCCESSFUL != response.getStatus())
                throw new CertificateVerificationException("OCSP response status not SUCCESSFUL");

            BasicOCSPResp basicResponse = (BasicOCSPResp) response.getResponseObject();
            SingleResp[] responses = (basicResponse == null) ? null : basicResponse.getResponses();

            if (responses == null)
                throw new CertificateVerificationException("Cant get OCSP response");

            SingleResp resp = responses[0];
            this.setCacheValue(cacheValue.serialNumber, resp, request, serviceUrl);

        } catch (Exception e){
            log.debug("Cant replace old CacheValue with new CacheValue. So remove", e);
            //If cant be replaced remove.
            cacheValue.removeThisCacheValue();
        }
    }

    public synchronized SingleResp getCacheValue(BigInteger serialNumber) {
        OCSPCacheValue cacheValue = hashMap.get(serialNumber);
        if(cacheValue != null) {
            //If who ever gets this cache value before Cache manager task found its invalid, update it and get the
            // new value.
            if (!cacheValue.isValid()) {
                cacheValue.updateCacheWithNewValue();
                OCSPCacheValue ocspCacheValue = hashMap.get(serialNumber);
                return (ocspCacheValue!=null? ocspCacheValue.getValue(): null);
            }

            return cacheValue.getValue();
        }
        else
            return null;
    }

    public synchronized void setCacheValue(BigInteger serialNumber, SingleResp singleResp, OCSPReq request, String serviceUrl) {
        OCSPCacheValue cacheValue = new OCSPCacheValue(serialNumber, singleResp, request, serviceUrl);
        if (log.isDebugEnabled()) {
            log.debug("Before set - HashMap size " + hashMap.size());
        }
        hashMap.put(serialNumber, cacheValue);
        if (log.isDebugEnabled()) {
            log.debug("After set - HashMap size " + hashMap.size());
        }
    }

    public synchronized void removeCacheValue(BigInteger serialNumber) {
        if (log.isDebugEnabled()) {
            log.debug("Before remove - HashMap size " + hashMap.size());
        }
        hashMap.remove(serialNumber);
        if (log.isDebugEnabled()) {
            log.debug("After remove - HashMap size " + hashMap.size());
        }
    }

    /**
     * This is the wrapper class of the actual cache value which is a SingleResp.
     */
    private class OCSPCacheValue implements ManageableCacheValue {

        private BigInteger serialNumber;
        private SingleResp singleResp;
        private OCSPReq request;
        private String serviceUrl;
        private long timeStamp = System.currentTimeMillis();

        public OCSPCacheValue(BigInteger serialNumber, SingleResp singleResp, OCSPReq request, String serviceUrl) {
            this.serialNumber = serialNumber;
            this.singleResp = singleResp;
            //request and serviceUrl are needed to update the cache with new values.
            this.request = request;
            this.serviceUrl = serviceUrl;
        }

        public BigInteger getKey() {
            return serialNumber;
        }

        public SingleResp getValue() {
            timeStamp = System.currentTimeMillis();
            return singleResp;
        }

        /**
         * An OCSP response is valid during its validity period.
         */
        @Override
        public boolean isValid() {
            Date now = new Date();
            Date nextUpdate = singleResp.getNextUpdate();
            return nextUpdate != null && nextUpdate.after(now);
        }

        @Override
        public long getTimeStamp() {
            return timeStamp;
        }

        /**
         * Used by cacheManager to remove invalid entries.
         */
        @Override
        public void removeThisCacheValue() {
            removeCacheValue(serialNumber);
        }

        @Override
        public void updateCacheWithNewValue() {
            replaceNewCacheValue(this);
        }
    }
}
