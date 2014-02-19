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

import static com.google.common.base.Optional.absent;
import static com.google.common.base.Optional.fromNullable;
import static com.google.common.base.Optional.of;
import static com.google.common.collect.ArrayListMultimap.create;
import static com.google.common.collect.Maps.newHashMap;
import static com.google.common.collect.Ordering.from;
import static java.util.Collections.emptySet;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.pterodactylus.sone.core.Core;
import net.pterodactylus.sone.core.Options;
import net.pterodactylus.sone.core.Options.DefaultOption;
import net.pterodactylus.sone.core.Options.Option;
import net.pterodactylus.sone.core.Options.OptionWatcher;
import net.pterodactylus.sone.core.Preferences;
import net.pterodactylus.sone.core.SoneInserter;
import net.pterodactylus.sone.data.impl.DefaultPostBuilder;
import net.pterodactylus.sone.data.impl.DefaultPostReplyBuilder;
import net.pterodactylus.sone.database.Database;
import net.pterodactylus.sone.database.PostBuilder.PostCreated;
import net.pterodactylus.sone.database.PostReplyBuilder;
import net.pterodactylus.sone.fcp.FcpInterface.FullAccessRequired;
import net.pterodactylus.sone.database.PostReplyBuilder.PostReplyCreated;
import net.pterodactylus.sone.utils.IntegerRangePredicate;
import net.pterodactylus.sone.web.WebInterface;
import net.pterodactylus.sone.web.page.FreenetRequest;

import freenet.clients.http.HTTPRequestImpl;
import freenet.clients.http.SessionManager.Session;
import freenet.clients.http.ToadletContext;
import freenet.support.api.HTTPRequest;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Predicates;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Ordering;
import com.google.common.collect.SetMultimap;
import org.mockito.Matchers;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

/**
 * Mocks reusable in multiple tests.
 *
 * @author <a href="mailto:bombe@pterodactylus.net">David ‘Bombe’ Roden</a>
 */
public class Mocks {

	private final Multimap<Sone, Post> sonePosts = create();
	private final Map<String, Sone> sones = newHashMap();
	private Optional<Sone> currentSone = absent();
	private final Map<String, Post> posts = newHashMap();
	private final Multimap<Post, PostReply> postReplies = create();
	private final Multimap<String, Post> directedPosts = create();
	private final SetMultimap<Post, Sone> postLikingSones = HashMultimap.create();
	private final SetMultimap<PostReply, Sone> postReplyLikingSones = HashMultimap.create();
	public final Database database;
	public final Core core;
	public final WebInterface webInterface;

	public Mocks() {
		database = mockDatabase();
		core = mockCore(database);
		webInterface = mockWebInterface(core);
		when(database.getSone()).thenReturn(new Function<String, Optional<Sone>>() {
			@Override
			public Optional<Sone> apply(String soneId) {
				return (soneId == null) ? Optional.<Sone>absent() : fromNullable(sones.get(soneId));
			}
		});
		Answer<Sone> returnCurrentSone = new Answer<Sone>() {
			@Override
			public Sone answer(InvocationOnMock invocation) throws Throwable {
				return currentSone.orNull();
			}
		};
		when(webInterface.getCurrentSone(any(ToadletContext.class))).then(returnCurrentSone);
		when(webInterface.getCurrentSone(any(Session.class))).then(returnCurrentSone);
		when(core.getSones()).then(new Answer<Collection<Sone>>() {
			@Override
			public Collection<Sone> answer(InvocationOnMock invocation) throws Throwable {
				return sones.values();
			}
		});
		when(core.getLocalSone(anyString())).then(new Answer<Optional<Sone>>() {
			@Override
			public Optional<Sone> answer(InvocationOnMock invocation) throws Throwable {
				Sone localSone = sones.get(invocation.getArguments()[0]);
				return ((localSone == null) || (!localSone.isLocal())) ? Optional.<Sone>absent() : of(localSone);
			}
		});
		when(core.getLocalSones()).then(new Answer<Collection<Sone>>() {
			@Override
			public Collection<Sone> answer(InvocationOnMock invocation) throws Throwable {
				return FluentIterable.from(sones.values()).filter(Sone.LOCAL_SONE_FILTER).toList();
			}
		});
		when(core.postCreated()).thenReturn(Optional.<PostCreated>of(new PostCreated() {
			@Override
			public void postCreated(Post post) {
				posts.put(post.getId(), post);
				sonePosts.put(post.getSone(), post);
			}
		}));
		when(core.postReplyCreated()).then(new Answer<Optional<PostReplyCreated>>() {
			@Override
			public Optional<PostReplyCreated> answer(InvocationOnMock invocation) throws Throwable {
				return Optional.<PostReplyCreated>of(new PostReplyCreated() {
					@Override
					public void postReplyCreated(PostReply postReply) {
						postReplies.put(postReply.getPost().get(), postReply);
					}
				});
			}
		});
		Options options = createOptions();
		when(core.getPreferences()).thenReturn(new Preferences(options));
		when(database.getDirectedPosts(anyString())).then(new Answer<Collection<Post>>() {
			@Override
			public Collection<Post> answer(InvocationOnMock invocation) throws Throwable {
				return directedPosts.get((String) invocation.getArguments()[0]);
			}
		});
	}

