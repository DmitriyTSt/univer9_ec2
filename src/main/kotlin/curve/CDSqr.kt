package curve

import util.Legendre
import java.math.BigInteger

object CDSqr {
	/** Числа до столько бит будут раскладываться перебором */
	private const val THRESHOLD = 12

	/**
	 * Вычисление разложения c^2 + d^2 = p в sqrt(-d)
	 */
	fun get(p: BigInteger, d: BigInteger): Pair<BigInteger, BigInteger>? {
		// если число достаточно маленькое, то найдем разложение перебором
		if (p.bitLength() <= THRESHOLD) {
			return getCDSimple(p)
		}

		// проверяем по символу Лежандра
		val sl = Legendre.get(-d, p)
		if (sl == -1) {
			return null
		}

		// Вычисляем U = sqrt(-d) mod p, в нашем случае d = -3
		val uuu = getU(-d, p) ?: return null
		// Далее набираем массив u m по алгориму 7.8.1 Ростовцева (шаги 3 - 5)
		val u = mutableListOf(uuu)
		val m = mutableListOf(p)
		var i = 0
		do {
			val ui = u.last()
			val mi = m.last()
			val mi1 = (ui * ui + d) / mi
			val ui1 = if (ui % mi1 < (mi1 - ui) % mi1) {
				ui % mi1
			} else {
				(mi1 - ui) % mi1
			}
			u.add(ui1)
			m.add(mi1)
			if (mi1 != BigInteger.ONE) {
				i++
			}
		} while (mi1 != BigInteger.ONE)

		// Создаем массивы a и b нужной длины и заполняем их согласно алгориму 7.8.1 Ростовцева (шаг 6)
		val a = MutableList(i + 1) { BigInteger.ZERO }
		a[a.lastIndex] = u[i]
		val b = MutableList(i + 1) { BigInteger.ZERO }
		b[b.lastIndex] = BigInteger.ONE
		// если i = 0, то результат уже есть
		if (i == 0) {
			return a.last() to b.last()
		}

		// иначе проходим в обратном порядке и вычисляем a[i] и b[i] по алгориму 7.8.1 Ростовцева (шаги 7-8)
		while (i > 0) {
			// выбираем так, чтобы деление было целочисленное из ap1 и ap2
			val ap1 = u[i - 1] * a[i] + d * b[i]
			val ap2 = -u[i - 1] * a[i] + d * b[i]
			// на что делим
			val div = a[i] * a[i] + d * b[i] * b[i]
			a[i - 1] = if (ap1 % div == BigInteger.ZERO) {
				ap1 / div
			} else {
				ap2 / div
			}
			// выбираем так, чтобы деление было целочисленное из bp1 и bp2
			val bp1 = -a[i] + u[i - 1] * b[i]
			val bp2 = -a[i] - u[i - 1] * b[i]
			b[i - 1] = if (bp1 % div == BigInteger.ZERO) {
				bp1 / div
			} else {
				bp2 / div
			}
			i--
		}
		// выводим abs от результата, так как они стоят под квадратом, а с положительными числами работать легче
		return a.first().abs() to b.first().abs()
	}

	/**
	 * Вычисление sqrt (-d) mod p
	 */
	private fun getU(d: BigInteger, p: BigInteger): BigInteger? {
		// ищем, какое именно у нас число p для использования быстрых алгоритмов
		if (p % 4.toBigInteger() == 3.toBigInteger()) {
			return getU34(d, p)
		}
		if (p % 8.toBigInteger() == 5.toBigInteger()) {
			return getU58(d, p)
		}
		// если число p имеет неподходящий вид, то будем генерировать новое
		return null
	}

	/**
	 * Извлечение квадратного корня в случае q % 4 == 3
	 */
	private fun getU34(a: BigInteger, q: BigInteger): BigInteger? {
		val x = a.modPow((q + BigInteger.ONE) / 4.toBigInteger(), q)
		return if ((x * x) % q == (a % q + q) % q) {
			x
		} else {
			null
		}
	}

	/**
	 * Извлечение квадратного корня в случае q % 8 == 5
	 */
	private fun getU58(a: BigInteger, q: BigInteger): BigInteger? {
		val b = a.modPow((q + 3.toBigInteger()) / 8.toBigInteger(), q)
		val c = a.modPow((q - BigInteger.ONE) / 4.toBigInteger(), q)
		return when {
			c.abs() != BigInteger.ONE -> null
			c == BigInteger.ONE -> b
			else -> {
				val i = 2.toBigInteger().modPow((q - BigInteger.ONE) / 4.toBigInteger(), q)
				(b * i) % q
			}
		}
	}

	/**
	 * Разложение на c и d перебором для маленьких чисел
	 */
	private fun getCDSimple(p: BigInteger): Pair<BigInteger, BigInteger>? {
		var c = BigInteger.ONE
		while (c < p) {
			var d = BigInteger.ONE
			while (d < p) {
				if (c * c + 3.toBigInteger() * d * d == p) {
					return c to d
				}
				d++
			}
			c++
		}
		return null
	}
}