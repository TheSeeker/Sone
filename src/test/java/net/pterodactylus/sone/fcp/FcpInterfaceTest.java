/*
 * Sone - FcpInterfaceTest.java - Copyright © 2013 David Roden
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

import static freenet.pluginmanager.FredPluginFCP.ACCESS_DIRECT;
import static freenet.pluginmanager.FredPluginFCP.ACCESS_FCP_FULL;
import static freenet.pluginmanager.FredPluginFCP.ACCESS_FCP_RESTRICTED;
import static net.pterodactylus.sone.fcp.FcpInterface.FullAccessRequired.ALWAYS;
import static net.pterodactylus.sone.fcp.FcpInterface.FullAccessRequired.NO;
import static net.pterodactylus.sone.fcp.FcpInterface.FullAccessRequired.WRITING;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.Mockito.mock;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import net.pterodactylus.sone.core.Core;
import net.pterodactylus.sone.freenet.SimpleFieldSetBuilder;
import net.pterodactylus.sone.freenet.fcp.FcpException;

import freenet.pluginmanager.PluginNotFoundException;
import freenet.pluginmanager.PluginReplySender;
import freenet.support.SimpleFieldSet;
import freenet.support.api.Bucket;
import freenet.support.io.ArrayBucket;

import com.google.common.collect.Lists;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.junit.Test;

/**
 * Unit test for {@link FcpInterface}.
 *
 * @author <a href="mailto:bombe@pterodactylus.net">David ‘Bombe’ Roden</a>
 */
public class FcpInterfaceTest {

	private final Core core = mock(Core.class);
	private final FcpInterface fcpInterface = new FcpInterface(core);
	private final CapturingPluginReplySender pluginReplySender = new CapturingPluginReplySender();

	public FcpInterfaceTest() {
		fcpInterface.setActive(true);
		fcpInterface.setFullAccessRequired(ALWAYS);
	}

	@Test
	public void testThatAnInactiveFcpInterfaceReturnsAnErrorForDirectAccess() throws PluginNotFoundException {
		fcpInterface.setActive(false);
		SimpleFieldSet fieldSet = new SimpleFieldSetBuilder().get();
		fcpInterface.handle(pluginReplySender, fieldSet, null, ACCESS_DIRECT);
		verifyErrorWithCode("400");
	}

	@Test
	public void testThatAnInactiveFcpInterfaceReturnsAnErrorForRestrictedFcpAccess() throws PluginNotFoundException {
		fcpInterface.setActive(false);
		SimpleFieldSet fieldSet = new SimpleFieldSetBuilder().get();
		fcpInterface.handle(pluginReplySender, fieldSet, null, ACCESS_FCP_RESTRICTED);
		verifyErrorWithCode("400");
	}

	@Test
	public void testThatAnInactiveFcpInterfaceReturnsAnErrorForFullFcpAccess() throws PluginNotFoundException {
		fcpInterface.setActive(false);
		SimpleFieldSet fieldSet = new SimpleFieldSetBuilder().get();
		fcpInterface.handle(pluginReplySender, fieldSet, null, ACCESS_FCP_FULL);
		verifyErrorWithCode("400");
	}

	@Test
	public void testThatAnActiveFcpInterfaceReturnsAnErrorForAnUnknownMessage() {
		SimpleFieldSet fieldSet = new SimpleFieldSetBuilder().put("Message", "Foo").get();
		fcpInterface.handle(pluginReplySender, fieldSet, null, ACCESS_DIRECT);
		verifyError();
	}

	@Test
	public void testThatAnActiveFcpInterfaceReturnsAnErrorForAMessageWithoutIdentifier() {
		fcpInterface.addCommand("ReadOnlyPing", new ReadOnlyPing());
		SimpleFieldSet fieldSet = new SimpleFieldSetBuilder().put("Message", "ReadOnlyPing").get();
		fcpInterface.handle(pluginReplySender, fieldSet, null, ACCESS_DIRECT);
		verifyError();
	}

	@Test
	public void testThatAnActiveFcpInterfaceRequiringFullAccessAllowsDirectFcpAccessForReadOnlyCommand() {
		fcpInterface.addCommand("ReadOnlyPing", new ReadOnlyPing());
		SimpleFieldSet fieldSet = new SimpleFieldSetBuilder().put("Message", "ReadOnlyPing").put("Identifier", "foo").get();
		fcpInterface.handle(pluginReplySender, fieldSet, null, ACCESS_DIRECT);
		verifyReplyWithMessage("ReadOnlyPong");
	}

