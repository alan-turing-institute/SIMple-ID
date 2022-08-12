package QRSTK;

import uicc.system.*;
import uicc.access.*;
import uicc.access.fileadministration.*; 
import uicc.toolkit.*;
import javacard.framework.*;

//import com.taisys.apis.sim.simome.SIMoMEApi;
//import com.taisys.apis.sim.simome.SIMoMEException;

/**
 * QR codes on low-cost mobile devices using SIM/UICC toolkit
 * 
 * @author Chris Hicks, 2022
 */

public class QRSTK extends Applet implements ToolkitInterface, uicc.toolkit.ToolkitConstants, AppletEvent {
	
	private static ToolkitRegistry tkRegistry;
	static final byte DCS_8_BIT_DATA = 0x04;
	// Command Qualifier. TS 102 223. 81h = high priority, wait for user 
	static final byte DTQ_HIGH_PRIORITY = (byte)0x01;
	static final byte DTQ_WAIT_FOR_USER = (byte)0x80;

	// ProcessToolkit(...) variables
	EditHandler editHandler;	// Used to execute filesystem commands: EDIT, UPDATE etc

	// Dedicated FileView objects
	AdminFileView fvEF_DF_TELECOM;
	//AdminFileView fvEF_DF_GRAPHICS;

	// Elementary FileView objects 
	FileView fvEF_SUME;
	FileView fvEF_IMG;
	FileView fvEF_RAW_ICON;
	FileView fvEF_INSTANCE;

	// FIDs
	static final short FID_EF_SUME 		= (short) 0x6F54;
	static final short FID_EF_IMG  		= (short) 0x4F20;
	static final short FID_EF_INSTANCE  = (short) 0x4F06;
	static final short FID_EF_RAW_ICON  = (short) 0x4F01;

	// Offsets into ISO commands for adjusting variables
	static final short FILE_ID_RESIZE_CMD_OFFSET = (short)2;
	static final short FILE_LENGTH_RESIZE_CMD_OFFSET = (short)6;

	// Template(s) for ISO commands.
  	private static byte [] resizeCommand =  {(byte) 0x83, (byte) 0x02, (byte) 0x6F, (byte) 0x54,
											 (byte) 0x80, (byte) 0x02, (byte) 0x00, (byte) 0x00 };
	
	// See ETSI TS 131 102 V16.7.0 4.6.12 Image Instance Data Files for file atributes
											    //Tag file, length, transparent, data coding
	/*private static byte [] createEFCommand = { (byte) 0x82, (byte) 0x02, (byte) 0x41, (byte) 0x21,
												//Tag File ID, length, File ID
											   (byte) 0x83, (byte) 0x02, (byte) 0x4f, (byte) 0x01,
												//Tag LCSI, length, LCSI = 0x05 (activated)
											   (byte) 0x8A, (byte) 0x01, (byte) 0x05, 
												//Tag Security Attributes Referenced (8B), length, EF_ARR FID, record number
											   (byte) 0x8B, (byte) 0x03, (byte) 0x2F, (byte) 0x06, (byte) 0x10,
											    //Tag Reserved file size, length, total image file size
											   (byte) 0x80, (byte) 0x02, (byte) 0x00, (byte) 0xE8,
											    //Tag SFI (Short File Identifier), length, SFI
											   (byte) 0x88, (byte) 0x00	
	};*/
	
	private byte menuItem_showNIN;
	private byte menuItem_virtualNIN;
	private byte menuItem_oldID;
	private byte menuItem_showPubK;
	private byte menuItem_renameMenu;

	// Title(s) of the STK menu item(s)
	private static byte [] menuTitle_showNIN = {(byte)'S', (byte)'h', (byte)'o', (byte)'w', (byte)' ',
											    (byte)'m', (byte)'y', (byte)' ',
											    (byte)'N', (byte)'I', (byte)'N'};

	private static byte [] menuTitle_newVID = {(byte)'V', (byte)'i', (byte)'r', (byte)'t', (byte)'u', (byte)'a', (byte)'l',(byte)' ',
											   (byte)'N', (byte)'I', (byte)'N'};

	private static byte [] menuTitle_oldID = {(byte)'S', (byte)'h', (byte)'o', (byte)'w', (byte)' ', 
											  (byte)'a', (byte)'g', (byte)'a', (byte)'i', (byte)'n'};

