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

import java.lang.reflect.Constructor;

import jakarta.enterprise.inject.Typed;

@Typed()
public abstract class ExceptionUtils {
	private ExceptionUtils() {
		// prevent instantiation
	}

	public static RuntimeException throwAsRuntimeException(final Throwable throwable) {
		// Attention: helper which allows to use a trick to throw a catched checked exception without a wrapping exception
		new ExceptionHelper<RuntimeException>().throwException(throwable);
		return null; // not needed due to the helper trick, but it's easier for using it
	}

	public static void changeAndThrowException(final Throwable throwable, final String customMessage) {
		final Throwable newThrowable = createNewException(throwable, customMessage);
		// Attention: helper which allows to use a trick to throw a cached checked exception without a wrapping exception
		new ExceptionHelper<RuntimeException>().throwException(newThrowable);
	}

	private static Throwable createNewException(final Throwable throwable, final String message) {
		final Class<? extends Throwable> throwableClass = throwable.getClass();

		try {
			final Constructor<? extends Throwable> constructor = throwableClass.getDeclaredConstructor(String.class);
			constructor.setAccessible(true);
			final Throwable result = constructor.newInstance(message);
			result.initCause(throwable.getCause());
			return result;
		} catch (final Exception e) {
			return new Exception(e);
		}
	}

	@SuppressWarnings({ "unchecked" })
	private static class ExceptionHelper<T extends Throwable> {
		private void throwException(final Throwable exception) throws T {
			try {
				// exception-type is only checked at compile-time
				throw (T) exception;
			} catch (final ClassCastException e) {
				// doesn't happen with existing JVMs! - if that changes the local ClassCastException needs to be ignored -> throw original
				// exception
				if (e.getStackTrace()[0].toString().contains(getClass().getName())) {
					if (exception instanceof RuntimeException) {
						throw (RuntimeException) exception;
					}
					throw new RuntimeException(exception);
				}
				// if the exception to throw is a ClassCastException, throw it
				throw e;
			}
		}
	}
}
