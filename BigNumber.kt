package calculator

class BigNumber {
    val capacity = 3
    var negative: Boolean
    var number: String
    var value: Array<Int>

    constructor(_v: Array<Int>, _negative: Boolean) {
        this.negative = _negative
        this.value = _v
        this.number = this.toString()
    }

    constructor(_strNum: String) {
        this.number = _strNum
        this.negative = number.first() == '-'
        this.value = number.numToValue()
    }

    override fun toString(): String {
        var result = if (negative) "-" else ""
        result = result + value.first()
        for (i in 1..value.lastIndex) {
            var s = value[i].toString()
            if (s.length != capacity) {
                for (k in s.length until capacity) s = "0$s"
            }
            result += s
        }
        return result
    }

    fun String.numToValue(): Array<Int> {
        var num = if (negative) this.substring(1) else this
        while (num.length % capacity != 0) num = "0" + num
        val result = Array<Int>(num.length / capacity) { 0 }
        for (i in 0..num.lastIndex step capacity) {
            result[i / capacity] = num.substring(i, i + capacity).toInt()
        }
        return result
    }

    fun bigPlus(bn1: BigNumber, bn2: BigNumber): BigNumber {
        val rli = if (bn1.value.size > bn2.value.size) bn1.value.size else bn2.value.size
        val v1 = bn1.value
        val vli1 = bn1.value.lastIndex
        val v2 = bn2.value
        val vli2 = bn2.value.lastIndex
        val l = rli + 1
        val r = Array(l) { 0 }
        var cap = 1
        for (i in 1..capacity) cap *= 10
        for (i in 0..rli - 1) {
            if (vli1 - i >= 0) r[rli - i] = r[rli - i] + v1[vli1 - i]
            if (vli2 - i >= 0) {
                when (bn1.negative == bn2.negative) {
                    true -> if (r[rli - i] + v2[vli2 - i] >= cap) {
                        r[rli - i] = r[rli - i] + v2[vli2 - i] - cap
                        r[rli - i - 1]++
                    } else r[rli - i] = r[rli - i] + v2[vli2 - i]
                    false -> if (r[rli - i] >= v2[vli2 - i]) r[rli - i] = r[rli - i] - v2[vli2 - i] else {
                        r[rli - i] = r[rli - i] + cap - v2[vli2 - i]
                        r[rli - i - 1]--
                    }
                }
            } else {
                when (bn1.negative == bn2.negative) {
                    true -> if (r[rli - i] + 0 >= cap) {
                        r[rli - i] = r[rli - i] + 0 - cap
                        r[rli - i - 1]++
                    } else r[rli - i] = r[rli - i] + 0
                    false -> if (r[rli - i] >= 0) r[rli - i] = r[rli - i] - 0 else {
                        r[rli - i] = r[rli - i] + cap - 0
                        r[rli - i - 1]--
                    }
                }
            }
        }
        var neg = bn1.negative
        // val res = Array(rli) {0}
        if (r.first() < 0) {
            //r[0] = 0
            for (i in 0..r.lastIndex - 1) {
                if (r[r.lastIndex - i] != 0) {
                    r[r.lastIndex - i] = cap - r[r.lastIndex - i]
                    r[r.lastIndex - (i + 1)] = r[r.lastIndex - (i + 1)] + 1
                }
            }
            neg = !neg
        } // else for (i in 1..rli) res[i - 1] = r[i]
        return if (r.sum() != 0) {
            var s = r.size
            var x = 0
            while (r[x] == 0) {
                s--
                x++
            }
            val result = Array(s) { r[it + r.size - s] }
            BigNumber(result, neg)
        } else BigNumber("0")
    }