	@Test
	public void testThatAnActiveFcpInterfaceRequiringFullAccessForWritesAllowsDirectFcpAccessForReadOnlyCommand() {
		fcpInterface.setFullAccessRequired(WRITING);
		fcpInterface.addCommand("ReadOnlyPing", new ReadOnlyPing());
		SimpleFieldSet fieldSet = new SimpleFieldSetBuilder().put("Message", "ReadOnlyPing").put("Identifier", "foo").get();
		fcpInterface.handle(pluginReplySender, fieldSet, null, ACCESS_DIRECT);
		verifyReplyWithMessage("ReadOnlyPong");
	}

	@Test
	public void testThatAnActiveFcpInterfaceNotRequiringFullAccessAllowsDirectFcpAccessForReadOnlyCommand() {
		fcpInterface.setFullAccessRequired(NO);
		fcpInterface.addCommand("ReadOnlyPing", new ReadOnlyPing());
		SimpleFieldSet fieldSet = new SimpleFieldSetBuilder().put("Message", "ReadOnlyPing").put("Identifier", "foo").get();
		fcpInterface.handle(pluginReplySender, fieldSet, null, ACCESS_DIRECT);
		verifyReplyWithMessage("ReadOnlyPong");
	}

	@Test
	public void testThatAnActiveFcpInterfaceRequiringFullAccessAllowsDirectFcpAccessForReadWriteCommand() {
		fcpInterface.addCommand("ReadWritePing", new ReadWritePing());
		SimpleFieldSet fieldSet = new SimpleFieldSetBuilder().put("Message", "ReadWritePing").put("Identifier", "foo").get();
		fcpInterface.handle(pluginReplySender, fieldSet, null, ACCESS_DIRECT);
		verifyReplyWithMessage("ReadWritePong");
	}

	@Test
	public void testThatAnActiveFcpInterfaceRequiringFullAccessForWritesAllowsDirectFcpAccessForReadWriteCommand() {
		fcpInterface.setFullAccessRequired(WRITING);
		fcpInterface.addCommand("ReadWritePing", new ReadWritePing());
		SimpleFieldSet fieldSet = new SimpleFieldSetBuilder().put("Message", "ReadWritePing").put("Identifier", "foo").get();
		fcpInterface.handle(pluginReplySender, fieldSet, null, ACCESS_DIRECT);
		verifyReplyWithMessage("ReadWritePong");
	}

	@Test
	public void testThatAnActiveFcpInterfaceNotRequiringFullAccessAllowsDirectFcpAccessForReadWriteCommand() {
		fcpInterface.setFullAccessRequired(NO);
		fcpInterface.addCommand("ReadWritePing", new ReadWritePing());
		SimpleFieldSet fieldSet = new SimpleFieldSetBuilder().put("Message", "ReadWritePing").put("Identifier", "foo").get();
		fcpInterface.handle(pluginReplySender, fieldSet, null, ACCESS_DIRECT);
		verifyReplyWithMessage("ReadWritePong");
	}

	@Test
	public void testThatAnActiveFcpInterfaceRequiringFullAccessAllowsFullFcpAccessForReadOnlyCommand() {
		fcpInterface.addCommand("ReadOnlyPing", new ReadOnlyPing());
		SimpleFieldSet fieldSet = new SimpleFieldSetBuilder().put("Message", "ReadOnlyPing").put("Identifier", "foo").get();
		fcpInterface.handle(pluginReplySender, fieldSet, null, ACCESS_FCP_FULL);
		verifyReplyWithMessage("ReadOnlyPong");
	}

	@Test
	public void testThatAnActiveFcpInterfaceRequiringFullAccessForWritesAllowsFullFcpAccessForReadOnlyCommand() {
		fcpInterface.setFullAccessRequired(WRITING);
		fcpInterface.addCommand("ReadOnlyPing", new ReadOnlyPing());
		SimpleFieldSet fieldSet = new SimpleFieldSetBuilder().put("Message", "ReadOnlyPing").put("Identifier", "foo").get();
		fcpInterface.handle(pluginReplySender, fieldSet, null, ACCESS_FCP_FULL);
		verifyReplyWithMessage("ReadOnlyPong");
	}

