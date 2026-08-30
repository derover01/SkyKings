package net.skykings.combat.kill;

import net.skykings.core.netherstar.NetherstarOverflowException;
import net.skykings.core.netherstar.NetherstarService;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Minimales In-Memory-Test-Double fuer {@link NetherstarService} (kein PlayerProfile noetig). */
final class FakeNetherstarService implements NetherstarService {

    private final Map<UUID, Long> balances = new HashMap<>();
    private int depositCallCount;

    void seedBalance(UUID uuid, long balance) {
        balances.put(uuid, balance);
    }

    int getDepositCallCount() {
        return depositCallCount;
    }

    @Override
    public long getBalance(UUID uuid) {
        return balances.getOrDefault(uuid, 0L);
    }

    @Override
    public boolean has(UUID uuid, long amount) {
        return getBalance(uuid) >= amount;
    }

    @Override
    public void setBalance(UUID uuid, long amount, String actor, String reason) {
        balances.put(uuid, amount);
    }

    @Override
    public void deposit(UUID uuid, long amount, String actor, String reason) {
        depositCallCount++;
        long newBalance;
        try {
            newBalance = Math.addExact(getBalance(uuid), amount);
        } catch (ArithmeticException e) {
            throw new NetherstarOverflowException("overflow in test double", e);
        }
        balances.put(uuid, newBalance);
    }

    @Override
    public boolean withdraw(UUID uuid, long amount, String actor, String reason) {
        long balance = getBalance(uuid);
        if (balance < amount) {
            return false;
        }
        balances.put(uuid, balance - amount);
        return true;
    }
}
