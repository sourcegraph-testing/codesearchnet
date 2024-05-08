/*
 * Copyright 2007 Alin Dreghiciu.
 *
 * Licensed  under the  Apache License,  Version 2.0  (the "License");
 * you may not use  this file  except in  compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed  under the  License is distributed on an "AS IS" BASIS,
 * WITHOUT  WARRANTIES OR CONDITIONS  OF ANY KIND, either  express  or
 * implied.
 *
 * See the License for the specific language governing permissions and
 * limitations under the License. 
 */
package org.ops4j.pax.web.service.spi.util;

/**
 * Divers utilities related to request paths.
 *
 * @author Alin Dreghiciu
 * @since 0.2.1
 */
public class Path {

	/**
	 * Utility class. Ment to be used via static methods.
	 */
	private Path() {
		// utility class. Ment to be used via static methods.
	}

	/**
	 * Normalize the path for accesing a resource, meaning that will replace
	 * consecutive slashes and will remove a leading slash if present.
	 *
	 * @param path path to normalize
	 * @return normalized path or the original path if there is nothing to be
	 * replaced.
	 */
	public static String normalizeResourcePath(final String path) {
		if (path == null) {
			return null;
		}
		String normalizedPath = replaceSlashes(path.trim());
		if (normalizedPath.startsWith("/") && normalizedPath.length() > 1) {
			normalizedPath = normalizedPath.substring(1);
		}
		return normalizedPath;
	}

	/**
	 * Replaces multiple subsequent slashes with one slash. E.g. ////a//path//
	 * will becaome /a/path/
	 *
	 * @param target target sring to be replaced
	 * @return a string where the subsequent slashes are replaced with one slash
	 */
	static String replaceSlashes(final String target) {
		String replaced = target;
		if (replaced != null) {
			replaced = replaced.replaceAll("/+", "/");
		}
		return replaced;
	}

	/**
	 * Normalize an array of patterns.
	 *
	 * @param urlPatterns to mormalize
	 * @return array of nomalized patterns
	 */
	public static String[] normalizePatterns(final String[] urlPatterns) {
		String[] normalized = null;
		if (urlPatterns != null) {
			normalized = new String[urlPatterns.length];
			for (int i = 0; i < urlPatterns.length; i++) {
				normalized[i] = normalizePattern(urlPatterns[i]);
			}
		}
		return normalized;
	}

	/**
	 * Normalizes a pattern = prepends the path with slash (/) if the path does
	 * not start with a slash.
	 *
	 * @param pattern to normalize
	 * @return normalized pattern
	 */
	public static String normalizePattern(final String pattern) {
		if (pattern == null || "".equals(pattern.trim())) {
			return "/";
		}
		if (pattern.length() > 0 && !pattern.startsWith("/")
				&& !pattern.startsWith("*")) {
			return "/" + pattern;
		}
		return pattern;
	}
}
