package com.ticket.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

public final class Money {
    private final BigDecimal amount;

    public Money(BigDecimal amount){
        Objects.requireNonNull(amount);
        this.amount = amount.setScale(2, RoundingMode.HALF_UP);
    }
    public static Money of(double amount){
        return new Money(BigDecimal.valueOf(amount));
    }

    public Money applyDiscount(DiscountType d) {
        return new Money(d.apply(this.amount));
    }
    public BigDecimal getAmount() {
        return amount;
    }
    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Money m = (Money) o;
        return amount.compareTo(m.amount) == 0;
    }
    @Override public int hashCode() {
        return Objects.hash(amount.doubleValue());
    }
}

