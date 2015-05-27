package ru.ifmo.ctddev.shah.rmi;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.HashMap;
import java.util.Map;

/**
 * Implementing for {@link RemotePerson}
 * Created on 11.05.15.
 *
 * @author sultan
 */
public class RemotePersonImpl extends UnicastRemoteObject implements RemotePerson {
    private static final long serialVersionUID = 14L;
    private final String passportSeries;
    private final String name;
    private final String surname;

    public RemotePersonImpl(
            final String name, final String surname,
            final String passportSeries
    ) throws RemoteException {
        super();
        this.passportSeries = passportSeries;
        this.name = name;
        this.surname = surname;
    }

    public RemotePersonImpl(final Person person) throws RemoteException {
        this.passportSeries = person.getPassportSeries();
        this.name = person.getName();
        this.surname = person.getSurname();
    }

    @Override
    public String getPassportSeries() throws RemoteException {
        return passportSeries;
    }

    @Override
    public String getName() throws RemoteException {
        return name;
    }

    @Override
    public String getSurname() throws RemoteException {
        return surname;
    }

}
