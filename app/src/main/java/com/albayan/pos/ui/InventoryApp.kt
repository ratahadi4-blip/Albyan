package com.albayan.pos.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.albayan.pos.data.ProductDao
import com.albayan.pos.model.Product
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

fun formatIqd(amount: Long): String {
    val formatter = NumberFormat.getNumberInstance(Locale("en", "US"))
    return "${formatter.format(amount)} د.ع"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryApp(
    dao: ProductDao,
    onRequestCameraScan: (onResult: (String) -> Unit) -> Unit
) {
    val scope = rememberCoroutineScope()
    val products by dao.getAllProducts().collectAsState(initial = emptyList())

    var barcodeInput by remember { mutableStateOf("") }
    var nameInput by remember { mutableStateOf("") }
    var priceInput by remember { mutableStateOf("") }
    var quantityInput by remember { mutableStateOf("1") }

    var scannedProduct by remember { mutableStateOf<Product?>(null) }
    var notFoundBarcode by remember { mutableStateOf<String?>(null) }

    suspend fun lookupBarcode(code: String) {
        val cleanCode = code.trim()
        if (cleanCode.isEmpty()) return
        barcodeInput = cleanCode
        val found = dao.getByBarcode(cleanCode)
        if (found != null) {
            scannedProduct = found
            notFoundBarcode = null
            nameInput = found.name
            priceInput = found.priceIqd.toString()
            quantityInput = found.quantity.toString()
        } else {
            scannedProduct = null
            notFoundBarcode = cleanCode
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("البيان - الأسعار والمخزون (د.ع)", fontWeight = FontWeight.Bold) }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = barcodeInput,
                    onValueChange = { barcodeInput = it },
                    label = { Text("رقم الباركود") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                Button(
                    onClick = {
                        onRequestCameraScan { code ->
                            scope.launch { lookupBarcode(code) }
                        }
                    }
                ) {
                    Icon(Icons.Default.QrCodeScanner, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("مسح")
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { scope.launch { lookupBarcode(barcodeInput) } },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("فحص السعر والكمية")
                }
            }

            scannedProduct?.let { item ->
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(item.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text("السعر: ${formatIqd(item.priceIqd)}", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                        Text("الكمية المتوفرة: ${item.quantity}", style = MaterialTheme.typography.titleMedium)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = {
                                    if (item.quantity > 0) {
                                        scope.launch {
                                            val updated = item.copy(quantity = item.quantity - 1)
                                            dao.updateProduct(updated)
                                            scannedProduct = updated
                                        }
                                    }
                                }
                            ) {
                                Text("بيع قطعة (-1)")
                            }
                            OutlinedButton(
                                onClick = {
                                    scope.launch {
                                        val updated = item.copy(quantity = item.quantity + 1)
                                        dao.updateProduct(updated)
                                        scannedProduct = updated
                                    }
                                }
                            ) {
                                Text("إضافة (+1)")
                            }
                        }
                    }
                }
            }

            notFoundBarcode?.let { code ->
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "الباركود ($code) غير مسجل. املأ الاسم والسعر والكمية بالأسفل واضغط حفظ.",
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            HorizontalDivider()
            Text("إضافة / تحديث مادة", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

            OutlinedTextField(
                value = nameInput,
                onValueChange = { nameInput = it },
                label = { Text("اسم المادة") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = priceInput,
                    onValueChange = { priceInput = it.filter { ch -> ch.isDigit() } },
                    label = { Text("السعر (د.ع)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = quantityInput,
                    onValueChange = { quantityInput = it.filter { ch -> ch.isDigit() } },
                    label = { Text("الكمية") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
            }

            Button(
                onClick = {
                    val code = barcodeInput.trim()
                    val name = nameInput.trim()
                    val price = priceInput.toLongOrNull() ?: 0L
                    val qty = quantityInput.toIntOrNull() ?: 0
                    if (code.isNotEmpty() && name.isNotEmpty()) {
                        scope.launch {
                            val product = Product(code, name, price, qty)
                            dao.upsertProduct(product)
                            scannedProduct = product
                            notFoundBarcode = null
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text("حفظ المادة في المخزون")
            }

            Text("المواد المسجلة (${products.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(products, key = { it.barcode }) { product ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(product.name, fontWeight = FontWeight.Bold)
                                Text("باركود: ${product.barcode} | الكمية: ${product.quantity}")
                                Text(formatIqd(product.priceIqd), fontWeight = FontWeight.Bold)
                            }
                            IconButton(
                                onClick = {
                                    scope.launch {
                                        dao.deleteByBarcode(product.barcode)
                                        if (scannedProduct?.barcode == product.barcode) {
                                            scannedProduct = null
                                        }
                                    }
                                }
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = "حذف")
                            }
                        }
                    }
                }
            }
        }
    }
}