	private static byte [] menuTitle_showPubK = {(byte)'S', (byte)'h', (byte)'a', (byte)'r', (byte)'e', (byte)' ', 
											  	 (byte)'k', (byte)'e', (byte)'y'};

	private static byte [] menuTitle_renameMenu = {(byte)'C', (byte)'h', (byte)'a', (byte)'n', (byte)'g', (byte)'e', 
												   (byte)' ', (byte)'t', (byte)'i', (byte)'t',(byte)'l',  (byte)'e'};

	private static byte [] id_string = {(byte)'I', (byte)'D', (byte)' ', (byte)' '};
	private static byte [] err_string = {(byte)'E', (byte)'r', (byte)'r', (byte)'o', (byte)'r', (byte)'!'};
	private static boolean error = false;

	// Text shown to the user when changing menu title file
	private static byte [] insertNewTitle_text = {(byte)'I', (byte)'n', (byte)'s', (byte)'e', (byte)'r', (byte)'t',
		(byte)' ',(byte)'n',(byte)'e',(byte)'w',(byte)' ', (byte)'t', (byte)'i', (byte)'t',(byte)'l',  (byte)'e'};

	private static byte [] showPubK_helper_text = {(byte)'S', (byte)'h', (byte)'a', (byte)'r', (byte)'e', (byte)' ',
												   (byte)'m', (byte)'e', (byte)'!'};

	private static byte [] showNIN_helper_text = {(byte)'O', (byte)'T', (byte)'P', (byte)':', (byte)' ', 
												  (byte)' ', (byte)' ', (byte)' ', (byte)' ', (byte)' ', (byte)' '};

	private static byte [] showVID_helper_text = {(byte)'V', (byte)'I', (byte)'D', (byte)':', (byte)' ', 
												  (byte)' ', (byte)' ', (byte)' ', (byte)' ', (byte)' ', (byte)' '};

	private static byte [] showAgain_helper_text = {(byte)'L', (byte)'a', (byte)'s', (byte)'t', (byte)' ', 
												   (byte)'s', (byte)'h', (byte)'o', (byte)'w', (byte)'n'};

	// Display icon TAG, length, iconQualifier, iconRef in EF_IMG	
	// iconQualifier: 'bit 1 = 0': icon is self-explanatory, i.e. if displayed, it replaces the alpha identifier or text string	
	private static byte [] iconTLV =  {(byte)0x1E, (byte)0x02, (byte)0x00, (byte)0x02};
	private static byte [] qrIconFileTLV = {(byte)0x1E, (byte)0x02, (byte)0x00, (byte)0x01};	

	Crypto crypto;
	
	/**
	 * Register QRSTK Applet
	 */
	private QRSTK(byte[] bArray, short bOffset, byte bLength) {
		byte aidLen = bArray[bOffset];
		if (aidLen == (byte) 0) {
			register();
		}else {
			// Register this applet
			register(bArray, (short) (bOffset + 1),(byte) bArray[bOffset]);
		}

		initToolkit();
		crypto = new Crypto();
		
	}

