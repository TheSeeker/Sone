/*
 * Sone - StringBucketTest.java - Copyright © 2013 David Roden
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

import static net.pterodactylus.sone.Matchers.delivers;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;

import freenet.support.api.Bucket;

import org.junit.Test;

/**
 * TODO
 *
 * @author <a href="mailto:bombe@pterodactylus.net">David ‘Bombe’ Roden</a>
 */
public class StringBucketTest {

	private static final String TEST_STRING = "StringBücket Test";
	private final StringBucket stringBucket = new StringBucket(TEST_STRING, Charset.forName("UTF-8"));

	@Test
	public void shadowYieldsTheSameContent() throws IOException {
		Bucket secondBucket = stringBucket.createShadow();
		assertThat(secondBucket.getInputStream(), delivers(TEST_STRING.getBytes("UTF-8")));
	}

	@Test
	public void freeingTheBucketDoesNothingBad() {
		stringBucket.free();
	}

	@Test
	public void stringBucketHasTheCorrectSize() throws UnsupportedEncodingException {
		assertThat(stringBucket.size(), is((long) TEST_STRING.getBytes("UTF-8").length));
	}

	@Test
	public void inputStreamDeliversContent() throws UnsupportedEncodingException {
		assertThat(stringBucket.getInputStream(), delivers(TEST_STRING.getBytes("UTF-8")));
	}

	@Test
	public void nameContainsReferenceToStringBucket() {
		assertThat(stringBucket.getName(), containsString(stringBucket.getClass().getSimpleName()));
	}

	@Test
	public void noOutputStreamIsReturned() {
		assertThat(stringBucket.getOutputStream(), nullValue());
	}

	@Test
	public void theBucketIsReadOnly() {
		assertThat(stringBucket.isReadOnly(), is(true));
	}

	@Test
	public void setStringBucketReadOnly() {
		stringBucket.setReadOnly();
		assertThat(stringBucket.isReadOnly(), is(true));
	}

	@Test
	public void storingToObjectContainerDoesNothing() {
		stringBucket.storeTo(null);
	}

	@Test
	public void removalFromObjectContainerDoesNothing() {
		stringBucket.removeFrom(null);
	}

}