	@Test
	public void testThatAnActiveFcpInterfaceNotRequiringFullAccessAllowsFullFcpAccessForReadOnlyCommand() {
		fcpInterface.setFullAccessRequired(NO);
		fcpInterface.addCommand("ReadOnlyPing", new ReadOnlyPing());
		SimpleFieldSet fieldSet = new SimpleFieldSetBuilder().put("Message", "ReadOnlyPing").put("Identifier", "foo").get();
		fcpInterface.handle(pluginReplySender, fieldSet, null, ACCESS_FCP_FULL);
		verifyReplyWithMessage("ReadOnlyPong");
	}

	private void verifyReplyWithMessage(String messageName) {
		assertThat(pluginReplySender.results, hasSize(1));
		assertThat(pluginReplySender.results.get(0).fieldSet, notNullValue());
		assertThat(pluginReplySender.results.get(0).fieldSet.get("Message"), is(messageName));
	}

	@Test
	public void testThatAnActiveFcpInterfaceRequiringFullAccessAllowsFullFcpAccessForReadWriteCommand() {
		fcpInterface.addCommand("ReadWritePing", new ReadWritePing());
		SimpleFieldSet fieldSet = new SimpleFieldSetBuilder().put("Message", "ReadWritePing").put("Identifier", "foo").get();
		fcpInterface.handle(pluginReplySender, fieldSet, null, ACCESS_FCP_FULL);
		verifyReplyWithMessage("ReadWritePong");
	}

	@Test
	public void testThatAnActiveFcpInterfaceRequiringFullAccessForWritesAllowsFullFcpAccessForReadWriteCommand() {
		fcpInterface.setFullAccessRequired(WRITING);
		fcpInterface.addCommand("ReadWritePing", new ReadWritePing());
		SimpleFieldSet fieldSet = new SimpleFieldSetBuilder().put("Message", "ReadWritePing").put("Identifier", "foo").get();
		fcpInterface.handle(pluginReplySender, fieldSet, null, ACCESS_FCP_FULL);
		verifyReplyWithMessage("ReadWritePong");
	}

	@Test
	public void testThatAnActiveFcpInterfaceNotRequiringFullAccessAllowsFullFcpAccessForReadWriteCommand() {
		fcpInterface.setFullAccessRequired(NO);
		fcpInterface.addCommand("ReadWritePing", new ReadWritePing());
		SimpleFieldSet fieldSet = new SimpleFieldSetBuilder().put("Message", "ReadWritePing").put("Identifier", "foo").get();
		fcpInterface.handle(pluginReplySender, fieldSet, null, ACCESS_FCP_FULL);
		verifyReplyWithMessage("ReadWritePong");
	}

	@Test
	public void testThatAnActiveFcpInterfaceRequiringFullAccessDoesNotAllowRestrictedFcpAccessForReadOnlyCommand() {
		fcpInterface.addCommand("ReadOnlyPing", new ReadOnlyPing());
		SimpleFieldSet fieldSet = new SimpleFieldSetBuilder().put("Message", "ReadOnlyPing").put("Identifier", "foo").get();
		fcpInterface.handle(pluginReplySender, fieldSet, null, ACCESS_FCP_RESTRICTED);
		verifyErrorWithCode("401");
	}

	@Test
	public void testThatAnActiveFcpInterfaceRequiringFullAccessForWritesAllowsRestrictedFcpAccessForReadOnlyCommand() {
		fcpInterface.setFullAccessRequired(WRITING);
		fcpInterface.addCommand("ReadOnlyPing", new ReadOnlyPing());
		SimpleFieldSet fieldSet = new SimpleFieldSetBuilder().put("Message", "ReadOnlyPing").put("Identifier", "foo").get();
		fcpInterface.handle(pluginReplySender, fieldSet, null, ACCESS_FCP_RESTRICTED);
		verifyReplyWithMessage("ReadOnlyPong");
	}

