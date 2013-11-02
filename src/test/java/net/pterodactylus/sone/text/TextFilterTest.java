package net.pterodactylus.sone.text;

import static net.pterodactylus.sone.text.TextFilter.filter;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import org.junit.Test;

/**
 * Unit test for {@link TextFilter}.
 *
 * @author <a href="mailto:d.roden@xplosion.de">David Roden</a>
 */
public class TextFilterTest {

	private static final String ORIGINAL_TEXT = "1:https://127.0.0.1/ 2:http://127.0.0.1/ 3:http://127.0.0.1 4:https://127.0.0.1 5:http://localhost/ 6:http://localhost";
	private static final String FILTERED1 = "1: 2: 3: 4: 5:http://localhost/ 6:http://localhost";
	private static final String FILTERED2 = "1:https://127.0.0.1/ 2:http://127.0.0.1/ 3:http://127.0.0.1 4:https://127.0.0.1 5: 6:";

	@Test
	public void canCreateTextFilter() {
		new TextFilter();
	}

	@Test
	public void removeLinksTo127001() {
		assertThat(filter("127.0.0.1", ORIGINAL_TEXT), is(FILTERED1));
	}

	@Test
	public void removeLinksToLocalhost() {
		assertThat(filter("localhost", ORIGINAL_TEXT), is(FILTERED2));
	}

	@Test
	public void doNotRemoveLinksWithoutHostHeader() {
		assertThat(filter(null, ORIGINAL_TEXT), is(ORIGINAL_TEXT));
	}

}
