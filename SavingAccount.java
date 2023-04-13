import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class SavingAccount {
    int balance;
    ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    Condition preferredWithdrawCondition = lock.writeLock().newCondition();
    int preferredWithdrawCount = 0;

    // Deposit money into the account
    int deposit(int amount) {
        lock.writeLock().lock();
        try{
            // No need to check the balance before depositing
            balance = balance + amount;
            return balance;
        } finally {
            lock.writeLock().unlock();
        }
    }

    // Withdraw money from the account
    int withdraw(int amount) {
        lock.writeLock().lock();
        try {    

            // Wait for preferred withdraw to finish
            while (preferredWithdrawCount > 0) {
                try {
                    preferredWithdrawCondition.await();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            // Always check the balance before withdrawing
            if (amount > balance) {
                System.out.println("Insufficient balance");
                return -1;
            }
            balance = balance - amount;
            return balance;
        } finally {
            lock.writeLock().unlock();
        }
    }
    // Preferred withdraw money from the account
    // No withdraw is allowed while preferred withdraw is in progress
    int preferredWithdraw(int amount) {
        lock.writeLock().lock();
        try{
            preferredWithdrawCount += 1;

            // Always check the balance before withdrawing
            if (amount > balance) {
                System.out.println("Insufficient balance");
                lock.writeLock().unlock();
                return -1;
            }
            balance = balance - amount;
            preferredWithdrawCount -= 1;
            preferredWithdrawCondition.signalAll();
            return balance;
        }
        finally {
            lock.writeLock().unlock();
        }
    }

    // Transfer money from this account to another account
    void transfer(SavingAccount other, int amount) {
        lock.writeLock().lock();

        // Lock the other account to prevent deadlock
        other.lock.writeLock().lock();
        try {

            // always check the balance before transferring
            if (amount > balance) {
                System.out.println("Insufficient balance");
                return;
            }
            balance = balance - amount;
            other.deposit(amount);
        } finally {
            other.lock.writeLock().unlock();
            lock.writeLock().unlock();
        }
    }
}
