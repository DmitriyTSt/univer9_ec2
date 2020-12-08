import curve.EllipticCurve
import curve.Point
import elgamal.ElGamal
import elgamal.MessageHelper
import java.io.File
import kotlin.Exception

/**
 * Протокол цифровой подписи Эль-Гамаля на основе эллиптических кривых
 */
private const val STEP_COUNT = 12

private const val CURVE_PATH = "curve.txt"
private const val SECRET_PATH = "secret.txt"
private const val PUBLIC_PATH = "public.txt"
private const val MESSAGE_PATH = "message.txt"
private const val SIGN_K_PATH = "signK.txt"
private const val HASH_SEND_PATH = "hash_send.txt"

private const val SIGN_R_PATH = "sign_r.txt"
private const val SIGN_S_PATH = "sign_s.txt"

private const val SIGNATURE_PATH = "signature.txt"

private const val IS_VALID_SIGN_PATH = "is_valid_sign.txt"
private const val HASH_RECEIVE_PATH = "hash_receive.txt"
private const val CHECK_R1_PATH = "check_r1.txt"

fun main() {
    println("Доступные шаги")
    println("0. Создать эллиптическую кривую (в файл $CURVE_PATH)")
    println("1. Создать секретный ключ отправителя (в файл $SECRET_PATH) и открытый ключ отправителя (в файл $PUBLIC_PATH)")
    println("2. Создать сообщение (в файл $MESSAGE_PATH)")
    println("3. Выбрать случайное k для подписи сообщения из файла $MESSAGE_PATH")
    println("4. Посчитать хеш сообщения и записать в файл $HASH_SEND_PATH")
    println("5. Посчитать R и записать в файл $SIGN_R_PATH")
    println("6. Посчитать s и записать в файл $SIGN_S_PATH")
    println("7. Сформировать подпись из R и s в $SIGNATURE_PATH")
    println("8. Проверить корректность подписи из $SIGNATURE_PATH для текущей кривой и записать проверку в файл $IS_VALID_SIGN_PATH")
    println("9. Посчитать хеш присланного сообщения и записать в файл $HASH_RECEIVE_PATH")
    println("10. Посчитать R1 и записать в файл $CHECK_R1_PATH")
    println("11. Проверить подпись (из R1 и $SIGNATURE_PATH)")
    println()
    println("Введите номер шага")
    var step = readInt()
    while (step >= STEP_COUNT) {
        println("Введите корректный номер шага")
        step = readInt()
    }
    when (step) {
        0 -> createCurve()
        1 -> createKeys()
        2 -> createMessage()
        3 -> calcSignK()
        4 -> createMessageSendHash()
        5 -> calcSignR()
        6 -> calcSignS()
        7 -> makeSign()
        8 -> checkSignature()
//        8 -> checkSignValid()
//        9 -> createMessageReceiveHash()
//        10 -> calcVerifyR1()
//        11 -> checkSign()
    }
}

/**
 * Генерация кривой
 */
fun createCurve() {
    val curve = EllipticCurve.create(256)
    curve.saveToFile(CURVE_PATH)
    println("Кривая успешно создана и записана в файл $CURVE_PATH")
}

/**
 * Генерация ключей
 */
fun createKeys() {
    var curve = EllipticCurve.fromFile(CURVE_PATH)
    while (curve == null) {
        createCurve()
        curve = EllipticCurve.fromFile(CURVE_PATH)
    }
    val elGamal = ElGamal(curve)
    val (secretKey, publicKey) = elGamal.createKeyPair()
    ElGamal.savePublicKeyToFile(PUBLIC_PATH, publicKey)
    ElGamal.saveSecretKeyToFile(SECRET_PATH, secretKey)
    println("Ключи успешно сгенерированы в файлы $PUBLIC_PATH и $SECRET_PATH")
}

/**
 * Создание файла с сообщением
 */
fun createMessage() {
    MessageHelper.saveMessageToPath(MESSAGE_PATH)
    println("Сообщение успешно создано в файл $MESSAGE_PATH")
}

