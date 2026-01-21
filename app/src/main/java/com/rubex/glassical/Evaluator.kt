package com.rubex.glassical

import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode
import kotlin.math.PI
import kotlin.math.E

class Evaluator(private val precision: Int = 10) {

    var divisionByZero = false
    var domainError = false
    var syntaxError = false
    
    fun evaluate(expression: String): BigDecimal {
        divisionByZero = false
        domainError = false
        syntaxError = false
        
        // Remove spaces just in case
        val equation = expression.replace(" ", "")
        
        return object : Any() {
            var pos = -1
            var ch = 0

            fun nextChar() {
                ch = if (++pos < equation.length) equation[pos].code else -1
            }

            fun eat(charToEat: Int): Boolean {
                while (ch == ' '.code) nextChar()
                if (ch == charToEat) {
                    nextChar()
                    return true
                }
                return false
            }

            fun parse(): BigDecimal {
                nextChar()
                val x = parseExpression()
                if (pos < equation.length) {
                    // unexpected char
                    syntaxError = true
                }
                return x
            }

            // Expression: Term + Term ...
            fun parseExpression(): BigDecimal {
                var x = parseTerm()
                while (true) {
                    if (eat('+'.code)) x = x.add(parseTerm())
                    else if (eat('-'.code)) x = x.subtract(parseTerm())
                    else return x
                }
            }

            // Term: Factor * Factor ...
            fun parseTerm(): BigDecimal {
                var x = parseFactor()
                while (true) {
                    if (eat('*'.code)) x = x.multiply(parseFactor())
                    else if (eat('/'.code)) {
                        val denominator = parseFactor()
                        if (denominator.compareTo(BigDecimal.ZERO) == 0) {
                            divisionByZero = true
                            return BigDecimal.ZERO
                        }
                        // Use limited precision for division to avoid non-terminating decimal expansion
                        x = try {
                           x.divide(denominator) 
                        } catch (e: ArithmeticException) {
                           x.divide(denominator, precision + 5, RoundingMode.HALF_UP)
                        }
                    } else if (eat('^'.code)) {
                        // Simple power implementation if needed, though mostly handled strictly in factors usually
                        // OpenCalc handles power in parseFactor usually or via separate function
                        // We'll treat reasonable power usage: x = x.pow(parseFactor().toInt()) ... 
                        // But standard simple calc might not even have ^ button.
                        // For completeness:
                         val exponent = parseFactor()
                         try {
                             x = BigDecimal(Math.pow(x.toDouble(), exponent.toDouble()))
                         } catch(e: Exception) {
                             domainError = true
                         }
                    }
                    else return x
                }
            }

            // Factor: number, (expression), -factor
            fun parseFactor(): BigDecimal {
                if (eat('+'.code)) return parseFactor()
                if (eat('-'.code)) return parseFactor().negate()

                var x: BigDecimal = BigDecimal.ZERO
                val startPos = pos
                
                if (eat('('.code)) {
                    x = parseExpression()
                    if (!eat(')'.code)) syntaxError = true
                } else if ((ch >= '0'.code && ch <= '9'.code) || ch == '.'.code) {
                    // Parse number
                    while ((ch >= '0'.code && ch <= '9'.code) || ch == '.'.code) nextChar()
                    val str = equation.substring(startPos, pos)
                    // Check multiple dots
                    if (str.count { it == '.' } > 1) {
                        syntaxError = true
                    } else if (str == ".") {
                        x = BigDecimal.ZERO
                    } else {
                        try {
                            x = BigDecimal(str)
                        } catch (e: Exception) {
                            syntaxError = true
                        }
                    }
                } else {
                    // Unexpected
                    // Could be function names if we supported them
                    syntaxError = true
                }
                return x
            }
        }.parse()
    }
}
