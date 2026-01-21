package com.rubex.glassical

class Expression {

    // Simple error flagging mechanism for this class
    var isSyntaxError = false

    fun getCleanExpression(calculation: String, decimalSeparatorSymbol: String, groupingSeparatorSymbol: String): String {
        isSyntaxError = false
        var cleanCalculation = replaceSymbolsFromCalculation(calculation, decimalSeparatorSymbol, groupingSeparatorSymbol)
        cleanCalculation = addMultiply(cleanCalculation)
        // GlassiCal doesn't currently support square root or factorial in the UI, but keeping logic for robustness
        if (cleanCalculation.contains('√')) {
            cleanCalculation = formatSquare(cleanCalculation)
        }
        if (cleanCalculation.contains('%')) {
            cleanCalculation = getPercentString(cleanCalculation)
            cleanCalculation = cleanCalculation.replace("%", "/100")
        }
        // Factorial not yet implemented in GlassiCal UI symbols
        if (cleanCalculation.contains('!')) {
             cleanCalculation = formatFactorial(cleanCalculation)
        }
        cleanCalculation = addParenthesis(cleanCalculation)
        return cleanCalculation
    }

    private fun replaceSymbolsFromCalculation(calculation: String, decimalSeparatorSymbol: String, groupingSeparatorSymbol: String): String {
        var calculation2 = calculation.replace('×', '*')
        calculation2 = calculation2.replace('÷', '/')
        calculation2 = calculation2.replace("−", "-") // Ensure minus is standard hyphen
        // calculation2 = calculation2.replace("log₂(", "logtwo(") // Not implementing logs yet
        // calculation2 = calculation2.replace("log(", "logten(")
        calculation2 = calculation2.replace("E", "*10^")
        
        calculation2 = calculation2.replace(groupingSeparatorSymbol, "")
        // Handle decimal separator - ensure it becomes a dot for Double/BigDecimal parsing
        calculation2 = calculation2.replace(decimalSeparatorSymbol, ".")
        return calculation2
    }

    private fun getPercentString(calculation: String): String {
        var result = calculation
        var i = 0
        var parenthesisLevel = 0
        var subexpressionStart = -1
        val processedIndices = mutableSetOf<Int>()

        loop@ while (i < result.length) {
            when (result[i]) {
                '(' -> {
                    if (parenthesisLevel == 0) subexpressionStart = i
                    parenthesisLevel += 1
                }
                ')' -> {
                    parenthesisLevel -= 1
                    if (parenthesisLevel == 0 && subexpressionStart >= 0) {
                        val subexpression = result.substring(subexpressionStart + 1, i)
                        if (subexpression.contains('%')) {
                            val processedSubexpression = getPercentString(subexpression)
                            result = result.substring(0, subexpressionStart + 1) + processedSubexpression + result.substring(i)
                            i = 0 
                            processedIndices.clear() 
                            continue@loop
                        }
                        subexpressionStart = -1
                    } else if (parenthesisLevel < 0) {
                        isSyntaxError = true
                        return result
                    }
                }
                '%' -> {
                    if (parenthesisLevel == 0 && !processedIndices.contains(i)) {
                        processedIndices.add(i)
                        
                        // Check previous char context
                        if (i > 0 && result[i - 1] == ')') {
                            // (...)% case
                            var j = i - 2
                            var level = 1
                            while (j >= 0) {
                                if (result[j] == ')') level += 1
                                else if (result[j] == '(') level -= 1
                                if (level == 0) break
                                j -= 1
                            }
                            if (level != 0 || j < 0) { // Safety check j < 0
                                isSyntaxError = true
                                return result
                            }
                            val subexpression = result.substring(j + 1, i - 1)
                            val operatorPos = findLastOperator(result.substring(0, j))
                            
                            if (operatorPos < 0) {
                                result = result.substring(0, j) + "(($subexpression)/100)" + result.substring(i + 1)
                            } else if (result[operatorPos] == '*' || result[operatorPos] == '/') {
                                result = result.substring(0, operatorPos + 1) + "(($subexpression)/100)" + result.substring(i + 1)
                            } else {
                                val base = result.substring(0, operatorPos).trim()
                                if (base.isEmpty()) {
                                    isSyntaxError = true
                                    return result
                                }
                                result = "$base${result[operatorPos]}$base*(($subexpression)/100)" + result.substring(i + 1)
                            }
                        } else {
                            // Number% case
                            var start = i - 1
                            while (start >= 0 && (result[start].isDigit() || result[start] == '.')) {
                                start -= 1
                            }
                            // start is now at the operator or -1
                            val operatorPos = findLastOperator(result.substring(0, start + 1))
                            
                            // Check if start+1 to i is valid number
                            val number = result.substring(start + 1, i)
                            if (number.isEmpty()) {
                                isSyntaxError = true; return result
                            }

                            if (operatorPos < 0) {
                                // e.g. "50%" -> "(50/100)"
                                result = result.substring(0, start + 1) + "($number/100)" + result.substring(i + 1)
                            } else if (result[operatorPos] == '*' || result[operatorPos] == '/') {
                                // e.g. "2*50%" -> "2*(50/100)"
                                result = result.substring(0, operatorPos + 1) + "($number/100)" + result.substring(i + 1)
                            } else {
                                // e.g. "100+10%" -> "100+100*(10/100)"
                                val base = result.substring(0, operatorPos).trim()
                                // Base needs to be careful if it wraps complex things? 
                                // OpenCalc assumes base is everything before operator
                                if (base.isEmpty()) {
                                    isSyntaxError = true
                                    return result
                                }
                                result = "$base${result[operatorPos]}$base*($number/100)" + result.substring(i + 1)
                            }
                        }
                        i = 0
                        continue@loop
                    }
                }
            }
            i += 1
        }
        if (parenthesisLevel != 0) isSyntaxError = true
        return result
    }

