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

import java.io.IOException;
import java.net.URISyntaxException;
import java.security.InvalidKeyException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.cloud.stream.messaging.Sink;
import org.springframework.context.annotation.DependsOn;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessagingException;

// Include the following imports to use blob APIs.
import com.microsoft.azure.storage.CloudStorageAccount;
import com.microsoft.azure.storage.StorageException;
import com.microsoft.azure.storage.blob.BlobContainerPermissions;
import com.microsoft.azure.storage.blob.BlobContainerPublicAccessType;
import com.microsoft.azure.storage.blob.CloudBlob;
import com.microsoft.azure.storage.blob.CloudAppendBlob;
import com.microsoft.azure.storage.blob.CloudBlobClient;
import com.microsoft.azure.storage.blob.CloudBlobContainer;
import com.microsoft.azure.storage.blob.CloudBlockBlob;


/**
 * @author Kyle Dunn
 */
@EnableBinding(Sink.class)
@EnableConfigurationProperties(AzureBlobSinkProperties.class)
@SpringBootApplication
public class AzureBlobSinkConfiguration {

    @Autowired
    private AzureBlobSinkProperties properties;

    private static Log logger = LogFactory.getLog(AzureBlobSinkConfiguration.class);

    private CloudBlob blobService;

    @Autowired
    public void setBlobService() throws StorageException, URISyntaxException, InvalidKeyException {
        // Define the connection-string with your values
        final String storageConnectionString =
            "DefaultEndpointsProtocol=" + this.properties.getDefaultEndpointsProtocol() +
            ";AccountName=" + this.properties.getAccountName() +
            ";AccountKey=" + this.properties.getAccountKey();

        // Setup the cloud storage account.
        CloudStorageAccount account = CloudStorageAccount.parse(storageConnectionString);

        logger.info("getBlobService() : using account " + this.properties.getAccountName());

        // Create a blob service client
        CloudBlobClient blobClient = account.createCloudBlobClient();
        
        // Get a reference to a container
        // The container name must be lower case
        CloudBlobContainer container = blobClient.getContainerReference(this.properties.getContainerName().toLowerCase());

        logger.info("getBlobService() : using container " + this.properties.getContainerName());

        if (this.properties.getAutoCreateContainer()) {
            container.createIfNotExists();
        }            

        // Make the container public
        if (this.properties.getPublicPermission()) {
            logger.info("getBlobService() : making container publicly accessible");

            // Create a permissions object
            BlobContainerPermissions containerPermissions = new BlobContainerPermissions();

            // Include public access in the permissions object
            containerPermissions.setPublicAccess(BlobContainerPublicAccessType.CONTAINER);

            // Set the permissions on the container
            container.uploadPermissions(containerPermissions);
        }

        logger.info("getBlobService() : using blob name " + this.properties.getBlobName());
        
        if (this.properties.getAppendOnly()) {
            this.blobService = container.getAppendBlobReference(this.properties.getBlobName());
            if (this.properties.getOverwiteExistingAppend()) {
                ((CloudAppendBlob) blobService).createOrReplace();
            }
        }
        else {
            this.blobService = container.getBlockBlobReference(this.properties.getBlobName());
        }
    }

    @ServiceActivator(inputChannel=Sink.INPUT)
    public void pushToAzureBlob(Message<?> message) throws StorageException, IOException {
        // Upload the payload to the blob
        if (this.properties.getAppendOnly()) {
            ((CloudAppendBlob) blobService).appendText(message.getPayload().toString());
        }
        else {
            ((CloudBlockBlob) blobService).uploadText(message.getPayload().toString());
        }
    }

    public static void main(String[] args) {
        SpringApplication.run(AzureBlobSinkConfiguration.class, args);
    }

}
