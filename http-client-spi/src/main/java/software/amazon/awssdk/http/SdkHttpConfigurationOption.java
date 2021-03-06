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

package software.amazon.awssdk.http;

import java.time.Duration;
import software.amazon.awssdk.annotations.ReviewBeforeRelease;
import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.utils.AttributeMap;

/**
 * Type safe key for an HTTP related configuration option. These options are used for service specific configuration
 * and are treated as hints for the underlying HTTP implementation for better defaults. If an implementation does not support
 * a particular option, they are free to ignore it.
 *
 * @param <T> Type of option
 * @see AttributeMap
 */
@SdkProtectedApi
public final class SdkHttpConfigurationOption<T> extends AttributeMap.Key<T> {

    /**
     * Timeout for each read to the underlying socket.
     */
    public static final SdkHttpConfigurationOption<Duration> SOCKET_TIMEOUT =
            new SdkHttpConfigurationOption<>("SocketTimeout", Duration.class);

    /**
     * Timeout for establishing a connection to a remote service.
     */
    public static final SdkHttpConfigurationOption<Duration> CONNECTION_TIMEOUT =
            new SdkHttpConfigurationOption<>("ConnectionTimeout", Duration.class);

    /**
     * Maximum number of connections allowed in a connection pool.
     */
    public static final SdkHttpConfigurationOption<Integer> MAX_CONNECTIONS =
            new SdkHttpConfigurationOption<>("MaxConnections", Integer.class);

    /**
     * Whether or not to use strict hostname verification when establishing the SSL connection. For almost all services this
     * should be true. S3 however uses wildcard certificates for virtual bucket address (bucketname.s3.amazonaws.com) and
     * needs to disable strict hostname verification to allow for wildcard certs.
     */
    @ReviewBeforeRelease("This does not appear to be needed anymore for S3")
    public static final SdkHttpConfigurationOption<Boolean> USE_STRICT_HOSTNAME_VERIFICATION =
            new SdkHttpConfigurationOption<>("UseStrictHostnameVerification", Boolean.class);

    private static final Duration DEFAULT_SOCKET_TIMEOUT = Duration.ofSeconds(50);

    private static final Duration DEFAULT_CONNECTION_TIMEOUT = Duration.ofSeconds(10);

    private static final int DEFAULT_MAX_CONNECTIONS = 50;

    private static final Boolean DEFAULT_USE_STRICT_HOSTNAME_VERIFICATION = Boolean.TRUE;

    @ReviewBeforeRelease("Confirm defaults")
    public static final AttributeMap GLOBAL_HTTP_DEFAULTS = AttributeMap
            .builder()
            .put(SOCKET_TIMEOUT, DEFAULT_SOCKET_TIMEOUT)
            .put(CONNECTION_TIMEOUT, DEFAULT_CONNECTION_TIMEOUT)
            .put(MAX_CONNECTIONS, DEFAULT_MAX_CONNECTIONS)
            .put(USE_STRICT_HOSTNAME_VERIFICATION, DEFAULT_USE_STRICT_HOSTNAME_VERIFICATION)
            .build();

    private final String name;

    private SdkHttpConfigurationOption(String name, Class<T> clzz) {
        super(clzz);
        this.name = name;
    }

    /**
     * Note that the name is mainly used for debugging purposes. Two option key objects with the same name do not represent
     * the same option. Option keys are compared by reference when obtaining a value from an {@link AttributeMap}.
     *
     * @return Name of this option key.
     */
    public String name() {
        return name;
    }

    @Override
    public String toString() {
        return name;
    }
}

