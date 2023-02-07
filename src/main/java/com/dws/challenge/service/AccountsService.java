package com.dws.challenge.service;

import com.dws.challenge.Thread.TransferThread;
import com.dws.challenge.domain.Account;
import com.dws.challenge.exception.TransferException;
import com.dws.challenge.repository.AccountsRepository;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.concurrent.*;

@Service
public class AccountsService {

    @Getter
    private final AccountsRepository accountsRepository;

    @Getter
    private final NotificationService notificationService;

    private final ExecutorService executorService = Executors.newFixedThreadPool(10);

    @Autowired
    public AccountsService(AccountsRepository accountsRepository, NotificationService notificationService) {
        this.accountsRepository = accountsRepository;
        this.notificationService = notificationService;
    }

    public void createAccount(Account account) {
        this.accountsRepository.createAccount(account);
    }

    public Account getAccount(String accountId) {
        return this.accountsRepository.getAccount(accountId);
    }

    //This is service layer Implementation of transferAmount functionality which Internally uses ExecutorService thread pool
    //ThreadPool size is kept to 10 but can be increased based on performance when no of transaction requests increases
    public String transferAmount(String fromAccountId, String toAccountId, BigDecimal transferAmount) throws TransferException {
        Account fromAccount = this.accountsRepository.getAccount(fromAccountId);
        Account toAccount = this.accountsRepository.getAccount(toAccountId);
        if (fromAccount == null) {
            throw new TransferException("From Account id " + fromAccountId + " does not exists!");
        }
        if (toAccount == null) {
            throw new TransferException("To Account id " + toAccountId + " does not exists!");
        }

        try {
            TransferThread transferThread = new TransferThread(fromAccount, toAccount, transferAmount);
            Future<String> future = executorService.submit(transferThread);
            String status = future.get();
            if (status.equalsIgnoreCase("Success")) {
                notificationService.notifyAboutTransfer(fromAccount, "Amount Debited:"+transferAmount+" Current balance:"+fromAccount.getBalance());
                notificationService.notifyAboutTransfer(toAccount, "Amount Credited:"+transferAmount+" Current balance:"+toAccount.getBalance());
            } else {
                throw new TransferException(status);
            }
            return status;
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
    }
}