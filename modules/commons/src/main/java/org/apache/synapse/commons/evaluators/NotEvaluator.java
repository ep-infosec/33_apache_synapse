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

package org.apache.synapse.commons.evaluators;

/**
 * This encapsulates a boolean expression. This acts as not boolean operator.
 * It executes the boolean expression inside and return the NOT of this expression.</p>   
 *
 * <pre>
 * &lt;not&gt;
 *     one evaluator
 * &lt;/not&gt;
 * </pre>
 */
public class NotEvaluator implements Evaluator {
    private Evaluator evaluator;

    @Override
    public boolean evaluate(EvaluatorContext context) throws EvaluatorException {
        return !evaluator.evaluate(context);
    }

    @Override
    public String getName() {
        return EvaluatorConstants.NOT;
    }

    public void setEvaluator(Evaluator evaluator) {
        this.evaluator = evaluator;
    }

    public Evaluator getEvaluator() {
        return evaluator;
    }
}
