/*
 * Sone - ReceivedReplyEventTest.java - Copyright © 2013 David Roden
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

package net.pterodactylus.sone.freenet.plugin.event;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;

import net.pterodactylus.sone.freenet.SimpleFieldSetBuilder;
import net.pterodactylus.sone.freenet.StringBucket;
import net.pterodactylus.sone.freenet.plugin.PluginConnector;

import freenet.support.SimpleFieldSet;
import freenet.support.api.Bucket;

import org.junit.Test;

/**
 * Unit test for {@link ReceivedReplyEvent}.
 *
 * @author <a href="mailto:bombe@pterodactylus.net">David ‘Bombe’ Roden</a>
 */
public class ReceivedReplyEventTest {

	private final PluginConnector pluginConnector = mock(PluginConnector.class);
	private final String pluginName = "test.plugin.Plugin";
	private final String identifier = "Test-Connection-12";
	private final SimpleFieldSet fieldSet = new SimpleFieldSetBuilder().get();
	private final Bucket data = new StringBucket("Test");
	private final ReceivedReplyEvent receivedReplyEvent = new ReceivedReplyEvent(pluginConnector, pluginName, identifier, fieldSet, data);

	@Test
	public void eventCanStoreAndReturnParameters() {
		assertThat(receivedReplyEvent.pluginConnector(), is(pluginConnector));
		assertThat(receivedReplyEvent.pluginName(), is(pluginName));
		assertThat(receivedReplyEvent.identifier(), is(identifier));
		assertThat(receivedReplyEvent.fieldSet(), is(fieldSet));
		assertThat(receivedReplyEvent.data(), is(data));
	}

}
