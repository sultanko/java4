#!/bin/bash
export CLASSPATH=../../../../../../out/production/hw8
#export CLASSPATH=../../../../..


rmiregistry &
java ru.ifmo.ctddev.shah.rmi.Server
