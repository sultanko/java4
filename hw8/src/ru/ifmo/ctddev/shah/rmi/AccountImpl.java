package ru.ifmo.ctddev.shah.rmi;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

public class AccountImpl extends UnicastRemoteObject implements Account {
    private static final long serialVersionUID = 7L;
    private final String id;
    private int amount;

    public AccountImpl(String id) throws RemoteException {
        super();
        this.id = id;
        amount = 0;
    }

    public String getId() throws RemoteException {
        return id;
    }

    public int getAmount() throws RemoteException {
        return amount;
    }

    public void setAmount(int amount) throws RemoteException {
        System.out.println("Setting amount of money for account " + id);
        this.amount = amount;
    }
}
