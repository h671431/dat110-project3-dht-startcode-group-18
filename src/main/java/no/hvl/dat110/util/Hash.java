package no.hvl.dat110.util;

/**
 * exercise/demo purpose in dat110
 * @author tdoy
 *
 */

import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Hash { 
	
	
	public static BigInteger hashOf(String entity) {
		
		BigInteger hashint = null;
		
		try{
			//opretter en MessageDigest-instans for MD5
			MessageDigest md = MessageDigest.getInstance("MD5");

			//Oppdater innstrengen i meldingsdigesten
			md.update(entity.getBytes());

			//generer hash
			byte[] digest = md.digest();

			//knverter byte-tabellen til BigInteger
			hashint = new BigInteger(1, digest);

		} catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }


        return hashint;
	}
	
	public static BigInteger addressSize() {
		
		//Beregn antall bit = bitSize()
		int numBits = bitSize();

		//beregn størrelsen på adressen = 2 opphøyd i antall biter
		BigInteger adressSize = BigInteger.valueOf(2).pow(numBits);


		return adressSize;
	}
	
	public static int bitSize() {
		
		int digestlen = 0;
		
		try{
			//hent MessageDigest for MD5
			MessageDigest md = MessageDigest.getInstance("MD5");

			//hent lengden på digesten
			digestlen = md.getDigestLength();

		} catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }

        return digestlen*8;
	}
	
	public static String toHex(byte[] digest) {
		StringBuilder strbuilder = new StringBuilder();
		for(byte b : digest) {
			strbuilder.append(String.format("%02x", b&0xff));
		}
		return strbuilder.toString();
	}

}
