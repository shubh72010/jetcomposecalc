package com.example.jetcomposecalc

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color(0xFF1C1C1E)
                ) {
                    CalculatorScreen()
                }
            }
        }
    }
}

// --- View Model ---

@HiltViewModel
class CalculatorViewModel @Inject constructor() : ViewModel() {
    private val _uiState = MutableStateFlow(CalculatorState())
    val uiState: StateFlow<CalculatorState> = _uiState.asStateFlow()

    fun onEvent(event: CalculatorEvent) {
        when (event) {
            is CalculatorEvent.Number -> enterNumber(event.number)
            is CalculatorEvent.Operation -> enterOperation(event.operation)
            is CalculatorEvent.Delete -> delete()
            is CalculatorEvent.Clear -> clear()
            is CalculatorEvent.Calculate -> calculate()
            is CalculatorEvent.Decimal -> enterDecimal()
        }
    }

    private fun enterNumber(number: String) {
        if (_uiState.value.operation == null) {
            if (_uiState.value.firstNumber.length < 12) {
                _uiState.update { it.copy(firstNumber = it.firstNumber + number) }
            }
        } else {
            if (_uiState.value.secondNumber.length < 12) {
                _uiState.update { it.copy(secondNumber = it.secondNumber + number) }
            }
        }
    }

    private fun enterOperation(op: Operation) {
        if (_uiState.value.firstNumber.isNotEmpty()) {
            _uiState.update { it.copy(operation = op) }
        }
    }

    private fun enterDecimal() {
        if (_uiState.value.operation == null) {
            if (_uiState.value.firstNumber.contains(".")) return
            if (_uiState.value.firstNumber.isEmpty()) _uiState.update { it.copy(firstNumber = "0.") }
            else _uiState.update { it.copy(firstNumber = it.firstNumber + ".") }
        } else {
            if (_uiState.value.secondNumber.contains(".")) return
            if (_uiState.value.secondNumber.isEmpty()) _uiState.update { it.copy(secondNumber = "0.") }
            else _uiState.update { it.copy(secondNumber = it.secondNumber + ".") }
        }
    }

    private fun delete() {
        if (_uiState.value.secondNumber.isNotEmpty()) {
            _uiState.update { it.copy(secondNumber = it.secondNumber.dropLast(1)) }
        } else if (_uiState.value.operation != null) {
            _uiState.update { it.copy(operation = null) }
        } else if (_uiState.value.firstNumber.isNotEmpty()) {
            _uiState.update { it.copy(firstNumber = it.firstNumber.dropLast(1)) }
        }
    }

    private fun clear() {
        _uiState.value = CalculatorState()
    }

    private fun calculate() {
        val first = _uiState.value.firstNumber.toDoubleOrNull()
        val second = _uiState.value.secondNumber.toDoubleOrNull()
        val op = _uiState.value.operation

        if (first != null && second != null && op != null) {
            val result = when (op) {
                Operation.ADD -> first + second
                Operation.SUBTRACT -> first - second
                Operation.MULTIPLY -> first * second
                Operation.DIVIDE -> first / second
            }
            
            // Format result to remove unnecessary decimal trailing zeros
            val formattedResult = if (result % 1 == 0.0) {
                result.toInt().toString()
            } else {
                result.toString()
            }
            
            _uiState.update {
                it.copy(
                    firstNumber = formattedResult,
                    operation = null,
                    secondNumber = ""
                )
            }
        }
    }
}

data class CalculatorState(
    val firstNumber: String = "",
    val secondNumber: String = "",
    val operation: Operation? = null
)

sealed class CalculatorEvent {
    data class Number(val number: String) : CalculatorEvent()
    data class Operation(val operation: com.example.jetcomposecalc.Operation) : CalculatorEvent()
    object Clear : CalculatorEvent()
    object Delete : CalculatorEvent()
    object Calculate : CalculatorEvent()
    object Decimal : CalculatorEvent()
}

enum class Operation(val symbol: String) {
    ADD("+"), SUBTRACT("-"), MULTIPLY("×"), DIVIDE("÷")
}

// --- Composable UI ---