	@Test
	public void testThatAnActiveFcpInterfaceNotRequiringFullAccessAllowsRestrictedFcpAccessForReadOnlyCommand() {
		fcpInterface.setFullAccessRequired(NO);
		fcpInterface.addCommand("ReadOnlyPing", new ReadOnlyPing());
		SimpleFieldSet fieldSet = new SimpleFieldSetBuilder().put("Message", "ReadOnlyPing").put("Identifier", "foo").get();
		fcpInterface.handle(pluginReplySender, fieldSet, null, ACCESS_FCP_RESTRICTED);
		verifyReplyWithMessage("ReadOnlyPong");
	}

	@Test
	public void testThatAnActiveFcpInterfaceRequiringFullAccessDoesNotAllowRestrictedFcpAccessForReadWriteCommand() {
		fcpInterface.addCommand("ReadWritePing", new ReadWritePing());
		SimpleFieldSet fieldSet = new SimpleFieldSetBuilder().put("Message", "ReadWritePing").put("Identifier", "foo").get();
		fcpInterface.handle(pluginReplySender, fieldSet, null, ACCESS_FCP_RESTRICTED);
		verifyErrorWithCode("401");
	}

	@Test
	public void testThatAnActiveFcpInterfaceRequiringFullAccessForWritesDoesNotAllowRestrictedFcpAccessForReadWriteCommand() {
		fcpInterface.setFullAccessRequired(WRITING);
		fcpInterface.addCommand("ReadWritePing", new ReadWritePing());
		SimpleFieldSet fieldSet = new SimpleFieldSetBuilder().put("Message", "ReadWritePing").put("Identifier", "foo").get();
		fcpInterface.handle(pluginReplySender, fieldSet, null, ACCESS_FCP_RESTRICTED);
		verifyErrorWithCode("401");
	}

	@Test
	public void testThatAnActiveFcpInterfaceNotRequiringFullAccessAllowsRestrictedFcpAccessForReadWriteCommand() {
		fcpInterface.setFullAccessRequired(NO);
		fcpInterface.addCommand("ReadWritePing", new ReadWritePing());
		SimpleFieldSet fieldSet = new SimpleFieldSetBuilder().put("Message", "ReadWritePing").put("Identifier", "foo").get();
		fcpInterface.handle(pluginReplySender, fieldSet, null, ACCESS_FCP_RESTRICTED);
		verifyReplyWithMessage("ReadWritePong");
	}

	@Test
	public void testThatAFaultyCommandResultsInAnError() {
		fcpInterface.addCommand("Faulty", new FaultyCommand());
		SimpleFieldSet fieldSet = new SimpleFieldSetBuilder().put("Message", "Faulty").put("Identifier", "foo").get();
		fcpInterface.handle(pluginReplySender, fieldSet, null, ACCESS_FCP_FULL);
		verifyError();
	}

	@Test
	public void testThatAFaultyPluginReplySenderIsHandled() {
		SimpleFieldSet fieldSet = new SimpleFieldSetBuilder().put("Message", "Faulty").put("Identifier", "foo").get();
		fcpInterface.handle(new FaultyPluginReplySender(), fieldSet, null, ACCESS_FCP_FULL);
	}

	@Test
	public void testThatACommandWithDataIsHandledCorrectly() throws IOException {
		fcpInterface.addCommand("CommandWithData", new CommandWithData());
		SimpleFieldSet fieldSet = new SimpleFieldSetBuilder().put("Message", "CommandWithData").put("Identifier", "foo").get();
		fcpInterface.handle(pluginReplySender, fieldSet, null, ACCESS_FCP_FULL);
		verifyReplyWithMessage("ReturnedData");
		assertThat(pluginReplySender.results.get(0).bucket, notNullValue());
		assertThat(pluginReplySender.results.get(0).bucket.size(), is(3L));
		assertThat(pluginReplySender.results.get(0).bucket.getInputStream(), delivers(new byte[] { 1, 2, 3 }));
	}

	@Test
	public void testThatACommandWithABucketIsHandledCorrectly() throws IOException {
		fcpInterface.addCommand("CommandWithBucket", new CommandWithBucket());
		SimpleFieldSet fieldSet = new SimpleFieldSetBuilder().put("Message", "CommandWithBucket").put("Identifier", "foo").get();
		fcpInterface.handle(pluginReplySender, fieldSet, null, ACCESS_FCP_FULL);
		verifyReplyWithMessage("ReturnedBucket");
		assertThat(pluginReplySender.results.get(0).bucket, notNullValue());
		assertThat(pluginReplySender.results.get(0).bucket.size(), is(3L));
		assertThat(pluginReplySender.results.get(0).bucket.getInputStream(), delivers(new byte[] { 4, 5, 6 }));
	}

