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

import org.apache.axiom.om.OMAttribute;
import org.apache.axiom.om.OMElement;
import org.apache.synapse.Mediator;
import org.apache.synapse.SynapseException;
import org.apache.synapse.SynapseConstants;
import org.apache.synapse.mediators.transform.HeaderMediator;
import org.jaxen.JaxenException;

import javax.xml.namespace.QName;
import java.util.Iterator;
import java.util.Properties;

/**
 * Factory for {@link HeaderMediator} instances.
 * <p>
 * Configuration syntax to set a header:
 *   <pre>
 *      &lt;header name="qname" (value="literal" | expression="xpath")/&gt;
 *   </pre>
 *
 * Configuration syntax to remove a header:
 *   <pre>
 *      &lt;header name="qname" action="remove"/&gt;
 *   </pre>
 */
public class HeaderMediatorFactory extends AbstractMediatorFactory  {

    private static final QName HEADER_Q = new QName(XMLConfigConstants.SYNAPSE_NAMESPACE, "header");
    private static final QName ATT_ACTION = new QName("action");
    private static final QName ATT_SCOPE = new QName("scope");

    @Override
    public Mediator createSpecificMediator(OMElement elem, Properties properties) {

        HeaderMediator headerMediator = new HeaderMediator();
        OMAttribute name   = elem.getAttribute(ATT_NAME);
        OMAttribute value  = elem.getAttribute(ATT_VALUE);
        OMAttribute exprn  = elem.getAttribute(ATT_EXPRN);
        OMAttribute action = elem.getAttribute(ATT_ACTION);
        OMAttribute scope = elem.getAttribute(ATT_SCOPE);

        // Setting Header Name
        if (name == null || name.getAttributeValue() == null) {
            // Name is required unless otherwise we are setting an embedded XML
            if (!isEmbeddedXML(elem)) {
                handleException("A valid name attribute is required for the header mediator");
            }
        } else {

            if (scope == null) {
                String nameAtt = name.getAttributeValue();

                // Known Headers
                if (SynapseConstants.HEADER_TO.equals(nameAtt) ||
                    SynapseConstants.HEADER_FROM.equals(nameAtt) ||
                    SynapseConstants.HEADER_ACTION.equals(nameAtt) ||
                    SynapseConstants.HEADER_FAULT.equals(nameAtt) ||
                    SynapseConstants.HEADER_REPLY_TO.equals(nameAtt) ||
                    SynapseConstants.HEADER_RELATES_TO.equals(nameAtt)) {

                    headerMediator.setQName(new QName(nameAtt));
                } else {
                    // SOAP Headers
                    setSOAPHeader(headerMediator, elem, name);
                }
            } else {
                String scopeAttValue = scope.getAttributeValue();

                if (XMLConfigConstants.HEADER_SCOPE_SOAP.equalsIgnoreCase(scopeAttValue)) {
                    setSOAPHeader(headerMediator, elem, name);
                } else if (XMLConfigConstants.HEADER_SCOPE_TRANSPORT.equalsIgnoreCase(scopeAttValue)) {
                    headerMediator.setQName(new QName(name.getAttributeValue()));
                } else {
                    handleException("Unsupported Scope : " + scopeAttValue + " . Only " + XMLConfigConstants.HEADER_SCOPE_SOAP
                                    + " and " + XMLConfigConstants.HEADER_SCOPE_TRANSPORT + " allowed");
                }
                headerMediator.setScope(scopeAttValue);
            }
        }

        // after successfully creating the mediator
        // set its common attributes such as tracing etc
        processAuditStatus(headerMediator,elem);

        // The action attribute is optional, if provided and equals to 'remove' the
        // header mediator will act as a header remove mediator
        if (action != null && "remove".equals(action.getAttributeValue())) {
            headerMediator.setAction(HeaderMediator.ACTION_REMOVE);
        }

        if (headerMediator.getAction() == HeaderMediator.ACTION_SET &&
                value == null && exprn == null && !headerMediator.isImplicit()) {
            handleException("A 'value' or 'expression' attribute is required for a [set] " +
                    "header mediator");
        }

        if (value != null && value.getAttributeValue() != null) {
            headerMediator.setValue(value.getAttributeValue());

        } else if (exprn != null && exprn.getAttributeValue() != null) {
            try {
                headerMediator.setExpression(SynapseXPathFactory.getSynapseXPath(elem, ATT_EXPRN));
            } catch (JaxenException je) {
                handleException("Invalid XPath expression : " + exprn.getAttributeValue());
            }
        } else if (headerMediator.isImplicit()) { // we have an implicit, non standard header
            Iterator i = elem.getChildElements();
            if (i == null) {
                handleException("A non standard header with both value and expression null must " +
                        "contain an embedded XML definition.");
                return null;
            }
            while (i.hasNext()) {
                headerMediator.addEmbeddedXml((OMElement) i.next());
            }
        }
        return headerMediator;
    }

    private void setSOAPHeader(HeaderMediator headerMediator, OMElement elem, OMAttribute name) {
        String nameAtt = name.getAttributeValue();
        QName qname = elem.resolveQName(nameAtt);
        if (qname == null) {
            handleException("Invalid QName '" + nameAtt + "' in name attribute");
        } else if (qname.getNamespaceURI().isEmpty()) {
            handleException("Invalid SOAP header: " + nameAtt + " specified at the " +
                    "header mediator. All SOAP headers must be namespace qualified.");
        } else {
            headerMediator.setQName(qname);
        }
    }

    private boolean isEmbeddedXML(OMElement elem) {
        return (elem.getChildElements() != null && elem.getChildElements().hasNext());
    }

    @Override
    public QName getTagQName() {
        return HEADER_Q;
    }
}
