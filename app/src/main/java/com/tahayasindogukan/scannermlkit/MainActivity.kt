package com.tahayasindogukan.scannermlkit

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.content.ContextCompat
import coil3.compose.AsyncImage
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions.RESULT_FORMAT_JPEG
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions.RESULT_FORMAT_PDF
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions.SCANNER_MODE_FULL
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult
import com.tahayasindogukan.scannermlkit.ui.theme.ScannerMLKitTheme
import java.io.File
import java.io.FileOutputStream

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)



        enableEdgeToEdge()
        setContent {
            ScannerMLKitTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Greeting(
                        modifier = Modifier.padding(innerPadding)
                    )

                }
            }
        }
    }

    @Composable
    fun StoragePermissionScreen() {
        // İzin durumu için hatırlayıcı
        val context = LocalContext.current

        // API 33 ve sonrası için medya izinleri isteniyor
        val permissionLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestMultiplePermissions(),
            onResult = { permissions ->
                val granted = permissions.entries.all { it.value }
                if (granted) {
                    // İzin verildiyse işlem yapılır
                    Toast.makeText(context, "İzin verildi!", Toast.LENGTH_SHORT).show()
                    // Depolama işlemlerini burada başlatabilirsiniz
                } else {
                    // İzin verilmediyse, kullanıcıya bilgi verilir
                    Toast.makeText(context, "İzin reddedildi", Toast.LENGTH_SHORT).show()
                }
            }
        )

        val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13 ve sonrası için medya izinleri
            arrayOf(
                Manifest.permission.READ_MEDIA_IMAGES,
                Manifest.permission.READ_MEDIA_VIDEO,
                Manifest.permission.READ_MEDIA_AUDIO
            )
        } else {
            // Android 12 ve öncesi için depolama izni
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        }

        // İzin kontrolü yapılıp, gerekirse istek gönderilir
        LaunchedEffect(key1 = Unit) {
            when {
                permissions.all {
                    ContextCompat.checkSelfPermission(
                        context,
                        it
                    ) == PackageManager.PERMISSION_GRANTED
                } -> {
                    // İzinler zaten verilmişse
                    Toast.makeText(context, "İzinler zaten verilmiş!", Toast.LENGTH_SHORT).show()
                    // Depolama işlemlerini burada başlatabilirsiniz
                }

                else -> {
                    // İzinler verilmemişse, izin isteği başlatılır
                    permissionLauncher.launch(permissions)
                }
            }
        }

    }


    @Composable
    fun Greeting(modifier: Modifier = Modifier) {

        StoragePermissionScreen()

        val options = GmsDocumentScannerOptions.Builder()
            .setScannerMode(SCANNER_MODE_FULL)
            .setGalleryImportAllowed(true)
            .setPageLimit(5)
            .setResultFormats(RESULT_FORMAT_JPEG, RESULT_FORMAT_PDF)
            .build()

        val scanner = GmsDocumentScanning.getClient(options)

        var imageUris by remember {
            mutableStateOf<List<Uri>>(emptyList())
        }

        val scannerLauncher =
            rememberLauncherForActivityResult(
                contract = ActivityResultContracts.StartIntentSenderForResult(),
                onResult = {
                    if (it.resultCode == RESULT_OK) {
                        val result =
                            GmsDocumentScanningResult.fromActivityResultIntent(it.data)
                        imageUris = result?.pages?.map { it.imageUri } ?: emptyList()

                        result?.pdf?.let { pdf ->
                            val fos = FileOutputStream(File(filesDir, "scan.pdf"))
                            contentResolver.openInputStream(pdf.uri)?.use {
                                it.copyTo(fos)
                            }
                        }
                    }

                }
            )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            imageUris.forEach { uri ->
                AsyncImage(
                    model = uri,
                    contentDescription = null,
                    contentScale = ContentScale.FillWidth,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            Button(onClick = {
                scanner.getStartScanIntent(this@MainActivity)
                    .addOnSuccessListener {
                        scannerLauncher.launch(
                            IntentSenderRequest.Builder(it).build()
                        )

                    }
                    .addOnFailureListener {
                        Toast.makeText(
                            applicationContext,
                            it.message,
                            Toast.LENGTH_LONG
                        ).show()

                    }
            }) {
                Text(text = "Scan PDF")
            }
        }
    }

}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    ScannerMLKitTheme {

    }
}