	private Options createOptions() {
		Options options = new Options();
		options.addIntegerOption("InsertionDelay", new DefaultOption<Integer>(60, new IntegerRangePredicate(0, Integer.MAX_VALUE)));
		options.addIntegerOption("PostsPerPage", new DefaultOption<Integer>(10, new IntegerRangePredicate(1, Integer.MAX_VALUE)));
		options.addIntegerOption("ImagesPerPage", new DefaultOption<Integer>(9, new IntegerRangePredicate(1, Integer.MAX_VALUE)));
		options.addIntegerOption("CharactersPerPost", new DefaultOption<Integer>(400, Predicates.<Integer>or(new IntegerRangePredicate(50, Integer.MAX_VALUE), Predicates.equalTo(-1))));
		options.addIntegerOption("PostCutOffLength", new DefaultOption<Integer>(200, Predicates.<Integer>or(new IntegerRangePredicate(50, Integer.MAX_VALUE), Predicates.equalTo(-1))));
		options.addBooleanOption("RequireFullAccess", new DefaultOption<Boolean>(false));
		options.addIntegerOption("PositiveTrust", new DefaultOption<Integer>(75, new IntegerRangePredicate(0, 100)));
		options.addIntegerOption("NegativeTrust", new DefaultOption<Integer>(-25, new IntegerRangePredicate(-100, 100)));
		options.addStringOption("TrustComment", new DefaultOption<String>("Set from Sone Web Interface"));
		options.addBooleanOption("ActivateFcpInterface", new DefaultOption<Boolean>(false));
		options.addIntegerOption("FcpFullAccessRequired", new DefaultOption<Integer>(2));
		return options;
	}

	private static Core mockCore(Database database) {
		Core core = mock(Core.class);
		when(core.getDatabase()).thenReturn(database);
		when(core.getSone(anyString())).thenReturn(Optional.<Sone>absent());
		return core;
	}

	private Database mockDatabase() {
		Database database = mock(Database.class);
		when(database.getSone(anyString())).thenReturn(Optional.<Sone>absent());
		when(database.getPost(anyString())).then(new Answer<Optional<Post>>() {
			@Override
			public Optional<Post> answer(InvocationOnMock invocation) throws Throwable {
				return fromNullable(posts.get(invocation.getArguments()[0]));
			}
		});
		when(database.getPostReply(anyString())).thenReturn(Optional.<PostReply>absent());
		return database;
	}

	private static WebInterface mockWebInterface(Core core) {
		WebInterface webInterface = mock(WebInterface.class);
		when(webInterface.getCore()).thenReturn(core);
		return webInterface;
	}

	public SoneMocker mockSone(String id) {
		return new SoneMocker(id);
	}

