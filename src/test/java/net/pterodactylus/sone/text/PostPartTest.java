/*
 * Sone - PostPartTest.java - Copyright © 2013 David Roden
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

package net.pterodactylus.sone.text;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

import net.pterodactylus.sone.data.Mocks;
import net.pterodactylus.sone.data.Post;
import net.pterodactylus.sone.data.Sone;

import org.junit.Test;

/**
 * Unit test for {@link PostPart}.
 *
 * @author <a href="mailto:bombe@pterodactylus.net">David ‘Bombe’ Roden</a>
 */
public class PostPartTest {

	private final Mocks mocks = new Mocks();
	private final Sone sone = mocks.mockSone("Sone").create();
	private final Post post = mocks.mockPost(sone, "Post").withText("Text").create();
	private final PostPart postPart = new PostPart(post);

	@Test
	public void postPartCanStoreAndReturnPost() {
		assertThat(postPart.getPost(), is(post));
	}

	@Test
	public void postPartReturnsTextOfPost() {
		assertThat(postPart.getText(), is(post.getText()));
	}

	@Test
	public void postPartsAreEqualIfTheyContainTheSamePost() {
		PostPart secondPostPart = new PostPart(post);
		assertThat(secondPostPart, is(postPart));
	}

	@Test
	public void postPartsWithDifferentPostsButSameTextAreNotEqual() {
		Post secondPost = mocks.mockPost(sone, "Post2").withText(post.getText()).create();
		PostPart secondPostPart = new PostPart(secondPost);
		assertThat(secondPostPart, not(is(postPart)));
	}

	@Test
	public void postPartsThatAreEqualHaveTheSameHashCode() {
		PostPart secondPostPart = new PostPart(post);
		assertThat(secondPostPart, is(postPart));
		assertThat(secondPostPart.hashCode(), is(postPart.hashCode()));
	}

	@Test
	public void nullDoesNotMatchPostPart() {
		assertThat(postPart, not(is((Object) null)));
	}

}
