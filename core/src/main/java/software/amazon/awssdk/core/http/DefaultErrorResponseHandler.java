/*
 * Copyright 2010-2018 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package software.amazon.awssdk.core.http;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import javax.xml.parsers.ParserConfigurationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;
import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.exception.SdkServiceException;
import software.amazon.awssdk.core.http.pipeline.stages.ApplyTransactionIdStage;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.runtime.transform.Unmarshaller;
import software.amazon.awssdk.core.util.StringUtils;
import software.amazon.awssdk.core.util.XpathUtils;
import software.amazon.awssdk.utils.IoUtils;

/**
 * Implementation of HttpResponseHandler that handles only error responses from Amazon Web Services.
 * A list of unmarshallers is passed into the constructor, and while handling a response, each
 * unmarshaller is tried, in order, until one is found that can successfully unmarshall the error
 * response.  If no unmarshaller is found that can unmarshall the error response, a generic
 * SdkServiceException is created and populated with the AWS error response information (error
 * message, AWS error code, AWS request ID, etc).
 */
@SdkProtectedApi
public class DefaultErrorResponseHandler implements HttpResponseHandler<SdkServiceException> {
    private static final Logger log = LoggerFactory.getLogger(DefaultErrorResponseHandler.class);

    /**
     * The list of error response unmarshallers to try to apply to error responses.
     */
    private List<Unmarshaller<SdkServiceException, Node>> unmarshallerList;

    /**
     * Constructs a new DefaultErrorResponseHandler that will handle error responses from Amazon
     * services using the specified list of unmarshallers. Each unmarshaller will be tried, in
     * order, until one is found that can unmarshall the error response.
     *
     * @param unmarshallerList The list of unmarshallers to try using when handling an error
     *                         response.
     */
    public DefaultErrorResponseHandler(
            List<Unmarshaller<SdkServiceException, Node>> unmarshallerList) {
        this.unmarshallerList = unmarshallerList;
    }

    @Override
    public SdkServiceException handle(HttpResponse errorResponse,
                                      ExecutionAttributes executionAttributes) throws Exception {
        SdkServiceException exception = createServiceException(errorResponse);
        if (exception == null) {
            throw new SdkClientException("Unable to unmarshall error response from service");
        }
        exception.headers(errorResponse.getHeaders());
        if (StringUtils.isNullOrEmpty(exception.errorCode())) {
            exception.errorCode(errorResponse.getStatusCode() + " " + errorResponse.getStatusText());
        }
        return exception;
    }

    private SdkServiceException createServiceException(HttpResponse errorResponse) throws Exception {
        // Try to parse the error response as XML
        final Document document = documentFromContent(errorResponse.getContent(), idString(errorResponse));

        /*
         * We need to select which exception unmarshaller is the correct one to
         * use from all the possible exceptions this operation can throw.
         * Currently we rely on the unmarshallers to return null if they can't
         * unmarshall the response, but we might need something a little more
         * sophisticated in the future.
         */
        for (Unmarshaller<SdkServiceException, Node> unmarshaller : unmarshallerList) {
            SdkServiceException exception = unmarshaller.unmarshall(document);
            if (exception != null) {
                exception.statusCode(errorResponse.getStatusCode());
                return exception;
            }
        }
        return null;
    }

    private Document documentFromContent(InputStream content, String idString)
            throws ParserConfigurationException, SAXException, IOException {
        try {
            return parseXml(contentToString(content, idString), idString);
        } catch (Exception e) {
            // Generate an empty document to make the unmarshallers happy. Ultimately the default
            // unmarshaller will be called to unmarshall into the service base exception.
            return XpathUtils.documentFrom("<empty/>");
        }
    }

    private String contentToString(InputStream content, String idString) throws Exception {
        try {
            return IoUtils.toString(content);
        } catch (Exception e) {
            log.debug(String.format("Unable to read input stream to string (%s)", idString), e);
            throw e;
        }
    }

    private Document parseXml(String xml, String idString) throws Exception {
        try {
            return XpathUtils.documentFrom(xml);
        } catch (Exception e) {
            log.debug("Unable to parse HTTP response ({}) content to XML document '{}' ", idString, xml, e);
            throw e;
        }
    }

    private String idString(HttpResponse errorResponse) {
        StringBuilder idString = new StringBuilder();
        try {
            errorResponse.getRequest()
                         .firstMatchingHeader(ApplyTransactionIdStage.HEADER_SDK_TRANSACTION_ID)
                         .ifPresent(h -> idString.append("Invocation Id:").append(h));
            if (errorResponse.getHeaders().containsKey(X_AMZN_REQUEST_ID_HEADER)) {
                if (idString.length() > 0) {
                    idString.append(", ");
                }
                idString.append("Request Id:").append(errorResponse.getHeaders().get(X_AMZN_REQUEST_ID_HEADER));
            }
        } catch (NullPointerException npe) {
            log.debug("Error getting Request or Invocation ID from response", npe);
        }
        return idString.length() > 0 ? idString.toString() : "Unknown";
    }

    /**
     * Since this response handler completely consumes all the data from the underlying HTTP
     * connection during the handle method, we don't need to keep the HTTP connection open.
     *
     * @see HttpResponseHandler#needsConnectionLeftOpen()
     */
    public boolean needsConnectionLeftOpen() {
        return false;
    }

}
