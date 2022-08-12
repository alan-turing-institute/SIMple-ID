#!/usr/bin/env python3
# -*- coding: utf-8 -*-
# Chris Hicks 16-9-21
# See pySim doc at https://ftp.osmocom.org/docs/latest/osmopysim-usermanual.pdf
# 1. Writes DF_GRAPHICS (5f50), EF_IMG (4f20) and EF_INSTANCE (4f01) to a Sysmocom SJ-A2
# 2. Inserts EF_INSTANCE entry into EF.IMG
from pySim.transport.pcsc import PcscSimLink
from pySim.card_handler import card_handler
from pySim.cards import card_detect, Card
from pySim.commands import SimCardCommands
from pySim.utils import h2b, sanitize_pin_adm
from pySim.filesystem import CardMF, RuntimeState, CardDF, CardADF
from pySim.ts_51_011 import CardProfileSIM, DF_TELECOM, DF_GSM
from pySim.ts_102_221 import CardProfileUICC, CardCommandSet
from pySim.ts_31_102 import CardApplicationUSIM
from pySim.ts_31_103 import CardApplicationISIM

FID_MF = '3f00'
FID_DF_TELECOM = '7f10'
FID_DF_GRAPHICS = '5f50'
FID_EF_IMG = '4f20'
FID_EF_INSTANCE = '4f01'
FID_EF_INSTANCE2 = '4f02'
FID_EF_INSTANCE3 = '4f06'
FID_EF_INSTANCE4 = '4f07'