	public PostMocker mockPost(Sone sone, String postId) {
		return new PostMocker(postId, sone);
	}

	public PostReplyMocker mockPostReply(Sone sone, String replyId) {
		return new PostReplyMocker(replyId, sone);
	}

	public FreenetRequest mockRequest(String path) {
		HTTPRequest httpRequest = mock(HTTPRequest.class);
		when(httpRequest.getMethod()).thenReturn("GET");
		when(httpRequest.getPath()).thenReturn(path);
		FreenetRequest request = mock(FreenetRequest.class);
		when(request.getHttpRequest()).thenReturn(httpRequest);
		return request;
	}

	public class SoneMocker {

		private final Sone mockedSone = mock(Sone.class);
		private final String id;
		private boolean local;
		private boolean current;
		private Optional<String> name = absent();
		private long time;
		private Profile profile = new Profile(mockedSone);
		private Collection<String> friends = emptySet();

		private SoneMocker(String id) {
			this.id = id;
		}

		public SoneMocker local() {
			local = true;
			return this;
		}

		public SoneMocker current() {
			current = true;
			return this;
		}

		public SoneMocker withName(String name) {
			this.name = fromNullable(name);
			return this;
		}

		public SoneMocker withTime(long time) {
			this.time = time;
			return this;
		}

		public SoneMocker withProfileName(String firstName, String middleName, String lastName) {
			profile.modify().setFirstName(firstName).setMiddleName(middleName).setLastName(lastName).update();
			return this;
		}

		public SoneMocker addProfileField(String fieldName, String fieldValue) {
			profile.setField(profile.addField(fieldName), fieldValue);
			return this;
		}

		public SoneMocker withFriends(Collection<String> friends) {
			this.friends = friends;
			return this;
		}

		public Sone create() {
			when(mockedSone.getId()).thenReturn(id);
			when(mockedSone.isLocal()).thenReturn(local);
			if (current) {
				currentSone = of(mockedSone);
			}
			if (name.isPresent()) {
				when(mockedSone.getName()).thenReturn(name.get());
			}
			when(mockedSone.getTime()).thenReturn(time);
			when(mockedSone.getProfile()).thenReturn(profile);
			if (local) {
				when(mockedSone.newPostBuilder()).thenReturn(new DefaultPostBuilder(database, id));
				when(mockedSone.newPostReplyBuilder(anyString())).then(new Answer<PostReplyBuilder>() {
					@Override
					public PostReplyBuilder answer(InvocationOnMock invocation) throws Throwable {
						return new DefaultPostReplyBuilder(database, id, (String) invocation.getArguments()[0]);
					}
				});
				when(mockedSone.hasFriend(anyString())).thenReturn(false);
				when(mockedSone.getFriends()).thenReturn(friends);
				when(mockedSone.hasFriend(anyString())).then(new Answer<Boolean>() {
					@Override
					public Boolean answer(InvocationOnMock invocation) throws Throwable {
						return friends.contains(invocation.getArguments()[0]);
					}
				});
			} else {
				when(mockedSone.newPostBuilder()).thenThrow(IllegalStateException.class);
				when(mockedSone.newPostReplyBuilder(anyString())).thenThrow(IllegalStateException.class);
			}
			when(core.getSone(eq(id))).thenReturn(of(mockedSone));
			when(database.getSone(eq(id))).thenReturn(of(mockedSone));
			when(mockedSone.getPosts()).then(new Answer<List<Post>>() {
				@Override
				public List<Post> answer(InvocationOnMock invocationOnMock) throws Throwable {
					return from(Post.TIME_COMPARATOR).sortedCopy(sonePosts.get(mockedSone));
				}
			});
			when(mockedSone.toString()).thenReturn(String.format("Sone[%s]", id));
			sones.put(id, mockedSone);
			return mockedSone;
		}

	}

	public class PostMocker {

		private final Post post = mock(Post.class);
		private final String id;
		private final Sone sone;
		private Optional<String> recipientId = absent();
		private long time;
		private Optional<String> text = absent();