    fun bigTimes(bn1: BigNumber, bn2: BigNumber): BigNumber {
        val v_min: Array<Int>
        val vli_min: Int
        val v_max: Array<Int>
        val vli_max: Int
        if (bn1.value.size > bn2.value.size) {
            v_min = bn2.value
            vli_min = bn2.value.lastIndex
            v_max = bn1.value
            vli_max = bn1.value.lastIndex
        } else {
            v_min = bn1.value
            vli_min = bn1.value.lastIndex
            v_max = bn2.value
            vli_max = bn2.value.lastIndex
        }
        val lli = vli_max + vli_max + 2
        val l = lli + 1
        val r = Array(l) { 0 }
        var cap = 1
        for (i in 1..capacity) cap *= 10
        for (i in 0..vli_min) {
            val rt = Array(l) { 0 }
            for (j in 0..vli_max) {
                val m = v_min[vli_min - i] * v_max[vli_max - j] + rt[lli - j]
                if (m >= cap) {
                    rt[lli - j] = m % cap
                    rt[lli - j - 1] = m / cap
                } else rt[lli - j] = m
            }
            for (j in 0..lli - i) {
                val m = r[lli - j - i] + rt[lli - j]
                if (m >= cap) {
                    r[lli - j - i] = m - cap
                    r[lli - j - i - 1]++
                } else r[lli - j - i] = m
            }
        }
        val neg = bn1.negative != bn2.negative
        return if (r.sum() != 0) {
            var s = r.size
            var x = 0
            while (r[x] == 0) {
                s--
                x++
            }
            val result = Array(s) { r[it + r.size - s] }
            BigNumber(result, neg)
        } else BigNumber("0")
    }

    fun bigDigits(): Int {
        var digits = this.digits()
        return digits + (this.value.size - 1) * this.capacity
    }

    fun digits(): Int {
        var cap = 1
        var digits = 0
        for (i in 1..this.capacity) {
            if (this.value[0] / cap != 0) digits++
            cap *= 10
        }
        return digits
    }

    override operator fun equals(other: Any?): Boolean {
        if (this === other) return true
        other as BigNumber
        return this.value.contentEquals(other.value) && this.negative == other.negative
    }

    fun bigCompareTo(bn1: BigNumber, bn2: BigNumber): Int {
        val r = (bn1 - bn2)
        val x = 0
        for (i in 0..r.value.lastIndex) {
            if (r.value[x] != 0) break
        }
        return if (r.negative) -r.value[x] else r.value[x]
    }

    fun bigDiv(bn1: BigNumber, bn2: BigNumber): BigNumber {
        val digits1 = bn1.bigDigits()
        val digits2 = bn2.bigDigits()
        val abyss = digits1 - digits2
        if (abyss < 0 || bn1 == 0.toBigNumber()) return BigNumber("0")
        var result = ""
        val strDivisible =
            if (bn1.number.first() == '-') bn1.number.substring(1, digits2 + 1) else bn1.number.substring(0, digits2)
        if (digits2 <= capacity) {
            val divisor = bn2.number.toInt()
            var divisible = strDivisible.toInt()
            for (i in digits2..bn1.number.length) {
                var quotient = 0
                if (divisible >= divisor) {
                    while (divisible >= divisor) {
                        divisible -= divisor; quotient++
                    }
                    result += quotient.toString()
                } else {
                    result += 0
                }
                if (i <= bn1.number.lastIndex) divisible =
                    (divisible.toString() + bn1.number.substring(i, i + 1)).toInt()
            }
        } else {
            val divisor = bn2
            var divisible = strDivisible.toBigNumber()
            for (i in digits2..bn1.number.length) {
                var quotient = 0
                if (divisible >= divisor) {
                    while (divisible >= divisor) {
                        divisible -= divisor; quotient++
                    }
                    result += quotient.toString()
                } else {
                    result += 0
                }
                if (i <= bn1.number.lastIndex) divisible =
                    (divisible.toString() + bn1.number.substring(i, i + 1)).toBigNumber()
            }
        }
        return if (bn1.negative != bn2.negative) -result.toBigNumber() else result.toBigNumber()
    }

    fun bigPow(n: BigNumber): BigNumber {
        var a = this
        var c = 1.toBigNumber()
        val bi = n.number.toInt()
        for (i in 1..bi) c *= a
        return c
    }

    companion object {
        fun Int.toBigNumber(): BigNumber = BigNumber(this.toString())
        fun String.toBigNumber(): BigNumber = BigNumber(this)
        operator fun BigNumber.unaryMinus() = BigNumber("-" + this.number)
        operator fun BigNumber.plus(other: BigNumber): BigNumber = bigPlus(this, other)
        operator fun BigNumber.minus(other: BigNumber): BigNumber = bigPlus(this, -other)
        operator fun BigNumber.times(other: BigNumber): BigNumber = bigTimes(this, other)
        operator fun BigNumber.compareTo(other: BigNumber): Int = bigCompareTo(this, other)
        operator fun BigNumber.div(other: BigNumber): BigNumber = bigDiv(this, other)
    }
}




