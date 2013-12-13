/*
 * Sone - StringBucket.java - Copyright © 2010–2013 David Roden
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

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;

import freenet.support.api.Bucket;
import com.db4o.ObjectContainer;

/**
 * {@link Bucket} implementation wrapped around a {@link String}.
 *
 * @author <a href="mailto:bombe@pterodactylus.net">David ‘Bombe’ Roden</a>
 */
public class StringBucket implements Bucket {

	/** The string to deliver. */
	private final String string;

	/** The encoding for the data. */
	private final Charset encoding;

	/**
	 * Creates a new string bucket using the default encoding.
	 *
	 * @param string
	 *            The string to wrap
	 */
	public StringBucket(String string) {
		this(string, Charset.defaultCharset());
	}

	/**
	 * Creates a new string bucket, using the given encoding to create a byte
	 * array from the string.
	 *
	 * @param string
	 *            The string to wrap
	 * @param encoding
	 *            The encoding of the data
	 */
	public StringBucket(String string, Charset encoding) {
		this.string = string;
		this.encoding = encoding;
	}

	@Override
	public Bucket createShadow() {
		return new StringBucket(string, encoding);
	}

	@Override
	public void free() {
		/* ignore. */
	}

	@Override
	public InputStream getInputStream() {
		return new ByteArrayInputStream(string.getBytes(encoding));
	}

	@Override
	public String getName() {
		return getClass().getName() + "@" + hashCode();
	}

	@Override
	public OutputStream getOutputStream() {
		return null;
	}

	@Override
	public boolean isReadOnly() {
		return true;
	}

	@Override
	public void removeFrom(ObjectContainer objectContainer) {
		/* ignore. */
	}

	@Override
	public void setReadOnly() {
		/* ignore, it is already read-only. */
	}

	@Override
	public long size() {
		return string.getBytes(encoding).length;
	}

	@Override
	public void storeTo(ObjectContainer objectContainer) {
		/* ignore. */
	}

}
