package ru.ifmo.ctddev.shah.rmi;

import java.rmi.*;
import java.net.*;

public class Client {

    public static void printUsage() {
        System.out.println("firstName surName passportSeries accountId moneyChange");
    }

    public static void main(String[] args) throws RemoteException {
        Bank bank;
        try {
            bank = (Bank) Naming.lookup("rmi://localhost/bank");
        } catch (NotBoundException e) {
            System.out.println("Bank URL is invalid");
            return;
        } catch (MalformedURLException e) {
            System.out.println("Bank is not bound");
            return;
        }
        final String name;
        final String surname;
        final String passport;
        final String accountId;
        final Integer accountChange;
        try {
            name = args[0];
            surname = args[1];
            passport = args[2];
            accountId = args[3];
            accountChange = Integer.parseInt(args[4]);
        } catch (ArrayIndexOutOfBoundsException | NullPointerException | NumberFormatException e) {
            System.err.println(e.getMessage());
            printUsage();
            return;
        }
        RemotePerson person = bank.getRemotePerson(passport);
        if (person == null) {
            System.out.println("Creating person");
            bank.createPerson(name, surname, passport);
            person = bank.getRemotePerson(passport);
        }
        Account account = bank.getAccount(person, accountId);
        if (account == null) {
            System.out.println("Create a new account: " + accountId);
            account = bank.createAccount(person, accountId);
        }
        System.out.println("Money initial: " + account.getAmount());
        System.out.println("Change money: " + accountChange);
        account.setAmount(account.getAmount() + accountChange);
        System.out.println("Money updated: " + account.getAmount());
    }
}
