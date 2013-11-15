/*
 * Sone - PluginConnectorTest.java - Copyright © 2013 David Roden
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

package net.pterodactylus.sone.freenet.plugin;

import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import net.pterodactylus.sone.Matchers;
import net.pterodactylus.sone.freenet.SimpleFieldSetBuilder;
import net.pterodactylus.sone.freenet.StringBucket;
import net.pterodactylus.sone.freenet.plugin.event.ReceivedReplyEvent;

import freenet.pluginmanager.PluginNotFoundException;
import freenet.pluginmanager.PluginRespirator;
import freenet.pluginmanager.PluginTalker;
import freenet.support.SimpleFieldSet;
import freenet.support.api.Bucket;

import com.google.common.eventbus.EventBus;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.junit.Test;

/**
 * Unit test for {@link PluginConnector}.
 *
 * @author <a href="mailto:bombe@pterodactylus.net">David ‘Bombe’ Roden</a>
 */
public class PluginConnectorTest {

	private static final String PLUGIN_NAME = "org.example.plugin";
	private static final String IDENTIFIER = "Some-Identifier-17";

	private final EventBus eventBus = mock(EventBus.class);
	private final PluginRespirator pluginRespirator = mock(PluginRespirator.class);
	private final PluginConnector pluginConnector = new PluginConnector(eventBus, pluginRespirator);

	@Test
	public void canSendRequest() throws PluginException, PluginNotFoundException {
		SimpleFieldSet fieldSet = new SimpleFieldSetBuilder().put("Length", "35").get();
		Bucket data = new StringBucket("Test");
		PluginTalker pluginTalker = mock(PluginTalker.class);
		when(pluginRespirator.getPluginTalker(pluginConnector, PLUGIN_NAME, IDENTIFIER)).thenReturn(pluginTalker);
		pluginConnector.sendRequest(PLUGIN_NAME, IDENTIFIER, fieldSet, data);
		verify(pluginTalker).send(argThat(Matchers.matches(fieldSet)), eq(data));
	}

	@Test(expected = PluginException.class)
	public void unknownPluginNameCausesAnError() throws PluginNotFoundException, PluginException {
		when(pluginRespirator.getPluginTalker(pluginConnector, PLUGIN_NAME, IDENTIFIER)).thenThrow(PluginNotFoundException.class);
		pluginConnector.sendRequest(PLUGIN_NAME, IDENTIFIER, null, null);
	}

	@Test
	public void replyIsPostedToEventBus() {
		SimpleFieldSet fieldSet = new SimpleFieldSetBuilder().put("Length", "35").get();
		Bucket data = new StringBucket("Test");
		pluginConnector.onReply(PLUGIN_NAME, IDENTIFIER, fieldSet, data);
		verify(eventBus).post(argThat(matches(pluginConnector, PLUGIN_NAME, IDENTIFIER, fieldSet, data)));
	}

	private static Matcher<ReceivedReplyEvent> matches(final PluginConnector pluginConnector, final String pluginName, final String identitier, final SimpleFieldSet fieldSet, final Bucket data) {
		return new TypeSafeMatcher<ReceivedReplyEvent>() {
			@Override
			protected boolean matchesSafely(ReceivedReplyEvent receivedReplyEvent) {
				if (receivedReplyEvent.pluginConnector() != pluginConnector) {
					return false;
				}
				if (!receivedReplyEvent.pluginName().equals(pluginName)) {
					return false;
				}
				if (!receivedReplyEvent.identifier().equals(identitier)) {
					return false;
				}
				if (!Matchers.matches(fieldSet).matches(receivedReplyEvent.fieldSet())) {
					return false;
				}
				if (data != receivedReplyEvent.data()) {
					return false;
				}
				return true;
			}

			@Override
			public void describeTo(Description description) {
			}
		};
	}

}
