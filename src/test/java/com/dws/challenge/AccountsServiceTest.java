package com.dws.challenge;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

import java.math.BigDecimal;

import com.dws.challenge.domain.Account;
import com.dws.challenge.exception.DuplicateAccountIdException;
import com.dws.challenge.exception.TransferException;
import com.dws.challenge.service.AccountsService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@SpringBootTest
class AccountsServiceTest {

  @Autowired
  private AccountsService accountsService;

  @Test
  void addAccount() {
    Account account = new Account("Id-123");
    account.setBalance(new BigDecimal(1000));
    this.accountsService.createAccount(account);

    assertThat(this.accountsService.getAccount("Id-123")).isEqualTo(account);
  }

  @Test
  void addAccount_failsOnDuplicateId() {
    String uniqueId = "Id-" + System.currentTimeMillis();
    Account account = new Account(uniqueId);
    this.accountsService.createAccount(account);

    try {
      this.accountsService.createAccount(account);
      fail("Should have failed when adding duplicate account");
    } catch (DuplicateAccountIdException ex) {
      assertThat(ex.getMessage()).isEqualTo("Account id " + uniqueId + " already exists!");
    }
  }

  @Test
  void transferAmount() {
    Account account1 = new Account("123");
    account1.setBalance(new BigDecimal(1000));
    this.accountsService.createAccount(account1);

    Account account2 = new Account("234");
    account2.setBalance(new BigDecimal(1000));
    this.accountsService.createAccount(account2);

    try {
      this.accountsService.transferAmount("345","234",new BigDecimal("200"));
    } catch (TransferException e) {
      assertThat(e.getMessage()).isEqualTo("From Account id " + 345 + " does not exists!");
    }

    try {
      this.accountsService.transferAmount("123","890",new BigDecimal("200"));
    } catch (TransferException e) {
      assertThat(e.getMessage()).isEqualTo("To Account id " + 890 + " does not exists!");
    }

    try {
      this.accountsService.transferAmount("123","234",new BigDecimal("1100"));
    } catch (TransferException e) {
      assertThat(e.getMessage()).isEqualTo("Insufficient balance!");
    }

    try {
      this.accountsService.transferAmount("123","234",new BigDecimal("399"));
      assertThat(account1.getBalance()).isEqualTo("601");
      assertThat(account2.getBalance()).isEqualTo("1399");
    } catch (TransferException e) {
      assertThat(e.getMessage()).isEqualTo("Insufficient balance!");
    }
  }

  @Test
  void concurrencyDeadlockTest() throws TransferException {
    Account account3 = new Account("456");
    account3.setBalance(new BigDecimal(1000));
    this.accountsService.createAccount(account3);

    Account account4 = new Account("789");
    account4.setBalance(new BigDecimal(1000));
    this.accountsService.createAccount(account4);

    for(int i=0; i < 100 ; i++){
      this.accountsService.transferAmount("456","789",new BigDecimal("3"));
      this.accountsService.transferAmount("789","456",new BigDecimal("4"));
    }

    assertThat(account3.getBalance()).isEqualTo("1100");
    assertThat(account4.getBalance()).isEqualTo("900");

  }

  @Test
  void negativeBalanceTest() {
    Account account5 = new Account("12345");
    account5.setBalance(new BigDecimal(1000));
    this.accountsService.createAccount(account5);

    Account account6 = new Account("67890");
    account6.setBalance(new BigDecimal(1000));
    this.accountsService.createAccount(account6);

    for(int i=0; i < 100 ; i++){
      try {
        this.accountsService.transferAmount("12345", "67890", new BigDecimal("15"));
      } catch (TransferException e) {
        assertThat(e.getMessage()).isEqualTo("Insufficient balance!");
      }
    }
    assertThat(account5.getBalance()).isEqualTo("10");
    assertThat(account6.getBalance()).isEqualTo("1990");
  }
}
