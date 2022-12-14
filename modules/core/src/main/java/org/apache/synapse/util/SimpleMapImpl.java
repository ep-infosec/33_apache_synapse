/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.synapse.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.activation.DataHandler;
import javax.xml.namespace.QName;

import org.apache.axiom.attachments.ByteArrayDataSource;
import org.apache.axiom.om.OMAbstractFactory;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMFactory;
import org.apache.axiom.om.OMNamespace;
import org.apache.axiom.om.OMNode;
import org.apache.axiom.om.OMText;


public class SimpleMapImpl extends HashMap implements SimpleMap {

    private static final OMNamespace attrNS = OMAbstractFactory.getOMFactory().createOMNamespace("", "");
    private static final String TYPE = "type";

    private static final String NAME = "name";

    private static final String ENTRY = "entry";

    private static final String SHORT = "short";

    private static final String LONG = "long";

    private static final String DOUBLE = "double";
    private static final String INTEGER = "int";
    private static final String FLOAT = "float";

    private static final String BYTEARRAY = "byte[]";

    private static final String BYTE = "byte";

    private static final String STRING = "string";

    private static final String BOOLEAN = "boolean";

    private static final String CHAR = "char";

    private static final long serialVersionUID = 1L;

    public SimpleMapImpl() {
        super();
    }

    @Override
    public Object get(String name) {
        return this.get((Object) name);
    }

    @Override
    public boolean getBoolean(String name) {
        Object o = this.get((Object) name);
        if (o instanceof Boolean) {
            return (Boolean) o;
        } else {
            throw new RuntimeException("getBoolean(" + name + "): "
                    + o.getClass().getName() + " is not an instance of Boolean");
        }
    }

    @Override
    public byte getByte(String name) {
        Object o = this.get((Object) name);
        if (o instanceof Byte) {
            return (Byte) o;
        } else {
            throw new RuntimeException("getByte(" + name + "): "
                    + o.getClass().getName() + " is not an instance of Byte");
        }
    }

    @Override
    public byte[] getBytes(String name) {
        Object o = this.get((Object) name);
        if (o instanceof byte[]) {
            return (byte[]) o;
        } else {
            throw new RuntimeException("getByteArray(" + name + "): "
                    + o.getClass().getName() + " is not an instance of byte[]");
        }
    }

    @Override
    public char getChar(String name) {
        Object o = this.get((Object) name);
        if (o instanceof Character) {
            return (Character) o;
        } else {
            throw new RuntimeException("getChar(" + name + "): "
                    + o.getClass().getName()
                    + " is not an instance of Character");
        }
    }

    @Override
    public double getDouble(String name) {
        Object o = this.get((Object) name);
        if (o instanceof Double) {
            return (Double) o;
        } else {
            throw new RuntimeException("getDouble(" + name + "): "
                    + o.getClass().getName() + " is not an instance of Double");
        }
    }

    @Override
    public float getFloat(String name) {
        Object o = this.get((Object) name);
        if (o instanceof Float) {
            return (Float) o;
        } else {
            throw new RuntimeException("getFloat(" + name + "): "
                    + o.getClass().getName() + " is not an instance of Float");
        }
    }

    @Override
    public int getInt(String name) {
        Object o = this.get((Object) name);
        if (o instanceof Integer) {
            return (Integer) o;
        } else {
            throw new RuntimeException("getInt(" + name + "): "
                    + o.getClass().getName() + " is not an instance of Integer");
        }
    }

    @Override
    public long getLong(String name) {
        Object o = this.get((Object) name);
        if (o instanceof Long) {
            return (Long) o;
        } else {
            throw new RuntimeException("getLong(" + name + "): "
                    + o.getClass().getName() + " is not an instance of Long");
        }
    }

    @Override
    public short getShort(String name) {
        Object o = this.get((Object) name);
        if (o instanceof Short) {
            return (Short) o;
        } else {
            throw new RuntimeException("getShort(" + name + "): "
                    + o.getClass().getName() + " is not an instance of Short");
        }
    }

    @Override
    public String getString(String name) {
        Object o = this.get((Object) name);
        if (o instanceof String) {
            return ((String) o);
        } else {
            throw new RuntimeException("getString(" + name + "): "
                    + o.getClass().getName() + " is not an instance of String");
        }
    }

    @Override
    public void put(String name, Object value) {
        this.put((Object) name, value);
    }

    @Override
    public void putBoolean(String name, boolean b) {
        this.put((Object) name, b);
    }

    @Override
    public void putByte(String name, byte b) {
        this.put((Object) name, b);
    }

    @Override
    public void putBytes(String name, byte[] bytes) {
        this.put((Object) name, bytes);
    }

