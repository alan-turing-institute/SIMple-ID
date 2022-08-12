# Host tools for the QR code Java Card Applet

## File Descriptions

| File  | Description | Usage Example |
| -------------     |-------------|-------------|
| setup_fs.py          | Sets up a Sysmocom sysmoISIM-SJA2 with<br>several icon files for use with the QR code<br>Java Card applet.     | `python3 setup_fs.py` |


## Setup SJA2 for file access using Osmocom PySIM tool

Configure the Sysmocom SJA2 EF.ARR to permit writing the necessary files (not secure) and enable the icon service. Use the [pysim tool](https://github.com/osmocom/pysim).

```
pySim-shell.py -p0 --pin-adm PIN-ADM1
select EF.ARR
update_record 1 80017f9000ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff
update_record 1 80017f9000ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff
select MF
select ADF.USIM
select EF.UST
ust_service_activate 22
exit
```

## History

This file last updated 9 Feb 2022.