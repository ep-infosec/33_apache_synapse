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
import org.apache.synapse.mediators.bean.BeanConstants;
import org.apache.synapse.mediators.bean.BeanUtils;
import org.apache.synapse.mediators.bean.Target;
import org.apache.synapse.mediators.bean.enterprise.EJBConstants;
import org.apache.synapse.mediators.bean.enterprise.EJBMediator;

import javax.xml.namespace.QName;
import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.Properties;

/**
 * Creates an {@link EJBMediator} from the provided XML configuration.
 *
 * <p/>
 * <pre>
 * &lt;ejb beanstalk="string" class="string" [sessionId="string"] [remove="true | false"]
 * [method="string"] [target="string | {xpath}"] [jndiName="string"] /&gt;
 *   &lt;args&gt;
 *     &lt;arg (value="string | {xpath}")/&gt;*
 *   &lt;/args&gt;
 * &lt;/ejb&gt;
 * </pre>
 */
public class EJBMediatorFactory extends AbstractMediatorFactory {

    private static final QName EJB_Q =
            new QName(XMLConfigConstants.SYNAPSE_NAMESPACE, "ejb");

    @Override
    public Mediator createSpecificMediator(OMElement elem, Properties properties) {

        EJBMediator mediator = new EJBMediator();

        String attributeValue;

        attributeValue = elem.getAttributeValue(new QName(EJBConstants.BEANSTALK));
        if (attributeValue != null) {
            mediator.setBeanstalkName(attributeValue.trim());
        } else {
            handleException("'beanstalk' attribute of callEjb mediator is required");
        }

        attributeValue = elem.getAttributeValue(new QName(BeanConstants.CLASS));
        if (attributeValue != null) {
            mediator.setClassName(attributeValue.trim());
        } else {
            handleException("'class' attribute of callEjb mediator is required");
        }

        attributeValue = elem.getAttributeValue(new QName(EJBConstants.SESSION_ID));
        if (attributeValue != null) {
            mediator.setSessionId(new ValueFactory().createValue(EJBConstants.SESSION_ID, elem));
        }

        boolean remove;
        attributeValue = elem.getAttributeValue(new QName(EJBConstants.REMOVE));
        remove = Boolean.valueOf(attributeValue);
        if (remove) {
            mediator.setRemove(true);
        }

        if (elem.getAttributeValue(new QName(BeanConstants.TARGET)) != null) {
            mediator.setTarget(new Target(BeanConstants.TARGET, elem));
        }

        attributeValue = elem.getAttributeValue(new QName(EJBConstants.JNDI_NAME));
        if (attributeValue != null) {
            mediator.setJndiName(attributeValue);
        }

        OMElement argumentsElem = elem.getFirstChildWithName(
                new QName(XMLConfigConstants.SYNAPSE_NAMESPACE, EJBConstants.ARGS));

        if (argumentsElem != null) {

            Iterator itr = argumentsElem.getChildrenWithName(
                    new QName(XMLConfigConstants.SYNAPSE_NAMESPACE, EJBConstants.ARG));

            while (itr.hasNext()) {
                OMElement argElem = (OMElement) itr.next();

                if (argElem.getAttributeValue(ATT_VALUE) != null) {
                    mediator.addArgument(
                            new ValueFactory().createValue(BeanConstants.VALUE, argElem));
                } else {
                    handleException("'value' attribute of 'arg' element is required.");
                }
            }
        }

        attributeValue = elem.getAttributeValue(new QName(EJBConstants.METHOD));
        if (attributeValue != null) {
            Method method = null;
            try {
                method = BeanUtils.resolveMethod(
                                        Class.forName(mediator.getClassName()),
                                        attributeValue,
                                        mediator.getArgumentList().size());
            } catch (ClassNotFoundException e) {
                handleException("Could not load '" + mediator.getClassName() + "' class.", e);
            }
            mediator.setMethod(method);
        } else if (!remove) {
            handleException("'method' attribute of EJB mediator is optional only when it's a " +
                    "bean removal.");
        }

        return mediator;
    }

    @Override
    public QName getTagQName() {
        return EJB_Q;
    }

}
