@echo off

pushd ..\..
SET PROJECT_HOME="%cd%"
popd
Rem for developnement kit
SET JC_HOME=%PROJECT_HOME%\JavaLib\java_card_kit-3_0_4
SET JAVA_HOME=%PROJECT_HOME%\JavaLib\jdk1.6.0_38
SET JAVA_HOME_17=%PROJECT_HOME%\JavaLib\jdk1.7.0_45

SET JAVA=%JAVA_HOME%\bin\java.exe
SET JAVAC=%JAVA_HOME%\bin\javac.exe
SET JAVA17=%JAVA_HOME_17%\bin\java.exe



Rem for convert CAP file
SET CONVERT_EXP=%PROJECT_HOME%\Dev\Exp
SET CONVERT_LIB=%PROJECT_HOME%\Dev\lib

Rem for convert tool

SET ScriptGenerator=%PROJECT_HOME%\Bin\ConvertSCR\ScriptGenerator.exe

Rem for sample project
SET SAMPLE_NAME=QRAuthy
rem SET SAMPLE_HMOE=%PROJECT_HOME%\%SAMPLE_NAME%
SET SAMPLE_HMOE="%cd%"
SET SAMPLE_SRC=%SAMPLE_HMOE%\src
SET SAMPLE_BIN=%SAMPLE_HMOE%\classes

Rem for Compilation and Conversion CAP
SET UICC_API=%CONVERT_LIB%\102241_Annex_D.jar
SET USIM_API=%CONVERT_LIB%\31130_Annex_D.jar
SET GPAPI=%CONVERT_LIB%\gpapi-globalplatform.jar
SET ThreeGAPI=%UICC_API%;%USIM_API%
SET JCDKAPI=%JC_HOME%\lib\ant-contrib-1.0b3.jar;%JC_HOME%\lib\api_classic_annotations.jar;%JC_HOME%\lib\asm-all-3.1.jar;%JC_HOME%\lib\bcel-5.2.jar;%JC_HOME%\lib\commons-cli-1.0.jar;%JC_HOME%\lib\commons-codec-1.3.jar;%JC_HOME%\lib\commons-httpclient-3.0.jar;%JC_HOME%\lib\commons-logging-1.1.jar;%JC_HOME%\lib\jctasks.jar;%JC_HOME%\lib\tools.jar;%JC_HOME%\lib\api_classic.jar;%JC_HOME%\lib;
SET _CLASSES=%ThreeGAPI%;%SAMPLE_BIN%;%JCDKAPI%;%CONVERT_LIB%\tai_debug.jar;%CONVERT_LIB%\simome_api.jar;%CONVERT_LIB%\gpapi-globalplatform.jar;

Rem for Generate Script
SET CFG_PATH=%SAMPLE_HMOE%\install_cfg.xml
SET LIB_CLASSES=%TOOL_LIB%\ConvertSCR.jar;%TOOL_LIB%\tai_GP.jar;%TOOL_LIB%\tai_CFG.jar;%TOOL_LIB%\tai_reader.jar;%TOOL_LIB%\tai_util.jar;


echo =============================================================
echo      Compilation 
echo =============================================================

echo delete \QRSTK\*.class
del %SAMPLE_BIN%\QRSTK\*.class

pushd %SAMPLE_SRC%

echo compile QRSTK\*.java
%JAVAC% -classpath %_CLASSES% -d %SAMPLE_BIN% QRSTK\*.java


echo =============================================================
echo      Conversion CAP
echo =============================================================

cd %SAMPLE_BIN%

echo Conversion CAP
%JAVA% -Djc.home=%JC_HOME% -classpath %_CLASSES% com.sun.javacard.converter.Main -out JCA CAP EXP -exportpath %CONVERT_EXP%;%SAMPLE_BIN%;%JC_HOME%\api_export_files; -classdir . -applet 0xA0:0x00:0x00:0x00:0x00:0x01:0x02:0x03:0x08:0x01:0x00  QRSTK.QRSTK QRSTK 0xA0:0x00:0x00:0x00:0x00:0x01:0x02:0x03:0x08:0x00:0x00 1.0


echo =============================================================
echo      Generate Script
echo =============================================================

echo Generate Script
%ScriptGenerator% %SAMPLE_BIN% QRSTK %CFG_PATH%

cd ..