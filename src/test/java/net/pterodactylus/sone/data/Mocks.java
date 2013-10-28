/*
 * Sone - Mocks.java - Copyright © 2013 David Roden
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

import static com.google.common.base.Optional.of;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import net.pterodactylus.sone.core.Core;
import net.pterodactylus.sone.data.impl.DefaultPostBuilder;
import net.pterodactylus.sone.data.impl.DefaultPostReplyBuilder;
import net.pterodactylus.sone.database.Database;
import net.pterodactylus.sone.database.PostReplyBuilder;

import com.google.common.base.Optional;
import org.mockito.ArgumentCaptor;
import org.mockito.Matchers;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

/**
 * Mocks reusable in multiple tests.
 *
 * @author <a href="mailto:bombe@pterodactylus.net">David ‘Bombe’ Roden</a>
 */
public class Mocks {

	public static Core mockCore(Database database) {
		Core core = mock(Core.class);
		when(core.getDatabase()).thenReturn(database);
		when(core.getSone(anyString())).thenReturn(Optional.<Sone>absent());
		return core;
	}

	public static Database mockDatabase() {
		Database database = mock(Database.class);
		when(database.getSone(anyString())).thenReturn(Optional.<Sone>absent());
		when(database.getPost(anyString())).thenReturn(Optional.<Post>absent());
		when(database.getPostReply(anyString())).thenReturn(Optional.<PostReply>absent());
		return database;
	}

	public static Sone mockLocalSone(Core core, final String id) {
		Sone sone = mockRemoteSone(core, id);
		when(sone.isLocal()).thenReturn(true);
		final Database database = core.getDatabase();
		when(sone.newPostBuilder()).thenReturn(new DefaultPostBuilder(database, id));
		final ArgumentCaptor<String> postIdCaptor = forClass(String.class);
		when(sone.newPostReplyBuilder(postIdCaptor.capture())).then(new Answer<PostReplyBuilder>() {
			@Override
			public PostReplyBuilder answer(InvocationOnMock invocationOnMock) throws Throwable {
				return new DefaultPostReplyBuilder(database, id, postIdCaptor.getValue());
			}
		});
		return sone;
	}

	public static Sone mockRemoteSone(Core core, final String id) {
		Sone sone = mock(Sone.class);
		when(sone.getId()).thenReturn(id);
		when(sone.isLocal()).thenReturn(false);
		final Database database = core.getDatabase();
		when(sone.newPostBuilder()).thenReturn(new DefaultPostBuilder(database, id));
		when(sone.newPostReplyBuilder(Matchers.<String>anyObject())).thenThrow(IllegalStateException.class);
		when(core.getSone(eq(id))).thenReturn(of(sone));
		when(database.getSone(eq(id))).thenReturn(of(sone));
		return sone;
	}

	public static Post mockPost(Core core, Sone sone, String postId) {
		Post post = mock(Post.class);
		when(post.getId()).thenReturn(postId);
		when(post.getSone()).thenReturn(sone);
		Database database = core.getDatabase();
		when(database.getPost(eq(postId))).thenReturn(of(post));
		return post;
	}

	public static PostReply mockPostReply(Core core, Sone sone, String replyId) {
		PostReply postReply = mock(PostReply.class);
		when(postReply.getId()).thenReturn(replyId);
		when(postReply.getSone()).thenReturn(sone);
		Database database = core.getDatabase();
		when(database.getPostReply(eq(replyId))).thenReturn(of(postReply));
		return postReply;
	}

}
