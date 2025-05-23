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

package org.apache.ignite.internal.configuration.processor.validation;

import javax.annotation.processing.ProcessingEnvironment;
import org.apache.ignite.configuration.annotation.AbstractConfiguration;
import org.apache.ignite.configuration.annotation.PolymorphicId;
import org.apache.ignite.internal.configuration.processor.ClassWrapper;

/**
 * Validator for the {@link AbstractConfiguration} annotation.
 */
public class AbstractConfigurationValidator extends Validator {
    public AbstractConfigurationValidator(ProcessingEnvironment processingEnvironment) {
        super(processingEnvironment);
    }

    @Override
    public void validate(ClassWrapper classWrapper) {
        if (classWrapper.getAnnotation(AbstractConfiguration.class) == null) {
            return;
        }

        assertHasCompatibleTopLevelAnnotation(classWrapper, AbstractConfiguration.class);

        assertHasNoSuperClass(classWrapper);

        assertNotContainsFieldAnnotatedWith(classWrapper, PolymorphicId.class);
    }
}
