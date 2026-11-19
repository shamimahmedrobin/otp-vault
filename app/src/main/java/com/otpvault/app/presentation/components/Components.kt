package com.otpvault.app.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Mail
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Wallet
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.otpvault.app.R
import com.otpvault.app.core.util.PhoneMask
import com.otpvault.app.core.util.TimeFormat
import com.otpvault.app.domain.model.OtpItem
import com.otpvault.app.domain.model.OtpStatus
import com.otpvault.app.presentation.theme.VaultError
import com.otpvault.app.presentation.theme.VaultSuccess
import com.otpvault.app.presentation.theme.VaultWarning
import kotlinx.coroutines.delay

fun serviceIcon(name: String): ImageVector = when (name) {
    "account_balance" -> Icons.Filled.AccountBalance
    "account_balance_wallet" -> Icons.Filled.Wallet
    "payments" -> Icons.Filled.Payments
    "rocket_launch" -> Icons.Filled.RocketLaunch
    "touch_app" -> Icons.Filled.TouchApp
    "mail" -> Icons.Filled.Mail
    "share" -> Icons.Filled.Share
    "shopping_bag" -> Icons.Filled.ShoppingBag
    "phone_android" -> Icons.Filled.PhoneAndroid
    else -> Icons.Filled.Apps
}

@Composable
fun CountdownText(expiresAt: Long, modifier: Modifier = Modifier) {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(expiresAt) {
        while (true) { now = System.currentTimeMillis(); delay(1000) }
    }
    val remaining = expiresAt - now
    val color = when {
        remaining <= 0 -> VaultError
        remaining <= 60_000 -> VaultWarning
        else -> VaultSuccess
    }
    Text(
        text = stringResource(R.string.expires_in, TimeFormat.countdown(remaining)),
        color = color, style = MaterialTheme.typography.bodyMedium, modifier = modifier
    )
}

@Composable
fun OtpCard(
    item: OtpItem,
    revealed: Boolean,
    onToggleReveal: () -> Unit,
    onCopy: () -> Unit,
    onDelete: () -> Unit,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(44.dp).clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(serviceIcon(item.service.icon), contentDescription = item.service.name,
                        tint = MaterialTheme.colorScheme.primary)
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(item.service.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    if (!item.phone.isNullOrBlank()) {
                        Text(PhoneMask.mask(item.phone), style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                StatusChip(item.status())
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = if (revealed) item.code else "•".repeat(item.code.length.coerceIn(4, 10)),
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onToggleReveal) {
                    Icon(if (revealed) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                        contentDescription = stringResource(if (revealed) R.string.hide else R.string.show))
                }
                IconButton(onClick = onCopy) { Icon(Icons.Filled.ContentCopy, contentDescription = stringResource(R.string.copy)) }
                IconButton(onClick = onDelete) { Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.delete)) }
            }
            CountdownText(item.expiresAt)
        }
    }
}

@Composable
fun StatusChip(status: OtpStatus) {
    val (labelRes, color) = when (status) {
        OtpStatus.ACTIVE -> R.string.active to VaultSuccess
        OtpStatus.USED -> R.string.used to VaultWarning
        OtpStatus.EXPIRED -> R.string.expired to VaultError
    }
    Box(
        Modifier.clip(RoundedCornerShape(50)).background(color.copy(alpha = 0.14f)).padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(stringResource(labelRes), color = color, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
fun EmptyState(title: String, body: String) {
    Column(
        Modifier.fillMaxWidth().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("🛡️", style = MaterialTheme.typography.headlineMedium)
        Text(title, style = MaterialTheme.typography.titleLarge)
        Text(body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun PinDots(filled: Int, total: Int = 6) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        repeat(total) { i ->
            Box(
                Modifier.size(16.dp).clip(CircleShape)
                    .background(if (i < filled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
            )
        }
    }
}
