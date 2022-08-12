/**
 * Crypto operations for QR-from-SIM-based authentication protocols
 */
package QRSTK;

import javacard.security.Signature;
import javacard.security.*;
import javacard.framework.*;
import javacardx.crypto.*;

/**
 * 
 * @author Chris Hicks, Vasilios Mavroudis 2022, The Alan Turing Institute.
 */
public class Crypto {

    static final byte TYPE_PUBLIC_KEY = 0x40;
    static final byte TYPE_OTP = 0x41;
    static final byte TYPE_SIG = 0x42;
    static final byte TYPE_VID = 0x43;
    static final byte BYTE_MAX = (byte)0xFF;

    // National IDentification Number to be customised during applet install
	private static byte [] user_NIN = {(byte)'5', (byte)'5', (byte)'5', (byte)'5', (byte)'3', (byte)'3', (byte)'3', (byte)'3', (byte)'2', (byte)'2', (byte)'2', (byte)'2'};

    static byte [] otp = {(byte)'0', (byte)'0', (byte)'0', (byte)'0', (byte)'0', (byte)'0'};
    //static byte [] otp_counter = {0, 0, 0, 0, 0, 0, 0, 0};

    //static byte [] cache;
    static byte [] qr_msg;
    static byte [] publicW;
    static byte [] zeros;
    static byte s_len = 0;
    static short i = 0;
    static short json_offset = 0;

    // ECDSA keys and signature primitive
    //private static RandomData m_rngRandom = null;
    private static Key m_privateKey = null;
    private static ECPublicKey m_publicKey = null;
    private static KeyPair m_keyPair = null;
    private static Signature m_sign = null;

    // RSA keys and primitives
    private static RSAPublicKey m_RSA_publicKey = null;
    private static Cipher rsaCipher = null;
    private static byte [] rsa_buffer;
    private static byte [] issuerRSAKeyMod = { (byte)0xc6, (byte)0x84, (byte)0x36, (byte)0x1c, (byte)0xc6, (byte)0x38, (byte)0xc7, (byte)0xa9, (byte)0x69, (byte)0x60, (byte)0xbb, (byte)0xeb, (byte)0x8d, (byte)0x1c, (byte)0xb0, (byte)0x92, (byte)0x7b, (byte)0xb6, (byte)0xd2, (byte)0x71, (byte)0x62, (byte)0x97, (byte)0xc8, (byte)0x1e, (byte)0xd2, (byte)0x21, (byte)0x1e, (byte)0x04, (byte)0xf9, (byte)0x30, (byte)0xef, (byte)0x85, (byte)0x60, (byte)0x76, (byte)0xff, (byte)0xaa, (byte)0xc4, (byte)0x2f, (byte)0xa0, (byte)0xa8, (byte)0xe8, (byte)0xbb, (byte)0xbe, (byte)0x37, (byte)0xe6, (byte)0x30, (byte)0x99, (byte)0xab, (byte)0xab, (byte)0xba, (byte)0xd6, (byte)0xfb, (byte)0x5a, (byte)0xc8, (byte)0xbe, (byte)0x35, (byte)0x76, (byte)0xb1, (byte)0x38, (byte)0x4a, (byte)0x90, (byte)0x43, (byte)0x33, (byte)0x04, (byte)0x60, (byte)0xbc, (byte)0x97, (byte)0x4c, (byte)0x5b, (byte)0x8b, (byte)0x7a, (byte)0xbb, (byte)0x75, (byte)0xe8, (byte)0x1a, (byte)0x58, (byte)0x67, (byte)0xce, (byte)0x31, (byte)0x3b, (byte)0x99, (byte)0x91, (byte)0x7c, (byte)0x02, (byte)0xc9, (byte)0x96, (byte)0xff, (byte)0xc1, (byte)0x4b, (byte)0x0a, (byte)0xab, (byte)0x46, (byte)0x43, (byte)0xed, (byte)0x73, (byte)0xad }; 
    private static byte [] issuerRSAKeyExp = { (byte)0x01, (byte)0x00, (byte)0x01 };

    // HOTP keys and signature primitive
    private static HMACKey m_otp_key = null;
    private static byte [] HMAC_key = {(byte)0x0B,(byte)0x0B,(byte)0x0B,(byte)0x0B,(byte)0x0B,
                                       (byte)0x0B,(byte)0x0B,(byte)0x0B,(byte)0x0B,(byte)0x0B,
                                       (byte)0x0B,(byte)0x0B,(byte)0x0B,(byte)0x0B,(byte)0x0B,
                                       (byte)0x0B,(byte)0x0B,(byte)0x0B,(byte)0x0B,(byte)0x0B};