    private fun findLastOperator(str: String): Int {
         return str.lastIndexOfAny(charArrayOf('+', '-', '*', '/'))
    }

    private fun addParenthesis(calculation: String): String {
        var cleanCalculation = calculation
        var openParentheses = 0
        var closeParentheses = 0

        for (i in calculation.indices) {
            if (calculation[i] == '(') openParentheses += 1
            if (calculation[i] == ')') closeParentheses += 1
        }
        if (closeParentheses < openParentheses) {
            repeat(openParentheses - closeParentheses) {
                cleanCalculation += ')'
            }
        }
        if (closeParentheses > openParentheses) isSyntaxError = true
        return cleanCalculation
    }

    private fun addMultiply(calculation: String): String {
        var cleanCalculation = calculation
        var i = 0
        while (i < cleanCalculation.length) {
            // (...) (...) case
            if (cleanCalculation[i] == '(') {
                if (i != 0 && (cleanCalculation[i-1] in ".0123456789)")) {
                     cleanCalculation = cleanCalculation.addCharAtIndex('*', i)
                     // String length increased, but we want to process the '(' at new position next?
                     // OpenCalc increments length but loop uses live length.
                     // The char at 'i' is now '*', the char at 'i+1' is '('.
                     // Next loop i+1 check will see '('.
                     i++ // skip the added *
                }
            } else if (cleanCalculation[i] == ')') {
                if (i + 1 < cleanCalculation.length && cleanCalculation[i + 1] in "0123456789(.") {
                    cleanCalculation = cleanCalculation.addCharAtIndex('*', i + 1)
                    // We don't advance i here because we modified AFTER current char
                }
            } else if (cleanCalculation[i] == '%') {
                 if (i + 1 < cleanCalculation.length && (cleanCalculation[i + 1] in "0123456789(")) {
                    cleanCalculation = cleanCalculation.addCharAtIndex('*', i + 1)
                 }
            }
            i++
        }
        return cleanCalculation
    }

    private fun formatSquare(calculation: String): String {
         var cleanCalculation = calculation.replace("√", "sqrt")
         return cleanCalculation
    }
    
    private fun formatFactorial(calculation: String): String {
        // Not implementing full factorial parsing logic for now unless requested
        return calculation
    }

    private fun String.addCharAtIndex(char: Char, index: Int) =
        StringBuilder(this).apply { insert(index, char) }.toString()
}
