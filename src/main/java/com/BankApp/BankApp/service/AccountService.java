package com.BankApp.BankApp.service;

import com.BankApp.BankApp.model.Account;
import com.BankApp.BankApp.model.Transaction;
import com.BankApp.BankApp.repository.AccountRepository;
import com.BankApp.BankApp.repository.TransactionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.lang.reflect.Array;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

@Service
public class AccountService implements UserDetailsService {

    @Autowired
    PasswordEncoder passwordEncoder;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    public Account findAccountByUsername(String username){
        return accountRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("account not Found!"));
    }
    public Account registerAccount(String username,String password){
        if(accountRepository.findByUsername(username).isPresent()){
            throw new RuntimeException("Username alredy exist.");
        }
        Account account=new Account();
        account.setUsername(username);
        account.setPassword(passwordEncoder.encode(password));
        account.setBalance(BigDecimal.ZERO);
        return accountRepository.save(account);
    }

    public void deposit(Account account,BigDecimal amount){
        account.setBalance(account.getBalance().add(amount));

        accountRepository.save(account);

        Transaction transaction=new Transaction(
                amount,
                "Deposit",
                LocalDateTime.now(),
                account
        );
        transactionRepository.save(transaction);
    }
    public  void withdraw(Account account,BigDecimal amount){
        if(account.getBalance().compareTo(amount)<0){
            throw new RuntimeException("InSufficint funds");
        }
        account.setBalance(account.getBalance().subtract(amount));
        accountRepository.save(account);

        Transaction transaction=new Transaction(
                amount,
                "withdroawal",
                LocalDateTime.now(),
                account
        );
        transactionRepository.save(transaction);
    }
    public List<Transaction> getTransactionHistory(Account account){
        return transactionRepository.findByAccountId(account.getId());
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Account account = findAccountByUsername(username);
        if (account == null) {
            throw new UsernameNotFoundException("Username or password not found");
        }

        return org.springframework.security.core.userdetails.User.builder()
                .username(account.getUsername())
                .password(account.getPassword())
                .authorities(authorities()) // Correctly pass granted authorities
                .build();
    }


    public Collection<? extends GrantedAuthority> authorities(){
        return Arrays.asList(new SimpleGrantedAuthority("User"));
    }

    public void transferAmount(Account fromAccount,String toUsername,BigDecimal amount){
        if(fromAccount.getBalance().compareTo(amount)<0){
            throw new RuntimeException("insufficient funds");
        }
        Account toAccount=accountRepository.findByUsername(toUsername)
                .orElseThrow(() ->new RuntimeException("Recipient account not found"));

        //deduct
        fromAccount.setBalance(fromAccount.getBalance().subtract(amount));
        accountRepository.save(toAccount);

        //add
        toAccount.setBalance(toAccount.getBalance().add(amount));
        accountRepository.save(toAccount);

        //create transaction records
        Transaction debitTransaction=new Transaction(
                amount,
                "transfer Out to "+toAccount.getUsername(),
                LocalDateTime.now(),
                fromAccount
        );
        transactionRepository.save(debitTransaction);


        Transaction creditTransaction=new Transaction(
                amount,
                "transfer in to "+fromAccount.getUsername(),
                LocalDateTime.now(),
                toAccount
        );
        transactionRepository.save(creditTransaction);
    }




}