		public PostMocker(String id, Sone sone) {
			this.id = id;
			this.sone = sone;
		}

		public PostMocker withRecipient(String recipientId) {
			this.recipientId = fromNullable(recipientId);
			return this;
		}

		public PostMocker withTime(long time) {
			this.time = time;
			return this;
		}

		public PostMocker withText(String text) {
			this.text = fromNullable(text);
			return this;
		}

		public Post create() {
			when(post.getId()).thenReturn(id);
			when(post.getSone()).thenReturn(sone);
			when(post.getRecipientId()).thenReturn(recipientId);
			if (recipientId.isPresent()) {
				directedPosts.put(recipientId.get(), post);
			}
			when(post.getTime()).thenReturn(time);
			if (text.isPresent()) {
				when(post.getText()).thenReturn(text.get());
			}
			when(database.getPost(eq(id))).thenReturn(of(post));
			sonePosts.put(sone, post);
			when(post.getReplies()).then(new Answer<List<PostReply>>() {
				@Override
				public List<PostReply> answer(InvocationOnMock invocation) throws Throwable {
					return Ordering.from(Reply.TIME_COMPARATOR).sortedCopy(postReplies.get(post));
				}
			});
			doAnswer(new Answer<Void>() {
				@Override
				public Void answer(InvocationOnMock invocation) throws Throwable {
					postLikingSones.put(post, (Sone) invocation.getArguments()[0]);
					return null;
				}
			}).when(post).like(Matchers.<Sone>any());
			doAnswer(new Answer<Void>() {
				@Override
				public Void answer(InvocationOnMock invocation) throws Throwable {
					postLikingSones.remove(post, (Sone) invocation.getArguments()[0]);
					return null;
				}
			}).when(post).unlike(Matchers.<Sone>any());
			when(post.getLikes()).thenAnswer(new Answer<Set<Sone>>() {
				@Override
				public Set<Sone> answer(InvocationOnMock invocation) throws Throwable {
					return postLikingSones.get(post);
				}
			});
			return post;
		}

	}

	public class PostReplyMocker {

		private final PostReply postReply = mock(PostReply.class);
		private final String id;
		private final Sone sone;
		private Optional<Post> post = absent();
		private long time;
		private Optional<String> text = absent();

		public PostReplyMocker(String id, Sone sone) {
			this.id = id;
			this.sone = sone;
		}

		public PostReplyMocker inReplyTo(Post post) {
			this.post = fromNullable(post);
			return this;
		}

		public PostReplyMocker withTime(long time) {
			this.time = time;
			return this;
		}

		public PostReplyMocker withText(String text) {
			this.text = fromNullable(text);
			return this;
		}

		public PostReply create() {
			when(postReply.getId()).thenReturn(id);
			when(postReply.getSone()).thenReturn(sone);
			when(postReply.getTime()).thenReturn(time);
			when(database.getPostReply(eq(id))).thenReturn(of(postReply));
			if (post.isPresent()) {
				postReplies.put(post.get(), postReply);
			}
			if (text.isPresent()) {
				when(postReply.getText()).thenReturn(text.get());
			}
			doAnswer(new Answer<Void>() {
				@Override
				public Void answer(InvocationOnMock invocation) throws Throwable {
					postReplyLikingSones.put(postReply, (Sone) invocation.getArguments()[0]);
					return null;
				}
			}).when(postReply).like(Matchers.<Sone>any());
			doAnswer(new Answer<Void>() {
				@Override
				public Void answer(InvocationOnMock invocation) throws Throwable {
					postReplyLikingSones.remove(postReply, invocation.getArguments()[0]);
					return null;
				}
			}).when(postReply).unlike(Matchers.<Sone>any());
			when(postReply.getLikes()).thenAnswer(new Answer<Set<Sone>>() {
				@Override
				public Set<Sone> answer(InvocationOnMock invocation) throws Throwable {
					return postReplyLikingSones.get(postReply);
				}
			});
			return postReply;
		}
	}

}