@Composable
fun CalculatorScreen(viewModel: CalculatorViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsState()

    val buttonSpacing = 8.dp

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(buttonSpacing),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Display Area
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 32.dp),
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.Bottom
            ) {
                Text(
                    text = state.firstNumber + (state.operation?.symbol ?: "") + state.secondNumber,
                    fontSize = 64.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1,
                    textAlign = TextAlign.End,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // First Row: AC, Delete, %, / (or use simple logic)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(buttonSpacing)
            ) {
                CalculatorButton(
                    symbol = "AC",
                    modifier = Modifier
                        .weight(1f)
                        .height(64.dp),
                    onClick = { viewModel.onEvent(CalculatorEvent.Clear) }
                )
                CalculatorButton(
                    symbol = "DEL",
                    modifier = Modifier
                        .weight(1f)
                        .height(64.dp),
                    onClick = { viewModel.onEvent(CalculatorEvent.Delete) }
                )
                CalculatorButton(
                    symbol = "÷",
                    modifier = Modifier
                        .weight(1f)
                        .height(64.dp),
                    onClick = { viewModel.onEvent(CalculatorEvent.Operation(Operation.DIVIDE)) }
                )
                CalculatorButton(
                    symbol = "×",
                    modifier = Modifier
                        .weight(1f)
                        .height(64.dp),
                    onClick = { viewModel.onEvent(CalculatorEvent.Operation(Operation.MULTIPLY)) }
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(buttonSpacing)
            ) {
                CalculatorButton(
                    symbol = "7",
                    modifier = Modifier
                        .weight(1f)
                        .height(64.dp),
                    onClick = { viewModel.onEvent(CalculatorEvent.Number("7")) }
                )
                CalculatorButton(
                    symbol = "8",
                    modifier = Modifier
                        .weight(1f)
                        .height(64.dp),
                    onClick = { viewModel.onEvent(CalculatorEvent.Number("8")) }
                )
                CalculatorButton(
                    symbol = "9",
                    modifier = Modifier
                        .weight(1f)
                        .height(64.dp),
                    onClick = { viewModel.onEvent(CalculatorEvent.Number("9")) }
                )
                CalculatorButton(
                    symbol = "-",
                    modifier = Modifier
                        .weight(1f)
                        .height(64.dp),
                    onClick = { viewModel.onEvent(CalculatorEvent.Operation(Operation.SUBTRACT)) }
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(buttonSpacing)
            ) {
                CalculatorButton(
                    symbol = "4",
                    modifier = Modifier
                        .weight(1f)
                        .height(64.dp),
                    onClick = { viewModel.onEvent(CalculatorEvent.Number("4")) }
                )
                CalculatorButton(
                    symbol = "5",
                    modifier = Modifier
                        .weight(1f)
                        .height(64.dp),
                    onClick = { viewModel.onEvent(CalculatorEvent.Number("5")) }
                )
                CalculatorButton(
                    symbol = "6",
                    modifier = Modifier
                        .weight(1f)
                        .height(64.dp),
                    onClick = { viewModel.onEvent(CalculatorEvent.Number("6")) }
                )
                CalculatorButton(
                    symbol = "+",
                    modifier = Modifier
                        .weight(1f)
                        .height(64.dp),
                    onClick = { viewModel.onEvent(CalculatorEvent.Operation(Operation.ADD)) }
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(buttonSpacing)
            ) {
                // Numbers 1, 2, 3
                Column(modifier = Modifier.weight(3f)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(buttonSpacing)) {
                         CalculatorButton(
                            symbol = "1",
                            modifier = Modifier
                                .weight(1f)
                                .height(64.dp),
                            onClick = { viewModel.onEvent(CalculatorEvent.Number("1")) }
                        )
                         CalculatorButton(
                            symbol = "2",
                            modifier = Modifier
                                .weight(1f)
                                .height(64.dp),
                            onClick = { viewModel.onEvent(CalculatorEvent.Number("2")) }
                        )
                         CalculatorButton(
                            symbol = "3",
                            modifier = Modifier
                                .weight(1f)
                                .height(64.dp),
                            onClick = { viewModel.onEvent(CalculatorEvent.Number("3")) }
                        )
                    }
                    Spacer(modifier = Modifier.height(buttonSpacing))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(buttonSpacing)) {
                         CalculatorButton(
                            symbol = "0",
                            modifier = Modifier
                                .weight(2f)
                                .height(64.dp),
                            onClick = { viewModel.onEvent(CalculatorEvent.Number("0")) }
                        )
                         CalculatorButton(
                            symbol = ".",
                            modifier = Modifier
                                .weight(1f)
                                .height(64.dp),
                            onClick = { viewModel.onEvent(CalculatorEvent.Decimal) }
                        )
                    }
                }
                
                // Equals button
                CalculatorButton(
                    symbol = "=",
                    modifier = Modifier
                        .weight(1f)
                        .height(132.dp + buttonSpacing), // Height of two rows + spacing
                    onClick = { viewModel.onEvent(CalculatorEvent.Calculate) }
                )
            }
        }
    }
}

@Composable
fun CalculatorButton(
    symbol: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val isNumber = symbol.all { it.isDigit() }
    val isSpecial = symbol == "AC" || symbol == "DEL"
    
    val containerColor = when {
        isSpecial -> Color(0xFFA5A5A5)
        symbol == "=" -> Color(0xFFF5B237)
        !isNumber && symbol != "." -> Color(0xFFF5B237)
        else -> Color(0xFF333333)
    }
    
    val contentColor = if (isSpecial || (!isNumber && symbol != "." && symbol != "=")) Color.Black else Color.White

    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp)),
        color = containerColor,
        onClick = onClick
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = symbol,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = contentColor
            )
        }
    }
}