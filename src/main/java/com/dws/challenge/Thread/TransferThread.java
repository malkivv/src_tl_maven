package com.dws.challenge.Thread;

import com.dws.challenge.domain.Account;
import java.math.BigDecimal;
import java.util.concurrent.Callable;

public class TransferThread implements Callable<String> {

    final Account fromAccount;
    final Account toAccount;
    BigDecimal transferAmount;

    public TransferThread(Account fromAccount, Account toAccount, BigDecimal transferAmount){
        this.fromAccount = fromAccount;
        this.toAccount = toAccount;
        this.transferAmount = transferAmount;
    }

    //TransferThread contains the core business logic for transferring amount from one account to another
    //Synchronization order is defined based on account id to avoid deadlocks during inter account transfers in reverse order
    //Account id is assumed to be numeric value
    @Override
    public String call() {
        long fromAccountIdLong = Long.parseLong(fromAccount.getAccountId());
        long toAccountIdLong = Long.parseLong(toAccount.getAccountId());
        if (fromAccountIdLong < toAccountIdLong) {
            synchronized (fromAccount) {
                synchronized (toAccount) {
                    if (fromAccount.getBalance().subtract(transferAmount).doubleValue() >= 0) {
                        fromAccount.setBalance(fromAccount.getBalance().subtract(transferAmount));
                        toAccount.setBalance(toAccount.getBalance().add(transferAmount));
                    } else {
                        return "Insufficient balance!";
                    }
                }
            }
        } else {
            synchronized (toAccount) {
                synchronized (fromAccount) {
                    if (fromAccount.getBalance().subtract(transferAmount).doubleValue() >= 0) {
                        fromAccount.setBalance(fromAccount.getBalance().subtract(transferAmount));
                        toAccount.setBalance(toAccount.getBalance().add(transferAmount));
                    } else {
                        return "Insufficient balance!";
                    }
                }
            }
        }
        return "Success";
    }
}
