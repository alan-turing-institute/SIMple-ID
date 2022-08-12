# QR codes for authentication using STK

This repository contains a Java Card 2.2.1 applet which generates and displays QR codes for authentication using (almost*) the standard SIM ToolKit (STK) API. 

[See video!](X)

# Requirements

Tested with:
* [sysmoISIM-SJA2](http://shop.sysmocom.de/products/sysmoISIM-SJA2) with ADM keys. Do not unlock the card.
* Nokia 106 Feature Phone and TTfone TT240 Smart-Feature Phone
* PCSC card reader e.g. Gemalto IDBridge CT30
* OpenJDK 11
* Debian bullseye and OSX X 10.15.7
* Python 2.7.18
* The pycrypto and pyscard libraries
```
pip2 install --user pycrypto pyscard
```

Recommended:
* FF to 2FF smartcard converter, or equivalent for your development device, [for example](https://www.aliexpress.com/item/32769577127.html?spm=a2g0s.9042311.0.0.5b4b4c4d68yrxs).

# Build

Ensure you have OpenJDK 11, the `ant` build tool and configure JDK 11 as your Java environment:
```
 sudo apt install openjdk-11-jdk ant
 sudo update-alternatives --config java
```
or
```
brew install openjdk@11 ant
export JAVA_HOME=$(/usr/libexec/java_home -v 11)
```

Clone this repository:
```
git clone https://github.com/alan-turing-institute/UICC_QR_Code_Authenticator.git
cd UICC_QR_Code_Authenticator
```

Clone the Javacard SDK and sim-tools dependencies:
```
git submodule update --init --recursive
```

Then run `ant` to build the Java Card applet, QRSTK.cap. 


# Install

Load and install the STK applet. It is vitally important that you replace `KIC1` and `KID1` with the specific keys for your Java Card. These are provided at the time of purchase and enable the Over The Air (OTA) security needed for loading STK applets to your card.

```
python2 sim-tools/shadysim/shadysim_isim.py --pcsc \
      -l ./bin/QRSTK.cap \
      -i ./bin/QRSTK.cap \
      --kic KIC1 \
      --kid KID1 \
      --instance-aid f07002CA44900101 \
      --module-aid f07002CA44900101 \
      --nonvolatile-memory-required 0fff \
      --volatile-memory-for-install 0fff \
      --access-domain 00000010 \
      --enable-uicc-toolkit \
      --enable-uicc-file-access \
      --max-menu-entry-text 20 \
      --max-menu-entries 06 
```

To uninstall the STK applet, again replace `KIC1` and `KID1` with your card keys and then run the following.

```
python2 sim-tools/shadysim/shadysim_isim.py --pcsc -d f07002CA44\
      --kic KIC --kid KID
```

# Credits and Gratitude

* This work was supported, in whole or in part, by the Bill & Melinda Gates Foundation [INV-001309].
* We are very thankful for @mrlnc's [HelloSTK2 repository](https://github.com/mrlnc/HelloSTK2) which made getting STK to work on the sysmoISIM-SJA2 a walk in the park!
