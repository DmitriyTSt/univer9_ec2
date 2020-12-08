package curve

import java.io.File
import java.math.BigInteger

/**
 * Эллиптическая кривая
 */
class EllipticCurve(
    val p: BigInteger,
    private val b: BigInteger,
    val q: Point,
    val r: BigInteger
) {
    companion object {
        /**
         * Сгенерировать новую эллиптическую кривую
         *
         * @param l - число бит p
         * @param m - параметр безопасности
         */
        fun create(l: Int, m: Int = 3): EllipticCurve {
            var p = Utils.generatePrimeP(l, null)
            var result = EllipticCurveHelper.generate(p, m)
            while (result == null) {
                p = Utils.generatePrimeP(l, p)
                result = EllipticCurveHelper.generate(p, m)
            }
            return result
        }

        /**
         * Считать эллиптическую кривую из файла
         */
        fun fromFile(path: String): EllipticCurve? {
            try {
                val fileLines = File(path).readLines()
                return EllipticCurve(
                    fileLines[0].toBigInteger(),
                    fileLines[1].toBigInteger(),
                    fileLines[2].split(" ").let {
                        Point(it[0].toBigInteger(), it[1].toBigInteger(), fileLines[0].toBigInteger())
                    },
                    fileLines[3].toBigInteger()
                )
            } catch (e: Exception) {
                println("Ошибка чтения кривой из файла, кривая будет сгенерирована заново")
                return null
            }
        }
    }

    /**
     * Сохранить эллиптическую кривую в файл
     * @param path путь к файлу
     */
    fun saveToFile(path: String) {
        File(path).apply {
            if (!exists()) {
                createNewFile()
            }
            writeText("$p\n$b\n${q.x} ${q.y}\n$r")
        }
    }


}