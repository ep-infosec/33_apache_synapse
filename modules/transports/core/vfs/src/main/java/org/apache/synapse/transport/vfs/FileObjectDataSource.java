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
package org.apache.synapse.transport.vfs;

import org.apache.axiom.ext.activation.SizeAwareDataSource;
import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemException;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Data source that reads data from a VFS {@link FileObject}.
 * This class is similar to VFS' own FileObjectDataSource implementation, but in addition
 * implements {@link SizeAwareDataSource}.
 */
public class FileObjectDataSource implements SizeAwareDataSource {
    
    private final FileObject file;
    private final String contentType;

    public FileObjectDataSource(FileObject file, String contentType) {
        this.file = file;
        this.contentType = contentType;
    }

    @Override
    public long getSize() {
        try {
            return file.getContent().getSize();
        } catch (FileSystemException ex) {
            return -1;
        }
    }

    @Override
    public String getContentType() {
        return contentType;
    }

    @Override
    public String getName() {
        return file.getName().getURI();
    }

    @Override
    public InputStream getInputStream() throws IOException {
        return file.getContent().getInputStream();
    }

    @Override
    public OutputStream getOutputStream() throws IOException {
        return file.getContent().getOutputStream();
    }
}
