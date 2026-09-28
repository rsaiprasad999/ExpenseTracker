package com.example.expensetracker

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import java.text.NumberFormat
import java.util.Locale

private val green = Color(0xFF69D9A1)
private val red = Color(0xFFFF817C)
private fun money(value: Double): String = NumberFormat.getCurrencyInstance(Locale("en", "IN")).format(value)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MaterialTheme(colorScheme = darkColorScheme(primary = green,
            background = Color(0xFF101418), surface = Color(0xFF1C2228))) { Dashboard() } }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterialApi::class)
@Composable private fun Dashboard(vm: ExpenseViewModel = viewModel()) {
    val state = vm.state
    val context = LocalContext.current
    var sheet by remember { mutableStateOf(false) }
    LaunchedEffect(state.message) {
        state.message?.let { Toast.makeText(context, it, Toast.LENGTH_LONG).show(); vm.clearMessage() }
    }
    val current = state.transactions.filter { it.book == state.selectedBook }
    val income = current.filter { it.type.equals("income", true) }.sumOf { it.amount }
    val expense = current.filter { !it.type.equals("income", true) }.sumOf { it.amount }
    val pull = rememberPullRefreshState(state.loading, vm::refresh)
    Scaffold(floatingActionButton = { FloatingActionButton(onClick = { sheet = true }) { Text("+ Add") } }) { padding ->
        Box(Modifier.fillMaxSize().padding(padding).pullRefresh(pull)) {
            LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                item { Text("Expense Tracker", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold) }
                item {
                    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        state.books.forEach { book -> FilterChip(selected = book == state.selectedBook,
                            onClick = { vm.selectBook(book) }, label = { Text(book) }) }
                    }
                }
                item {
                    Card(shape = RoundedCornerShape(24.dp), modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text("${state.selectedBook} balance")
                            Text(money(income - expense), style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Income  ${money(income)}", color = green)
                                Text("Expense  ${money(expense)}", color = red)
                            }
                        }
                    }
                }
                item { Text("Recent transactions", style = MaterialTheme.typography.titleLarge) }
                if (current.isEmpty() && !state.loading) item { Text("No transactions. Pull down to refresh or add one.") }
                items(current.reversed(), key = { it.id.ifBlank { "${it.date}-${it.book}-${it.amount}-${it.notes}" } }) { tx ->
                    Card(shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
                        Row(Modifier.padding(16.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(tx.category.ifBlank { "Other" }, fontWeight = FontWeight.Bold)
                                Text("${tx.date} · ${tx.paymentMethod}", style = MaterialTheme.typography.bodySmall)
                                if (tx.notes.isNotBlank()) Text(tx.notes, style = MaterialTheme.typography.bodySmall)
                            }
                            Text("${if (tx.type.equals("income", true)) "+" else "−"}${money(tx.amount)}",
                                color = if (tx.type.equals("income", true)) green else red, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
            PullRefreshIndicator(state.loading, pull, Modifier.align(Alignment.TopCenter))
        }
    }
    if (sheet) AddSheet(state.submitting, onDismiss = { sheet = false }) { amount, type, category, method, notes ->
        vm.add(amount, type, category, method, notes) { sheet = false }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable private fun AddSheet(submitting: Boolean, onDismiss: () -> Unit,
    onAdd: (Double, String, String, String, String) -> Unit) {
    var amount by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("Expense") }
    var category by remember { mutableStateOf("Food") }
    var method by remember { mutableStateOf("UPI") }
    var notes by remember { mutableStateOf("") }
    val categories = listOf("Food", "Fuel", "Bills", "Groceries", "Salary", "Other")
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 30.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Add transaction", style = MaterialTheme.typography.headlineSmall)
            OutlinedTextField(amount, { amount = it }, label = { Text("Amount (₹)") }, singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth())
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Expense", "Income").forEach { item -> FilterChip(item == type, { type = item }, label = { Text(item) }) }
            }
            Text("Category")
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                categories.forEach { item -> FilterChip(item == category, { category = item }, label = { Text(item) }) }
            }
            Text("Payment method")
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("UPI", "Cash", "Card", "Net Banking").forEach { item ->
                    FilterChip(item == method, { method = item }, label = { Text(item) })
                }
            }
            OutlinedTextField(notes, { notes = it }, label = { Text("Notes") }, modifier = Modifier.fillMaxWidth())
            Button(onClick = { amount.toDoubleOrNull()?.takeIf { it > 0 }?.let { onAdd(it, type, category, method, notes) } },
                enabled = !submitting && (amount.toDoubleOrNull() ?: 0.0) > 0,
                modifier = Modifier.fillMaxWidth()) {
                if (submitting) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                else Text("Save transaction")
            }
        }
    }
}