    //private static Signature m_hmac_sha1 = null;

	
    static final short counterSize = 8;
    static final short maxCheckOffset = 10;
    static byte[] k_ipad;
    static byte[] k_opad;
    static byte[] counter;
    static byte[] counterBeforeCheck;
    static boolean checking = false;
    static byte[] shaBuffer;
    static byte[] outBuffer;
    static MessageDigest digest;
    static byte[] asciiBuffer;
    static byte[] outputCodeDigits;
    static short digits = 6;
	
    
    /**
     * Construct crypto module including key generation
     */
    public Crypto() {

        //m_rngRandom = RandomData.getInstance(RandomData.ALG_PSEUDO_RANDOM);

        // Generate new EC Keys
        m_privateKey = KeyBuilder.buildKey( KeyBuilder.TYPE_EC_FP_PRIVATE_TRANSIENT_DESELECT, KeyBuilder.LENGTH_EC_FP_160, false ); 
        m_publicKey = (ECPublicKey) KeyBuilder.buildKey( KeyBuilder.TYPE_EC_FP_PUBLIC, KeyBuilder.LENGTH_EC_FP_160, true ); 
        m_keyPair = new KeyPair( KeyPair.ALG_EC_FP, (short) m_publicKey.getSize() );
        m_keyPair.genKeyPair();
        m_publicKey = (ECPublicKey) m_keyPair.getPublic(); 
        m_privateKey = m_keyPair.getPrivate();

        // Set RSA keys and initialise cipher
        rsaCipher = Cipher.getInstance(Cipher.ALG_RSA_PKCS1, false);
        m_RSA_publicKey = (RSAPublicKey) KeyBuilder.buildKey(KeyBuilder.TYPE_RSA_PUBLIC, KeyBuilder.LENGTH_RSA_768, false);
        m_RSA_publicKey.setModulus(issuerRSAKeyMod, (short) 0, (short) issuerRSAKeyMod.length);
        m_RSA_publicKey.setExponent(issuerRSAKeyExp, (short) 0, (short) issuerRSAKeyExp.length);
        try {
            rsaCipher.init(m_RSA_publicKey, Cipher.MODE_ENCRYPT);
        } catch (CryptoException e) {
            // to do
        } 

        // Initialise memory for signatures etc to be written     
        rsa_buffer = JCSystem.makeTransientByteArray( (short)256, JCSystem.CLEAR_ON_RESET );
        //cache = JCSystem.makeTransientByteArray( (short)64, JCSystem.CLEAR_ON_RESET );
        qr_msg = JCSystem.makeTransientByteArray( (short)64, JCSystem.CLEAR_ON_RESET );
        publicW   = JCSystem.makeTransientByteArray( (short)130, JCSystem.CLEAR_ON_RESET );
        zeros = JCSystem.makeTransientByteArray( (short)64, JCSystem.CLEAR_ON_RESET );

        // Initialise ECDSA signature object 
        try {
            m_sign = Signature.getInstance(Signature.ALG_ECDSA_SHA, false);
        } catch (CryptoException e) {
            // to do
        } 
        try {
            m_sign.init(m_privateKey, Signature.MODE_SIGN);
        } catch (CryptoException e) {
            // to do
        }

        // Generate (or import) HOTP key and initialise signature object (HMAC-SHA-1)
		//m_otp_key = (HMACKey) KeyBuilder.buildKey(KeyBuilder.TYPE_HMAC, KeyBuilder.LENGTH_HMAC_SHA_1_BLOCK_64, false);
        //m_otp_key.setKey(HMAC_key, (short) 0, (short) HMAC_key.length);
        //try {
            //m_hmac_sha1 = Signature.getInstance(Signature.ALG_HMAC_SHA1, false);
        //} catch (CryptoException e) {
            //if (e.getReason() == CryptoException.NO_SUCH_ALGORITHM) {
                // to do
            //}
        //}
        //try {
        //    m_hmac_sha1.init(m_otp_key, Signature.MODE_SIGN);
        //} catch (CryptoException e) {
        //    // to do
        //    otp[0] = (byte)'F';
        //}
        
		
		
		//
		//Adapted from Petr Svenda's: https://github.com/petrs/hotp_via_ndef/
		//Set counter to 0
        counter = new byte[counterSize];
        Util.arrayFillNonAtomic(counter, (short) 0, counterSize, (byte) 0);
        
        k_opad = new byte[64];
        k_ipad = new byte[64];
        shaBuffer = new byte[84];
        outBuffer = new byte[20];
        asciiBuffer = new byte[10];
        outputCodeDigits = new byte[digits];
        for (short i = (short) 0; i < (short) 64; i++){
            if(i < HMAC_key.length){
                k_opad[i] = (byte) (HMAC_key[i] ^ 0x5c);
                k_ipad[i] = (byte) (HMAC_key[i] ^ 0x36);
            } else {
                k_opad[i] = (byte) (0x5c);
                k_ipad[i] = (byte) (0x36);
            }
        }
        digest = MessageDigest.getInstance(MessageDigest.ALG_SHA, true);
    }

