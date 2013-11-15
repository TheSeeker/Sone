/*
 * Sone - WebOfTrustConnectorTest.java - Copyright © 2013 David Roden
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

package net.pterodactylus.sone.freenet.wot;

import static com.google.common.base.Objects.equal;
import static com.google.common.collect.FluentIterable.from;
import static com.google.common.collect.ImmutableSet.of;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.util.Collection;
import java.util.Map.Entry;
import java.util.Set;

import net.pterodactylus.sone.Matchers;
import net.pterodactylus.sone.freenet.SimpleFieldSetBuilder;
import net.pterodactylus.sone.freenet.plugin.PluginConnector;
import net.pterodactylus.sone.freenet.plugin.PluginException;
import net.pterodactylus.sone.freenet.plugin.event.ReceivedReplyEvent;

import freenet.support.SimpleFieldSet;
import freenet.support.api.Bucket;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableSet;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

/**
 * Unit test for {@link WebOfTrustConnector}.
 *
 * @author <a href="mailto:bombe@pterodactylus.net">David ‘Bombe’ Roden</a>
 */
public class WebOfTrustConnectorTest {

	private static final String WOT_PLUGIN_NAME = "plugins.WebOfTrust.WebOfTrust";
	private final PluginConnector pluginConnector = mock(PluginConnector.class);
	private final WebOfTrustConnector webOfTrustConnector = new WebOfTrustConnector(pluginConnector);

	@Test
	public void loadingAllOwnIdentities() throws WebOfTrustException {
		final Set<OwnIdentity> ownIdentities = ImmutableSet.of(
				new DefaultOwnIdentity("Id0", "Nickname0", "RequestURI0", "InsertURI0").addContext("TestA").setProperty("Key A", "Value A").setProperty("Key B", "Value B"),
				new DefaultOwnIdentity("Id1", "Nickname1", "RequestURI1", "InsertURI1").addContext("TestB").addContext("TestC").setProperty("Key C", "Value C")
		);
		providerAnswer(createFieldSetForOwnIdentities(ownIdentities));
		Set<OwnIdentity> parsedOwnIdentities = webOfTrustConnector.loadAllOwnIdentities();
		verify(pluginConnector).sendRequest(eq(WOT_PLUGIN_NAME), anyString(), argThat(Matchers.matches(new SimpleFieldSetBuilder().put("Message", "GetOwnIdentities").get())), any(Bucket.class));
		verifyOwnIdentities(parsedOwnIdentities, ownIdentities);
	}

	@Test
	public void loadingTrustedIdentities() throws PluginException {
		final OwnIdentity ownIdentity = new DefaultOwnIdentity("OwnId", "OwnNick", "OwnRequest", "OwnInsert");
		final Collection<Identity> identities = of(
				new DefaultIdentity("Id1", "Nickname1", "Request1").addContext("TestA").addContext("TestB").setProperty("Key A", "Value A").setTrust(ownIdentity, new Trust(5, 17, 2)),
				new DefaultIdentity("Id2", "Nickname2", "Request2").addContext("TestC").setProperty("Key B", "Value B").setProperty("Key C", "Value C").setTrust(ownIdentity, new Trust(80, 23, 1))
		);
		providerAnswer(createFieldSetForIdentities(identities, ownIdentity));
		Set<Identity> parsedIdentities = webOfTrustConnector.loadTrustedIdentities(ownIdentity, "Test");
		verify(pluginConnector).sendRequest(eq(WOT_PLUGIN_NAME), anyString(), argThat(Matchers.matches(new SimpleFieldSetBuilder().put("Message", "GetIdentitiesByScore").put("Truster", ownIdentity.getId()).put("Selection", "+").put("Context", "Test").put("WantTrustValues", "true").get())), any(Bucket.class));
		verifyIdentities(parsedIdentities, identities, ownIdentity);
	}

	private static void verifyIdentities(Set<Identity> parsedIdentities, Collection<Identity> identities, final OwnIdentity ownIdentity) {
		assertThat(parsedIdentities, hasSize(identities.size()));
		assertThat(parsedIdentities, containsInAnyOrder(from(identities).transform(new Function<Identity, Matcher<? super Identity>>() {
			@Override
			public Matcher<Identity> apply(Identity identity) {
				return matches(identity, ownIdentity);
			}
		}).toSet()));
	}