	private Matcher<InputStream> delivers(final byte[] data) {
		return new TypeSafeMatcher<InputStream>() {
			byte[] readData = new byte[data.length];

			@Override
			protected boolean matchesSafely(InputStream inputStream) {
				int offset = 0;
				try {
					while (true) {
						int r = inputStream.read();
						if (r == -1) {
							return offset == data.length;
						}
						if (offset == data.length) {
							return false;
						}
						if (data[offset] != (readData[offset] = (byte) r)) {
							return false;
						}
						offset++;
					}
				} catch (IOException ioe1) {
					return false;
				}
			}

			@Override
			public void describeTo(Description description) {
				description.appendValue(data);
			}

			@Override
			protected void describeMismatchSafely(InputStream item, Description mismatchDescription) {
				mismatchDescription.appendValue(readData);
			}
		};
	}

	private void verifyError() {
		assertThat(pluginReplySender.results, hasSize(1));
		assertThat(pluginReplySender.results.get(0).fieldSet, notNullValue());
		assertThat(pluginReplySender.results.get(0).fieldSet.get("Message"), is("Error"));
	}

	private void verifyErrorWithCode(String errorCode) {
		verifyError();
		assertThat(pluginReplySender.results.get(0).fieldSet.get("ErrorCode"), is(errorCode));
	}

	private static class CapturingPluginReplySender extends PluginReplySender {

		public final List<PluginReplySenderResult> results = Lists.newArrayList();

		public CapturingPluginReplySender() {
			super(null, null);
		}

		@Override
		public void send(SimpleFieldSet params, Bucket bucket) throws PluginNotFoundException {
			results.add(new PluginReplySenderResult(params, bucket));
		}

	}

	private static class PluginReplySenderResult {

		public final SimpleFieldSet fieldSet;
		public final Bucket bucket;

		public PluginReplySenderResult(SimpleFieldSet fieldSet, Bucket bucket) {
			this.fieldSet = fieldSet;
			this.bucket = bucket;
		}

	}

	private static class ReadOnlyPing extends AbstractSoneCommand {

		public ReadOnlyPing() {
			super(null, false);
		}

		@Override
		public Response execute(SimpleFieldSet parameters, Bucket data, AccessType accessType) throws FcpException {
			return new Response("ReadOnlyPong", new SimpleFieldSetBuilder().get());
		}

	}

	private static class ReadWritePing extends AbstractSoneCommand {

		public ReadWritePing() {
			super(null, true);
		}

		@Override
		public Response execute(SimpleFieldSet parameters, Bucket data, AccessType accessType) throws FcpException {
			return new Response("ReadWritePong", new SimpleFieldSetBuilder().get());
		}

	}

	private static class FaultyCommand extends AbstractSoneCommand {

		public FaultyCommand() {
			super(null, false);
		}

		@Override
		public Response execute(SimpleFieldSet parameters, Bucket data, AccessType accessType) throws FcpException {
			throw new RuntimeException("I’m faulty!");
		}

	}

	private static class FaultyPluginReplySender extends PluginReplySender {

		public FaultyPluginReplySender() {
			super(null, null);
		}

		@Override
		public void send(SimpleFieldSet params, Bucket bucket) throws PluginNotFoundException {
			throw new PluginNotFoundException();
		}

	}

	private static class CommandWithData extends AbstractSoneCommand {

		protected CommandWithData() {
			super(null);
		}

		@Override
		public Response execute(SimpleFieldSet parameters, Bucket data, AccessType accessType) throws FcpException {
			return new Response("ReturnedData", new SimpleFieldSetBuilder().get(), new byte[] { 1, 2, 3 });
		}

	}

	private static class CommandWithBucket extends AbstractSoneCommand {

		protected CommandWithBucket() {
			super(null);
		}

		@Override
		public Response execute(SimpleFieldSet parameters, Bucket data, AccessType accessType) throws FcpException {
			return new Response("ReturnedBucket", new SimpleFieldSetBuilder().get(), new ArrayBucket(new byte[] { 4, 5, 6 }));
		}

	}

}
