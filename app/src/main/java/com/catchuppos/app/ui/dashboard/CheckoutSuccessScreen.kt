package com.catchuppos.app.ui.dashboard

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.catchuppos.app.theme.*
import java.text.SimpleDateFormat
import java.util.*

// ── Checkout Data ──

data class CheckoutData(
    val items: List<CartItem>,
    val subtotal: Double,
    val total: Double,
    val amountTendered: Double,
    val changeReturned: Double,
    val transactionId: String,
    val dateTime: String,
    val terminal: String = "Terminal 01",
    val cashier: String = "Admin",
    val customerName: String = "Sir Lyme"
)

// ════════════════════════════════════════════════════════════════════
// Checkout Success Screen
// ════════════════════════════════════════════════════════════════════

@Composable
fun CheckoutSuccessScreen(
    checkoutData: CheckoutData,
    onPrintReceipt: () -> Unit = {},
    onNewOrder: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        // ── Left Panel: Success & Actions ──
        Column(
            modifier = Modifier
                .weight(3f)
                .fillMaxHeight()
                .verticalScroll(rememberScrollState())
                .padding(40.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            // ── Success Indicator ──
            Box(
                modifier = Modifier.size(80.dp),
                contentAlignment = Alignment.Center
            ) {
                // Outer ring
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(StatusGreen.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    // Inner checkmark
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(StatusGreen),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Success",
                            tint = TextWhite,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "PAYMENT SUCCESSFUL!",
                style = MaterialTheme.typography.headlineMedium,
                color = TextWhite,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Transaction completed successfully.",
                style = MaterialTheme.typography.bodyLarge,
                color = TextMuted,
                modifier = Modifier.padding(top = 4.dp)
            )

            Spacer(modifier = Modifier.height(32.dp))

            // ── Change Returned ──
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFF111111),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "CHANGE RETURNED",
                        style = MaterialTheme.typography.labelLarge,
                        color = TextMuted,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "₱${String.format("%.2f", checkoutData.changeReturned)}",
                        style = MaterialTheme.typography.displayLarge,
                        color = StatusGreen,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // ── Fulfillment Alert ──
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = DarkCard,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = null,
                        tint = OrangeAccent,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = "Thank you ${checkoutData.customerName} for choosing Catch Up. We will call your name with a bell if we finish preparing your coffee. 😊",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextWhite,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // ── Action Buttons ──
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Print Receipt
                OutlinedButton(
                    onClick = onPrintReceipt,
                    modifier = Modifier.weight(1f).height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, DarkBorder),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = DarkCard,
                        contentColor = TextWhite
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Print,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Print Receipt",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                // New Order
                Button(
                    onClick = onNewOrder,
                    modifier = Modifier.weight(1f).height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = OrangeAccent,
                        contentColor = TextWhite
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Home,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "New Order",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }

        // ── Right Panel: Digital Receipt ──
        Surface(
            modifier = Modifier
                .width(360.dp)
                .fillMaxHeight(),
            color = Color(0xFF0D0D0D),
            tonalElevation = 0.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp)
            ) {
                // ── Status Header ──
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = StatusGreen,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "TRANSACTION COMPLETE",
                        style = MaterialTheme.typography.labelLarge,
                        color = StatusGreen,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))
                HorizontalDivider(color = DarkBorder, thickness = 0.5.dp)
                Spacer(modifier = Modifier.height(16.dp))

                // ── ORDER SUMMARY ──
                Text(
                    text = "ORDER SUMMARY",
                    style = MaterialTheme.typography.labelLarge,
                    color = TextMuted,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(12.dp))

                checkoutData.items.forEach { item ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = item.product.title,
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextWhite,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "(${item.size})",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextGray
                            )
                        }
                        Text(
                            text = "x${item.quantity}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextMuted,
                            modifier = Modifier.padding(horizontal = 12.dp)
                        )
                        Text(
                            text = "₱${String.format("%.2f", item.product.sellingPrice * item.quantity)}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextWhite,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = DarkBorder, thickness = 0.5.dp)
                Spacer(modifier = Modifier.height(8.dp))

                // Subtotal & Total
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Subtotal",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextMuted
                    )
                    Text(
                        text = "₱${String.format("%.2f", checkoutData.subtotal)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextWhite
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "TOTAL",
                        style = MaterialTheme.typography.titleMedium,
                        color = OrangeAccent,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "₱${String.format("%.2f", checkoutData.total)}",
                        style = MaterialTheme.typography.titleLarge,
                        color = OrangeAccent,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))
                HorizontalDivider(color = DarkBorder, thickness = 0.5.dp)
                Spacer(modifier = Modifier.height(16.dp))

                // ── PAYMENT DETAILS ──
                Text(
                    text = "PAYMENT DETAILS",
                    style = MaterialTheme.typography.labelLarge,
                    color = TextMuted,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(12.dp))

                ReceiptInfoRow("Payment Method", "Cash")
                ReceiptInfoRow("Amount Tendered", "₱${String.format("%.2f", checkoutData.amountTendered)}")
                ReceiptInfoRow("Change Returned", "₱${String.format("%.2f", checkoutData.changeReturned)}", valueColor = StatusGreen)

                Spacer(modifier = Modifier.height(20.dp))
                HorizontalDivider(color = DarkBorder, thickness = 0.5.dp)
                Spacer(modifier = Modifier.height(16.dp))

                // ── TRANSACTION INFO ──
                Text(
                    text = "TRANSACTION INFO",
                    style = MaterialTheme.typography.labelLarge,
                    color = TextMuted,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(12.dp))

                ReceiptInfoRow("Transaction ID", checkoutData.transactionId)
                ReceiptInfoRow("Date & Time", checkoutData.dateTime)
                ReceiptInfoRow("Terminal", checkoutData.terminal)
                ReceiptInfoRow("Cashier", checkoutData.cashier)

                Spacer(modifier = Modifier.height(24.dp))
                HorizontalDivider(color = DarkBorder, thickness = 0.5.dp)
                Spacer(modifier = Modifier.height(20.dp))

                // ── Branded Footer ──
                Text(
                    text = "Catch you again!",
                    style = MaterialTheme.typography.titleLarge,
                    color = OrangeAccent,
                    fontStyle = FontStyle.Italic,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "CATCH",
                    style = MaterialTheme.typography.headlineSmall,
                    color = TextWhite,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 4.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = "UP!",
                    style = MaterialTheme.typography.headlineSmall,
                    color = OrangeAccent,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 4.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

// ── Receipt Info Row ──

@Composable
private fun ReceiptInfoRow(
    label: String,
    value: String,
    valueColor: Color = TextWhite
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = TextMuted
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = valueColor,
            fontWeight = FontWeight.Medium
        )
    }
}