    /**
     * Compute a 'virtual' NIN token by first signing it and then encrypting 
     * 
     * Chris Hicks 2022, The Alan Turing Institute.
     */
    public static void vid_sign() {
        Util.arrayFillNonAtomic(publicW, (short) 0, (short) publicW.length, (byte) 0); // Clear W
        hOTP();                                                                        // Generate a new OTP

        publicW[0] = (byte) TYPE_VID;
        publicW[1] = (byte)(m_RSA_publicKey.getSize()/8); // size in bytes

        // Build message to be encrypted as otp|NIN|sign(otp|NIN)
        Util.arrayCopyNonAtomic(otp, (short) 0, rsa_buffer, (short) 0, (short) otp.length);
        Util.arrayCopyNonAtomic(user_NIN, (short) 0, rsa_buffer, (short) otp.length, (short) user_NIN.length);

        // Sign otp
        m_sign.sign(rsa_buffer,                               // the input buffer of data to be signed
                   (short) 0,                                 // the offset into the input buffer at which to begin signature generation
                   (short)(otp.length+user_NIN.length),       // the byte length to sign
                   rsa_buffer,                                //  the output buffer to store signature data
                   (short)(otp.length+user_NIN.length));      // the offset into sigBuff at which to begin signature data*/

        try {
            rsaCipher.doFinal(rsa_buffer, 
                             (short) 0,                                                   // In offset
                             (short)(otp.length + user_NIN.length + 40),                  // In length
                             publicW,                                                     // Out buffer
                             (short) 2);                                                  // Out offset

        } catch (CryptoException e) {
			//todo
		}
    }

	
	
	private static void incrementCounter(){
        short i = 7;
        while(i >= 0){
            if(counter[i] != (byte) 0xFF){
                counter[i]++;
                return;
            }else{
                counter[i] = (byte) 0x00;
                // Keep incrementing the next number
                i--;
            }
        }
        
    }
    
	// From Petr Svenda's: https://github.com/petrs/hotp_via_ndef/
    private static byte toAsciiHex(byte toAscii){
        if(toAscii <= 0x09){
            return (byte) (toAscii + '0');
        } else {
            return (byte) (toAscii + 'a' - 10);
        }
    }
    

	
	/**
     * Standard-compliant Hmac generation
     * Limitation: max 20 bytes data
	 *
     * Vasilios Mavroudis 2022, The Alan Turing Institute.
     * Part adapted from Petr Svenda's: https://github.com/petrs/hotp_via_ndef/
	 * 
	 */ 
    public static byte[] generateHmac(){
        incrementCounter();
        digest.update(k_ipad, (short) 0, (short) k_ipad.length);
        digest.update(counter, (short) 0, (short) counter.length);
        digest.doFinal(shaBuffer, (short)0, (short) 0, outBuffer, (short)0);
        digest.update(k_opad, (short) 0, (short) k_opad.length);
        digest.update(outBuffer, (short) 0, (short) 20);
        digest.doFinal(shaBuffer, (short)0, (short) 0, outBuffer, (short)0);
        return outBuffer;
    }

    /**
     * Fills byte array publicW with the ECDSA public key as ANSI X9.62 octet string
     * 
     * Chris Hicks, Vasilios Mavroudis 2022, The Alan Turing Institute.
     * Part adapted from Petr Svenda's: https://github.com/petrs/hotp_via_ndef/
	 * 
	 */    
    public static void hOTP(){
		//tmp_output = new byte[digits]; 
        byte toAscii[] = generateHmac();
        byte startPositionOfCode = (byte) 0;
        startPositionOfCode = (byte) (toAscii[19] & 0x0F);
        
        // Warning - outbuffer no longer contains valid hmac!!
        toAscii[startPositionOfCode] = (byte) (toAscii[startPositionOfCode] & 0x7F);
        UtilBCD.toBCD(toAscii, startPositionOfCode, (short) 4, asciiBuffer, (short) 0);
        UtilBCD.bcdToAscii(asciiBuffer, (short) 0, (short) 10);
        Util.arrayCopyNonAtomic(asciiBuffer, (short) (10 - digits), otp, (short) 0, digits);

        // Finish up by setting the qr_msg as TYPE | OTP | NIN
        qr_msg[0] = TYPE_OTP;
        Util.arrayCopyNonAtomic(otp, (short) 0, qr_msg, (short) 1, (short) otp.length);
        Util.arrayCopyNonAtomic(user_NIN, (short) 0, qr_msg, (short)(otp.length+1), (short) user_NIN.length);
    } 
	
	
    /**
     * Fills byte array publicW with the ECDSA public key as ANSI X9.62 octet string
     * 
     * Chris Hicks 2022, The Alan Turing Institute.
     */
    public static void get_public_key() {
        Util.arrayFillNonAtomic(publicW, (short) 0, (short) publicW.length, (byte) 0);
        publicW[0] = (byte) TYPE_PUBLIC_KEY;
        publicW[1] = (byte)(m_publicKey.getSize()); // size in bytes.
        try {
            m_publicKey.getW(publicW, (short) 2); // Copy W to memory
            //m_RSA_publicKey.getExponent(publicW, (short) 2);
        } catch (CryptoException e) {
            switch(e.getReason()) {
                case CryptoException.UNINITIALIZED_KEY:
                    // to do
                    break;
                default:
                    // copy uninit key message to public key
                    break;
            }
        }

    }
    
}





