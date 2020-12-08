package curve

import logd
import util.Legendre
import java.math.BigInteger
import kotlin.system.exitProcess

object EllipticCurveHelper {
    /**
     * Построение кривой, если хотя бы один пункт не соответствует проверкам, возвращаем null и идем за новым p
     */
    fun generate(p: BigInteger?, m: Int): EllipticCurve? {
        // если возможное p вылетело за границу размера, то решений нет
        if (p == null) {
            println("Для заданного l решение не найдено")
            exitProcess(0)
        }
        logd("p = $p")
        // вычисление разложения c^2 + d^2 = p в sqrt(-d) (Ростовцев, 15.3.1, п 2)
        val cd = CDSqr.get(p, 3.toBigInteger()) ?: return null
        val c = cd.first
        val d = cd.second
        logd("$p = ($c)^2 + 3*($d)^2,    c = $c, d = $d")
        // нахождение чисел N r (Ростовцев, 15.3.1, п 3)
        val nr = Utils.getNR(p, c, d) ?: return null
        val n = nr.first
        val r = nr.second
        logd("N = $n, r = $r")
        // проверки на отношение p и r, при неудаче - перегенерируем (Ростовцев, 15.3.1, п 4)
        if (p == r) return null
        var pp = p
        repeat(m) {
            if (pp % r == BigInteger.ONE) {
                return null
            }
            pp *= p
        }
        logd("p != r, p^i != 1 (mod r)")
        var result: EllipticCurve?  = null
        // нашли нужные n r, генерируем точку и пытаемся проверить созданную точку b из произвольной точки
        // (Ростовцев, 15.3.1, п 5)
        while (result == null) {
            val point = Utils.generatePoint(p)
            val x0 = point.x
            val y0 = point.y
            val b = ((y0 * y0 % n - (x0 * x0 * x0) % n) + n) % n
            logd("($x0, $y0) b = $b")
            val checkB = when (n / r) {
                // проверка b - квадратичный и кубический НЕвычет для n == r
                1.toBigInteger() -> !Legendre.isSqrResidue(b, n) && Legendre.isCubeResidue(b, r) == false
                // проверка b - квадратичный и кубический вычет для n == 6r
                6.toBigInteger() -> Legendre.isSqrResidue(b, n) && Legendre.isCubeResidue(b, r) == true
                // проверка b - квадратичный НЕвычет и кубический вычет для n == 2r
                2.toBigInteger() -> !Legendre.isSqrResidue(b, n) && Legendre.isCubeResidue(b, r) == true
                // проверка b - квадратичный вычет и кубический НЕвычет для n == 3r
                3.toBigInteger() -> Legendre.isSqrResidue(b, r) &&
                        Legendre.isSqrResidue(b, 3.toBigInteger()) && Legendre.isCubeResidue(b, r) == false
                else -> false
            }
            // если прошли проверку B, то проходим проверку на произведение точки на число, иначе result останется null
            // и будет сгенерирована новая произвольная точка
            if (checkB) {
                // проверка N * point = P(inf) (знаменатель углового коэф в формуле сложения обращается в 0)
                // (Ростовцев, 15.3.1, п 6)
                val newP = point * n
                logd("n * point = $newP")
                if (newP.coord == null) {
                    // вычисляем Q (Ростовцев, 15.3.1, п 7)
                    val q = point * (n / r)
                    // нашли удовлетворяющий нас результат
                    result = EllipticCurve(p, b, q, r)
                }
            }
        }

        return result
    }
}