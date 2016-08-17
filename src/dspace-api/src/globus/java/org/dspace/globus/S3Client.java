/*******************************************************************************
 * Copyright 2016 University of Chicago. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/

package org.dspace.globus;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.UUID;

import org.apache.log4j.Logger;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.auth.InstanceProfileCredentialsProvider;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;

public class S3Client {

    private static final Logger logger = Logger.getLogger(Globus.class);

	private String bucketName;
	private AmazonS3 s3Client;

	public S3Client(String bucketName, String aws_key, String aws_secret){
    	this.bucketName = bucketName;

    	if (aws_key != null && aws_secret != null){
    		s3Client = new AmazonS3Client(new BasicAWSCredentials(aws_key,aws_secret));
    	} else {
    		s3Client = new AmazonS3Client(new InstanceProfileCredentialsProvider());
    	}
	}

	public String createObject(String metadata){
        UUID key = UUID.randomUUID();
		try {
			byte [] metadataBytes = metadata.getBytes("UTF-8");
			InputStream stream = new ByteArrayInputStream(metadataBytes);
			ObjectMetadata s3ObjectMeta = new ObjectMetadata();
			s3ObjectMeta.setContentLength(metadataBytes.length);
			s3Client.putObject(new PutObjectRequest(bucketName, key.toString(), stream, s3ObjectMeta));
            return key.toString();
        } catch (AmazonServiceException ase) {
            logger.error("AmazonServiceException: " + ase.getMessage());
        } catch (AmazonClientException ace) {
        	logger.error("AmazonClientException: " + ace.getMessage());
        } catch (Exception e){
        	logger.error("Exception: " + e.getMessage());
        }
		return null;
    }


	public void deleteObject(String key){
		try{
            s3Client.deleteObject(bucketName, key);
        } catch (AmazonServiceException ase) {
        	logger.error("AmazonServiceException: " + ase.getMessage());
        } catch (AmazonClientException ace) {
        	logger.error("AmazonClientException: " + ace.getMessage());
        }
	}

}