// ── Helper to generate transaction ID ──

fun generateTransactionId(): String {
    val now = Calendar.getInstance()
    val dateStr = String.format(
        "%02d%02d%04d",
        now.get(Calendar.MONTH) + 1,
        now.get(Calendar.DAY_OF_MONTH),
        now.get(Calendar.YEAR)
    )
    val timeStr = String.format("%02d%02d", now.get(Calendar.HOUR_OF_DAY), now.get(Calendar.MINUTE))
    return "TXN-$dateStr-$timeStr"
}

fun formatDateTime(): String {
    val sdf = SimpleDateFormat("MMMM d, yyyy h:mm a", Locale.US)
    return sdf.format(Date())
}

// ── Payment Dialog ──

@Composable
fun PaymentDialog(
    total: Double,
    onConfirm: (Double) -> Unit,
    onDismiss: () -> Unit
) {
    var amountTendered by remember { mutableStateOf("") }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .widthIn(min = 380.dp, max = 420.dp)
                .wrapContentHeight(),
            shape = RoundedCornerShape(20.dp),
            color = Color(0xFF1A1A1A),
            tonalElevation = 0.dp
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header
                Text(
                    text = "Complete Payment",
                    style = MaterialTheme.typography.titleLarge,
                    color = TextWhite,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Enter amount tendered by customer",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Total display
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = DarkCard,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Total Due",
                            style = MaterialTheme.typography.labelLarge,
                            color = TextMuted
                        )
                        Text(
                            text = "₱${String.format("%.2f", total)}",
                            style = MaterialTheme.typography.headlineMedium,
                            color = OrangeAccent,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Amount Tendered Input
                OutlinedTextField(
                    value = amountTendered,
                    onValueChange = { newVal ->
                        if (newVal.isEmpty() || newVal.matches(Regex("^\\d*\\.?\\d{0,2}$"))) {
                            amountTendered = newVal
                        }
                    },
                    label = { Text("Amount Tendered") },
                    prefix = { Text("₱", color = TextWhite, fontWeight = FontWeight.Bold) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = OrangeAccent,
                        unfocusedBorderColor = InputBorder,
                        cursorColor = OrangeAccent,
                        focusedTextColor = TextWhite,
                        unfocusedTextColor = TextWhite,
                        focusedLabelColor = OrangeAccent,
                        unfocusedLabelColor = TextMuted
                    )
                )

                // Change preview
                val tenderedValue = amountTendered.toDoubleOrNull() ?: 0.0
                if (tenderedValue >= total && tenderedValue > 0) {
                    val change = tenderedValue - total
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Change: ₱${String.format("%.2f", change)}",
                        style = MaterialTheme.typography.titleMedium,
                        color = StatusGreen,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))
                HorizontalDivider(color = DarkBorder, thickness = 0.5.dp)
                Spacer(modifier = Modifier.height(16.dp))

                // Quick amount buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val quickAmounts = listOf(100.0, 200.0, 500.0, 1000.0)
                    quickAmounts.forEach { amount ->
                        OutlinedButton(
                            onClick = { amountTendered = String.format("%.2f", amount) },
                            modifier = Modifier.weight(1f).height(40.dp),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, DarkBorder),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = DarkCard,
                                contentColor = TextWhite
                            )
                        ) {
                            Text(
                                text = "₱${amount.toInt()}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, DarkBorder),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = DarkCard,
                            contentColor = TextWhite
                        )
                    ) {
                        Text("Cancel", fontWeight = FontWeight.SemiBold)
                    }

                    Button(
                        onClick = { onConfirm(tenderedValue) },
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (tenderedValue >= total && tenderedValue > 0) OrangeAccent else OrangeAccent.copy(alpha = 0.4f),
                            contentColor = TextWhite
                        ),
                        enabled = tenderedValue >= total && tenderedValue > 0
                    ) {
                        Text(
                            text = "Confirm",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