# See ETSI TS 102 222 V15.0.0 e.g. Table 3: Coding of the data field of the CREATE FILE command (in case of creation of a DF/ADF)
create_df_graphics = '82027821' 					 	# Tag File Descriptor, length, FD = '78' (DF), data coding
create_df_graphics = create_df_graphics + '83025F50' 	# Tag File ID, length, File ID: DF_GRAPHICS = 5F50
create_df_graphics = create_df_graphics + '8A0105' 	 	# Tag LCSI, length, LCSI = 0x05 (activated)
create_df_graphics = create_df_graphics + '8B032F0602'	# Tag Security Attributes Referenced (8B), length, attributes: EF_ARR FID, record number
create_df_graphics = create_df_graphics + '81020000'	# Tag Total DF size, length, total file size 
create_df_graphics = create_df_graphics + 'C609'		# Tag PIN Status Template DO, length
create_df_graphics = create_df_graphics + '900180'		# Tag PS_DO, len, PS_DO - bit indicates which following pin(s) enabled
create_df_graphics = create_df_graphics + '950100'		# Tag Usage Qualifier for PS_DO, length, usage qualifier
create_df_graphics = create_df_graphics + '83010a'		# Tag PIN, length, PIN reference
create_df_graphics = create_df_graphics + '830101'		# ...
create_df_graphics = '62' + '{:02x}'.format(len(create_df_graphics)//2) + create_df_graphics

# See ETSI TS 102 222 V15.0.0 e.g. Table 4: Coding of the data field of the CREATE FILE command (in case of the creation of an EF)
create_ef_img = '82044221000A' 					# Tag File Descriptor, length, FD = '42' (linear fixed EF), data coding ='21' (const.), record length, n_records
create_ef_img = create_ef_img + '83024F20' 		# Tag File ID, length, File ID: EF_IMG = 4F20
create_ef_img = create_ef_img + '8A0105' 	 	# Tag Life Cycle Status Info., length, LCSI = 0x05 (activated)
create_ef_img = create_ef_img + '8B032F0601'	# Tag Security Attributes Referenced (8B), length, attributes: EF_ARR FID, record number
create_ef_img = create_ef_img + '80020028'		# Tag Reserved file size, length, total file size = n_records*record_len i.e. 4*10
create_ef_img = create_ef_img + '8800'			# SFI (Short File Identifier), length, SFI
create_ef_img = '62' + '{:02x}'.format(len(create_ef_img)//2) + create_ef_img

ef_instance_data = '252500000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000'

#ef_instance_data2 = '0505FEEBBFFFFFFF'

#ef_instance_data3 =  '1b1bfffffff0178406fac7bed14db45a289a8b4590516fa3fbec055501ffe6bff31d768795fce3e66733799dad4bd5688c66528e7f23837bf105d8b099301ffeb0e9c06794fbe8ab8745090468b6f0cd160023be956070133b47ffffffff'

#ef_instance_data4 =	'3636fffffffffffffffffffffffffffc000cff00c000f00033fc030003cffccf03fcffcf3ff33c0ff3ff3cc0cc3cf3cc0cf30330f3cf3033cc0cc0c3ccc0cf3033030f33033cc0cf0c00cc0cf3033c30033033cffcc0fffcffcf3ff303fff3ff3c000cccccc000f0003333330003fffffc3ccffffffffff0f33ffffc3c0fccfcf300f0f03f33f3cc03fc333fff0fc0fff0ccfffc3f03ff0f0f0fc3c3cffc3c3c3f0f0f3fc3c3f3ccf330cf0f0fcf33ccc33fccccf30303c0ff3333cc0c0f03c3c330cc0fc3ff0f0cc3303f0fffc303f003cff3ff0c0fc00f3fcfff030033f3c0cffc0c00cfcf033c030f0c3c000ff00c3c30f0003fffffccf00fcc3fffff33c03f30fc000f0ff0cc3ff0003c3fc330ffcffcc0cccfc03f3ff303333f00fcc0cc030c00c0f303300c300303cc0cf3cff00f0f3033cf3fc03c3cc0cf00000303f3033c00000c0fcffcc3333c003f3ff30cccf000fc000c3c3f3cc0f00030f0fcf303ffffffffffffffffffffffffffff'

# #img instances, width, height, coding: 11 basic, 21: colour, 22: colour with transparency, 31: spacial raw format
ef_img_record_1 = '01' + '25' + '25' + '31' + FID_EF_INSTANCE + '0000' + '{:04x}'.format(len(ef_instance_data)//2)

#ef_img_record_2 = '01' + '1b' + '1b' + '11' + FID_EF_INSTANCE3 + '0000' + '{:04x}'.format(len(ef_instance_data3)//2)

#ef_img_record_3 = '01' + '36' + '36' + '11' + FID_EF_INSTANCE4 + '0000' + '{:04x}'.format(len(ef_instance_data4)//2)

#3636
# 6f is limit len(ef_instance_data4)//2 on TT240
#ef_img_record_4 = '01' + '36' + '36' + '11' + FID_EF_INSTANCE4 + '0000' + '{:04x}'.format(len(ef_instance_data3)//2)

# See ETSI TS 131 102 V16.7.0 4.6.12 Image Instance Data Files for file atributes
# 4FXX, transparent EF. From TS 102 221, FD = 0b01000001 = 0x41
create_ef_instance = '82024121' 						# Tag File Descriptor, length, FD = '41' (transparent EF), data coding ='21' (const.)
create_ef_instance = create_ef_instance + '8302' + FID_EF_INSTANCE 	# Tag File ID, length, File ID
create_ef_instance = create_ef_instance + '8A0105' 	 	# Tag LCSI, length, LCSI = 0x05 (activated)
create_ef_instance = create_ef_instance + '8B032F0601'	# Tag Security Attributes Referenced (8B), length, attributes: EF_ARR FID, record number
create_ef_instance = create_ef_instance + '800200E8'	# Tag Reserved file size, length, total image file size**
create_ef_instance = create_ef_instance + '8800'		# SFI (Short File Identifier), length, SFI
create_ef_instance = '62' + '{:02x}'.format(len(create_ef_instance)//2) + create_ef_instance

#create_ef_instance2 = create_ef_instance[0:16] + FID_EF_INSTANCE2 + create_ef_instance[20:40] + '{:04x}'.format(len(ef_instance_data2)//2)
#create_ef_instance2 = create_ef_instance2 + create_ef_instance[44:48]

#create_ef_instance3 = create_ef_instance[0:16] + FID_EF_INSTANCE3 + create_ef_instance[20:40] + '{:04x}'.format(len(ef_instance_data3)//2)
#create_ef_instance3 = create_ef_instance3 + create_ef_instance[44:48]

#create_ef_instance4 = create_ef_instance[0:16] + FID_EF_INSTANCE4 + create_ef_instance[20:40] + '{:04x}'.format(len(ef_instance_data4)//2)
#create_ef_instance4 = create_ef_instance4 + create_ef_instance[44:48]

#print(create_ef_instance4)

def main():

	reader_no = 0
	pin_adm1 = '0000'

	sl = PcscSimLink(reader_no)

	# Create command layer
	scc = SimCardCommands(transport=sl)

	sl.wait_for_card()

	cardhandler = card_handler(sl)

	card = card_detect("auto", scc)
	if card is None:
		print("No card detected!")
		sys.exit(-1)

	profile = CardProfileUICC()
	profile.add_application(CardApplicationUSIM)
	profile.add_application(CardApplicationISIM)

	rs = RuntimeState(card, profile)
	sl.set_sw_interpreter(rs) # inform the transport that we can do context-specific SW interpretation

	rs.mf.add_file(DF_TELECOM())
	rs.mf.add_file(DF_GSM())

	# Use PIN ADM1 
	pin = sanitize_pin_adm(pin_adm1)
	if pin:
		try:
			card.verify_adm(h2b(pin))
		except Exception as e:
			print(e)

	print()

	df_graphics_check = scc.try_select_path([FID_MF, FID_DF_TELECOM, FID_DF_GRAPHICS])
	resp = sl.send_apdu_checksw(scc.cla_byte + "a4" + scc.sel_ctrl + "02" + FID_MF) #scc.select_file(FID_MF)
	resp = scc.select_file(FID_DF_TELECOM)
	
	if df_graphics_check[-1][1] != '9000':
		print('Error: DF.GRAPHICS not found at {}'.format(FID_DF_GRAPHICS))
		print('Attempting to Create DF.GRAPHICS...')

		# Using ETSI TS 102 222 V15.0.0 (2018-06)
		# CREATE FILE: CLA INS='E0' P1='00' P2='00' data_length data '{:02x}'.format(len(create_df_graphics)//2)
		sl.send_apdu_checksw(scc.cla_byte + "e0" + '0000' + '{:02x}'.format(len(create_df_graphics)//2) + create_df_graphics)
		# File is NOT automatically selected after creation
		
	resp = scc.select_file(FID_DF_GRAPHICS)
	df_img_check = scc.try_select_path([FID_EF_IMG])
	if df_img_check[-1][1] != '9000':
		print('Error: EF.IMG not found at {}'.format(FID_EF_IMG))
		print('Attempting to Create EF.IMG...')
		sl.send_apdu_checksw(scc.cla_byte + "e0" + '0000' + '{:02x}'.format(len(create_ef_img)//2) + create_ef_img)
	else:
		# Check the file is current, if not then resize and re-write the file.
		try:
			pass
		except Exception as e:
			pass

	df_img_instance_check = scc.try_select_path([FID_EF_INSTANCE])
	if df_img_instance_check[-1][1] != '9000':
		print('Error: EF.INSTANCE not found at {}'.format(FID_EF_INSTANCE))
		print('Attempting to Create sample EF.INSTANCE at {}...'.format(FID_EF_INSTANCE))
		sl.send_apdu_checksw(scc.cla_byte + "e0" + '0000' + '{:02x}'.format(len(create_ef_instance)//2) + create_ef_instance)
	else:
		# Delete first to ensure latest: E4 is delete INS
		sl.send_apdu_checksw(scc.cla_byte + "e4" + '0000' + '{:02x}'.format(len(FID_EF_INSTANCE)//2) + FID_EF_INSTANCE)
		sl.send_apdu_checksw(scc.cla_byte + "e0" + '0000' + '{:02x}'.format(len(create_ef_instance)//2) + create_ef_instance)

	#df_img_instance_check = scc.try_select_path([FID_EF_INSTANCE2])
	#if df_img_instance_check[-1][1] != '9000':
	#	print('Error: EF.INSTANCE not found at {}'.format(FID_EF_INSTANCE2))
	#	print('Attempting to Create sample EF.INSTANCE at {}...'.format(FID_EF_INSTANCE2))
	#	sl.send_apdu_checksw(scc.cla_byte + "e0" + '0000' + '{:02x}'.format(len(create_ef_instance2)//2) + create_ef_instance2)

	#df_img_instance_check = scc.try_select_path([FID_EF_INSTANCE3])
	#if df_img_instance_check[-1][1] != '9000':
	#	print('Error: EF.INSTANCE not found at {}'.format(FID_EF_INSTANCE3))
		# To delete just specify file ID after the delete INS=E4
		# print('Attempting to delete sample EF.INSTANCE at {}...'.format(FID_EF_INSTANCE3))
		# sl.send_apdu_checksw(scc.cla_byte + "e4" + '0000' + '{:02x}'.format(len(FID_EF_INSTANCE3)//2) + FID_EF_INSTANCE3) 

	#	print('Attempting to Create sample EF.INSTANCE at {}...'.format(FID_EF_INSTANCE3))
	#	sl.send_apdu_checksw(scc.cla_byte + "e0" + '0000' + '{:02x}'.format(len(create_ef_instance3)//2) + create_ef_instance3)

	#df_img_instance_check = scc.try_select_path([FID_EF_INSTANCE4])
	#if df_img_instance_check[-1][1] != '9000':
	#	print('Error: EF.INSTANCE not found at {}'.format(FID_EF_INSTANCE4))
	#	print('Attempting to Create sample EF.INSTANCE at {}...'.format(FID_EF_INSTANCE4))
	#	sl.send_apdu_checksw(scc.cla_byte + "e0" + '0000' + '{:02x}'.format(len(create_ef_instance4)//2) + create_ef_instance4)


	# These should verify if it's there first so save EEPROM from write cycles
	print('Writing EF.INSTANCE data to {}...'.format(FID_EF_INSTANCE))
	resp = scc.update_binary(FID_EF_INSTANCE, ef_instance_data, offset=0, verify=True)
	# Ensure correct size. 83 tag file ID, 80 tag file size
	#resize_file = '8302' + FID_EF_INSTANCE + '8002' + '{:02x}'.format(len(ef_instance_data)//2)
	#resize_file = '62' + '{:02x}'.format(len(resize_file)//2) + resize_file
	# CLA + INS + P1 + P2 + Lc + Data field
	#sl.send_apdu_checksw(scc.cla_byte + "d4" + '00' + '00' + '{:02x}'.format(len(resize_file)//2) + resize_file)

	#print('Writing EF.INSTANCE data to {}...'.format(FID_EF_INSTANCE2))
	#resp = scc.update_binary(FID_EF_INSTANCE2, ef_instance_data2, offset=0, verify=True)

	#print('Writing EF.INSTANCE data to {}...'.format(FID_EF_INSTANCE3))
	# Ensure correct size
	#resize_file = '8302' + FID_EF_INSTANCE3 + '8002' + '{:02x}'.format(len(ef_instance_data3)//2)
	#resize_file = '62' + '{:02x}'.format(len(resize_file)//2) + resize_file
	# CLA + INS + P1 + P2 + Lc + Data field
	#sl.send_apdu_checksw('80' + "d4" + '00' + '00' + '{:02x}'.format(len(resize_file)//2) + resize_file)
	#resp = scc.update_binary(FID_EF_INSTANCE3, ef_instance_data3, offset=0, verify=True)

	#print('Writing EF.INSTANCE data to {}...'.format(FID_EF_INSTANCE4))
	#resp = scc.update_binary(FID_EF_INSTANCE4, ef_instance_data4[0:100], offset=0, verify=False) 		#Writes first 100
	#resp = scc.update_binary(FID_EF_INSTANCE4, ef_instance_data4[100:200], offset=50, verify=False)		#Writes next 100
	#resp = scc.update_binary(FID_EF_INSTANCE4, ef_instance_data4[200:300], offset=100, verify=False)	#...
	#resp = scc.update_binary(FID_EF_INSTANCE4, ef_instance_data4[400:500], offset=150, verify=False)	
	#resp = scc.update_binary(FID_EF_INSTANCE4, ef_instance_data4[500:600], offset=200, verify=False)	
	#resp = scc.update_binary(FID_EF_INSTANCE4, ef_instance_data4[600:700], offset=250, verify=False)	
	#resp = scc.update_binary(FID_EF_INSTANCE4, ef_instance_data4[700:], offset=300, verify=False)	
	#resp = scc.update_binary(FID_EF_INSTANCE4, ef_instance_data4[800:], offset=350, verify=False)	
	#resp=scc.read_binary(FID_EF_INSTANCE4)
	#print(resp)

	print('Patching EF.IMG...')
	resp = scc.update_record(FID_EF_IMG, 1, ef_img_record_1, verify=True)
	#resp = scc.update_record(FID_EF_IMG, 2, ef_img_record_2, verify=True)
	#resp = scc.update_record(FID_EF_IMG, 3, ef_img_record_3, verify=True)
	#resp = scc.update_record(FID_EF_IMG, 4, ef_img_record_4, verify=True)

	# Make sure Service No. 22 is enabled on USIM Service Table (EF.UST)


	print('Done! :)')


if __name__ == '__main__':
	main()