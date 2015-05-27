#!/bin/bash
export CLASSPATH=../../../../..

javac Server.java Client.java
rmic -d $CLASSPATH ru.ifmo.ctddev.shah.rmi.RemotePersonImpl \
ru.ifmo.ctddev.shah.rmi.BankImpl ru.ifmo.ctddev.shah.rmi.AccountImpl