	private static SimpleFieldSet createFieldSetForIdentities(Collection<Identity> identities, OwnIdentity ownIdentity) {
		SimpleFieldSetBuilder identitiesBuilder = new SimpleFieldSetBuilder();
		int identityIndex = 0;
		for (Identity identity : identities) {
			addIdentityToFieldSet(identitiesBuilder, identityIndex++, identity, ownIdentity);
		}
		return identitiesBuilder.get();
	}

	private static void addIdentityToFieldSet(SimpleFieldSetBuilder identitiesBuilder, int index, Identity identity, OwnIdentity ownIdentity) {
		addCommonIdentityFieldsToFieldSet(identitiesBuilder, index, identity);
		Trust trust = identity.getTrust(ownIdentity);
		identitiesBuilder.put("Trust" + index, trust.getExplicit()).put("Score" + index, trust.getImplicit()).put("Rank" + index, trust.getDistance());
	}

	@Test
	public void addingAContext() throws PluginException {
		final OwnIdentity ownIdentity = new DefaultOwnIdentity("OwnId", "OwnNick", "OwnRequest", "OwnInsert");
		providerAnswer(new SimpleFieldSetBuilder().get());
		webOfTrustConnector.addContext(ownIdentity, "Test");
		verify(pluginConnector).sendRequest(eq(WOT_PLUGIN_NAME), anyString(), argThat(Matchers.matches(new SimpleFieldSetBuilder().put("Message", "AddContext").put("Identity", ownIdentity.getId()).put("Context", "Test").get())), any(Bucket.class));
	}

	@Test
	public void removingAContext() throws PluginException {
		final OwnIdentity ownIdentity = new DefaultOwnIdentity("OwnId", "OwnNick", "OwnRequest", "OwnInsert");
		providerAnswer(new SimpleFieldSetBuilder().get());
		webOfTrustConnector.removeContext(ownIdentity, "Test");
		verify(pluginConnector).sendRequest(eq(WOT_PLUGIN_NAME), anyString(), argThat(Matchers.matches(new SimpleFieldSetBuilder().put("Message", "RemoveContext").put("Identity", ownIdentity.getId()).put("Context", "Test").get())), any(Bucket.class));
	}

	@Test
	public void gettingAProperty() throws PluginException {
		final Identity identity = new DefaultIdentity("Id", "Nick", "R").setProperty("KeyA", "ValueA");
		providerAnswer(createFieldSetForGettingAProperty(identity, "KeyA"));
		String value = webOfTrustConnector.getProperty(identity, "KeyA");
		verify(pluginConnector).sendRequest(eq(WOT_PLUGIN_NAME), anyString(), argThat(Matchers.matches(new SimpleFieldSetBuilder().put("Message", "GetProperty").put("Identity", identity.getId()).put("Property", "KeyA").get())), any(Bucket.class));
		assertThat(value, is("ValueA"));
	}

	private static SimpleFieldSet createFieldSetForGettingAProperty(Identity identity, String key) {
		return new SimpleFieldSetBuilder()
				.put("Property", identity.getProperty(key))
				.get();
	}

	@Test
	public void settingAProperty() throws PluginException {
		OwnIdentity ownIdentity = new DefaultOwnIdentity("Id", "Nick", "R", "I");
		providerAnswer(new SimpleFieldSetBuilder().get());
		webOfTrustConnector.setProperty(ownIdentity, "KeyA", "ValueA");
		verify(pluginConnector).sendRequest(eq(WOT_PLUGIN_NAME), anyString(), argThat(Matchers.matches(new SimpleFieldSetBuilder().put("Message", "SetProperty").put("Identity", ownIdentity.getId()).put("Property", "KeyA").put("Value", "ValueA").get())), any(Bucket.class));
	}

