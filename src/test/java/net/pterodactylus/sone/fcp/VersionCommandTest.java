/*
 * Sone - VersionCommandTest.java - Copyright © 2013 David Roden
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

package net.pterodactylus.sone.fcp;

import static net.pterodactylus.sone.freenet.fcp.Command.AccessType.DIRECT;
import static net.pterodactylus.sone.main.SonePlugin.VERSION;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

import net.pterodactylus.sone.data.Mocks;
import net.pterodactylus.sone.freenet.SimpleFieldSetBuilder;
import net.pterodactylus.sone.freenet.fcp.Command.Response;

import freenet.support.SimpleFieldSet;

import org.junit.Test;

/**
 * Unit test for {@link VersionCommand}.
 *
 * @author <a href="mailto:bombe@pterodactylus.net">David ‘Bombe’ Roden</a>
 */
public class VersionCommandTest {

	private final Mocks mocks = new Mocks();
	private final VersionCommand versionCommand = new VersionCommand(mocks.core);

	@Test
	public void theCorrectVersionNumberIsReturned() {
		SimpleFieldSet versionFieldSet = new SimpleFieldSetBuilder().put("Message", "Version").get();
		Response response = versionCommand.execute(versionFieldSet, null, DIRECT);
		assertThat(response, notNullValue());
		assertThat(response.getReplyParameters(), notNullValue());
		assertThat(response.getReplyParameters().get("Message"), is("Version"));
		assertThat(response.getReplyParameters().get("Version"), is(VERSION.toString()));
	}

}
