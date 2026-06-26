# PocketPulse 📊

PocketPulse is an offline-first, privacy-centric personal finance management application for Android that automates expense tracking without requiring manual data entry. Instead of relying on SMS read permissions, PocketPulse leverages the Android Accessibility Service API to detect, parse, and record transaction receipts in real time from popular payment applications such as PhonePe, enabling seamless financial tracking while keeping all user data securely on-device.

---

## 🚀 Key Features

### 🔄 Zero-Effort Automated Expense Tracking

Utilizes an Android Accessibility Service pipeline to monitor completed payment screens, extract transaction amounts and merchant details, and automatically log expenses without user intervention.

### 🔒 Privacy-First & Offline-First Architecture

All financial records are stored locally on the device. No transaction data is transmitted to external servers, ensuring complete user privacy.

### ⚡ Thread-Safe Local Persistence

Built on SQLite Room Database with asynchronous background operations for efficient transaction storage, retrieval, and deduplication.

### 🌙 Intelligent Daily Summaries

Schedules an exact midnight task that aggregates daily spending statistics, updates dashboard metrics, and generates a daily financial summary notification.

### 💵 Dual-Ledger Expense Management

Supports both automatically detected digital transactions and manually entered cash expenses through separate tracking pipelines.

### 📈 Smart Analytics Dashboard

Provides spending insights through category-based pie charts, budget analysis, and historical expense trends.

---

## 🛠️ Tech Stack

* **Language:** Java
* **Platform:** Android
* **IDE:** Android Studio
* **Database:** SQLite Room Database
* **Architecture:** Repository Pattern + DAO Layer
* **Core Android APIs:**

  * Accessibility Service API
  * AlarmManager
  * BroadcastReceiver
  * NotificationManager
* **UI Components:**

  * Material Design Components
  * RecyclerView
  * Custom PieChartView
  * LinearProgressIndicator

---

## 🧠 Technical Highlights

### 1. Accessibility-Based Transaction Detection

PocketPulse listens for transaction confirmation screens from supported payment applications and extracts structured financial information directly from the rendered UI hierarchy.

### 2. Debounced UI Processing

To avoid inaccurate reads during payment-screen transitions, a custom 750ms debounce mechanism waits for UI stabilization before parsing accessibility nodes.

### 3. Heuristic Text Traversal Engine

The extraction engine combines regular expressions, text-proximity heuristics, and structural filtering to identify:

* Transaction Amount
* Merchant Name
* Transaction Context

while ignoring noise such as:

* Reference IDs
* Timestamps
* Status Labels
* Bank Metadata

### 4. Lifecycle-Aware Dashboard Refresh

Dashboard data is refreshed during fragment lifecycle transitions using `onResume()` callbacks to ensure spending metrics remain synchronized without requiring application restarts.

### 5. Exact Midnight Scheduling

PocketPulse uses:

```java
alarmManager.setExactAndAllowWhileIdle(
    AlarmManager.RTC_WAKEUP,
    targetMidnightMillis,
    pendingIntent
);
```

to bypass Android's background batching mechanisms and guarantee accurate midnight summary generation.

---

## 📱 Future Enhancements

* AI-powered spending insights
* Smart budget recommendations
* Multi-bank transaction support
* Cloud backup and synchronization
* Goal-based savings tracking
* Advanced financial forecasting

---

## 👩‍💻 Developer

**Sarasija Gonapati**

Computer Science & Engineering Student

GitHub: https://github.com/sarasijagonapati

---

## 📄 License

This project is intended for educational, research, and portfolio purposes.
