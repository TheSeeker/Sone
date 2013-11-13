/*
 * Sone - L10nFilterTest.java - Copyright © 2013 David Roden
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

package net.pterodactylus.sone.freenet;

import static freenet.l10n.BaseL10n.LANGUAGE.ENGLISH;
import static freenet.l10n.BaseL10n.LANGUAGE.GERMAN;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.Map;

import net.pterodactylus.sone.web.WebInterface;

import freenet.l10n.BaseL10n;
import freenet.l10n.BaseL10n.LANGUAGE;

import com.google.common.collect.ImmutableMap;
import org.junit.Before;
import org.junit.Test;

/**
 * Unit test for {@link L10nFilter}.
 *
 * @author <a href="mailto:bombe@pterodactylus.net">David ‘Bombe’ Roden</a>
 */
public class L10nFilterTest {

	private final BaseL10n l10n = mock(BaseL10n.class);
	private final WebInterface webInterface = mock(WebInterface.class);
	private final L10nFilter l10nFilter = new L10nFilter(webInterface);

	@Before
	public void setup() {
		when(webInterface.getL10n()).thenReturn(l10n);
		when(l10n.getString("SingleString")).thenReturn("Translated Value");
		when(l10n.getString("MultiString")).thenReturn("{0,number}, {1}");
	}

	@Test
	public void formatStringWithoutParameters() {
		String translatedValue = l10nFilter.format(null, "SingleString", Collections.<String, Object>emptyMap());
		assertThat(translatedValue, is("Translated Value"));
	}

	@Test
	public void formatStringWithParametersInEnglish() {
		setLanguage(ENGLISH);
		Map<String, Object> parameters = ImmutableMap.<String, Object>of("0", 17.123, "1", "Value C");
		String translatedValue = l10nFilter.format(null, "MultiString", parameters);
		assertThat(translatedValue, is("17.123, Value C"));
	}

	@Test
	public void formatStringWithParametersInGerman() {
		setLanguage(GERMAN);
		Map<String, Object> parameters = ImmutableMap.<String, Object>of("0", 17.123, "1", "Value C");
		String translatedValue = l10nFilter.format(null, "MultiString", parameters);
		assertThat(translatedValue, is("17,123, Value C"));
	}

	private void setLanguage(LANGUAGE language) {
		when(l10n.getSelectedLanguage()).thenReturn(language);
	}

}
