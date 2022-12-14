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


package org.apache.synapse.commons.evaluators.config;

import org.apache.synapse.commons.evaluators.Evaluator;
import org.apache.synapse.commons.evaluators.OrEvaluator;
import org.apache.synapse.commons.evaluators.EvaluatorException;
import org.apache.axiom.om.OMElement;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;

/**
 * This factory creates a {@link OrFactory} using the following XML configuration.</p>
 *
 * <pre>
 * &lt;or&gt;
 *     two or more evaluators
 * &lt;/or&gt;
 * </pre>
 */
public class OrFactory implements EvaluatorFactory {
    private Log log = LogFactory.getLog(OrFactory.class);

    @Override
    public Evaluator create(OMElement e) throws EvaluatorException {
        OrEvaluator o = new OrEvaluator();

        Iterator it = e.getChildElements();

        List<Evaluator> evaluators = new ArrayList<Evaluator>();

        while (it.hasNext()) {
            OMElement evaluatorElement = (OMElement) it.next();

            EvaluatorFactory ef = EvaluatorFactoryFinder.getInstance().
                    findEvaluatorFactory(evaluatorElement.getLocalName());

            if (ef == null) {
                handleException("Invalid configuration element: " +
                        evaluatorElement.getLocalName());
                return null;
            }

            Evaluator evaluator = ef.create(evaluatorElement);
            evaluators.add(evaluator);
        }

        if (evaluators.size() > 1) {
            o.setEvaluators(evaluators.toArray(new Evaluator[evaluators.size()]));
        } else {
            handleException("Two or more expressions " +
                    "should be provided under Or");

            return null;
        }
        return o;
    }

    private void handleException(String message) throws EvaluatorException {
        log.error(message);
        throw new EvaluatorException(message);
    }
}
