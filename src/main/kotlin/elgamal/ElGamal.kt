package elgamal

import curve.EllipticCurve
import curve.Point
import nextBigInteger
import toBigInteger
import java.io.File
import java.math.BigInteger
import java.security.MessageDigest
import kotlin.random.Random

/**
 * Все необходимое для работы протокола цифровой подписи Эль Гамаля на эллиптических кривых
 * */
class ElGamal(
    private val curve: EllipticCurve
) {

    companion object {
        /**
         * Сохранение секретного ключа
         * @param secret
         * в файл
         * @param secretPath
         */
        fun saveSecretKeyToFile(secretPath: String, secret: BigInteger) {
            File(secretPath).apply {
                if (!exists()) {
                    createNewFile()
                }
                writeText(secret.toString())
            }
        }

        /**
         * Сохранение открытого ключа
         * @param public
         * в файл
         * @param publicPath
         */
        fun savePublicKeyToFile(publicPath: String, public: Point) {
            File(publicPath).apply {
                if (!exists()) {
                    createNewFile()
                }
                writeText("${public.x}\n${public.y}")
            }
        }

        /**
         * Запись подписи
         * @param signature
         * в файл
         * @param path
         */
        fun saveSignatureToFile(path: String, signature: Pair<BigInteger, BigInteger>) {
            File(path).apply {
                if (!exists()) {
                    createNewFile()
                }
                writeText("${signature.first}\n${signature.second}")
            }
        }

        /** Чтение сигнатуры из файла */
        fun readSignatureFromFile(path: String): Pair<BigInteger, BigInteger>? {
            try {
                val lines = File(path).readLines().map { it.toBigInteger() }
                return lines[0] to lines[1]
            } catch (e: Exception) {
                println("Ошибка чтения сигнатуры из файла, сигнатура будет сгенерирована заново")
                return null
            }
        }

        /**
         * Хеш функция для сообщения
         */
        fun hash(x: ByteArray): ByteArray {
            val md = MessageDigest.getInstance("SHA1")
            return md.digest(x)
        }
    }

    /**
     * Создает ключевую пару открытый ключ и закрытый ключ
     *
     * @return Pair(secretKey, publicKey)
     */
    fun createKeyPair(): Pair<BigInteger, Point> {
        var l: BigInteger
        var p: Point
        do {
            l = Random.nextBigInteger(curve.r - BigInteger.ONE) + BigInteger.ONE
            p = curve.q * l
        } while (p.coord == null)
        return l to p
    }

    /**
     * Чтение секретного ключа из файла
     * @param path
     */
    fun readSecretKeyFromFile(path: String): BigInteger? {
        try {
            val fileLines = File(path).readLines().map { it.toBigInteger() }
            return fileLines.first()
        } catch (e: Exception) {
            println("Ошибка чтения закрытого ключа из файла, ключи будут сгенерированы заново")
            return null
        }
    }

    /**
     * Чтение открытого ключа из файла
     * @param path
     */
    fun readPublicKeyFromFile(path: String): Point? {
        try {
            val fileLines = File(path).readLines().map { it.toBigInteger() }
            return Point(fileLines[0], fileLines[1], curve.p)
        } catch (e: Exception) {
            println("Ошибка чтения открытого ключа из файла, ключи будут сгенерированы заново")
            return null
        }
    }

    /**
     * Генерация случайного k для подписи
     */
    fun getSignatureK(m: ByteArray, l: BigInteger): BigInteger {
        var e = hash(m).toBigInteger() % curve.r
        if (e == BigInteger.ZERO) e = BigInteger.ONE
        var k: BigInteger
        var r: Point
        var s: BigInteger
        do {
            k = Random.nextBigInteger(curve.r - BigInteger.ONE) + BigInteger.ONE
            r = curve.q * k
            s = (l * r.x + k * e) % curve.r
        } while (r.x % curve.r == BigInteger.ZERO || s == BigInteger.ZERO)
        return k
    }

    /**
     * Хеш сообщения для подписи
     */
    fun getMessageHash(m: ByteArray): BigInteger {
        var e = hash(m).toBigInteger() % curve.r
        if (e == BigInteger.ZERO) e = BigInteger.ONE
        return e
    }

    /**
     * Получение точки R для подписи
     */
    fun getSignatureR(k: BigInteger): Point {
        return curve.q * k
    }

    /**
     * Получение части подписи s
     */
    fun getSignatureS(e: BigInteger, r: Point, l: BigInteger, k: BigInteger): BigInteger {
        return (l * r.x + k * e) % curve.r
    }

    /**
     * Подпись сообщения (Протокол 15.6.1 из Ростовцева)
     * @param m
     * с помощью закрытого ключа
     * @param l
     * и случайного значения
     * @param k
     *
     * @return Pair(R.x, s)
     */
    fun getSignature(m: ByteArray, l: BigInteger, k: BigInteger): Pair<BigInteger, BigInteger> {
        var e = hash(m).toBigInteger() % curve.r
        if (e == BigInteger.ZERO) e = BigInteger.ONE
        val r = curve.q * k
        val s = (l * r.x + k * e) % curve.r
        return r.x to s
    }

    /**
     * Проверка подписи
     * @param signature
     * сообщения
     * @param m
     * с открытым ключом
     * @param p
     *
     * @return Верна ли подпись
     */
    fun isValid(m: ByteArray, p: Point, signature: Pair<BigInteger, BigInteger>): Boolean {
        val xr = signature.first
        val s = signature.second
        if (!(BigInteger.ZERO < xr % curve.r && xr % curve.r < curve.r)) {
            return false
        }
        if (!(BigInteger.ZERO < s && s < curve.r)) {
            return false
        }
        var e = hash(m).toBigInteger() % curve.r
        if (e == BigInteger.ZERO) e = BigInteger.ONE

        val r1 = curve.q * ((s * e.modInverse(curve.r)) % curve.r) -
                p * ((xr * e.modInverse(curve.r)) % curve.r)
        return r1.x == xr
    }

}