package curve

import java.math.BigInteger

/**
 * Точка эллиптической кривой
 * @param coord - координаты точки на плоскости, если null - точка на бесконечности
 * @param mod - в каком кольце мы действуем
 */
data class Point(
    val coord: Coord?,
    val mod: BigInteger
) {

    constructor(x: BigInteger, y: BigInteger, mod: BigInteger) : this(Coord(x, y), mod)

    val x get() = coord?.x ?: throw Exception("error get coordinate of inf point")
    val y get() = coord?.y ?: throw Exception("error get coordinate of inf point")

    override fun toString(): String {
        return if (coord == null) "P(inf)" else "($x, $y)"
    }

    /**
     * Переопределение оператора сложения (+) для класса curve.Point согласно лекции
     */
    operator fun plus(p: Point): Point {
        if (p.mod != this.mod) throw Exception("Sum point in different field")
        if (this.coord == null) {
            return p
        }
        if ((this == p && p.y == BigInteger.ZERO) || (this != p && this.x == p.x)) {
            return Point(null, mod)
        }
        val a = if (this == p) {
            ((3.toBigInteger() * this.x * this.x) * (2.toBigInteger() * this.y).modInverse(mod)).module()
        } else {
            ((p.y - this.y).module() * (p.x - this.x).modInverse(mod)).module()
        }

        val x = (a * a - p.x - this.x).module()
        val y = (a * (this.x - x) - this.y).module()
        return Point(x, y, mod)
    }

    /**
     * Переопределение оператора вычитания (-) для класса curve.Point согласно лекции
     */
    operator fun minus(p: Point): Point {
        return this + (-p)
    }

    /**
     * Переопределение оператора унарного минуса (-P) для класса curve.Point согласно лекции
     */
    operator fun unaryMinus(): Point {
        return if (this.coord == null) this else Point(x, (-y).module(), mod)
    }

    /**
     * Умный модуль для отрицательных чисел
     */
    private fun BigInteger.module(): BigInteger {
        return (this % mod + mod) % mod
    }

    /**
     * Бинарный алгоритм умножения точки на число (переопределен оператор *)
     */
    operator fun times(a: BigInteger): Point {
        val binary = a.toString(2).reversed()
        var result = Point(null, mod)
        var addEnd = this
        binary.forEach {
            if (it == '1') {
                result += addEnd
            }
            addEnd += addEnd
        }
        return result
    }
}