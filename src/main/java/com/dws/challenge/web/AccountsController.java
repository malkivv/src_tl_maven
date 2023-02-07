package com.dws.challenge.web;

import com.dws.challenge.domain.Account;
import com.dws.challenge.exception.DuplicateAccountIdException;
import com.dws.challenge.exception.TransferException;
import com.dws.challenge.service.AccountsService;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.math.BigDecimal;

@RestController
@RequestMapping("/v1/accounts")
@Slf4j
public class AccountsController {

    private final AccountsService accountsService;

    @Autowired
    public AccountsController(AccountsService accountsService) {
        this.accountsService = accountsService;
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Object> createAccount(@RequestBody @Valid Account account) {
        log.info("Creating account {}", account);

        try {
            this.accountsService.createAccount(account);
        } catch (DuplicateAccountIdException daie) {
            return new ResponseEntity<>(daie.getMessage(), HttpStatus.BAD_REQUEST);
        }

        return new ResponseEntity<>(HttpStatus.CREATED);
    }

    @GetMapping(path = "/{accountId}")
    public Account getAccount(@PathVariable String accountId) {
        log.info("Retrieving account for id {}", accountId);
        return this.accountsService.getAccount(accountId);
    }

    //This rest endpoint is used to transfer amount from one account to another account
    //Required json params - fromAccountId , toAccountId , transferAmount
    @PostMapping(path = "/transferAmount", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Object> transferAmount(@RequestBody String transferJson) {
        JSONObject jsonObj = new JSONObject(transferJson);
        String fromAccountId, toAccountId;
        BigDecimal transferAmount;
        try{
            fromAccountId = jsonObj.getString("fromAccountId");
            toAccountId = jsonObj.getString("toAccountId");
            transferAmount = new BigDecimal(jsonObj.getString("transferAmount"));
            if(fromAccountId.equalsIgnoreCase(toAccountId)){
                return new ResponseEntity<>("From account id and to account id should not be same", HttpStatus.BAD_REQUEST);
            }
            if (transferAmount.doubleValue() <= 0) {
                return new ResponseEntity<>("Transfer amount should be greater than 0", HttpStatus.BAD_REQUEST);
            }
            String status = this.accountsService.transferAmount(fromAccountId,toAccountId,transferAmount);
            return new ResponseEntity<>(status,HttpStatus.ACCEPTED);
        } catch (JSONException | TransferException e){
            return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }
}
