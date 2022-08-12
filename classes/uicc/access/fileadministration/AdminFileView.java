package uicc.access.fileadministration;
import javacard.framework.Shareable;
public interface AdminFileView extends Shareable {
short select(short param1, byte[] param2, short param3, short param4);
void select(short param1);
void select(byte param1);
short status(byte[] param1, short param2, short param3);
short readBinary(short param1, byte[] param2, short param3, short param4);
void updateBinary(short param1, byte[] param2, short param3, short param4);
short readRecord(short param1, byte param2, short param3, byte[] param4, short param5, short param6);
void updateRecord(short param1, byte param2, short param3, byte[] param4, short param5, short param6);
short searchRecord(byte param1, short param2, short param3, byte[] param4, short param5, short param6, short[] param7, short param8, short param9);
short increase(byte[] param1, short param2, short param3, byte[] param4, short param5);
void deactivateFile();
void activateFile();
void deleteFile(short param1);
void createFile(uicc.toolkit.ViewHandler param1);
void resizeFile(uicc.toolkit.ViewHandler param1);
}
