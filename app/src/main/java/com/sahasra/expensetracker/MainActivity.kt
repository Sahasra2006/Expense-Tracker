package com.sahasra.expensetracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.sahasra.expensetracker.data.AppDatabase
import com.sahasra.expensetracker.data.Transaction
import com.sahasra.expensetracker.ui.theme.ExpenseTrackerTheme
import java.text.NumberFormat
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// Helper function to format amounts according to the Indian numbering system
fun formatInr(amount: Double): String {
    val formatter = NumberFormat.getNumberInstance(Locale.forLanguageTag("en-IN")).apply {
        minimumFractionDigits = 2
        maximumFractionDigits = 2
    }
    return formatter.format(amount)
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ExpenseTrackerTheme {
                ExpenseTrackerApp()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpenseTrackerApp() {
    val context = LocalContext.current
    val database = remember { AppDatabase.getDatabase(context) }
    val coroutineScope = rememberCoroutineScope()

    // Real-time list of transactions observed from Room Database
    val transactions by remember(database) {
        database.transactionDao().getAllTransactions()
    }.collectAsState(initial = emptyList())

    // Guard sample pre-population so it runs only once and never overwrites user data
    val prefs = remember { context.getSharedPreferences("expense_tracker_prefs", android.content.Context.MODE_PRIVATE) }
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            val hasSeeded = prefs.getBoolean("has_seeded_sample_data", false)
            if (!hasSeeded) {
                if (database.transactionDao().getTransactionCount() == 0) {
                    database.transactionDao().insertAll(
                        listOf(
                            Transaction(title = "Monthly Salary", category = "Income", amount = 4500.00, date = "Today", isIncome = true),
                            Transaction(title = "Grocery Shopping", category = "Food", amount = 124.50, date = "Today", isIncome = false),
                            Transaction(title = "Electricity Bill", category = "Utilities", amount = 68.20, date = "Yesterday", isIncome = false),
                            Transaction(title = "Freelance Work", category = "Income", amount = 350.00, date = "Nov 05", isIncome = true),
                            Transaction(title = "Coffee & Snacks", category = "Food & Drink", amount = 12.80, date = "Nov 04", isIncome = false),
                            Transaction(title = "Gym Membership", category = "Health", amount = 45.00, date = "Nov 02", isIncome = false)
                        )
                    )
                }
                prefs.edit().putBoolean("has_seeded_sample_data", true).apply()
            }
        }
    }

    // State controlling dialogs
    var showAddDialog by remember { mutableStateOf(false) }
    var transactionToEdit by remember { mutableStateOf<Transaction?>(null) }
    var transactionToDelete by remember { mutableStateOf<Transaction?>(null) }

    // Calculate summary numbers from transaction data
    val totalIncome = transactions.filter { it.isIncome }.sumOf { it.amount }
    val totalExpenses = transactions.filter { !it.isIncome }.sumOf { it.amount }
    val totalBalance = totalIncome - totalExpenses

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "Smart Expense Tracker",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Text(
                    text = "+",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    ) { innerPadding ->
        HomeScreen(
            totalBalance = totalBalance,
            income = totalIncome,
            expenses = totalExpenses,
            transactions = transactions,
            onEditTransaction = { transactionToEdit = it },
            onDeleteTransaction = { transactionToDelete = it },
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        )

        // Add Transaction Dialog (Persists to Room)
        if (showAddDialog) {
            AddTransactionDialog(
                onDismiss = { showAddDialog = false },
                onSave = { newTransaction ->
                    coroutineScope.launch(Dispatchers.IO) {
                        database.transactionDao().insertTransaction(newTransaction)
                    }
                    showAddDialog = false
                }
            )
        }

        // Edit Transaction Dialog (Updates in Room)
        if (transactionToEdit != null) {
            EditTransactionDialog(
                transaction = transactionToEdit!!,
                onDismiss = { transactionToEdit = null },
                onSave = { updatedTransaction ->
                    coroutineScope.launch(Dispatchers.IO) {
                        database.transactionDao().updateTransaction(updatedTransaction)
                    }
                    transactionToEdit = null
                }
            )
        }

        // Delete Confirmation Dialog (Deletes from Room)
        if (transactionToDelete != null) {
            AlertDialog(
                onDismissRequest = { transactionToDelete = null },
                title = {
                    Text(
                        text = "Delete Transaction",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Text(
                        text = "Are you sure you want to delete \"${transactionToDelete!!.title}\"? This action cannot be undone.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val toDelete = transactionToDelete!!
                            coroutineScope.launch(Dispatchers.IO) {
                                database.transactionDao().deleteTransaction(toDelete)
                            }
                            transactionToDelete = null
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError
                        )
                    ) {
                        Text("Delete")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { transactionToDelete = null }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

@Composable
fun TransactionFormDialog(
    dialogTitle: String,
    confirmButtonText: String,
    initialTransaction: Transaction? = null,
    onDismiss: () -> Unit,
    onSave: (Transaction) -> Unit
) {
    var title by remember { mutableStateOf(initialTransaction?.title ?: "") }
    var amountText by remember {
        mutableStateOf(
            if (initialTransaction != null) {
                if (initialTransaction.amount % 1.0 == 0.0) {
                    initialTransaction.amount.toLong().toString()
                } else {
                    initialTransaction.amount.toString()
                }
            } else ""
        )
    }
    var category by remember { mutableStateOf(initialTransaction?.category ?: "") }
    var isIncome by remember { mutableStateOf(initialTransaction?.isIncome ?: false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val suggestedCategories = if (isIncome) {
        listOf("Salary", "Freelance", "Investment", "Gift", "Other")
    } else {
        listOf("Food", "Shopping", "Transport", "Bills", "Health", "Other")
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = dialogTitle,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Income / Expense Type Selector
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            isIncome = false
                            errorMessage = null
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (!isIncome) Color(0xFFC62828) else MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = if (!isIncome) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    ) {
                        Text("Expense", fontWeight = FontWeight.SemiBold)
                    }

                    Button(
                        onClick = {
                            isIncome = true
                            errorMessage = null
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isIncome) Color(0xFF2E7D32) else MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = if (isIncome) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    ) {
                        Text("Income", fontWeight = FontWeight.SemiBold)
                    }
                }

                // Title Input
                OutlinedTextField(
                    value = title,
                    onValueChange = {
                        title = it
                        errorMessage = null
                    },
                    label = { Text("Title") },
                    placeholder = { Text(if (isIncome) "e.g. Monthly Bonus" else "e.g. Dinner with friends") },
                    singleLine = true,
                    isError = errorMessage != null && title.trim().isEmpty(),
                    modifier = Modifier.fillMaxWidth()
                )

                // Numeric Amount Input
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { input ->
                        if (input.isEmpty() || input.matches(Regex("""^\d*\.?\d{0,2}$"""))) {
                            amountText = input
                            errorMessage = null
                        }
                    },
                    label = { Text("Amount (₹)") },
                    placeholder = { Text("0.00") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    isError = errorMessage != null && (amountText.toDoubleOrNull() == null || amountText.toDoubleOrNull()!! <= 0.0),
                    modifier = Modifier.fillMaxWidth()
                )

                // Category Input
                OutlinedTextField(
                    value = category,
                    onValueChange = {
                        category = it
                        errorMessage = null
                    },
                    label = { Text("Category") },
                    placeholder = { Text(if (isIncome) "e.g. Salary" else "e.g. Food") },
                    singleLine = true,
                    isError = errorMessage != null && category.trim().isEmpty(),
                    modifier = Modifier.fillMaxWidth()
                )

                // Quick Category Suggestions
                Text(
                    text = "Suggested Categories:",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    suggestedCategories.forEach { cat ->
                        SuggestionChip(
                            onClick = {
                                category = cat
                                errorMessage = null
                            },
                            label = { Text(cat) }
                        )
                    }
                }

                // Error Message Display
                if (errorMessage != null) {
                    Text(
                        text = errorMessage!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val parsedAmount = amountText.toDoubleOrNull()
                    when {
                        title.trim().isEmpty() -> {
                            errorMessage = "Please enter a transaction title."
                        }
                        parsedAmount == null || parsedAmount <= 0.0 -> {
                            errorMessage = "Amount must be greater than 0."
                        }
                        category.trim().isEmpty() -> {
                            errorMessage = "Please enter or select a category."
                        }
                        else -> {
                            onSave(
                                Transaction(
                                    id = initialTransaction?.id ?: 0,
                                    title = title.trim(),
                                    category = category.trim(),
                                    amount = parsedAmount,
                                    date = initialTransaction?.date ?: "Today",
                                    isIncome = isIncome
                                )
                            )
                        }
                    }
                }
            ) {
                Text(confirmButtonText)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun AddTransactionDialog(
    onDismiss: () -> Unit,
    onSave: (Transaction) -> Unit
) {
    TransactionFormDialog(
        dialogTitle = "Add Transaction",
        confirmButtonText = "Save",
        initialTransaction = null,
        onDismiss = onDismiss,
        onSave = onSave
    )
}

@Composable
fun EditTransactionDialog(
    transaction: Transaction,
    onDismiss: () -> Unit,
    onSave: (Transaction) -> Unit
) {
    TransactionFormDialog(
        dialogTitle = "Edit Transaction",
        confirmButtonText = "Update",
        initialTransaction = transaction,
        onDismiss = onDismiss,
        onSave = onSave
    )
}

@Composable
fun HomeScreen(
    totalBalance: Double,
    income: Double,
    expenses: Double,
    transactions: List<Transaction>,
    onEditTransaction: (Transaction) -> Unit,
    onDeleteTransaction: (Transaction) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Total Balance Card
        TotalBalanceCard(balance = totalBalance)

        // Income & Expenses Summary Cards
        IncomeExpenseSection(income = income, expenses = expenses)

        // Recent Transactions Section
        RecentTransactionsSection(
            transactions = transactions,
            onEditTransaction = onEditTransaction,
            onDeleteTransaction = onDeleteTransaction
        )
    }
}

@Composable
fun TotalBalanceCard(
    balance: Double,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Total Balance",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
            )
            Spacer(modifier = Modifier.height(8.dp))
            val balanceText = if (balance < 0) "-₹${formatInr(-balance)}" else "₹${formatInr(balance)}"
            Text(
                text = balanceText,
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

@Composable
fun IncomeExpenseSection(
    income: Double,
    expenses: Double,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Income Card
        StatCard(
            title = "Income",
            amount = "+₹${formatInr(income)}",
            amountColor = Color(0xFF2E7D32), // Forest Green
            modifier = Modifier.weight(1f)
        )

        // Expenses Card
        StatCard(
            title = "Expenses",
            amount = "-₹${formatInr(expenses)}",
            amountColor = Color(0xFFC62828), // Deep Red
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun StatCard(
    title: String,
    amount: String,
    amountColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = amount,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = amountColor
            )
        }
    }
}

@Composable
fun RecentTransactionsSection(
    transactions: List<Transaction>,
    onEditTransaction: (Transaction) -> Unit,
    onDeleteTransaction: (Transaction) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = "Recent Transactions",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        if (transactions.isEmpty()) {
            Card(
                modifier = modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Text(
                    text = "No transactions yet. Click + to add one.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(20.dp)
                )
            }
        } else {
            transactions.forEach { transaction ->
                TransactionItem(
                    transaction = transaction,
                    onEdit = { onEditTransaction(transaction) },
                    onDelete = { onDeleteTransaction(transaction) }
                )
            }
        }
    }
}

@Composable
fun TransactionItem(
    transaction: Transaction,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = transaction.title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${transaction.category} • ${transaction.date}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            val formattedAmount = formatInr(transaction.amount)
            val amountText = if (transaction.isIncome) "+₹$formattedAmount" else "-₹$formattedAmount"
            val amountColor = if (transaction.isIncome) Color(0xFF2E7D32) else Color(0xFFC62828)

            Text(
                text = amountText,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = amountColor
            )

            // Three-dot action menu
            var menuExpanded by remember { mutableStateOf(false) }

            Box {
                IconButton(onClick = { menuExpanded = true }) {
                    Text(
                        text = "⋮",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Edit") },
                        onClick = {
                            menuExpanded = false
                            onEdit()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                        onClick = {
                            menuExpanded = false
                            onDelete()
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true)
@Composable
fun HomeScreenPreview() {
    val sampleTransactions = listOf(
        Transaction(1, "Monthly Salary", "Income", 4500.00, "Today", isIncome = true),
        Transaction(2, "Grocery Shopping", "Food", 124.50, "Today", isIncome = false),
        Transaction(3, "Electricity Bill", "Utilities", 68.20, "Yesterday", isIncome = false),
        Transaction(4, "Freelance Work", "Income", 350.00, "Nov 05", isIncome = true),
        Transaction(5, "Coffee & Snacks", "Food & Drink", 12.80, "Nov 04", isIncome = false),
        Transaction(6, "Gym Membership", "Health", 45.00, "Nov 02", isIncome = false)
    )
    ExpenseTrackerTheme {
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            text = "Smart Expense Tracker",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )
            }
        ) { innerPadding ->
            HomeScreen(
                totalBalance = 4599.50,
                income = 4850.00,
                expenses = 250.50,
                transactions = sampleTransactions,
                onEditTransaction = {},
                onDeleteTransaction = {},
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            )
        }
    }
}
