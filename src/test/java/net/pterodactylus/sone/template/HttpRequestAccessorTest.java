/*
 * Sone - HttpRequestAccessorTest.java - Copyright © 2013 David Roden
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package net.pterodactylus.sone.template;

import static java.lang.Integer.valueOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import freenet.support.api.HTTPRequest;
import org.junit.Before;
import org.junit.Test;

/**
 * Unit test for {@link HttpRequestAccessor}.
 *
 * @author <a href="mailto:bombe@pterodactylus.net">David ‘Bombe’ Roden</a>
 */
public class HttpRequestAccessorTest {

	private final HTTPRequest httpRequest = mock(HTTPRequest.class);
	private final HttpRequestAccessor httpRequestAccessor = new HttpRequestAccessor();

	@Before
	public void setup() {
		when(httpRequest.getHeader("KeyA")).thenReturn("ValueA");
	}

	@Test
	public void headerFieldsCanBeAccessed() {
		String headerValue = (String) httpRequestAccessor.get(null, httpRequest, "KeyA");
		assertThat(headerValue, is("ValueA"));
	}

	@Test
	public void normalMethodsAreCheckedFirst() {
		Object hashCode = httpRequestAccessor.get(null, httpRequest, "hashCode");
		assertThat(hashCode, is((Object) valueOf(httpRequest.hashCode())));
	}

}