/**
 * Получение случайного k для подписи
 */
fun calcSignK() {
    // чтение кривой
    var curve = EllipticCurve.fromFile(CURVE_PATH)
    while (curve == null) {
        createCurve()
        curve = EllipticCurve.fromFile(CURVE_PATH)
    }
    val elGamal = ElGamal(curve)
    // чтение закрытого ключа
    var secretKey = elGamal.readSecretKeyFromFile(SECRET_PATH)
    while (secretKey == null) {
        createKeys()
        secretKey = elGamal.readSecretKeyFromFile(SECRET_PATH)
    }
    // чтение сообщения
    var message = MessageHelper.readMessageFromFile(MESSAGE_PATH)
    while (message == null) {
        createMessage()
        message = MessageHelper.readMessageFromFile(MESSAGE_PATH)
    }
    val k = elGamal.getSignatureK(message.toByteArray(Charsets.UTF_8), secretKey)
    File(SIGN_K_PATH).writeText(k.toString())
    println("Выбрано случайное k для подписи сообщения и добавлено в файл $SIGN_K_PATH")
}

fun createMessageSendHash() {
    // чтение кривой
    var curve = EllipticCurve.fromFile(CURVE_PATH)
    while (curve == null) {
        createCurve()
        curve = EllipticCurve.fromFile(CURVE_PATH)
    }
    val elGamal = ElGamal(curve)
    // чтение сообщения
    var message = MessageHelper.readMessageFromFile(MESSAGE_PATH)
    while (message == null) {
        createMessage()
        message = MessageHelper.readMessageFromFile(MESSAGE_PATH)
    }
    val hash = elGamal.getMessageHash(message.toByteArray(Charsets.UTF_8))
    File(HASH_SEND_PATH).writeText(hash.toString())
    println("Хеш отправляемого сообщения добавлен в файл $HASH_SEND_PATH")
}

fun calcSignR() {
    // чтение кривой
    var curve = EllipticCurve.fromFile(CURVE_PATH)
    while (curve == null) {
        createCurve()
        curve = EllipticCurve.fromFile(CURVE_PATH)
    }
    val elGamal = ElGamal(curve)
    // чтение k для подписи
    var k = try {
        File(SIGN_K_PATH).readLines().firstOrNull()?.toBigIntegerOrNull()!!
    } catch (e: Exception) {
        null
    }
    while (k == null) {
        println("k == null, k будет сгенерировано заново")
        calcSignK()
        k = try {
            File(SIGN_K_PATH).readLines().firstOrNull()?.toBigIntegerOrNull()!!
        } catch (e: Exception) {
            null
        }
    }
    val r = elGamal.getSignatureR(k)
    File(SIGN_R_PATH).writeText("${r.x}\n${r.y}")
    println("Точка R успешно записана в файл $SIGN_R_PATH")
}

fun calcSignS() {
    // чтение кривой
    var curve = EllipticCurve.fromFile(CURVE_PATH)
    while (curve == null) {
        createCurve()
        curve = EllipticCurve.fromFile(CURVE_PATH)
    }
    val elGamal = ElGamal(curve)
    // чтение k для подписи
    var k = try {
        File(SIGN_K_PATH).readLines().firstOrNull()?.toBigIntegerOrNull()!!
    } catch (e: Exception) {
        null
    }
    while (k == null) {
        println("k == null, k будет сгенерировано заново")
        calcSignK()
        k = try {
            File(SIGN_K_PATH).readLines().firstOrNull()?.toBigIntegerOrNull()!!
        } catch (e: Exception) {
            null
        }
    }
    // чтение хеша
    var hash = try {
        File(HASH_SEND_PATH).readLines().firstOrNull()?.toBigIntegerOrNull()!!
    } catch (e: Exception) {
        null
    }
    while (hash == null) {
        println("hash == null, hash будет сгенерировано заново")
        createMessageSendHash()
        hash = try {
            File(HASH_SEND_PATH).readLines().firstOrNull()?.toBigIntegerOrNull()!!
        } catch (e: Exception) {
            null
        }
    }
    // чтение закрытого ключа
    var secretKey = elGamal.readSecretKeyFromFile(SECRET_PATH)
    while (secretKey == null) {
        createKeys()
        secretKey = elGamal.readSecretKeyFromFile(SECRET_PATH)
    }
    // чтение точки R
    var point = try {
        File(SIGN_R_PATH).readLines().let { Point(it[0].toBigInteger(), it[1].toBigInteger(), curve.p) }
    } catch (e: Exception) {
        null
    }
    while (point == null) {
        println("R = null, R будет сгенерировано заново")
        calcSignR()
        point = try {
            File(SIGN_R_PATH).readLines().let { Point(it[0].toBigInteger(), it[1].toBigInteger(), curve.p) }
        } catch (e: Exception) {
            null
        }
    }
    // вычисление s
    val s = elGamal.getSignatureS(hash, point, secretKey, k)
    File(SIGN_S_PATH).writeText(s.toString())
    println("s успешно записано в файл $SIGN_S_PATH")
}

