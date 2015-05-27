package ru.ifmo.ctddev.shah.rmi;

import java.io.Serializable;
import java.rmi.*;

public interface Account extends Remote, Serializable {
    /**
     * Returns id of account
     * @return id of account
     * @throws RemoteException if remote errors occur
     */
    public String getId()
        throws RemoteException;

    /**
     * Returns amount of account.
     * @return amount of account
     * @throws RemoteException if remote errors occur
     */
    public int getAmount()
        throws RemoteException;

    /**
     * Set new amount for account.
     * @throws RemoteException if remote errors occur
     */
    public void setAmount(int amount)
        throws RemoteException;
}