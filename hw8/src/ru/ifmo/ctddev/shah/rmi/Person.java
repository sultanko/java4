package ru.ifmo.ctddev.shah.rmi;

import java.rmi.RemoteException;

/**
 * Created on 11.05.15.
 *
 * @author sultan
 */
public interface Person {

    String getPassportSeries() throws RemoteException;
    String getName() throws RemoteException;
    String getSurname() throws RemoteException;

}
