# Expense Tracker

A native Android application that helps users manage their personal finances by tracking income and expenses.

## Features

- View total balance, income, and expenses.
- Add income and expense transactions.
- Edit existing transactions.
- Delete transactions with confirmation.
- Categorize transactions.
- Automatic balance calculations.
- Persistent local storage using Room Database.
- Indian Rupee (₹) currency formatting.

## Tech Stack

- **Language:** Kotlin
- **IDE:** Android Studio
- **UI:** Jetpack Compose
- **Database:** Room Database
- **Asynchronous Programming:** Kotlin Coroutines and Flow
- **Version Control:** Git and GitHub

## Architecture and Database

The application uses Room Database for local data persistence.

- **Entity:** Transaction
- **DAO:** TransactionDao
- **Database:** AppDatabase

Kotlin Flow provides reactive updates, allowing the dashboard to refresh when transactions change.

## Installation

1. Clone this repository:
   ```bash
   git clone https://github.com/Sahasra2006/Expense-Tracker.git
   ```
2. Open the project in Android Studio.
3. Allow Gradle synchronization to finish.
4. Run the application on an Android emulator or physical device.

## Future Improvements

- Monthly expense summaries.
- Transaction search and filtering.
- Visual spending charts.

## Author

**Sahasra**

GitHub: [@Sahasra2006](https://github.com/Sahasra2006)