	/**
	 * Toolkit registration and initialisation
	 * 
	 * Chris Hicks, 2022
	 **/
	public void initToolkit()
	{

		tkRegistry = ToolkitRegistrySystem.getEntry(); // Register to the UICC toolkit 

		menuItem_showNIN = tkRegistry.initMenuEntry(menuTitle_showNIN,  
													(short)0, 
													(short) menuTitle_showNIN.length,
													(byte)0,	// Next action
													false,		// Help supported
													(byte)0,	// Icon Qualifier
													(short)0); 	// Icon Identifier

		menuItem_virtualNIN = tkRegistry.initMenuEntry(menuTitle_newVID,  
													  (short)0, 
													  (short) menuTitle_newVID.length,
													  (byte)0,	// Next action
													  false,		// Help supported
													  (byte)0,	// Icon Qualifier
													  (short)0); 	// Icon Identifier

		menuItem_oldID = tkRegistry.initMenuEntry(menuTitle_oldID, 
													(short)0, 
													(short) menuTitle_oldID.length,
													(byte)0,	
													false,		
													(byte)0,	
													(short)0); 	

   		/*menuItem_showPubK = tkRegistry.initMenuEntry(menuTitle_showPubK, 
		   												(short)0, 
														(short) menuTitle_showPubK.length,
														(byte)0,	
														false,		
														(byte)0,	
														(short)0); 	*/

		/*menuItem_renameMenu = tkRegistry.initMenuEntry(menuTitle_renameMenu, 
														(short)0, 
														(short) menuTitle_renameMenu.length,
														(byte)0,	
														false,		
														(byte)0,	
														(short)0); 	*/

		// ?
		tkRegistry.setEvent(ToolkitConstants.EVENT_EVENT_DOWNLOAD_LANGUAGE_SELECTION);		
		tkRegistry.setEvent((short) uicc.usim.toolkit.ToolkitConstants.EVENT_UNFORMATTED_SMS_PP_ENV);

		// The DF Telecom and the EF SUME are selected in the two FileView and AdminFileView objects.
		// DF Telecom points to an administrative File View object to enable a resize operation on EF_SUME
		fvEF_DF_TELECOM = AdminFileViewBuilder.getTheUICCAdminFileView(JCSystem.NOT_A_TRANSIENT_OBJECT);
		fvEF_SUME = UICCSystem.getTheUICCView(JCSystem.NOT_A_TRANSIENT_OBJECT);

		//fvEF_DF_GRAPHICS = AdminFileViewBuilder.getTheUICCAdminFileView(JCSystem.NOT_A_TRANSIENT_OBJECT);
		//fvEF_IMG = UICCSystem.getTheUICCView(JCSystem.NOT_A_TRANSIENT_OBJECT);
		fvEF_RAW_ICON = UICCSystem.getTheUICCView(JCSystem.NOT_A_TRANSIENT_OBJECT);

		fvEF_SUME.select(UICCConstants.FID_DF_TELECOM); 
		fvEF_SUME.select(FID_EF_SUME);

		// DF.TELECOM->DF.GRAPHICS->EF.IMG (4f06)
		//fvEF_IMG.select(UICCConstants.FID_DF_TELECOM);
		//fvEF_IMG.select(UICCConstants.FID_DF_GRAPHICS);
		//fvEF_IMG.select(FID_EF_INSTANCE);

		// DF.TELECOM->DF.GRAPHICS->EF.IMG (4f01)
		fvEF_RAW_ICON.select(UICCConstants.FID_DF_TELECOM);
		fvEF_RAW_ICON.select(UICCConstants.FID_DF_GRAPHICS);
		fvEF_RAW_ICON.select(FID_EF_RAW_ICON);

		// DF.TELECOM (for EF.SUME)
		fvEF_DF_TELECOM.select(UICCConstants.FID_DF_TELECOM);
		
		// DF.TELECOM->DF.GRAPHICS(5f50)->
		//fvEF_DF_GRAPHICS.select(UICCConstants.FID_DF_TELECOM); 
		//fvEF_DF_GRAPHICS.select(UICCConstants.FID_DF_GRAPHICS);

		/*try {
			// Try to select icon file, testing whether we need to create it or not
			fvEF_DF_GRAPHICS.select(FID_EF_RAW_ICON); 
		} catch (UICCException e) { 
			if (e.getReason() == UICCException.FILE_NOT_FOUND) { // File not found routine 
		
				// Initialise the editHandler with the size of the createEF ISO command template
				editHandler = (EditHandler)HandlerBuilder.buildTLVHandler(HandlerBuilder.EDIT_HANDLER, (short)createEFCommand.length);

				// Try to create the file
				editHandler.clear();
				editHandler.appendArray(createEFCommand, (short)0, (short)createEFCommand.length);
				fvEF_DF_GRAPHICS.createFile(editHandler);
			}
		}*/

	}

	/**
	 * Applet install
	 */
	public static void install(byte[] bArray, short bOffset, byte bLength) throws ISOException {
		// Create the Java UICC toolkit applet
		QRSTK applet = new QRSTK(bArray, bOffset, bLength);				
	}

	/**
	 * Used to process incoming ADPUs by standard JC applets but unused by STK
	 * @param apdu Incoming ADPU
	 * @throws ISOException	ADPU Bad
	 */
	public void process(APDU apdu) throws ISOException {
		// Ignore the applet select command dispached to the process
		if (selectingApplet())
			return;
	}