    @Override
    public void putChar(String name, char c) {
        this.put((Object) name, c);
    }

    @Override
    public void putDouble(String name, double d) {
        this.put((Object) name, d);
    }

    @Override
    public void putFloat(String name, float fl) {
        this.put((Object) name, fl);
    }

    @Override
    public void putInt(String name, int i) {
        this.put((Object) name, i);
    }

    @Override
    public void putLong(String name, long l) {
        this.put((Object) name, l);
    }

    @Override
    public void putShort(String name, short s) {
        this.put((Object) name, s);
    }

    @Override
    public void putString(String name, String value) {
        this.put((Object) name, value);
    }

    public OMElement getOMElement() {
        return getOMElement(OMAbstractFactory.getOMFactory());
    }

    public OMElement getOMElement(OMFactory fac) {
        OMElement mapElement = fac.createOMElement(PayloadHelper.MAPELT);

        for (Object entryObj : this.entrySet()) {
            Object key = ((Map.Entry) entryObj).getKey();
            Object o = ((Map.Entry) entryObj).getValue();

            if (key instanceof String) {
                OMElement entry = fac.createOMElement(new QName(
                        PayloadHelper.AXIOMPAYLOADNS, ENTRY), mapElement);
                entry.addAttribute(NAME, (String) key, attrNS);

                if (o instanceof Character) {
                    entry.addAttribute(TYPE, CHAR, attrNS);
                    entry.setText(o.toString());
                } else if (o instanceof Boolean) {
                    entry.addAttribute(TYPE, BOOLEAN, attrNS);
                    entry.setText(o.toString());
                } else if (o instanceof String) {
                    entry.addAttribute(TYPE, STRING, attrNS);
                    entry.setText(o.toString());
                } else if (o instanceof Byte) {
                    entry.addAttribute(TYPE, BYTE, attrNS);
                    entry.setText(o.toString());
                } else if (o instanceof byte[]) {
                    entry.addAttribute(TYPE, BYTEARRAY, attrNS);
                    OMText text = fac.createOMText(new DataHandler(
                            new ByteArrayDataSource((byte[]) o)), true);
                    entry.addChild(text);
                } else if (o instanceof Float) {
                    entry.addAttribute(TYPE, FLOAT, attrNS);
                    entry.setText(o.toString());
                } else if (o instanceof Double) {
                    entry.addAttribute(TYPE, DOUBLE, attrNS);
                    entry.setText(o.toString());
                } else if (o instanceof Long) {
                    entry.addAttribute(TYPE, LONG, attrNS);
                    entry.setText(o.toString());
                } else if (o instanceof Short) {
                    entry.addAttribute(TYPE, SHORT, attrNS);
                    entry.setText(o.toString());
                } else if (o instanceof Integer) {
                    entry.addAttribute(TYPE, INTEGER, attrNS);
                    entry.setText(o.toString());
                }

            } else {
                // shouldn't be any non-string keys. Ignore!
            }
        }

        return mapElement;
    }

    // create an instance from an OMElement (if its the right shape!!!)
    public SimpleMapImpl(OMElement el) {
        super();
        if (el.getQName().equals(PayloadHelper.MAPELT)) {
            for (Iterator it = el.getChildElements(); it.hasNext(); ) {
                OMElement child = (OMElement)it.next();
                if (child.getLocalName().equals(ENTRY)) {
                    String name = child.getAttributeValue(new QName("",NAME));
                    String type = child.getAttributeValue(new QName("", TYPE));
                    if (type == null || name == null) {
                        //bad!
                        continue;
                    }
                    OMNode data = child.getFirstOMChild();
                    if (data.getType() != OMNode.TEXT_NODE) {
                        continue; // BAD!
                    }
                    OMText text = (OMText)data;
                    if (type.equals(INTEGER)) {
                        this.put(name, new Integer(text.getText()));
                    } else if (type.equals(CHAR)) {
                        this.put(name, (text.getText().charAt(0)));
                    } else if (type.equals(DOUBLE)) {
                        this.put(name, new Double(text.getText()));
                    } else if (type.equals(FLOAT)) {
                        this.put(name, new Float(text.getText()));
                    } else if (type.equals(BYTE)) {
                        this.put(name, text.getText().getBytes()[0]);
                    } else if (type.equals(SHORT)) {
                        this.put(name, new Short(text.getText()));
                    } else if (type.equals(LONG)) {
                        this.put(name, new Long(text.getText()));
                    } else if (type.equals(STRING)) {
                        this.put(name, text.getText());
                    } else if (type.equals(BYTEARRAY)) {
                        DataHandler dh = (DataHandler) text.getDataHandler();
                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        try {
                            dh.writeTo(baos);
                            this.put(name, baos.toByteArray());
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }
    }

}
