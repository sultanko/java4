package ru.ifmo.ctddev.shah.rmi;

import java.util.*;
import java.rmi.server.*;
import java.rmi.*;
import java.util.concurrent.ConcurrentHashMap;

public class BankImpl extends UnicastRemoteObject implements Bank {
    private static final long serialVersionUID = 17L;
    private final Map<String, Person> persons = new ConcurrentHashMap<>();
    private final Map<String, Map<String, Account>> personsAccounts = new ConcurrentHashMap<>();

    public BankImpl(final int port) throws RemoteException {
        super(port);
    }

    @Override
    public void createPerson(final String name, final String surname, final String passportSeries) throws RemoteException {
        persons.putIfAbsent(passportSeries,
                new RemotePersonImpl(name, surname, passportSeries));
    }

    @Override
    public LocalPerson getLocalPerson(final String id) throws RemoteException {
        Person person = persons.get(id);
        return person == null ? null : new LocalPersonImpl(person);
    }

    @Override
    public RemotePerson getRemotePerson(final String id) throws RemoteException {
        Person person = persons.get(id);
        return person == null ? null : ((RemotePersonImpl)person);
    }

    @Override
    public Account createAccount(final Person person, final String id) throws RemoteException {
        personsAccounts.putIfAbsent(person.getPassportSeries(), new ConcurrentHashMap<>());
        Map<String, Account> accounts = personsAccounts.get(person.getPassportSeries());
        accounts.putIfAbsent(id, new AccountImpl(id));
        return accounts.get(id);
    }

    @Override
    public Account getAccount(final Person person, final String id) throws RemoteException {
        final Map<String, Account> accounts = personsAccounts.get(person.getPassportSeries());
        if (accounts == null) {
            return null;
        } else {
            return accounts.get(id);
        }
    }
}
