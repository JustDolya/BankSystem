import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.Random;

class Account {
    private double balance;
    private final Lock lock = new ReentrantLock();
    private final Condition sufficientFunds = lock.newCondition(); // Условие для ожидания средств

    // Конструктор для начального баланса
    public Account(double initialBalance) {
        this.balance = initialBalance;
    }

    // Метод для пополнения счета
    public void deposit(double amount) {
        lock.lock();
        try {
            balance += amount;
            System.out.println("Пополнено: " + amount + ". Текущий баланс: " + balance);
            sufficientFunds.signalAll(); // Уведомляем ожидающие потоки, что баланс изменился
        } finally {
            lock.unlock();
        }
    }

    // Метод для снятия средств с аккаунта
    public void withdraw(double amount) {
        lock.lock();
        try {
            while (balance < amount) {
                System.out.println("Недостаточно средств для снятия " + amount + ". Ожидание...");
                sufficientFunds.await(); // Ожидаем пополнения
            }
            balance -= amount;
            System.out.println("Снято: " + amount + ". Оставшийся баланс: " + balance);
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            lock.unlock();
        }
    }

    // Метод для получения текущего баланса
    public double getBalance() {
        return balance;
    }
}

class DepositThread extends Thread {
    private final Account account;

    public DepositThread(Account account) {
        this.account = account;
    }

    @Override
    public void run() {
        Random random = new Random();
        for (int i = 0; i < 5; i++) {
            account.deposit(random.nextInt(1000)); // Случайная сумма для пополнения
            try {
                Thread.sleep(random.nextInt(1500)); // Задержка перед следующим пополнением
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}

class WithdrawThread extends Thread {
    private final Account account;
    private final double amountToWithdraw;

    public WithdrawThread(Account account, double amountToWithdraw) {
        this.account = account;
        this.amountToWithdraw = amountToWithdraw;
    }

    @Override
    public void run() {
        account.withdraw(amountToWithdraw);
    }
}

public class Main {
    public static void main(String[] args) {
        Account account = new Account(100000); // Начальный баланс

        account.deposit(3000);
        // Запуск потока для пополнения
        DepositThread depositThread = new DepositThread(account);
        depositThread.start();

        // Запуск потока для снятия
        WithdrawThread withdrawThread = new WithdrawThread(account, 105000);
        withdrawThread.start();
    }
}
