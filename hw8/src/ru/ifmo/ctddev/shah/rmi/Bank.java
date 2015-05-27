package ru.ifmo.ctddev.shah.rmi;

import java.rmi.*;

public interface Bank extends Remote {
    /**
     * Returns new person.
     * @param name firstname
     * @param surname surname
     * @param passportSeries passport series
     * @throws RemoteException if remote errors occur
     */
    void createPerson(final String name, final String surname, final String passportSeries)
        throws RemoteException;

    /**
     * Returns person if exists.
     * @param id passport series
     * @return LocalPerson with given id or null
     * @throws RemoteException if remote errors occur
     */
    LocalPerson getLocalPerson(final String id)
        throws RemoteException;

    /**
     * Returns person if exists.
     * @param id passport series
     * @return RemotePerson with given id or null
     * @throws RemoteException if remote errors occur
     */
    RemotePerson getRemotePerson(final String id)
        throws RemoteException;

    Account createAccount(final Person person, final String id)
        throws RemoteException;

    Account getAccount(final Person person, final String id)
        throws RemoteException;

}
