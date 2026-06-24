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

// CHECKSTYLE:OFF
import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import jakarta.enterprise.inject.spi.AnnotatedConstructor;
import jakarta.enterprise.inject.spi.AnnotatedField;
import jakarta.enterprise.inject.spi.AnnotatedMethod;
import jakarta.enterprise.inject.spi.AnnotatedType;

/**
 * An implementation of {@link AnnotatedType} to be used in CDI life cycle events and
 * {@link org.apache.deltaspike.core.util.metadata.builder.AnnotatedTypeBuilder}.
 */
class AnnotatedTypeImpl<X> extends AnnotatedImpl implements AnnotatedType<X> {

	private final Set<AnnotatedConstructor<X>> constructors;
	private final Set<AnnotatedField<? super X>> fields;
	private final Set<AnnotatedMethod<? super X>> methods;

	private final Class<X> javaClass;

	/**
	 * We make sure that there is a NewAnnotatedMember for every public method/field/constructor.
	 * <p/>
	 * If annotation have been added to other methods as well we add them to
	 */
	AnnotatedTypeImpl(final Class<X> clazz, final AnnotationStore typeAnnotations, final Map<Field, AnnotationStore> fieldAnnotations,
			final Map<Method, AnnotationStore> methodAnnotations,
			final Map<Method, Map<Integer, AnnotationStore>> methodParameterAnnotations,
			final Map<Constructor<?>, AnnotationStore> constructorAnnotations,
			final Map<Constructor<?>, Map<Integer, AnnotationStore>> constructorParameterAnnotations, final Map<Field, Type> fieldTypes,
			final Map<Method, Map<Integer, Type>> methodParameterTypes,
			final Map<Constructor<?>, Map<Integer, Type>> constructorParameterTypes) {
		super(clazz, typeAnnotations, null, null);
		javaClass = clazz;
		constructors = new HashSet<>();
		final Set<Constructor<?>> cset = new HashSet<>();
		final Set<Method> mset = new HashSet<>();
		final Set<Field> fset = new HashSet<>();
		for (final Constructor<?> c : clazz.getConstructors()) {
			final AnnotatedConstructor<X> nc = new AnnotatedConstructorImpl<>(this, c, constructorAnnotations.get(c),
					constructorParameterAnnotations.get(c), constructorParameterTypes.get(c));
			constructors.add(nc);
			cset.add(c);
		}
		for (final Map.Entry<Constructor<?>, AnnotationStore> c : constructorAnnotations.entrySet()) {
			if (!cset.contains(c.getKey())) {
				final AnnotatedConstructor<X> nc = new AnnotatedConstructorImpl<>(this, c.getKey(), c.getValue(),
						constructorParameterAnnotations.get(c.getKey()), constructorParameterTypes.get(c.getKey()));
				constructors.add(nc);
			}
		}
		methods = new HashSet<>();
		for (final Method m : clazz.getMethods()) {
			if (!m.getDeclaringClass().equals(Object.class) && !m.getDeclaringClass().equals(Annotation.class)) {
				final AnnotatedMethodImpl<X> met = new AnnotatedMethodImpl<>(this, m, methodAnnotations.get(m),
						methodParameterAnnotations.get(m), methodParameterTypes.get(m));
				methods.add(met);
				mset.add(m);
			}
		}
		for (final Map.Entry<Method, AnnotationStore> c : methodAnnotations.entrySet()) {
			if (!c.getKey().getDeclaringClass().equals(Object.class) && !mset.contains(c.getKey())) {
				final AnnotatedMethodImpl<X> nc = new AnnotatedMethodImpl<>(this, c.getKey(), c.getValue(),
						methodParameterAnnotations.get(c.getKey()), methodParameterTypes.get(c.getKey()));
				methods.add(nc);
			}
		}
		fields = new HashSet<>();
		for (final Field f : clazz.getFields()) {
			final AnnotatedField<X> b = new AnnotatedFieldImpl<>(this, f, fieldAnnotations.get(f), fieldTypes.get(f));
			fields.add(b);
			fset.add(f);
		}
		for (final Map.Entry<Field, AnnotationStore> e : fieldAnnotations.entrySet()) {
			if (!fset.contains(e.getKey())) {
				fields.add(new AnnotatedFieldImpl<>(this, e.getKey(), e.getValue(), fieldTypes.get(e.getKey())));
			}
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Set<AnnotatedConstructor<X>> getConstructors() {
		return Collections.unmodifiableSet(constructors);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Set<AnnotatedField<? super X>> getFields() {
		return Collections.unmodifiableSet(fields);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Class<X> getJavaClass() {
		return javaClass;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Set<AnnotatedMethod<? super X>> getMethods() {
		return Collections.unmodifiableSet(methods);
	}
}
// CHECKSTYLE:ON