	/**
	 * Handles toolkit events
	 * 
	 * @param event Event that triggered call to handler
	 * @throws ToolkitException Event unhandled
	 * 
	 * Chris Hicks, 2022
	 */
	public void processToolkit(short event) throws ToolkitException {
		
		if (event == EVENT_MENU_SELECTION) {

			ProactiveHandler proh = ProactiveHandlerSystem.getTheHandler();
			EnvelopeHandler envh = EnvelopeHandlerSystem.getTheHandler();

			byte selectedItemId = envh.getItemIdentifier();
			byte res;

			if (selectedItemId == menuItem_showNIN) {		
				Crypto.hOTP();		// Writes latest otp to Crypto.otp
				//Crypto.signECDSA();	// Signs Crypto.otp and writes signature to Crypto.signArray

				proh.init(ToolkitConstants.PRO_CMD_DISPLAY_TEXT, 
							(byte)(DTQ_HIGH_PRIORITY|DTQ_WAIT_FOR_USER), 
							ToolkitConstants.DEV_ID_DISPLAY);

				// Update FID_EF_RAW_ICON with latest crypto token
				try {
					fvEF_RAW_ICON.updateBinary((short)2, 							// file offset
												Crypto.qr_msg, 					// data
												(short)0, 							// data offset
												(short)Crypto.qr_msg.length);	// data length
				} catch (Exception e) {
					error = true;
					proh.appendTLV((byte)(ToolkitConstants.TAG_TEXT_STRING|ToolkitConstants.TAG_SET_CR),
								DCS_8_BIT_DATA, 
								err_string, 
								(short) 0x0000, 
								(short) err_string.length);
				} 

				if (!error) {
					Util.arrayCopyNonAtomic(Crypto.otp, (short) 0, showNIN_helper_text, (short) 5, (short) Crypto.otp.length);
					proh.appendTLV((byte)(ToolkitConstants.TAG_TEXT_STRING|ToolkitConstants.TAG_SET_CR),
								DCS_8_BIT_DATA, 
								showNIN_helper_text, 
								(short) 0x0000, 
								(short) showNIN_helper_text.length);
				
					// Include crypto token icon
					proh.appendArray(qrIconFileTLV, (short)0x00, (short)qrIconFileTLV.length);
				}
			
				error = false;
				res = proh.send();

			} else if (selectedItemId == menuItem_virtualNIN) {

				//Crypto.signECDSA();	// Signs Crypto.otp and writes signature to Crypto.signArray
				Crypto.vid_sign();

				proh.init(ToolkitConstants.PRO_CMD_DISPLAY_TEXT, 
						 (byte)(DTQ_HIGH_PRIORITY|DTQ_WAIT_FOR_USER), 
						 ToolkitConstants.DEV_ID_DISPLAY);

				// Update FID_EF_RAW_ICON with latest crypto token
				try {
					fvEF_RAW_ICON.updateBinary((short)2, 							// file offset
												Crypto.publicW, 					// data
												(short)0, 							// data offset
												(short)Crypto.publicW.length);		// data length
				} catch (Exception e) {
					error = true;
					proh.appendTLV((byte)(ToolkitConstants.TAG_TEXT_STRING|ToolkitConstants.TAG_SET_CR),
								DCS_8_BIT_DATA, 
								err_string, 
								(short) 0x0000, 
								(short) err_string.length);
				} 

				if (!error) {
					Util.arrayCopyNonAtomic(Crypto.otp, (short) 0, showVID_helper_text, (short) 5, (short) Crypto.otp.length);
					proh.appendTLV((byte)(ToolkitConstants.TAG_TEXT_STRING|ToolkitConstants.TAG_SET_CR),
								DCS_8_BIT_DATA, 
								showVID_helper_text, 
								(short) 0x0000, 
								(short) showVID_helper_text.length);
				
					// Include crypto token icon
					proh.appendArray(qrIconFileTLV, (short)0x00, (short)qrIconFileTLV.length);
				}
			
				error = false;
				res = proh.send();

			} else if (selectedItemId == menuItem_oldID) {	// Show the token which was last shown (no new compute)

				proh.init(ToolkitConstants.PRO_CMD_DISPLAY_TEXT, 
							(byte)(DTQ_HIGH_PRIORITY|DTQ_WAIT_FOR_USER), 
							ToolkitConstants.DEV_ID_DISPLAY);

			
				proh.appendTLV((byte)(ToolkitConstants.TAG_TEXT_STRING|ToolkitConstants.TAG_SET_CR),
								DCS_8_BIT_DATA, 
								showAgain_helper_text, 
								(short) 0x0000, 
								(short) showAgain_helper_text.length);

				proh.appendArray(qrIconFileTLV, (short)0x00, (short)qrIconFileTLV.length);				
				res = proh.send();

			} else if (selectedItemId == menuItem_showPubK) {

				Crypto.get_public_key();

				proh.init(ToolkitConstants.PRO_CMD_DISPLAY_TEXT, 
							(byte)(DTQ_HIGH_PRIORITY|DTQ_WAIT_FOR_USER), 
							ToolkitConstants.DEV_ID_DISPLAY);

				// Update FID_EF_RAW_ICON with latest crypto token
				try {
					fvEF_RAW_ICON.updateBinary((short)2, Crypto.publicW, (short)0, (short)Crypto.publicW.length);//Crypto.publicW.length);
				} catch (Exception e) {
					error = true;
					proh.appendTLV((byte)(ToolkitConstants.TAG_TEXT_STRING|ToolkitConstants.TAG_SET_CR),
								DCS_8_BIT_DATA, 
								err_string, 
								(short) 0x0000, 
								(short) err_string.length);
				}
				
				if(!error) {
					proh.appendTLV((byte)(ToolkitConstants.TAG_TEXT_STRING|ToolkitConstants.TAG_SET_CR),
								DCS_8_BIT_DATA, 
								showPubK_helper_text, 
								(short) 0x0000, 
								(short) showPubK_helper_text.length);

					proh.appendArray(qrIconFileTLV, (short)0x00, (short)qrIconFileTLV.length);
				}
			
				error = false;					
				res = proh.send();

			} else if (selectedItemId == menuItem_renameMenu) {

				// User is prompted with a get input to change the menu title
				proh.initGetInput( (byte)0x01,							// CQ see TS 102 223. Bits 1-4 toggle alphabet(s) 
									DCS_8_BIT_DATA,
									insertNewTitle_text, 
									(short)0,							// Prompt text offset
									(short)insertNewTitle_text.length, 	// Length of displayed text string (prompt)
									(short)1, 							// Min resp. length
									(short)20 							// Max. resp. length
									);

				res = proh.send();

				// If the user presses ok...
				if (res == (byte)0x00)
				{
					// The user input is retrieved by the Proactive Response Handler
					ProactiveResponseHandler proresh = ProactiveResponseHandlerSystem.getTheHandler();
					byte[] globalBuffer;
					short dataLen;

					// The Volatile Byte Array is used (no memory allocation) to perform the copy operation
					globalBuffer = UICCPlatform.getTheVolatileByteArray();
					dataLen = proresh.copyTextString(globalBuffer, (short) 2);

					// Data is stored inside the Menu Resize in a TLV structure
					globalBuffer[0] = ToolkitConstants.TAG_ALPHA_IDENTIFIER; globalBuffer[1] = (byte)(dataLen-2);
						
					// The file is resized to fit the buffer
					resizeMenuFile(dataLen);

					// File content is updated
					fvEF_SUME.updateBinary((short)0, globalBuffer, (short)0, dataLen); 
				}
			
			} 

		}

	}

