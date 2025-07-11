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

package org.apache.ignite.internal.network.processor.serialization;

import static org.apache.ignite.internal.network.processor.MessageGeneratorUtils.addByteArrayPostfix;
import static org.apache.ignite.internal.network.processor.MessageGeneratorUtils.propertyName;

import com.squareup.javapoet.CodeBlock;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import org.apache.ignite.internal.network.annotations.Marshallable;
import org.apache.ignite.internal.network.processor.ProcessingException;
import org.apache.ignite.internal.network.serialization.MessageCollectionItemType;
import org.apache.ignite.internal.network.serialization.MessageWriter;

/**
 * Class for resolving {@link MessageWriter} "write*" methods for the corresponding message field type.
 */
class MessageWriterMethodResolver {
    /** Method name resolver. */
    private final BaseMethodNameResolver methodNameResolver;

    /** Type converter. */
    private final MessageCollectionItemTypeConverter typeConverter;

    /**
     * Constructor.
     *
     * @param processingEnvironment Processing environment.
     */
    MessageWriterMethodResolver(ProcessingEnvironment processingEnvironment) {
        methodNameResolver = new BaseMethodNameResolver(processingEnvironment);
        typeConverter = new MessageCollectionItemTypeConverter(processingEnvironment);
    }

    /**
     * Resolves the "write" method by the type of the given message's getter method.
     *
     * @param getter getter method
     * @return code for the method for writing a field based on the getter type
     */
    CodeBlock resolveWriteMethod(ExecutableElement getter) {
        if (!getter.getParameters().isEmpty() || getter.getReturnType().getKind() == TypeKind.VOID) {
            var errorMsg = String.format(
                    "Invalid getter method %s: getters must return a value and must not have any parameters", getter
            );

            throw new ProcessingException(errorMsg, null, getter);
        }

        TypeMirror getterReturnType = getter.getReturnType();

        String getterName = getter.getSimpleName().toString();
        String propertyName = propertyName(getter);

        if (getter.getAnnotation(Marshallable.class) != null) {
            getterName = addByteArrayPostfix(getterName);
            return CodeBlock.builder()
                    .add("writeByteArray($S, message.$L())", propertyName, getterName)
                    .build();
        }

        String methodName = methodNameResolver.resolveBaseMethodName(getterReturnType);

        switch (methodName) {
            case "ObjectArray":
                return resolveWriteObjectArray((ArrayType) getterReturnType, getterName, propertyName);
            case "Collection":
                return resolveWriteCollection((DeclaredType) getterReturnType, getterName, propertyName);
            case "List":
                return resolveWriteList((DeclaredType) getterReturnType, getterName, propertyName);
            case "Set":
                return resolveWriteSet((DeclaredType) getterReturnType, getterName, propertyName);
            case "Map":
                return resolveWriteMap((DeclaredType) getterReturnType, getterName, propertyName);
            default:
                return CodeBlock.builder()
                        .add("write$L($S, message.$L())", methodName, propertyName, getterName)
                        .build();
        }
    }

    /**
     * Creates a {@link MessageWriter#writeObjectArray(String, Object[], MessageCollectionItemType)} method call.
     */
    private CodeBlock resolveWriteObjectArray(ArrayType parameterType, String parameterName, String propertyName) {
        TypeMirror componentType = parameterType.getComponentType();

        return CodeBlock.builder()
                .add(
                        "writeObjectArray($S, message.$L(), $T.$L)",
                        propertyName,
                        parameterName,
                        MessageCollectionItemType.class,
                        typeConverter.fromTypeMirror(componentType)
                )
                .build();
    }

    /**
     * Creates a {@link MessageWriter#writeCollection(String, Collection, MessageCollectionItemType)} method call.
     */
    private CodeBlock resolveWriteCollection(DeclaredType parameterType, String parameterName, String propertyName) {
        TypeMirror collectionGenericType = parameterType.getTypeArguments().get(0);

        return CodeBlock.builder()
                .add(
                        "writeCollection($S, message.$L(), $T.$L)",
                        propertyName,
                        parameterName,
                        MessageCollectionItemType.class,
                        typeConverter.fromTypeMirror(collectionGenericType)
                )
                .build();
    }

    /**
     * Creates a {@link MessageWriter#writeList(String, List, MessageCollectionItemType)} method call.
     */
    private CodeBlock resolveWriteList(DeclaredType parameterType, String parameterName, String propertyName) {
        TypeMirror listGenericType = parameterType.getTypeArguments().get(0);

        return CodeBlock.builder()
                .add(
                        "writeList($S, message.$L(), $T.$L)",
                        propertyName,
                        parameterName,
                        MessageCollectionItemType.class,
                        typeConverter.fromTypeMirror(listGenericType)
                )
                .build();
    }

    /**
     * Creates a {@link MessageWriter#writeSet(String, Set, MessageCollectionItemType)} method call.
     */
    private CodeBlock resolveWriteSet(DeclaredType parameterType, String parameterName, String propertyName) {
        TypeMirror setGenericType = parameterType.getTypeArguments().get(0);

        return CodeBlock.builder()
                .add(
                        "writeSet($S, message.$L(), $T.$L)",
                        propertyName,
                        parameterName,
                        MessageCollectionItemType.class,
                        typeConverter.fromTypeMirror(setGenericType)
                )
                .build();
    }

    /**
     * Creates a {@link MessageWriter#writeMap(String, Map, MessageCollectionItemType, MessageCollectionItemType)} method call.
     */
    private CodeBlock resolveWriteMap(DeclaredType parameterType, String parameterName, String propertyName) {
        List<? extends TypeMirror> typeArguments = parameterType.getTypeArguments();

        MessageCollectionItemType mapKeyType = typeConverter.fromTypeMirror(typeArguments.get(0));
        MessageCollectionItemType mapValueType = typeConverter.fromTypeMirror(typeArguments.get(1));

        return CodeBlock.builder()
                .add(
                        "writeMap($S, message.$L(), $T.$L, $T.$L)",
                        propertyName,
                        parameterName,
                        MessageCollectionItemType.class,
                        mapKeyType,
                        MessageCollectionItemType.class,
                        mapValueType
                )
                .build();
    }
}
