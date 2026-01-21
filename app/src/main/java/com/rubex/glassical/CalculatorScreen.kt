package com.rubex.glassical

import androidx.compose.foundation.background
import android.content.Context
import android.util.AttributeSet

import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.text.TextRange
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalTextInputService
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun CalculatorScreen(
    viewModel: CalculatorViewModel = viewModel()
) {
    val state by viewModel.state.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(bottom = 8.dp), // Minimal additional padding, Grid has 16dp
        verticalArrangement = Arrangement.Bottom
    ) {
        
        // Display Area
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(bottom = 24.dp),
            contentAlignment = Alignment.BottomEnd
        ) {
                // Display Content
                Column(
                    horizontalAlignment = Alignment.End,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Main Expression using AndroidView for exact EditText control
                    // This allows us to use showSoftInputOnFocus = false to kill the keyboard completely
                    // while keeping the cursor movable.
                    
                    AndroidView(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp), // Slight padding
                        factory = { context ->
                            NoKeyboardEditText(context).apply {
                                setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 64f)
                                setTextColor(android.graphics.Color.WHITE)
                                setBackground(null) // Remove underline
                                gravity = android.view.Gravity.END
                                showSoftInputOnFocus = false // API 21+ property
                                
                                // FORCE HORIZONTAL SCROLLING
                                setSingleLine(true)
                                maxLines = 1
                                setHorizontallyScrolling(true)
                                
                                // Set custom cursor color via drawable (API 29+)
                                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                                    setTextCursorDrawable(com.rubex.glassical.R.drawable.vertical_cursor)
                                    // Handle for the insertion point (teardrop)
                                    setTextSelectHandle(com.rubex.glassical.R.drawable.orange_handle)
                                    setTextSelectHandleLeft(com.rubex.glassical.R.drawable.orange_handle)
                                    setTextSelectHandleRight(com.rubex.glassical.R.drawable.orange_handle)
                                }
                                
                                // Selection change listener to update ViewModel cursor position
                                onSelectionChangedListener = { start, end ->
                                    val currentTfv = state.displayExpression
                                    // Avoid loop if selection is same
                                    if (start != currentTfv.selection.start || end != currentTfv.selection.end) {
                                         // Callback logic
                                    }
                                }
                            }
                        },
                        update = { editText ->
                            val currentText = state.displayExpression.text
                            val selection = state.displayExpression.selection
                            
                            // Text Sync
                            if (editText.text.toString() != currentText) {
                                editText.setText(currentText)
                            }

                            // Selection Sync
                            val limit = editText.text.length
                            val safeStart = selection.start.coerceIn(0, limit)
                            val safeEnd = selection.end.coerceIn(0, limit)
                            
                            if (editText.selectionStart != safeStart || editText.selectionEnd != safeEnd) {
                                editText.setSelection(safeStart, safeEnd)
                            }

                            // Update listener to invoke ViewModel action
                            (editText as? NoKeyboardEditText)?.onSelectionChangedListener = { start, end ->
                                // Only update if valid and changed in ViewModel's view
                                // We need to be careful not to trigger circular updates, 
                                // but updating ViewModel state is the source of truth.
                                val newTfv = TextFieldValue(
                                     text = editText.text.toString(),
                                     selection = TextRange(start, end)
                                )
                                viewModel.onAction(CalcAction.UpdateExpression(newTfv))
                            }
                        }
                    )

                    // Live Result (Smaller, Colored)
                    if (state.liveResult.isNotEmpty()) {
                        Text(
                            text = state.liveResult,
                            fontSize = 36.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFFEAA3BD), // Pinkish color
                            textAlign = TextAlign.End,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }
        }

        // Keypad Grid
        LazyVerticalGrid(
            columns = GridCells.Fixed(4),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            // Row 1: AC, %, ⌫, ÷
            item { RealmeButton("AC", { viewModel.onAction(CalcAction.Clear) }, isAction = true) }
            item { RealmeButton("%", { viewModel.onAction(CalcAction.Percent) }, isAction = true) }
            item { RealmeButton("⌫", { viewModel.onAction(CalcAction.Delete) }, isAction = true) }
            item { RealmeButton("÷", { viewModel.onAction(CalcAction.Operation("÷")) }, isOperation = true) }

            // Row 2: 7, 8, 9, ×
            item { RealmeButton("7", { viewModel.onAction(CalcAction.Number(7)) }) }
            item { RealmeButton("8", { viewModel.onAction(CalcAction.Number(8)) }) }
            item { RealmeButton("9", { viewModel.onAction(CalcAction.Number(9)) }) }
            item { RealmeButton("×", { viewModel.onAction(CalcAction.Operation("×")) }, isOperation = true) }

            // Row 3: 4, 5, 6, −
            item { RealmeButton("4", { viewModel.onAction(CalcAction.Number(4)) }) }
            item { RealmeButton("5", { viewModel.onAction(CalcAction.Number(5)) }) }
            item { RealmeButton("6", { viewModel.onAction(CalcAction.Number(6)) }) }
            item { RealmeButton("−", { viewModel.onAction(CalcAction.Operation("−")) }, isOperation = true) }

            // Row 4: 1, 2, 3, +
            item { RealmeButton("1", { viewModel.onAction(CalcAction.Number(1)) }) }
            item { RealmeButton("2", { viewModel.onAction(CalcAction.Number(2)) }) }
            item { RealmeButton("3", { viewModel.onAction(CalcAction.Number(3)) }) }
            item { RealmeButton("+", { viewModel.onAction(CalcAction.Operation("+")) }, isOperation = true) }

            // Row 5: 00 (Wide), 0, ., =
            item(span = { GridItemSpan(1) }) { 
                 RealmeButton("00", { viewModel.onAction(CalcAction.DoubleZero) })
            }
            item { RealmeButton("0", { viewModel.onAction(CalcAction.Number(0)) }) }
            item { RealmeButton(".", { viewModel.onAction(CalcAction.Decimal) }) }
            item { RealmeButton("=", { viewModel.onAction(CalcAction.Calculate) }, isAccent = true) }
        }
    }
}

// Custom EditText to disable soft keyboard but keep cursor
class NoKeyboardEditText @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = android.R.attr.editTextStyle
) : android.widget.EditText(context, attrs, defStyleAttr) {

    var onSelectionChangedListener: ((Int, Int) -> Unit)? = null

    init {
        showSoftInputOnFocus = false
    }

    override fun onSelectionChanged(selStart: Int, selEnd: Int) {
        super.onSelectionChanged(selStart, selEnd)
        onSelectionChangedListener?.invoke(selStart, selEnd)
    }
}