	/**
	* Resize the EF Sume file. 
	* @param newFileLength Size of file if operation succeeds
	**/
	public void resizeMenuFile(short newFileLength) {
		// The new length is set in the byte array
		Util.setShort(resizeCommand, FILE_LENGTH_RESIZE_CMD_OFFSET, newFileLength); 
		
		editHandler.clear();
		editHandler.appendArray(resizeCommand, (short)0, (short)resizeCommand.length);

		fvEF_DF_TELECOM.resizeFile(editHandler); 
	}
	
	/**
	 * Boilerplate
	 */
	public Shareable getShareableInterfaceObject(AID clientAID, byte parameter) {
	      // According to CAT Runtime Environment behaviour for ToolkitInterface object retrieval
	      if ((clientAID == null) && (parameter == (byte)0x01)) {
	          return((Shareable) this);
	      } else {
	          return(null);
	      }
	}
	
	/**
	 * Null the class object variables
	 */
	public void uninstall() {
		tkRegistry = null;
		insertNewTitle_text = null;
		fvEF_DF_TELECOM = null;
		fvEF_SUME = null;
		fvEF_IMG = null;
		fvEF_RAW_ICON = null;
		fvEF_INSTANCE = null;
		resizeCommand = null;
		crypto = null;
	}
	
}