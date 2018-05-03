// Copyright (c) 1999-2004 Brian Wellington (bwelling@xbill.org)

package org.xbill.DNS;

import java.io.*;
import org.xbill.DNS.utils.*;

/**
 * Transport Layer Security Authentication
 *
 * @author Brian Wellington
 */

public class TLSARecord extends Record {

private static final long serialVersionUID = 356494267028580169L;

public static class CertificateUsage {
	private CertificateUsage() {}

	public static final int CA_CONSTRAINT = 0;
	public static final int SERVICE_CERTIFICATE_CONSTRAINT = 1;
	public static final int TRUST_ANCHOR_ASSERTION = 2;
	public static final int DOMAIN_ISSUED_CERTIFICATE = 3;
}

public static class Selector {
	private Selector() {}

	/**
	 * Full certificate; the Certificate binary structure defined in
	 * [RFC5280]
	 */
	public static final int FULL_CERTIFICATE = 0;

	/**
	 * SubjectPublicKeyInfo; DER-encoded binary structure defined in
	 * [RFC5280]
	 */
	public static final int SUBJECT_PUBLIC_KEY_INFO = 1;
}

public static class MatchingType {
	private MatchingType() {}

	/** Exact match on selected content */
	public static final int EXACT = 0;

	/** SHA-256 hash of selected content [RFC6234] */
	public static final int SHA256 = 1;

	/** SHA-512 hash of selected content [RFC6234] */
	public static final int SHA512 = 2;
}

private int certificateUsage;
private int selector;
private int matchingType;
private byte [] certificateAssociationData;

TLSARecord() {}

Record
getObject() {
	return new TLSARecord();
}

/**
 * Creates an TLSA Record from the given data
 * @param certificateUsage The provided association that will be used to
 * match the certificate presented in the TLS handshake. 
 * @param selector The part of the TLS certificate presented by the server
 * that will be matched against the association data. 
 * @param matchingType How the certificate association is presented.
 * @param certificateAssociationData The "certificate association data" to be
 * matched.
 */
public
TLSARecord(Name name, int dclass, long ttl, 
	   int certificateUsage, int selector, int matchingType,
	   byte [] certificateAssociationData)
{
	super(name, Type.TLSA, dclass, ttl);
	this.certificateUsage = checkU8("certificateUsage", certificateUsage);
	this.selector = checkU8("selector", selector);
	this.matchingType = checkU8("matchingType", matchingType);
	this.certificateAssociationData = checkByteArrayLength(
						"certificateAssociationData",
						certificateAssociationData,
						0xFFFF);
}

void
rrFromWire(DNSInput in) throws IOException {
	certificateUsage = in.readU8();
	selector = in.readU8();
	matchingType = in.readU8();
	certificateAssociationData = in.readByteArray();
}

void
rdataFromString(Tokenizer st, Name origin) throws IOException {
	certificateUsage = st.getUInt8();
	selector = st.getUInt8();
	matchingType = st.getUInt8();
	certificateAssociationData = st.getHex();
}

/** Converts rdata to a String */
String
rrToString() {
	StringBuffer sb = new StringBuffer();
	sb.append(certificateUsage);
	sb.append(" ");
	sb.append(selector);
	sb.append(" ");
	sb.append(matchingType);
	sb.append(" ");
	sb.append(base16.toString(certificateAssociationData));

	return sb.toString();
}

void
rrToWire(DNSOutput out, Compression c, boolean canonical) {
	out.writeU8(certificateUsage);
	out.writeU8(selector);
	out.writeU8(matchingType);
	out.writeByteArray(certificateAssociationData);
}

/** Returns the certificate usage of the TLSA record */
public int
getCertificateUsage() {
	return certificateUsage;
}

/** Returns the selector of the TLSA record */
public int
getSelector() {
	return selector;
}

/** Returns the matching type of the TLSA record */
public int
getMatchingType() {
	return matchingType;
}

/** Returns the certificate associate data of this TLSA record */
public final byte []
getCertificateAssociationData() {
	return certificateAssociationData;
}

}
