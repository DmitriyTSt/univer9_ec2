import curve.EllipticCurve
import elgamal.ElGamal
import elgamal.MessageHelper
import java.io.File

/**
 * Протокол цифровой подписи Эль-Гамаля на основе эллиптических кривых
 */
private const val STEP_COUNT = 6

private const val CURVE_PATH = "curve.txt"
private const val SECRET_PATH = "secret.txt"
private const val PUBLIC_PATH = "public.txt"
private const val MESSAGE_PATH = "message.txt"
private const val SIGN_K_PATH = "signK.txt"
private const val SIGNATURE_PATH = "signature.txt"

fun main() {
    println("Доступные шаги")
    println("0. Создать эллиптическую кривую (в файл $CURVE_PATH)")
    println("1. Создать секретный ключ отправителя (в файл $SECRET_PATH) и открытый ключ отправителя (в файл $PUBLIC_PATH)")
    println("2. Создать сообщение (в файл $MESSAGE_PATH)")
    println("3. Выбрать случайное k для подписи сообщения из файла $MESSAGE_PATH")
    println("4. Подписать сообщение из файла $MESSAGE_PATH в файл $SIGNATURE_PATH\n(если каких то файлов нет, будут выполнены предыдущие шаги)")
    println("5. Проверить подпись из файла $SIGNATURE_PATH сообщения $MESSAGE_PATH\n(если каких то файлов нет, будут выполнены предыдущие шаги)")
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
        3 -> getKForSign()
        4 -> sign()
        5 -> check()
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
fun getKForSign() {
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

/**
 * Подпись сообщения
 */
fun sign() {
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
    // чтение выбранного k из файла
    var k = try {
        File(SIGN_K_PATH).readLines().first().toBigIntegerOrNull()
    } catch (e: Exception) {
        println("Ошибка чтения параметра подписи k из файла, параметр будет сгенерирован заново")
        null
    }
    while (k == null) {
        getKForSign()
        k = try {
            File(SIGN_K_PATH).readLines().first().toBigIntegerOrNull()
        } catch (e: Exception) {
            println("Ошибка чтения параметра подписи k из файла, параметр будет сгенерирован заново")
            null
        }
    }
    // вычисление подписи
    val signature = elGamal.getSignature(message.toByteArray(Charsets.UTF_8), secretKey, k)
    ElGamal.saveSignatureToFile(SIGNATURE_PATH, signature)
    println("Подпись успешно добавлена в файл $SIGNATURE_PATH")
}

/**
 * Проверка сообщения
 */
fun check() {
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
        sign()
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
