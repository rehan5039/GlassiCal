package com.rubex.glassical

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.math.BigDecimal
import java.text.DecimalFormat

data class CalculatorState(
    val displayExpression: TextFieldValue = TextFieldValue(""),
    val liveResult: String = ""
)

sealed class CalcAction {
    data class Number(val number: Int) : CalcAction()
    data object DoubleZero : CalcAction()
    data object Clear : CalcAction()
    data object Delete : CalcAction()
    data object Decimal : CalcAction()
    data object Calculate : CalcAction()
    data object Percent : CalcAction()
    data class Operation(val operation: String) : CalcAction()
    data class UpdateExpression(val newValue: TextFieldValue) : CalcAction()
}

class CalculatorViewModel : ViewModel() {
    private val _state = MutableStateFlow(CalculatorState())
    val state: StateFlow<CalculatorState> = _state.asStateFlow()

    private var isResultState = false
    private val evaluator = Evaluator()
    private val expressionHelper = Expression()

    fun onAction(action: CalcAction) {
        when (action) {
            is CalcAction.Number -> insertString(action.number.toString())
            is CalcAction.DoubleZero -> insertString("00")
            is CalcAction.Decimal -> insertDecimal()
            is CalcAction.Clear -> clear()
            is CalcAction.Delete -> delete()
            is CalcAction.Operation -> insertOperation(action.operation)
            is CalcAction.Calculate -> calculateFinal()
            is CalcAction.Percent -> insertPercent()
            is CalcAction.UpdateExpression -> {
                _state.update { it.copy(displayExpression = action.newValue) }
            }
        }
    }

    private fun insertString(str: String) {
        if (isResultState) {
            clear()
            isResultState = false
        }
        
        val currentTF = _state.value.displayExpression
        val currentText = currentTF.text
        val cursorStart = currentTF.selection.start
        val cursorEnd = currentTF.selection.end
        
        val newText = StringBuilder(currentText).replace(cursorStart, cursorEnd, str).toString()
        val newCursor = cursorStart + str.length
        
        _state.update { 
            it.copy(
                displayExpression = TextFieldValue(newText, TextRange(newCursor))
            )
        }
        updateState()
    }

    private fun insertDecimal() {
        if (isResultState) {
            clear()
            insertString("0.")
            isResultState = false
            return
        }
        
        // Basic check for decimal validity in current number block
        val currentTF = _state.value.displayExpression
        val cursor = currentTF.selection.start
        val text = currentTF.text
        
        var i = cursor - 1
        var hasDot = false
        while(i >= 0) {
            val c = text[i]
            if (isOperator(c)) break
            if (c == '.') { hasDot = true; break }
            i--
        }
        
        if (!hasDot) insertString(".")
    }

    private fun insertOperation(op: String) {
         if (isResultState) {
            val res = state.value.displayExpression.text
            isResultState = false
             _state.update { 
                it.copy(displayExpression = TextFieldValue(res, TextRange(res.length))) 
             }
        }
        
        val currentTF = _state.value.displayExpression
        val text = currentTF.text
        val cursor = currentTF.selection.start
        
        if (cursor > 0 && isOperator(text[cursor-1])) {
             val newText = StringBuilder(text).replace(cursor-1, cursor, op).toString()
             _state.update { it.copy(displayExpression = TextFieldValue(newText, TextRange(cursor))) }
        } else {
             insertString(op)
        }
        updateState()
    }
    
    // CHANGED: Simply insert % symbol now. Context handled by Expression/Evaluator.
    private fun insertPercent() {
        insertString("%")
    }

    private fun delete() {
        if (isResultState) {
            clear()
            return
        }
        
        val currentTF = _state.value.displayExpression
        val text = currentTF.text
        val start = currentTF.selection.start
        val end = currentTF.selection.end
        
        if (start != end) {
            val newText = StringBuilder(text).delete(start, end).toString()
             _state.update { it.copy(displayExpression = TextFieldValue(newText, TextRange(start))) }
        } else if (start > 0) {
            val newText = StringBuilder(text).deleteCharAt(start - 1).toString()
            _state.update { it.copy(displayExpression = TextFieldValue(newText, TextRange(start - 1))) }
        }
        updateState()
    }

    private fun clear() {
        _state.update { CalculatorState() }
        isResultState = false
    }

    private fun calculateFinal() {
        val expr = _state.value.displayExpression.text
        if (expr.isNotEmpty()) {
            val result = performEvaluation(expr)
            // If empty (error condition usually), don't update main display unless we want to show Error
            if (result.isNotEmpty() && result != "Error") {
                 _state.update { 
                    it.copy(
                        displayExpression = TextFieldValue(result, TextRange(result.length)), 
                        liveResult = "" 
                    ) 
                }
                isResultState = true
            } else if (result == "Error") {
                 // Optional: Show error in display
                 // _state.update { it.copy(liveResult = "Error") }
            }
        }
    }

    private fun updateState() {
        val expStr = _state.value.displayExpression.text
        if (expStr.isEmpty()) {
            _state.update { it.copy(liveResult = "") }
            return
        }
        
        // Don't show live result if it's identical to input
        val result = performEvaluation(expStr)
        val finalLive = if (result == expStr || result == "Error") "" else result
        
        _state.update {
            it.copy(liveResult = finalLive)
        }
    }
    
    private fun performEvaluation(rawExpr: String): String {
        // Strip trailing operator for live preview/soft evaluation
        var exprToEval = rawExpr
        if (exprToEval.isNotEmpty() && isOperator(exprToEval.last())) {
            exprToEval = exprToEval.dropLast(1)
        }
        if (exprToEval.isEmpty()) return ""

        return try {
            // 1. Clean expression (handle percentages, replace symbols)
            val cleanExpr = expressionHelper.getCleanExpression(exprToEval, ".", ",")
            
            if (expressionHelper.isSyntaxError) return "Error"
            
            // 2. Evaluate
            val resultBD = evaluator.evaluate(cleanExpr)
            
            if (evaluator.syntaxError || evaluator.divisionByZero || evaluator.domainError) {
                return "Error"
            }
            
            // 3. Format
            val plainStr = resultBD.stripTrailingZeros().toPlainString()
            formatDisplay(plainStr)
            
        } catch (e: Exception) {
            "Error"
        }
    }

    // --- Helper Logic ---

    private fun isOperator(c: Char): Boolean {
        // Includes GlassiCal generic operator chars
        return c == '+' || c == '−' || c == '×' || c == '÷' || c == '-' || c == '*' || c == '/'
    }

    private fun formatDisplay(str: String): String {
        return try {
            if (str == "Error") return "Error"
            if (str.isEmpty()) return ""
            if (str.contains("E")) return str // scientific notation handling can be improved later
            val bd = BigDecimal(str)
            val df = DecimalFormat("#,###.#####")
            df.format(bd)
        } catch (e: Exception) {
            str
        }
    }
}
