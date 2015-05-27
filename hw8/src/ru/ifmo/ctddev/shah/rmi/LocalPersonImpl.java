package ru.ifmo.ctddev.shah.rmi;

import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.Map;

/**
 * Implementing for {@link LocalPerson}
 * Created on 11.05.15.
 *
 * @author sultan
 */
public class LocalPersonImpl implements LocalPerson {
    private static final long serialVersionUID = 14L;
    private final String passportSeries;
    private final String name;
    private final String surname;

    public LocalPersonImpl(final String name, final String surname, final String passportSeries) {
        this.passportSeries = passportSeries;
        this.name = name;
        this.surname = surname;
    }

    public LocalPersonImpl(final Person person) throws RemoteException {
        this.passportSeries = person.getPassportSeries();
        this.name = person.getName();
        this.surname = person.getSurname();
    }

    @Override
    public String getPassportSeries() {
        return passportSeries;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getSurname() {
        return surname;
    }

}
