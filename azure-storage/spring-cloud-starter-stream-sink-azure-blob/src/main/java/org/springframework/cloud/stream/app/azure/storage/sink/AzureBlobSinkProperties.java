/*
 * Copyright 2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.cloud.stream.app.azure.storage.sink;

import static org.springframework.integration.handler.LoggingHandler.Level.*;

import javax.validation.constraints.NotNull;

import org.hibernate.validator.constraints.NotBlank;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.integration.handler.LoggingHandler;

/**
 * Configuration properties for the Azure Blob Sink module.
 *
 * @author Kyle Dunn
 */
@ConfigurationProperties("azure.blob")
public class AzureBlobSinkProperties {

    /**
     * The default Azure endpoint protocol.
     */
    private String defaultEndpointsProtocol = "http";

    /**
     * The Azure Storage Account name.
     */
    private String account;

    /**
     * The Azure Storage Account key.
     */
    private String key;

    /**
     * The Azure Storage Container name.
     */
    private String container;

    /**
     * The Azure Storage Blob name.
     */
    private String blob;

    /**
     * Create a container if it doesn't already exist.
     */
    private boolean createContainer = true;

    /**
     * Set container access policy to public.
     */
    private boolean publicPermission = true;
    
    /**
     * Specify if using an CloudAppendBlob
     */
    private boolean appendOnly = true;

    /**
     * Specify whether to silently overwrite 
     * existing an CloudAppendBlob with the same name
     */
    private boolean overwriteExistingAppend = false;

    public String getDefaultEndpointsProtocol() {
        return defaultEndpointsProtocol;
    }

    public void setDefaultEndpointsProtocol(String p) {
        this.defaultEndpointsProtocol = p;
    }

    public String getAccountName() {
        return account;
    }

    public void setAccountName(String n) {
        this.account = n;
    }

    public String getAccountKey() {
        return key;
    }

    public void setAccountKey(String k) {
        this.key = k;
    }

    public String getContainerName() {
        return container;
    }

    public void setContainerName(String c) {
        this.container = c;
    }

    public String getBlobName() {
        return blob;
    }

    public void setBlobName(String b) {
        this.blob = b;
    }

    public boolean getAutoCreateContainer() {
        return createContainer;
    }

    // Automatically create the container
    public void setAutoCreateContainer(Boolean b) {
        this.createContainer = b;
    }

    public boolean getPublicPermission() {
        return publicPermission;
    }

    // Include public access in the permissions object 
    public void setPublicPermission(Boolean b) {
        this.publicPermission = b;
    }

    public boolean getAppendOnly() {
        return appendOnly;
    }

    public void setAppendOnly(Boolean appendOnly) {
        this.appendOnly = appendOnly;
    }

    public boolean getOverwiteExistingAppend() {
        return overwriteExistingAppend;
    }

    public void setOverwriteExistingAppend(Boolean overwrite) {
        this.overwriteExistingAppend = overwrite;
    }
}
