/*
 * Sone - SoneTest.java - Copyright © 2013 David Roden
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

package net.pterodactylus.sone.data;

import static net.pterodactylus.sone.data.Sone.TO_POSTS;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;

import org.junit.Test;

/**
 * Unit test for the static functions and predicates in {@link Sone}.
 *
 * @author <a href="mailto:bombe@pterodactylus.net">David ‘Bombe’ Roden</a>
 */
public class SoneTest {

	private final Mocks mocks = new Mocks();

	@Test
	public void verifyThatTransformingASoneIntoItsPostsWorks() {
		Sone sone = mocks.mockLocalSone("Sone");
		Post post1 = mocks.mockPost(sone, "Post1");
		when(post1.getTime()).thenReturn(1000L);
		Post post2 = mocks.mockPost(sone, "Post2");
		when(post2.getTime()).thenReturn(2000L);
		Post post3 = mocks.mockPost(sone, "Post3");
		when(post3.getTime()).thenReturn(3000L);
		assertThat(TO_POSTS.apply(sone), contains(is(post3), is(post2), is(post1)));
	}

}
