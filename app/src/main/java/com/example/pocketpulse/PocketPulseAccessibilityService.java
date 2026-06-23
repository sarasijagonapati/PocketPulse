package com.example.pocketpulse;

import android.accessibilityservice.AccessibilityService;
import android.content.Intent;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PocketPulseAccessibilityService extends AccessibilityService {
    private static final String TAG = "PP_TRACKER";
    private PocketPulseRepository repository;
    private final Pattern amountPattern = Pattern.compile("(?:₹|Rs\\.?|INR)\\s*([0-9,]+(?:\\.[0-9]{2})?)");

    private String lastCapturedAmount = "";
    private long lastTriggeredTime = 0;

    // ⏱️ UI Settle Mechanics: Prevents Android from reading stale screen caches during transitions
    private final android.os.Handler settleHandler = new android.os.Handler(android.os.Looper.getMainLooper());
    private Runnable settleRunnable;

    @Override
    public void onCreate() {
        super.onCreate();
        repository = new PocketPulseRepository(this);
        Log.d(TAG, "🟢 PocketPulse Accessibility Service successfully started initialization!");
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event == null) return;

        int eventType = event.getEventType();
        // Only trigger on actual window adjustments or content updates
        if (eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED ||
                eventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED) {

            // Cancel any pending scan jobs. We only want to execute ONCE after the screen stops flickering.
            if (settleRunnable != null) {
                settleHandler.removeCallbacks(settleRunnable);
            }

            // Schedule the scanner to run exactly 300ms after the page finishes layout shifts
            settleRunnable = () -> {
                AccessibilityNodeInfo rootNode = getRootInActiveWindow();
                if (rootNode == null) return;

                List<String> screenTexts = new ArrayList<>();
                traverseNodeTree(rootNode, screenTexts);
                rootNode.recycle();

                Log.d(TAG, "📄 Settled text discovered on screen layout: " + screenTexts.toString());

                // 1. Verify it matches a successful payment layout pattern
                if (verifySuccessState(screenTexts)) {

                    // 📅 2. Date Safety Layer: Drop if it's from a previous day
                    if (!isTransactionFromToday(screenTexts)) {
                        return;
                    }

                    // ⏰ 3. Time Safety Layer: Drop if it's older than our 4-minute window
                    if (!isTransactionWithinTimeWindow(screenTexts)) {
                        return;
                    }

                    Log.d(TAG, "🚀 Clear pass! Processing authentic live transaction values...");
                    parseAndProcessScreen(screenTexts);
                }
            };

            settleHandler.postDelayed(settleRunnable, 600); // 600ms window settling delay
        }
    }

    private void traverseNodeTree(AccessibilityNodeInfo node, List<String> texts) {
        if (node == null) return;
        if (node.getText() != null) {
            String val = node.getText().toString().trim();
            if (!val.isEmpty()) {
                texts.add(val);
            }
        }
        for (int i = 0; i < node.getChildCount(); i++) {
            traverseNodeTree(node.getChild(i), texts);
        }
    }

    private boolean verifySuccessState(List<String> textNodes) {
        boolean hasSuccessWord = false;
        boolean isIncomingMoney = false;
        boolean isHistoryOrListScreen = false;

        for (String node : textNodes) {
            String text = node.toLowerCase().trim();

            // BLOCK HISTORY & SEARCH VIEWS
            if (text.contains("my statements") || text.contains("transaction history") ||
                    text.contains("all transactions") || text.equals("search") || text.equals("history")) {
                isHistoryOrListScreen = true;
            }

            // Check for standard individual receipt indicators
            if (text.contains("successful") || text.contains("paid successfully") ||
                    text.contains("money sent") || text.contains("payment successful") ||
                    text.contains("paid to")) {
                hasSuccessWord = true;
            }

            // Detect if this is actually incoming money
            if (text.contains("received from") || text.contains("credited to")) {
                isIncomingMoney = true;
            }
        }

        return hasSuccessWord && !isIncomingMoney && !isHistoryOrListScreen;
    }

    private void parseAndProcessScreen(List<String> nodes) {
        String amountStr = "";
        String extractedRecipient = "";

        for (int i = 0; i < nodes.size(); i++) {
            String cleanLine = nodes.get(i);
            String lowerLine = cleanLine.toLowerCase();

            if (lowerLine.contains("balance") || lowerLine.contains("xxxx") || lowerLine.contains("utr")) {
                continue;
            }

            Matcher matcher = amountPattern.matcher(cleanLine);
            if (matcher.find()) {
                amountStr = matcher.group(1).replace(",", "");
                Log.d(TAG, "🎯 Matched amount via regex engine: " + amountStr);

                // Window search radar (Checks up to 3 nodes away for user profile name text)
                for (int offset = 1; offset <= 3; offset++) {
                    if (i - offset >= 0) {
                        String candidate = nodes.get(i - offset);
                        if (isValidMerchantName(candidate)) {
                            extractedRecipient = candidate;
                            break;
                        }
                    }
                }

                if (extractedRecipient.isEmpty()) {
                    for (int offset = 1; offset <= 3; offset++) {
                        if (i + offset < nodes.size()) {
                            String candidate = nodes.get(i + offset);
                            if (isValidMerchantName(candidate)) {
                                extractedRecipient = candidate;
                                break;
                            }
                        }
                    }
                }
                break;
            }
        }

        if (amountStr.isEmpty()) {
            Log.d(TAG, "❌ Aborting: Screen parser failed to identify currency digits.");
            return;
        }

        long currentTime = System.currentTimeMillis();
        if (amountStr.equals(lastCapturedAmount) && (currentTime - lastTriggeredTime < 6000)) {
            Log.d(TAG, "🛡️ Aborting duplicate scan action loop via debouncer window interval.");
            return;
        }

        lastCapturedAmount = amountStr;
        lastTriggeredTime = currentTime;

        final double finalAmount = Double.parseDouble(amountStr);
        final String finalRecipient = extractedRecipient.isEmpty() ? "Unknown Merchant" : extractedRecipient;

        Log.d(TAG, "🚀 Dispatching overlay command. Amount: " + finalAmount + " | Recipient: " + finalRecipient);

        repository.checkDuplicateTransaction(finalAmount, isDuplicate -> {
            if (!isDuplicate) {
                Intent dialogIntent = new Intent(this, TransactionCategorizerActivity.class);
                dialogIntent.putExtra("EXTRA_AMOUNT", finalAmount);
                dialogIntent.putExtra("EXTRA_RECIPIENT", finalRecipient);
                dialogIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(dialogIntent);
                Log.d(TAG, "🎉 Bottom Sheet intent launched over screen layout background!");
            } else {
                Log.d(TAG, "🛑 Database flagged this transaction entry as a timestamp duplicate.");
            }
        });
    }

    private boolean isValidMerchantName(String input) {
        if (input == null || input.trim().isEmpty()) return false;
        String check = input.toLowerCase().trim();

        if (check.contains("successful") || check.contains("paid successfully") ||
                check.contains("debited") || check.contains("sent") || check.contains("received") ||
                check.contains("credited") || check.contains("utr") || check.contains("transaction id") ||
                check.contains("ref") || check.contains("banking name") || check.contains("powered by") ||
                check.contains("history") || check.contains("balance") || check.contains("view") ||
                check.contains("share") || check.contains("money") || check.contains("transfer details") ||
                check.equals("to") || check.equals("from")) {
            return false;
        }

        if (check.contains("@")) return false;
        if (check.contains("xxxx") || check.matches(".*\\d{3,}.*")) return false;

        return input.length() > 2;
    }

    private boolean isTransactionFromToday(List<String> textNodes) {
        java.util.Calendar cal = java.util.Calendar.getInstance();
        java.text.SimpleDateFormat fullDateFormat = new java.text.SimpleDateFormat("d MMMM yyyy", java.util.Locale.ENGLISH);
        java.text.SimpleDateFormat shortDateFormat = new java.text.SimpleDateFormat("d MMM yyyy", java.util.Locale.ENGLISH);

        String todayFull = fullDateFormat.format(cal.getTime()).toLowerCase();
        String todayShort = shortDateFormat.format(cal.getTime()).toLowerCase();

        for (String node : textNodes) {
            String text = node.toLowerCase();
            if (text.contains(todayFull) || text.contains(todayShort) || text.contains("today")) {
                return true;
            }
        }
        return false;
    }

    private boolean isTransactionWithinTimeWindow(List<String> textNodes) {
        Pattern timePattern = Pattern.compile("\\b([0-9]{1,2}):([0-9]{2})\\s*(am|pm|AM|PM)\\b");

        java.util.Calendar now = java.util.Calendar.getInstance();
        int currentHour = now.get(java.util.Calendar.HOUR);
        int currentMinute = now.get(java.util.Calendar.MINUTE);
        int currentAmPm = now.get(java.util.Calendar.AM_PM);

        for (String node : textNodes) {
            Matcher matcher = timePattern.matcher(node);
            if (matcher.find()) {
                try {
                    int receiptHour = Integer.parseInt(matcher.group(1));
                    int receiptMinute = Integer.parseInt(matcher.group(2));
                    String amPmStr = matcher.group(3).toLowerCase();
                    int receiptAmPm = (amPmStr.contains("pm")) ? java.util.Calendar.PM : java.util.Calendar.AM;

                    if (currentAmPm != receiptAmPm) continue;

                    int currentTotalMinutes = (currentHour == 12 ? 0 : currentHour) * 60 + currentMinute;
                    int receiptTotalMinutes = (receiptHour == 12 ? 0 : receiptHour) * 60 + receiptMinute;

                    int minuteDifference = Math.abs(currentTotalMinutes - receiptTotalMinutes);

                    if (minuteDifference <= 4) {
                        return true;
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error parsing layout time string", e);
                }
            }
        }
        return false;
    }

    @Override public void onInterrupt() {}
}