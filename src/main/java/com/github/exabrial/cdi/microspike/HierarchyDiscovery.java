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

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import jakarta.enterprise.inject.Typed;

/**
 * Utility class for resolving all bean types from a given type.
 */
// X TODO: Look at merging this with ClassUtils perhaps X TODO: JavaDoc X TODO review
@Typed()
public class HierarchyDiscovery {
	private final Type type;

	private Map<Type, Class<?>> types;

	public HierarchyDiscovery(final Type type) {
		this.type = type;
	}

	protected void add(final Class<?> clazz, final Type type) {
		types.put(type, clazz);
	}

	public Set<Type> getTypeClosure() {
		if (types == null) {
			init();
		}
		// Return an independent set with no ties to the BiMap used
		return new HashSet<>(types.keySet());
	}

	private void init() {
		types = new HashMap<>();
		try {
			discoverTypes(type);
		} catch (final StackOverflowError e) {
			final Logger logger = Logger.getLogger(HierarchyDiscovery.class.getName());

			logger.log(Level.WARNING, "type: " + type.toString(), e);
			Thread.dumpStack();
			throw e;
		}
	}

	public Type getResolvedType() {
		if (type instanceof final Class<?> clazz) {
			return resolveType(clazz);
		}
		return type;
	}

	private void discoverTypes(final Type type) {
		if (type != null) {
			if (type instanceof final Class<?> clazz) {
				add(clazz, resolveType(clazz));
				discoverFromClass(clazz);
			} else {
				Class<?> clazz = null;
				if (type instanceof ParameterizedType) {
					final Type rawType = ((ParameterizedType) type).getRawType();
					if (rawType instanceof Class<?>) {
						discoverFromClass((Class<?>) rawType);
						clazz = (Class<?>) rawType;
					}
				}
				add(clazz, type);
			}
		}
	}

	private Type resolveType(final Class<?> clazz) {
		if (clazz.getTypeParameters().length > 0) {
			final TypeVariable<?>[] actualTypeParameters = clazz.getTypeParameters();

			final ParameterizedType parameterizedType = new ParameterizedTypeImpl(clazz, actualTypeParameters, clazz.getDeclaringClass());
			return parameterizedType;
		} else {
			return clazz;
		}
	}

	/**
	 * Gets the actual types by resolving TypeParameters.
	 *
	 * @return actual type
	 */
	private Type resolveType(final Type beanType, final Type beanType2, final Type type) {
		if (type instanceof ParameterizedType) {
			if (beanType instanceof ParameterizedType) {
				return resolveParameterizedType((ParameterizedType) beanType, (ParameterizedType) type);
			}
			if (beanType instanceof Class<?>) {
				return resolveType(((Class<?>) beanType).getGenericSuperclass(), beanType2, type);
			}
		}

		if (type instanceof TypeVariable<?>) {
			if (beanType instanceof ParameterizedType) {
				return resolveTypeParameter((ParameterizedType) beanType, beanType2, (TypeVariable<?>) type);
			}
			if (beanType instanceof Class<?>) {
				return resolveType(((Class<?>) beanType).getGenericSuperclass(), beanType2, type);
			}
		}
		return type;
	}

	private void discoverFromClass(final Class<?> clazz) {
		discoverTypes(resolveType(type, type, clazz.getGenericSuperclass()));
		for (final Type c : clazz.getGenericInterfaces()) {
			discoverTypes(resolveType(type, type, c));
		}
	}

	private Type resolveParameterizedType(final ParameterizedType beanType, final ParameterizedType parameterizedType) {
		final Type rawType = parameterizedType.getRawType();
		final Type[] actualTypes = parameterizedType.getActualTypeArguments();

		final Type resolvedRawType = resolveType(beanType, beanType, rawType);
		final Type[] resolvedActualTypes = new Type[actualTypes.length];

		for (int i = 0; i < actualTypes.length; i++) {
			resolvedActualTypes[i] = resolveType(beanType, beanType, actualTypes[i]);
		}
		// reconstruct ParameterizedType by types resolved TypeVariable.
		return new ParameterizedTypeImpl(resolvedRawType, resolvedActualTypes, parameterizedType.getOwnerType());
	}

	private Type resolveTypeParameter(final ParameterizedType type, final Type beanType, final TypeVariable<?> typeVariable) {
		// step1. raw type
		final Class<?> actualType = (Class<?>) type.getRawType();
		final TypeVariable<?>[] typeVariables = actualType.getTypeParameters();
		final Type[] actualTypes = type.getActualTypeArguments();
		for (int i = 0; i < typeVariables.length; i++) {
			if (typeVariables[i].equals(typeVariable) && !actualTypes[i].equals(typeVariable)) {
				return resolveType(this.type, beanType, actualTypes[i]);
			}
		}

		// step2. generic super class
		final Type genericSuperType = actualType.getGenericSuperclass();
		final Type resolvedGenericSuperType = resolveType(genericSuperType, beanType, typeVariable);
		if (!(resolvedGenericSuperType instanceof TypeVariable<?>)) {
			return resolvedGenericSuperType;
		}

		// step3. generic interfaces
		if (beanType instanceof ParameterizedType) {
			for (final Type interfaceType : ((Class<?>) ((ParameterizedType) beanType).getRawType()).getGenericInterfaces()) {
				final Type resolvedType = resolveType(interfaceType, interfaceType, typeVariable);
				if (!(resolvedType instanceof TypeVariable<?>)) {
					return resolvedType;
				}
			}
		}

		// don't resolve type variable
		return typeVariable;
	}
}
