/*
 * Sone - GetLocalSonesCommandTest.java - Copyright © 2013 David Roden
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

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static net.pterodactylus.sone.fcp.Verifiers.verifyAnswer;
import static net.pterodactylus.sone.freenet.fcp.Command.AccessType.DIRECT;
import static org.hamcrest.MatcherAssert.assertThat;

import java.util.Collection;
import java.util.List;

import net.pterodactylus.sone.data.Mocks;
import net.pterodactylus.sone.data.Sone;
import net.pterodactylus.sone.freenet.SimpleFieldSetBuilder;
import net.pterodactylus.sone.freenet.fcp.Command.Response;

import freenet.node.FSParseException;
import freenet.support.SimpleFieldSet;

import com.google.common.collect.Lists;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.junit.Test;

/**
 * Unit test for {@link GetLocalSonesCommand}.
 *
 * @author <a href="mailto:bombe@pterodactylus.net">David ‘Bombe’ Roden</a>
 */
public class GetLocalSonesCommandTest {

	private final Mocks mocks = new Mocks();
	private final GetLocalSonesCommand getLocalSonesCommand = new GetLocalSonesCommand(mocks.core);

	@Test
	public void verifyThatOnlyLocalSonesAreReturned() throws FSParseException {
		Collection<Sone> localSones = asList(
				mocks.mockSone("LSone1").local().create(),
				mocks.mockSone("LSone2").local().create(),
				mocks.mockSone("LSone3").local().create()
		);
		mocks.mockSone("RSone1").create();
		mocks.mockSone("RSone2").create();
		SimpleFieldSet getLocalSonesFieldSet = new SimpleFieldSetBuilder().put("Message", "GetLocalSones").get();
		Response response = getLocalSonesCommand.execute(getLocalSonesFieldSet, null, DIRECT);
		verifyAnswer(response, "ListLocalSones");
		Collection<ParsedSone> parsedSones = parseSones(response.getReplyParameters(), "LocalSones.");
		assertThat(parsedSones, matchesSones(localSones));
	}

	private Matcher<Collection<ParsedSone>> matchesSones(final Collection<Sone> sones) {
		return new TypeSafeMatcher<Collection<ParsedSone>>() {
			@Override
			protected boolean matchesSafely(Collection<ParsedSone> parsedSones) {
				if (sones.size() != parsedSones.size()) {
					return false;
				}
				List<Sone> remainingSonesToMatch = Lists.newArrayList(sones);
				for (ParsedSone parsedSone : parsedSones) {
					boolean matchedSone = false;
					for (Sone sone : remainingSonesToMatch) {
						if (!sone.getId().equals(parsedSone.id) || (sone.getTime() != parsedSone.time)) {
							continue;
						}
						remainingSonesToMatch.remove(sone);
						matchedSone = true;
						break;
					}
					if (!matchedSone) {
						return false;
					}
				}
				return true;
			}

			@Override
			protected void describeMismatchSafely(Collection<ParsedSone> parsedSones, Description mismatchDescription) {
				mismatchDescription.appendValue(parsedSones);
			}

			@Override
			public void describeTo(Description description) {
				description.appendValue(sones);
			}
		};
	}

	private Collection<ParsedSone> parseSones(SimpleFieldSet simpleFieldSet, String prefix) throws FSParseException {
		List<ParsedSone> parsedSones = Lists.newArrayList();
		int count = simpleFieldSet.getInt(prefix + "Count");
		for (int index = 0; index < count; ++index) {
			String id = simpleFieldSet.get(format("%s%d.ID", prefix, index));
			String name = simpleFieldSet.get(format("%s%d.Name", prefix, index));
			String niceName = simpleFieldSet.get(format("%s%d.NiceName", prefix, index));
			long time = simpleFieldSet.getLong(format("%s%d.Time", prefix, index));
			parsedSones.add(new ParsedSone(id, name, niceName, time));
		}
		return parsedSones;
	}

	private static class ParsedSone {

		public final String id;
		public final String name;
		public final String niceName;
		public final long time;

		private ParsedSone(String id, String name, String niceName, long time) {
			this.id = id;
			this.name = name;
			this.niceName = niceName;
			this.time = time;
		}

		@Override
		public String toString() {
			return format("Sone[%s,%s,%s,%d]", id, name, niceName, time);
		}
	}

}