	@Test
	public void removingAProperty() throws PluginException {
		OwnIdentity ownIdentity = new DefaultOwnIdentity("Id", "Nick", "R", "I");
		providerAnswer(new SimpleFieldSetBuilder().get());
		webOfTrustConnector.removeProperty(ownIdentity, "KeyA");
		verify(pluginConnector).sendRequest(eq(WOT_PLUGIN_NAME), anyString(), argThat(Matchers.matches(new SimpleFieldSetBuilder().put("Message", "RemoveProperty").put("Identity", ownIdentity.getId()).put("Property", "KeyA").get())), any(Bucket.class));
	}

	@Test
	public void gettingTrust() throws PluginException {
		OwnIdentity ownIdentity = new DefaultOwnIdentity("OId", "ONick", "OR", "OI");
		Identity identity = new DefaultIdentity("Id", "Nick", "R");
		providerAnswer(createFieldSetForGettingTrust(5, 17, 2));
		Trust trust = webOfTrustConnector.getTrust(ownIdentity, identity);
		verify(pluginConnector).sendRequest(eq(WOT_PLUGIN_NAME), anyString(), argThat(Matchers.matches(new SimpleFieldSetBuilder().put("Message", "GetIdentity").put("Truster", ownIdentity.getId()).put("Identity", identity.getId()).get())), any(Bucket.class));
		assertThat(trust.getExplicit(), is(5));
		assertThat(trust.getImplicit(), is(17));
		assertThat(trust.getDistance(), is(2));
	}

	private SimpleFieldSet createFieldSetForGettingTrust(int explicit, int implicit, int distance) {
		return new SimpleFieldSetBuilder()
				.put("Trust", explicit)
				.put("Score", implicit)
				.put("Rank", distance)
				.get();
	}

	@Test
	public void settingTrust() throws PluginException {
		OwnIdentity ownIdentity = new DefaultOwnIdentity("OId", "ONick", "OR", "OI");
		Identity identity = new DefaultIdentity("Id", "Nick", "R");
		providerAnswer(new SimpleFieldSetBuilder().get());
		webOfTrustConnector.setTrust(ownIdentity, identity, 45, "Set manually.");
		verify(pluginConnector).sendRequest(eq(WOT_PLUGIN_NAME), anyString(), argThat(Matchers.matches(new SimpleFieldSetBuilder().put("Message", "SetTrust").put("Truster", ownIdentity.getId()).put("Trustee", identity.getId()).put("Value", 45).put("Comment", "Set manually.").get())), any(Bucket.class));
	}

	@Test
	public void removingTrust() throws WebOfTrustException {
		OwnIdentity ownIdentity = new DefaultOwnIdentity("OId", "ONick", "OR", "OI");
		Identity identity = new DefaultIdentity("Id", "Nick", "R");
		providerAnswer(new SimpleFieldSetBuilder().get());
		webOfTrustConnector.removeTrust(ownIdentity, identity);
		verify(pluginConnector).sendRequest(eq(WOT_PLUGIN_NAME), anyString(), argThat(Matchers.matches(new SimpleFieldSetBuilder().put("Message", "RemoveTrust").put("Truster", ownIdentity.getId()).put("Trustee", identity.getId()).get())), any(Bucket.class));
	}

	@Test
	public void pinging() throws PluginException {
		providerAnswer(new SimpleFieldSetBuilder().get());
		webOfTrustConnector.ping();
		verify(pluginConnector).sendRequest(eq(WOT_PLUGIN_NAME), anyString(), argThat(Matchers.matches(new SimpleFieldSetBuilder().put("Message", "Ping").get())), any(Bucket.class));
	}

	private void providerAnswer(final SimpleFieldSet fieldSet) throws PluginException {
		doAnswer(new Answer<Void>() {
			@Override
			public Void answer(InvocationOnMock invocation) throws Throwable {
				String identifier = (String) invocation.getArguments()[1];
				SimpleFieldSet replyFieldSet = fieldSet;
				ReceivedReplyEvent receivedReplyEvent = new ReceivedReplyEvent(pluginConnector, WOT_PLUGIN_NAME, identifier, replyFieldSet, null);
				webOfTrustConnector.receivedReply(receivedReplyEvent);
				return null;
			}
		}).when(pluginConnector).sendRequest(anyString(), anyString(), any(SimpleFieldSet.class), any(Bucket.class));
	}

