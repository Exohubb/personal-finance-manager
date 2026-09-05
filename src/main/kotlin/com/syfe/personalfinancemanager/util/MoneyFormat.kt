package com.syfe.personalfinancemanager.util

import java.math.BigDecimal
import java.math.RoundingMode

fun BigDecimal.toMoney(): BigDecimal {
    return if (this.compareTo(BigDecimal.ZERO) == 0) {
        BigDecimal.ZERO
    } else {
        this.setScale(2, RoundingMode.HALF_UP)
    }
}
