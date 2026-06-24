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

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;

/**
 * <p>
 * A small helper class to create an Annotation instance of the given annotation class via {@link java.lang.reflect.Proxy}. The
 * annotation literal gets filled with the default values.
 * </p>
 * <p/>
 * <p>
 * This class can be used to dynamically create Annotations which can be usd in AnnotatedTyp. This is e.g. the case if you configure an
 * annotation via properties or XML file. In those cases you cannot use {@link javax.enterprise.util.AnnotationLiteral} because the
 * type is not known at compile time.
 * </p>
 * <p>
 * usage:
 * </p>
 *
 * <pre>
 * String annotationClassName = ...;
 * Class<? extends annotation> annotationClass =
 *     (Class<? extends Annotation>) ClassUtils.getClassLoader(null).loadClass(annotationClassName);
 * Annotation a = AnnotationInstanceProvider.of(annotationClass)
 * </pre>
 */
@SuppressWarnings("rawtypes")
public class AnnotationInstanceProvider implements Annotation, InvocationHandler, Serializable {
	private static final long serialVersionUID = -2345068201195886173L;
	private static final Object[] EMPTY_OBJECT_ARRAY = {};
	private static final Class[] EMPTY_CLASS_ARRAY = {};

	private final Class<? extends Annotation> annotationClass;
	private final Map<String, ?> memberValues;

	/**
	 * Required to use the result of the factory instead of a default implementation of {@link javax.enterprise.util.AnnotationLiteral}.
	 *
	 * @param annotationClass
	 *          class of the target annotation
	 */
	private AnnotationInstanceProvider(final Class<? extends Annotation> annotationClass, final Map<String, ?> memberValues) {
		this.annotationClass = annotationClass;
		this.memberValues = memberValues;
	}

	/**
	 * Creates an annotation instance for the given annotation class.
	 *
	 * @param annotationClass
	 *          type of the target annotation
	 * @param values
	 *          A non-null map of the member values, keys being the name of the members
	 * @param <T>
	 *          current type
	 * @return annotation instance for the given type
	 */
	@SuppressWarnings("unchecked")
	public static <T extends Annotation> T of(final Class<T> annotationClass, final Map<String, ?> values) {
		if (values == null) {
			throw new IllegalArgumentException("Map of values must not be null");
		}

		return (T) initAnnotation(annotationClass, values);
	}

	/**
	 * Creates an annotation instance for the given annotation class.
	 *
	 * @param annotationClass
	 *          type of the target annotation
	 * @param <T>
	 *          current type
	 * @return annotation instance for the given type
	 */
	@SuppressWarnings("unchecked")
	public static <T extends Annotation> T of(final Class<T> annotationClass) {
		return (T) of(annotationClass, Collections.EMPTY_MAP);
	}

	private static synchronized <T extends Annotation> Annotation initAnnotation(final Class<T> annotationClass,
			final Map<String, ?> values) {
		return (Annotation) Proxy.newProxyInstance(annotationClass.getClassLoader(), new Class[] { annotationClass },
				new AnnotationInstanceProvider(annotationClass, values));
	}