	private static void addCommonIdentityFieldsToFieldSet(SimpleFieldSetBuilder identitiesBuilder, int index, Identity identity) {
		identitiesBuilder
				.put("Identity" + index, identity.getId())
				.put("Nickname" + index, identity.getNickname())
				.put("RequestURI" + index, identity.getRequestUri());
		int contextIndex = 0;
		for (String context : identity.getContexts()) {
			identitiesBuilder.put("Contexts" + index + ".Context" + contextIndex++, context);
		}
		int propertyIndex = 0;
		for (Entry<String, String> property : identity.getProperties().entrySet()) {
			identitiesBuilder
					.put("Properties" + index + ".Property" + propertyIndex + ".Name", property.getKey())
					.put("Properties" + index + ".Property" + propertyIndex++ + ".Value", property.getValue());
		}
	}

	private static SimpleFieldSet createFieldSetForOwnIdentities(Collection<OwnIdentity> ownIdentities) {
		SimpleFieldSetBuilder ownIdentitiesBuilder = new SimpleFieldSetBuilder();
		int ownIdentityIndex = 0;
		for (OwnIdentity ownIdentity : ownIdentities) {
			addOwnIdentityToFieldSet(ownIdentitiesBuilder, ownIdentityIndex++, ownIdentity);
		}
		return ownIdentitiesBuilder.get();
	}

	private static void addOwnIdentityToFieldSet(SimpleFieldSetBuilder fieldSetBuilder, int index, OwnIdentity ownIdentity) {
		addCommonIdentityFieldsToFieldSet(fieldSetBuilder, index, ownIdentity);
		fieldSetBuilder.put("InsertURI" + index, ownIdentity.getInsertUri());
	}

	private static void verifyOwnIdentities(Set<OwnIdentity> ownIdentities, Set<OwnIdentity> ownIdentitiesToMatch) {
		assertThat(ownIdentities, hasSize(ownIdentities.size()));
		assertThat(ownIdentities, containsInAnyOrder(from(ownIdentitiesToMatch).transform(new Function<OwnIdentity, Matcher<? super OwnIdentity>>() {
			@Override
			public Matcher<OwnIdentity> apply(OwnIdentity ownIdentity) {
				return matches(ownIdentity);
			}
		}).toSet()));
	}

	private static Matcher<Identity> matches(final Identity identity, final OwnIdentity ownIdentity) {
		return new TypeSafeMatcher<Identity>() {
			@Override
			protected boolean matchesSafely(Identity item) {
				if (!matchesCommonFields(identity).matches(item)) {
					return false;
				}
				if (!equal(item.getTrust(ownIdentity), identity.getTrust(ownIdentity))) {
					return false;
				}
				return true;
			}

			@Override
			public void describeTo(Description description) {
				description.appendValue(identity);
			}
		};
	}

	private static Matcher<OwnIdentity> matches(final OwnIdentity ownIdentity) {
		return new TypeSafeMatcher<OwnIdentity>() {
			@Override
			protected boolean matchesSafely(OwnIdentity item) {
				if (!matchesCommonFields(ownIdentity).matches(item)) {
					return false;
				}
				if (!equal(item.getInsertUri(), ownIdentity.getInsertUri())) {
					return false;
				}
				return true;
			}

			@Override
			public void describeTo(Description description) {
				description.appendValue(ownIdentity);
			}
		};
	}

	private static Matcher<? extends Identity> matchesCommonFields(final Identity identity) {
		return new TypeSafeMatcher<Identity>() {
			@Override
			protected boolean matchesSafely(Identity item) {
				if (!equal(item.getId(), identity.getId()) || !equal(item.getNickname(), identity.getNickname()) || !equal(item.getRequestUri(), identity.getRequestUri())) {
					return false;
				}
				if (!containsInAnyOrder(identity.getContexts().toArray(new String[identity.getContexts().size()])).matches(item.getContexts())) {
					return false;
				}
				if (!equal(item.getProperties(), identity.getProperties())) {
					return false;
				}
				return true;
			}

			@Override
			public void describeTo(Description description) {
				description.appendValue(identity);
			}
		};
	}

}
