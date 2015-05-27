@echo off
set classpath=../..

call %java_home%\bin\javac Server.java Client.java
call %java_home%\bin\rmic -d %classpath% ru.ifmo.ctddev.shah.rmi.AccountImpl ru.ifmo.ctddev.shah.rmi.BankImpl
