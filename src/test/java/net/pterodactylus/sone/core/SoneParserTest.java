package net.pterodactylus.sone.core;

import static com.google.common.base.Optional.absent;
import static com.google.common.base.Optional.fromNullable;
import static com.google.common.base.Optional.of;
import static java.lang.String.format;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;

import net.pterodactylus.sone.data.Client;
import net.pterodactylus.sone.data.Image;
import net.pterodactylus.sone.data.Sone;
import net.pterodactylus.sone.database.Database;
import net.pterodactylus.sone.database.SoneBuilder.SoneCreated;
import net.pterodactylus.sone.database.memory.MemoryDatabase;

import com.google.common.base.Joiner;
import com.google.common.base.Optional;
import org.junit.Test;

/**
 * Unit test for {@link SoneParser}.
 *
 * @author <a href="mailto:d.roden@xplosion.de">David Roden</a>
 */
public class SoneParserTest {

	private final Core core = mock(Core.class);
	private final Database database = new MemoryDatabase(null);
	private final Sone originalSone = database.newSoneBuilder().by("test").using(new Client("TestClient", "1.0")).build(Optional.<SoneCreated>absent());
	private final SoneXmlBuilder soneXmlBuilder = new SoneXmlBuilder();
	private final SoneParser soneParser = new SoneParser();

	public SoneParserTest() {
		Optional<Image> image = mock(Optional.class);
		when(core.getImage(anyString())).thenReturn(image);
	}

	@Test
	public void verifyThatAnInvalidXmlDocumentIsNotParsed() throws UnsupportedEncodingException, SoneException {
		assertThat(soneParser.parseSone(database, originalSone, getInputStream("<xml>This is not valid XML.</invalid>")), nullValue());
	}

	@Test
	public void verifyThatANegativeProtocolVersionCausesAnError() throws SoneException {
		assertThat(soneParser.parseSone(database, originalSone, soneXmlBuilder.setProtocolVersion("-1").get()), nullValue());
	}

	@Test
	public void verifyThatAMissingClientCausesTheOriginalClientToBeUsed() throws SoneException {
		Sone sone = soneParser.parseSone(database, originalSone, soneXmlBuilder.removeClientInformation().get());
		assertThat(sone, notNullValue());
		assertThat(sone.getClient(), notNullValue());
		assertThat(sone.getClient(), is(originalSone.getClient()));
	}

	@Test
	public void verifyThatTheCreatedSoneMeetsAllExpectations() throws SoneException {
		Sone sone = soneParser.parseSone(database, originalSone, soneXmlBuilder.get());
		assertThat(sone, notNullValue());
		assertThat(sone.getTime(), is(1000L));
		assertThat(sone.getClient(), notNullValue());
		assertThat(sone.getClient().getName(), is("Test-Client"));
		assertThat(sone.getClient().getVersion(), is("1.0"));
		assertThat(sone.getProfile(), notNullValue());
		assertThat(sone.getProfile().getFirstName(), is("First"));
		assertThat(sone.getProfile().getMiddleName(), is("M."));
		assertThat(sone.getProfile().getLastName(), is("Last"));
		assertThat(sone.getProfile().getBirthYear(), is(2000));
		assertThat(sone.getProfile().getBirthMonth(), is(9));
		assertThat(sone.getProfile().getBirthDay(), is(13));
		assertThat(sone.getProfile().getAvatar(), is("avatar-id"));
	}

	public InputStream getInputStream(String content) throws UnsupportedEncodingException {
		return new ByteArrayInputStream(content.getBytes("UTF-8"));
	}

	private static class SoneXmlBuilder {

		private Optional<Long> time = of(1000L);
		private Optional<String> protocolVersion = of("0");
		private Optional<String> clientInformation = of("<name>Test-Client</name><version>1.0</version>");
		private Optional<String> profile = of(Joiner.on("").join(
				"<first-name>First</first-name>",
				"<middle-name>M.</middle-name>",
				"<last-name>Last</last-name>",
				"<birth-year>2000</birth-year>",
				"<birth-month>9</birth-month>",
				"<birth-day>13</birth-day>",
				"<avatar>avatar-id</avatar>",
				"<fields>",
				"<field><field-name>Custom Field</field-name><field-value>Custom Value</field-value></field>",
				"</fields>"
		));
		private Optional<String> posts = of("<post><id>post-id</id><time>1</time><recipient>recipient</recipient><text>Hello!</text></post>");
		private Optional<String> replies = of("<reply><id>reply-id</id><post-id>post-id</post-id><time>2</time><text>Reply!</text></reply>");

		public SoneXmlBuilder removeTime() {
			time = absent();
			return this;
		}

		public SoneXmlBuilder setProtocolVersion(String protocolVersion) {
			this.protocolVersion = fromNullable(protocolVersion);
			return this;
		}

		public SoneXmlBuilder removeProtocolVersion() {
			this.protocolVersion = absent();
			return this;
		}

		public SoneXmlBuilder setClientInformation(String name, String version) {
			clientInformation = of("<name>" + name + "</name><version>" + version + "</version>");
			return this;
		}

		public SoneXmlBuilder removeClientInformation() {
			clientInformation = absent();
			return this;
		}

		public SoneXmlBuilder removeProfile() {
			profile = absent();
			return this;
		}

		public SoneXmlBuilder removePost() {
			posts = absent();
			return this;
		}

		public SoneXmlBuilder removeReply() {
			replies = absent();
			return this;
		}

		public InputStream get() {
			StringBuilder content = new StringBuilder();
			content.append("<sone>");
			if (time.isPresent()) {
				content.append(createXmlElement("time", String.valueOf(time.get())));
			}
			if (protocolVersion.isPresent()) {
				content.append(createXmlElement("protocol-version", protocolVersion.get()));
			}
			if (clientInformation.isPresent()) {
				content.append(createXmlElement("client", clientInformation.get()));
			}
			if (profile.isPresent()) {
				content.append(createXmlElement("profile", profile.get()));
			}
			if (posts.isPresent()) {
				content.append(createXmlElement("posts", posts.get()));
			}
			content.append("</sone>");
			try {
				String xmlString = content.toString();
				System.out.println(xmlString);
				return new ByteArrayInputStream(xmlString.getBytes("UTF-8"));
			} catch (UnsupportedEncodingException e) {
				/* ignore. */
			}
			return null;
		}

		private String createXmlElement(String xmlElement, String content) {
			return format("<%s>%s</%1$s>", xmlElement, content);
		}

	}

}