fun makeSign() {
    // чтение кривой
    var curve = EllipticCurve.fromFile(CURVE_PATH)
    while (curve == null) {
        createCurve()
        curve = EllipticCurve.fromFile(CURVE_PATH)
    }
    // чтение точки R
    var point = try {
        File(SIGN_R_PATH).readLines().let { Point(it[0].toBigInteger(), it[1].toBigInteger(), curve.p) }
    } catch (e: Exception) {
        null
    }
    while (point == null) {
        println("R = null, R будет сгенерировано заново")
        calcSignR()
        point = try {
            File(SIGN_R_PATH).readLines().let { Point(it[0].toBigInteger(), it[1].toBigInteger(), curve.p) }
        } catch (e: Exception) {
            null
        }
    }
    // чтение s
    var s = try {
        File(SIGN_S_PATH).readLines().firstOrNull()?.toBigIntegerOrNull()!!
    } catch (e: Exception) {
        null
    }
    while (s == null) {
        println("s == null, s будет сгенерировано заново")
        calcSignS()
        s = try {
            File(SIGN_S_PATH).readLines().firstOrNull()?.toBigIntegerOrNull()!!
        } catch (e: Exception) {
            null
        }
    }
    // запись сигнатуры
    ElGamal.saveSignatureToFile(SIGNATURE_PATH, point.x to s)
    println("Полная сигнатура сохранена в файл $SIGNATURE_PATH")
}

fun checkSignature() {
    // чтение кривой
    var curve = EllipticCurve.fromFile(CURVE_PATH)
    while (curve == null) {
        createCurve()
        curve = EllipticCurve.fromFile(CURVE_PATH)
    }
    val elGamal = ElGamal(curve)
    // чтение открытого ключа
    var publicKey = elGamal.readPublicKeyFromFile(PUBLIC_PATH)
    while (publicKey == null) {
        createKeys()
        publicKey = elGamal.readPublicKeyFromFile(PUBLIC_PATH)
    }
    // чтение сообщения
    var message = MessageHelper.readMessageFromFile(MESSAGE_PATH)
    while (message == null) {
        createMessage()
        message = MessageHelper.readMessageFromFile(MESSAGE_PATH)
    }
    // чтение подписи
    var signature = ElGamal.readSignatureFromFile(SIGNATURE_PATH)
    while (signature == null) {
        makeSign()
        signature = ElGamal.readSignatureFromFile(SIGNATURE_PATH)
    }
    // проверка подписи
    val isValid = elGamal.isValid(message.toByteArray(Charsets.UTF_8), publicKey, signature)
    println("Верна ли подпись: $isValid")
}

/**
 * Умное чтение числа из консоли
 */
private fun readInt(): Int {
    var int = readLine()?.toIntOrNull()
    while (int == null) {
        println("Введите число")
        int = readLine()?.toIntOrNull()
    }
    return int
}
