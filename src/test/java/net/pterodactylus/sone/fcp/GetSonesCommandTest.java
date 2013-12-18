/*
 * Sone - GetSonesCommandTest.java - Copyright © 2013 David Roden
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
import static net.pterodactylus.sone.Verifiers.verifyAnswer;
import static net.pterodactylus.sone.Verifiers.verifySones;
import static net.pterodactylus.sone.freenet.fcp.Command.AccessType.DIRECT;

import java.util.Collections;
import java.util.List;

import net.pterodactylus.sone.data.Mocks;
import net.pterodactylus.sone.data.Sone;
import net.pterodactylus.sone.freenet.SimpleFieldSetBuilder;
import net.pterodactylus.sone.freenet.fcp.Command.Response;

import freenet.node.FSParseException;
import freenet.support.SimpleFieldSet;

import org.junit.Test;

/**
 * Unit test for {@link GetSonesCommand}.
 *
 * @author <a href="mailto:bombe@pterodactylus.net">David ‘Bombe’ Roden</a>
 */
public class GetSonesCommandTest {

	private final Mocks mocks = new Mocks();
	private final GetSonesCommand getSonesCommand = new GetSonesCommand(mocks.core);
	private final List<Sone> mockedSones = prepareSones();

	@Test
	public void gettingAllSones() throws FSParseException {
		SimpleFieldSet getSonesFieldSet = new SimpleFieldSetBuilder()
				.put("Message", "GetSones")
				.get();
		Response response = getSonesCommand.execute(getSonesFieldSet, null, DIRECT);
		verifyAnswer(response, "Sones");
		verifySones(response.getReplyParameters(), "", mockedSones);
	}

	@Test
	public void skipMoreSonesThanThereAre() throws FSParseException {
		SimpleFieldSet getSonesFieldSet = new SimpleFieldSetBuilder()
				.put("Message", "GetSones")
				.put("StartSone", "5")
				.get();
		Response response = getSonesCommand.execute(getSonesFieldSet, null, DIRECT);
		verifyAnswer(response, "Sones");
		verifySones(response.getReplyParameters(), "", Collections.<Sone>emptyList());
	}

	@Test
	public void gettingOnlyTwoSones() throws FSParseException {
		SimpleFieldSet getSonesFieldSet = new SimpleFieldSetBuilder()
				.put("Message", "GetSones")
				.put("MaxSones", "2")
				.get();
		Response response = getSonesCommand.execute(getSonesFieldSet, null, DIRECT);
		verifyAnswer(response, "Sones");
		verifySones(response.getReplyParameters(), "", mockedSones.subList(0, 2));
	}

	@Test
	public void gettingAllSonesSkippingTheFirst() throws FSParseException {
		SimpleFieldSet getSonesFieldSet = new SimpleFieldSetBuilder()
				.put("Message", "GetSones")
				.put("StartSone", "1")
				.get();
		Response response = getSonesCommand.execute(getSonesFieldSet, null, DIRECT);
		verifyAnswer(response, "Sones");
		verifySones(response.getReplyParameters(), "", mockedSones.subList(1, mockedSones.size()));
	}

	@Test
	public void gettingOnlyOneSonesSkippingTheFirst() throws FSParseException {
		SimpleFieldSet getSonesFieldSet = new SimpleFieldSetBuilder()
				.put("Message", "GetSones")
				.put("MaxSones", "1")
				.put("StartSone", "1")
				.get();
		Response response = getSonesCommand.execute(getSonesFieldSet, null, DIRECT);
		verifyAnswer(response, "Sones");
		verifySones(response.getReplyParameters(), "", mockedSones.subList(1, 2));
	}

	private List<Sone> prepareSones() {
		Sone sone3 = mocks.mockSone("Sone3").withName("Sone3").withProfileName("S.", "O.", "Ne3").withTime(3000L).create();
		Sone sone1 = mocks.mockSone("Sone1").withName("Sone1").withProfileName("S.", "O.", "Ne1").withTime(1000L).create();
		Sone sone2 = mocks.mockSone("Sone2").withName("Sone2").withProfileName("S.", "O.", "Ne2").withTime(2000L).create();
		return asList(sone1, sone2, sone3);
	}

}
