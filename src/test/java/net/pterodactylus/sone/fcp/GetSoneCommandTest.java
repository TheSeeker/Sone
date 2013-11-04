/*
 * Sone - GetSoneCommandTest.java - Copyright © 2013 David Roden
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

import static java.util.Arrays.asList;
import static net.pterodactylus.sone.fcp.Verifiers.verifyAnswer;
import static net.pterodactylus.sone.fcp.Verifiers.verifyFollowedSone;
import static net.pterodactylus.sone.fcp.Verifiers.verifyNotFollowedSone;
import static net.pterodactylus.sone.freenet.fcp.Command.AccessType.DIRECT;

import java.util.Arrays;

import net.pterodactylus.sone.data.Mocks;
import net.pterodactylus.sone.data.Sone;
import net.pterodactylus.sone.freenet.SimpleFieldSetBuilder;
import net.pterodactylus.sone.freenet.fcp.Command.Response;
import net.pterodactylus.sone.freenet.fcp.FcpException;

import freenet.node.FSParseException;
import freenet.support.SimpleFieldSet;

import org.junit.Test;

/**
 * Unit test for {@link GetSoneCommand}.
 *
 * @author <a href="mailto:bombe@pterodactylus.net">David ‘Bombe’ Roden</a>
 */
public class GetSoneCommandTest {

	private final Mocks mocks = new Mocks();
	private final GetSoneCommand getSoneCommand = new GetSoneCommand(mocks.core);

	@Test
	public void gettingAFollowedSone() throws FcpException, FSParseException {
		Sone followedSone = mocks.mockSone("FSone").withName("Followed Sone").withProfileName("F.", "Ollowed", "Sone").withTime(1000L).addProfileField("Field1", "Value1").create();
		mocks.mockSone("LSone").withFriends(asList(followedSone.getId())).local().create();
		SimpleFieldSet getSoneFieldSet = new SimpleFieldSetBuilder()
				.put("Message", "GetSone")
				.put("Sone", "FSone")
				.put("LocalSone", "LSone")
				.get();
		Response response = getSoneCommand.execute(getSoneFieldSet, null, DIRECT);
		verifyAnswer(response, "Sone");
		verifyFollowedSone(response.getReplyParameters(), "", followedSone);
	}

	@Test
	public void gettingANotFollowedSone() throws FcpException, FSParseException {
		Sone unfollowedSone = mocks.mockSone("FSone").withName("Followed Sone").withProfileName("F.", "Ollowed", "Sone").withTime(1000L).addProfileField("Field1", "Value1").create();
		mocks.mockSone("LSone").withFriends(Arrays.<String>asList()).local().create();
		SimpleFieldSet getSoneFieldSet = new SimpleFieldSetBuilder()
				.put("Message", "GetSone")
				.put("Sone", "FSone")
				.put("LocalSone", "LSone")
				.get();
		Response response = getSoneCommand.execute(getSoneFieldSet, null, DIRECT);
		verifyAnswer(response, "Sone");
		verifyNotFollowedSone(response.getReplyParameters(), "", unfollowedSone);
	}

	@Test
	public void gettingASoneWithoutALocalSone() throws FcpException, FSParseException {
		Sone sone = mocks.mockSone("Sone").withName("Some Sone").withProfileName("S.", "Ome", "Sone").withTime(1000L).addProfileField("Field1", "Value1").create();
		SimpleFieldSet getSoneFieldSet = new SimpleFieldSetBuilder()
				.put("Message", "GetSone")
				.put("Sone", "Sone")
				.get();
		Response response = getSoneCommand.execute(getSoneFieldSet, null, DIRECT);
		verifyAnswer(response, "Sone");
		verifyNotFollowedSone(response.getReplyParameters(), "", sone);
	}

	@Test(expected = FcpException.class)
	public void gettingANonExistingSoneCausesAnError() throws FcpException {
		SimpleFieldSet getSoneFieldSet = new SimpleFieldSetBuilder()
				.put("Message", "GetSone")
				.put("Sone", "Sone")
				.get();
		getSoneCommand.execute(getSoneFieldSet, null, DIRECT);
	}

}
