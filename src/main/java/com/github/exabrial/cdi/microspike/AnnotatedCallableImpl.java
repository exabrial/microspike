/*
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership. The ASF licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License
 * at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS"
 * BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package com.github.exabrial.cdi.microspike;

import java.lang.reflect.Member;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import jakarta.enterprise.inject.spi.AnnotatedCallable;
import jakarta.enterprise.inject.spi.AnnotatedParameter;
import jakarta.enterprise.inject.spi.AnnotatedType;

/**
 * Implementation of {@link AnnotatedCallable}.
 */
abstract class AnnotatedCallableImpl<X, Y extends Member> extends AnnotatedMemberImpl<X, Y> implements AnnotatedCallable<X> {

	private final List<AnnotatedParameter<X>> parameters;

	protected AnnotatedCallableImpl(final AnnotatedType<X> declaringType, final Y member, final Class<?> memberType,
			final Class<?>[] parameterTypes, final Type[] genericTypes, final AnnotationStore annotations,
			final Map<Integer, AnnotationStore> parameterAnnotations, final Type genericType,
			final Map<Integer, Type> parameterTypeOverrides) {
		super(declaringType, member, memberType, annotations, genericType, null);
		parameters = getAnnotatedParameters(this, parameterTypes, genericTypes, parameterAnnotations, parameterTypeOverrides);
	}

	private static <X, Y extends Member> List<AnnotatedParameter<X>> getAnnotatedParameters(final AnnotatedCallableImpl<X, Y> callable,
			final Class<?>[] parameterTypes, final Type[] genericTypes, final Map<Integer, AnnotationStore> parameterAnnotations,
			final Map<Integer, Type> parameterTypeOverrides) {
		final List<AnnotatedParameter<X>> parameters = new ArrayList<>();
		final int len = parameterTypes.length;

		for (int i = 0; i < len; ++i) {
			final AnnotationBuilder builder = new AnnotationBuilder();
			if (parameterAnnotations != null && parameterAnnotations.containsKey(i)) {
				builder.addAll(parameterAnnotations.get(i));
			}
			Type over = null;
			if (parameterTypeOverrides != null) {
				over = parameterTypeOverrides.get(i);
			}
			final AnnotatedParameterImpl<X> p = new AnnotatedParameterImpl<>(callable, parameterTypes[i], i, builder.create(),
					genericTypes[i], over);

			parameters.add(p);
		}
		return parameters;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public List<AnnotatedParameter<X>> getParameters() {
		return Collections.unmodifiableList(parameters);
	}

	public AnnotatedParameter<X> getParameter(final int index) {
		return parameters.get(index);

	}

}
