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

package org.apache.synapse.util.xpath;


import org.apache.commons.codec.binary.Base64;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jaxen.Context;
import org.jaxen.Function;
import org.jaxen.FunctionCallException;
import org.jaxen.function.StringFunction;

import java.io.UnsupportedEncodingException;
import java.util.List;

/**
 * Implements the XPath extension function synapse:base64Decode(string)
 */
public class Base64DecodeFunction implements Function {

    private static final Log log = LogFactory.getLog(Base64DecodeFunction.class);

    /**
     * Returns the base64 decoded string value of the first argument.
     *
     * @param context the context at the point in the expression when the function is called
     * @param args  arguments of the functions
     * @return The string value of a property
     * @throws FunctionCallException
     */
    @Override
    public Object call(Context context, List args) throws FunctionCallException {
        if (args == null || args.size() == 0) {
            if (log.isDebugEnabled()) {
                log.debug("Property key value for lookup is not specified");
            }
            return SynapseXPathConstants.NULL_STRING;
        }

        int size = args.size();
        if (size == 1) {
            // get the first argument, it can be a function returning a string as well
            String encodedValue = StringFunction.evaluate(args.get(0), context.getNavigator());
            // use the default UTF-8 decoding.
            return decode(log.isDebugEnabled(), SynapseXPathConstants.DEFAULT_CHARSET, encodedValue);
        } else if (size == 2) {
            // get the first argument, it can be a function returning a string as well
            String encodedValue = StringFunction.evaluate(args.get(0), context.getNavigator());
            // charset is in the second argument
            String charset = StringFunction.evaluate(args.get(1), context.getNavigator());
            return decode(log.isDebugEnabled(), charset, encodedValue);
        } else if (log.isDebugEnabled()) {
            log.debug("base64Decode function expects only two arguments maximum, returning empty string");
        }
        // return empty string if the arguments are wrong
        return SynapseXPathConstants.NULL_STRING;
    }


    private Object decode(boolean debugOn, String charset, String value) throws FunctionCallException {
        if (value == null || value.isEmpty()) {
            if (debugOn) {
                log.debug("Non empty string value should be provided for decode");
            }
            return SynapseXPathConstants.NULL_STRING;
        }

        byte[] decodedValue = new Base64().decode(value);
        String decodedString;
        try {
            decodedString = new String(decodedValue, charset).trim();
        } catch (UnsupportedEncodingException e) {
            String msg = "Unsupported Charset";
            log.error(msg, e);
            throw new FunctionCallException(msg, e);
        }

        if (debugOn) {
            log.debug("Decoded base64 encoded value: " + value + " with charset: " + charset +
                      " to String: " + decodedString);
        }

        return decodedString;
    }
}
