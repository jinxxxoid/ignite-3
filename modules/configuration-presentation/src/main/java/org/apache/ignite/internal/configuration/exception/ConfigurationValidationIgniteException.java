/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ignite.internal.configuration.exception;

import static org.apache.ignite.lang.ErrorGroups.CommonConfiguration.CONFIGURATION_VALIDATION_ERR;

import java.util.Collection;
import java.util.UUID;
import org.apache.ignite.configuration.validation.ConfigurationValidationException;
import org.apache.ignite.configuration.validation.ValidationIssue;
import org.apache.ignite.lang.IgniteException;

/**
 * Ignite exception wrapper for {@link ConfigurationValidationException}.
 */
public class ConfigurationValidationIgniteException extends IgniteException {
    private static final long serialVersionUID = 9106541244239857283L;

    private final ConfigurationValidationException validationEx;

    public ConfigurationValidationIgniteException(ConfigurationValidationException validationEx) {
        super(CONFIGURATION_VALIDATION_ERR, validationEx);
        this.validationEx = validationEx;
    }

    public ConfigurationValidationIgniteException(UUID traceId, ConfigurationValidationException validationEx) {
        super(traceId, CONFIGURATION_VALIDATION_ERR, validationEx);
        this.validationEx = validationEx;
    }

    public Collection<ValidationIssue> issues() {
        return validationEx.getIssues();
    }
}
