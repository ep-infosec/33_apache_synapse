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

package samples.ejb;

import javax.ejb.Remove;
import javax.ejb.Stateful;

/**
 * Implementation of the shopping cart stateful session bean which keeps an state internally.
 */
@Stateful
public class ShoppingCartBean implements ShoppingCart {

    private float total;

    private int itemCount;

    public void addItem(String itemId, int count) {
        total += getPriceById(itemId) * count;
        itemCount += count;
    }
 
    @Remove
    public float getTotal() {
        return total;
    }

    public int getItemCount() {
        return itemCount;
    }

    private float getPriceById(String itemId) {
        try {
            return Integer.parseInt(itemId.substring(0, itemId.length() - 1));
        } catch (NumberFormatException e) {
            return (float) (Math.random() * 100);
        }
    }
}