	/**
	 * Helper method for generating a hash code for an array.
	 *
	 * @param componentType
	 *          the component type of the array
	 * @param o
	 *          the array
	 * @return a hash code for the specified array
	 */
	private static int arrayMemberHash(final Class<?> componentType, final Object o) {
		if (componentType.equals(Byte.TYPE)) {
			return Arrays.hashCode((byte[]) o);
		}
		if (componentType.equals(Short.TYPE)) {
			return Arrays.hashCode((short[]) o);
		}
		if (componentType.equals(Integer.TYPE)) {
			return Arrays.hashCode((int[]) o);
		}
		if (componentType.equals(Character.TYPE)) {
			return Arrays.hashCode((char[]) o);
		}
		if (componentType.equals(Long.TYPE)) {
			return Arrays.hashCode((long[]) o);
		}
		if (componentType.equals(Float.TYPE)) {
			return Arrays.hashCode((float[]) o);
		}
		if (componentType.equals(Double.TYPE)) {
			return Arrays.hashCode((double[]) o);
		}
		if (componentType.equals(Boolean.TYPE)) {
			return Arrays.hashCode((boolean[]) o);
		}
		return Arrays.hashCode((Object[]) o);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Object invoke(final Object proxy, final Method method, final Object[] args) throws Exception {
		if ("hashCode".equals(method.getName())) {
			return hashCode();
		} else if ("equals".equals(method.getName())) {
			if (Proxy.isProxyClass(args[0].getClass()) && Proxy.getInvocationHandler(args[0]) instanceof AnnotationInstanceProvider) {
				return equals(Proxy.getInvocationHandler(args[0]));
			}
			return equals(args[0]);
		} else if ("annotationType".equals(method.getName())) {
			return annotationType();
		} else if ("toString".equals(method.getName())) {
			return toString();
		} else if (memberValues.containsKey(method.getName())) {
			return memberValues.get(method.getName());
		} else {
			// Default cause, probably won't ever happen, unless annotations get actual methods
			return method.getDefaultValue();
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Class<? extends Annotation> annotationType() {
		return annotationClass;
	}

	/**
	 * Copied from Apache OWB (javax.enterprise.util.AnnotationLiteral#toString()) with minor changes.
	 *
	 * @return the current state of the annotation as string
	 */
	@Override
	public String toString() {
		final Method[] methods = annotationClass.getDeclaredMethods();

		final StringBuilder sb = new StringBuilder("@" + annotationType().getName() + "(");
		final int length = methods.length;

		for (int i = 0; i < length; i++) {
			// Member name
			sb.append(methods[i].getName()).append("=");

			// Member value
			Object memberValue;
			try {
				memberValue = invoke(this, methods[i], EMPTY_OBJECT_ARRAY);
			} catch (final Exception e) {
				memberValue = "";
			}
			sb.append(memberValue);

			if (i < length - 1) {
				sb.append(",");
			}
		}

		sb.append(")");

		return sb.toString();
	}

	@Override
	public boolean equals(final Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof final AnnotationInstanceProvider that)) {
			if (annotationClass.isInstance(o)) {
				for (final Map.Entry<String, ?> entry : memberValues.entrySet()) {
					try {
						final Object oValue = annotationClass.getMethod(entry.getKey(), EMPTY_CLASS_ARRAY).invoke(o, EMPTY_OBJECT_ARRAY);
						if (oValue != null && entry.getValue() != null) {
							if (!oValue.equals(entry.getValue())) {
								return false;
							}
						} else {
							// This may not actually ever happen, unless null is a default for a member
							return false;
						}
					} catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
						throw new RuntimeException(e);
					}
				}
				return true;
			}
			return false;
		}

		if (!annotationClass.equals(that.annotationClass)) {
			return false;
		}

		return memberValues.equals(that.memberValues);
	}

	// besides modularity, this has the advantage of autoboxing primitives:

	@Override
	public int hashCode() {
		int result = 0;
		final Class<? extends Annotation> type = annotationClass;
		for (final Method m : type.getDeclaredMethods()) {
			try {
				final Object value = invoke(this, m, EMPTY_OBJECT_ARRAY);
				if (value == null) {
					throw new IllegalStateException(String.format("Annotation method %s returned null", m));
				}
				result += hashMember(m.getName(), value);
			} catch (final RuntimeException ex) {
				throw ex;
			} catch (final Exception ex) {
				throw new RuntimeException(ex);
			}
		}
		return result;
	}

	/**
	 * <p>
	 * Generate a hash code for the given annotation using the algorithm presented in the {@link Annotation#hashCode()} API docs.
	 * </p>
	 *
	 * @param a
	 *          the Annotation for a hash code calculation is desired, not {@code null}
	 * @return the calculated hash code
	 * @throws RuntimeException
	 *           if an {@code Exception} is encountered during annotation member access
	 * @throws IllegalStateException
	 *           if an annotation method invocation returns {@code null}
	 */
	private int hashCode(final Annotation a) {
		int result = 0;
		final Class<? extends Annotation> type = a.annotationType();
		for (final Method m : type.getDeclaredMethods()) {
			try {
				final Object value = m.invoke(a);
				if (value == null) {
					throw new IllegalStateException(String.format("Annotation method %s returned null", m));
				}
				result += hashMember(m.getName(), value);
			} catch (final RuntimeException ex) {
				throw ex;
			} catch (final Exception ex) {
				throw new RuntimeException(ex);
			}
		}
		return result;
	}

	/**
	 * Helper method for generating a hash code for a member of an annotation.
	 *
	 * @param name
	 *          the name of the member
	 * @param value
	 *          the value of the member
	 * @return a hash code for this member
	 */
	private int hashMember(final String name, final Object value) {
		final int part1 = name.hashCode() * 127;
		if (value.getClass().isArray()) {
			return part1 ^ arrayMemberHash(value.getClass().getComponentType(), value);
		}
		if (value instanceof Annotation) {
			return part1 ^ hashCode((Annotation) value);
		}
		return part1 ^ value.hashCode();
	}
}
