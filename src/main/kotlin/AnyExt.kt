import curve.Utils
import java.math.BigInteger
import kotlin.random.Random

fun logd(string: String) {
    if (Utils.DEBUG) {
        println(string)
    }
}

fun Random.Default.nextBigInteger(p: BigInteger): BigInteger {
    return BigInteger(p.bitLength(), java.util.Random()) % p
}

fun ByteArray.toBigInteger(): BigInteger {
    return BigInteger(this)
}