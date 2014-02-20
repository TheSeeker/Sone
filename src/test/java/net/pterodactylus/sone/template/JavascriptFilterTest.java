package net.pterodactylus.sone.template;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.util.Collections;

import org.junit.Test;

/**
 * Unit test for {@link JavascriptFilter}.
 *
 * @author <a href="mailto:bombe@pterodactylus.net">David ‘Bombe’ Roden</a>
 */
public class JavascriptFilterTest {

	private final JavascriptFilter javascriptFilter = new JavascriptFilter();

	@Test
	public void enclosesAStringInDoubleQuotes() {
		assertThat(formatString("Some String"), is("\"Some String\""));
	}

	@Test
	public void escapesDoubleQuotes() {
		assertThat(formatString("Some \" Quote"), is("\"Some \\\" Quote\""));
	}

	@Test
	public void escapesBackslash() {
		assertThat(formatString("Some \\ Quote"), is("\"Some \\\\ Quote\""));
	}

	@Test
	public void escapesCarriageReturn() {
		assertThat(formatString("Some \r Quote"), is("\"Some \\r Quote\""));
	}

	@Test
	public void escapesLineFeed() {
		assertThat(formatString("Some \n Quote"), is("\"Some \\n Quote\""));
	}

	@Test
	public void escapesTab() {
		assertThat(formatString("Some \t Quote"), is("\"Some \\t Quote\""));
	}

	@Test
	public void escapesControlCharacters() {
		assertThat(formatString("\u0001a\u0002"), is("\"\\x01a\\x02\""));
	}

	private String formatString(String string) {
		return (String) javascriptFilter.format(null, string, Collections.<String, Object>emptyMap());
	}

}
