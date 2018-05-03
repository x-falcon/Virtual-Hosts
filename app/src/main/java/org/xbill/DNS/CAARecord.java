// Copyright (c) 2004 Brian Wellington (bwelling@xbill.org)

package org.xbill.DNS;

import java.io.*;
import org.xbill.DNS.utils.*;

/**
 * Certification Authority Authorization
 *
 * @author Brian Wellington
 */

public class CAARecord extends Record {

private static final long serialVersionUID = 8544304287274216443L;

public static class Flags {
	private Flags() {}

	public static final int IssuerCritical = 128;
}

private int flags;
private byte [] tag;
private byte [] value;

CAARecord() {} 

Record
getObject() {
	return new CAARecord();
}

/**
 * Creates an CAA Record from the given data.
 * @param flags The flags.
 * @param tag The tag.
 * @param value The value.
 */
public
CAARecord(Name name, int dclass, long ttl, int flags, String tag, String value)
{
	super(name, Type.CAA, dclass, ttl);
	this.flags = checkU8("flags", flags);
	try {
		this.tag = byteArrayFromString(tag);
		this.value = byteArrayFromString(value);
	}
	catch (TextParseException e) {
		throw new IllegalArgumentException(e.getMessage());
	}
}

void
rrFromWire(DNSInput in) throws IOException {
	flags = in.readU8();
	tag = in.readCountedString();
	value = in.readByteArray();
}

void
rdataFromString(Tokenizer st, Name origin) throws IOException {
	flags = st.getUInt8();
	try {
		tag = byteArrayFromString(st.getString());
		value = byteArrayFromString(st.getString());
	}
	catch (TextParseException e) {
		throw st.exception(e.getMessage());
	}
}

String
rrToString() {
	StringBuffer sb = new StringBuffer();
	sb.append(flags);
	sb.append(" ");
	sb.append(byteArrayToString(tag, false));
	sb.append(" ");
	sb.append(byteArrayToString(value, true));
	return sb.toString();
}

/** Returns the flags. */
public int
getFlags() {
	return flags;
}

/** Returns the tag. */
public String
getTag() {
	return byteArrayToString(tag, false);
}

/** Returns the value */
public String
getValue() {
	return byteArrayToString(value, false);
}

void
rrToWire(DNSOutput out, Compression c, boolean canonical) {
	out.writeU8(flags);
	out.writeCountedString(tag);
	out.writeByteArray(value);
}

}
