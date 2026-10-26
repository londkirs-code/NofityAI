package com.example.ui.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ApiKeyEntity
import com.example.data.model.ProviderType

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AddKeyDialog(
    existingKey: ApiKeyEntity? = null,
    initialDecryptedKey: String = "",
    nextPriority: Int = 1,
    onDismiss: () -> Unit,
    onSave: (provider: ProviderType, name: String, rawKey: String, model: String, priority: Int) -> Unit
) {
    var selectedProvider by remember { mutableStateOf(existingKey?.provider ?: ProviderType.GEMINI) }
    var name by remember {
        mutableStateOf(
            existingKey?.name ?: "${selectedProvider.displayName} #${nextPriority}"
        )
    }
    var apiKeyText by remember { mutableStateOf(initialDecryptedKey) }
    var isKeyVisible by remember { mutableStateOf(false) }
    var selectedModel by remember {
        mutableStateOf(existingKey?.selectedModel ?: selectedProvider.defaultModel)
    }
    var isCustomModel by remember { mutableStateOf(false) }
    var customModelText by remember { mutableStateOf("") }
    var priority by remember { mutableIntStateOf(existingKey?.priority ?: nextPriority) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Key,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = if (existingKey == null) "הוספת מפתח API מאובטח" else "עריכת מפתח API",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Security Notice Banner
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Security,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "המפתח מוצפן מיידית בחומרת המכשיר (AES-256 GCM) ולא יועבר לשום גורם חיצוני.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Provider Selection
                    Text(
                        text = "בחר ספק AI:",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        ProviderType.values().forEach { provider ->
                            val isSelected = selectedProvider == provider
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    selectedProvider = provider
                                    selectedModel = provider.defaultModel
                                    if (name.isBlank() || name.contains("Google") || name.contains("Claude") || name.contains("OpenAI") || name.contains("Grok") || name.contains("OpenRouter") || name.contains("Hugging")) {
                                        name = "${provider.displayName} #${priority}"
                                    }
                                },
                                label = { Text(provider.displayName, fontSize = 12.sp) },
                                leadingIcon = if (isSelected) {
                                    { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp)) }
                                } else null,
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            )
                        }
                    }

                    // Key Name
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("כינוי למפתח") },
                        placeholder = { Text("למשל: מפתח עבודה ראשי") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("key_name_input")
                    )

                    // API Key input
                    OutlinedTextField(
                        value = apiKeyText,
                        onValueChange = {
                            apiKeyText = it.trim()
                            errorMessage = null
                        },
                        label = { Text("מפתח API") },
                        placeholder = { Text(selectedProvider.keyHint) },
                        visualTransformation = if (isKeyVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { isKeyVisible = !isKeyVisible }) {
                                Icon(
                                    imageVector = if (isKeyVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = if (isKeyVisible) "הסתר מפתח" else "הצג מפתח"
                                )
                            }
                        },
                        singleLine = true,
                        isError = errorMessage != null,
                        supportingText = {
                            if (errorMessage != null) {
                                Text(errorMessage!!, color = MaterialTheme.colorScheme.error)
                            } else {
                                Text("קישור להנפקה: ${selectedProvider.documentationUrl}", fontSize = 11.sp)
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("api_key_input")
                    )

                    // Model Selection
                    Text(
                        text = "בחר מודל לשימוש:",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        selectedProvider.suggestedModels.forEach { modelName ->
                            val isSelected = selectedModel == modelName && !isCustomModel
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    selectedModel = modelName
                                    isCustomModel = false
                                },
                                label = { Text(modelName, fontSize = 11.sp) }
                            )
                        }
                    }

                    // Priority selector
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "סדר עדיפות בניתוב (1 = הכי גבוה):",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "במקרה של עומס או חריגת מכסה, המערכת תדלג למפתח הבא.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = { if (priority > 1) priority-- },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(Icons.Default.Remove, contentDescription = "הורד עדיפות")
                            }
                            Text(
                                text = priority.toString(),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp)
                            )
                            IconButton(
                                onClick = { priority++ },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "העלה עדיפות")
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (apiKeyText.isBlank()) {
                            errorMessage = "נא להזין מפתח API תקין"
                            return@Button
                        }
                        val finalModel = if (isCustomModel && customModelText.isNotBlank()) {
                            customModelText.trim()
                        } else {
                            selectedModel
                        }
                        onSave(selectedProvider, name.trim(), apiKeyText.trim(), finalModel, priority)
                    },
                    modifier = Modifier.testTag("save_api_key_btn"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text("שמור מפתח מאובטח", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text("ביטול")
                }
            }
        )
    }
}
