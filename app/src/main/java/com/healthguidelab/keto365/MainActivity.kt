package com.healthguidelab.keto365

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import androidx.room.Room
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.healthguidelab.keto365.data.AppDatabase
import com.healthguidelab.keto365.data.SessionStore
import com.healthguidelab.keto365.data.UserEmailEntity
import com.healthguidelab.keto365.data.recipeFor
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val database = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "keto365_db"
        ).build()

        val sessionStore = SessionStore(applicationContext)

        setContent {
            MaterialTheme {
                Keto365App(
                    sessionStore = sessionStore,
                    database = database,
                    onSaveEmail = { email ->
                        lifecycleScope.launch {
                            database.userEmailDao().upsertEmail(UserEmailEntity(email = email))
                            sessionStore.setLoggedOnce(true)
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun Keto365App(
    sessionStore: SessionStore,
    database: AppDatabase,
    onSaveEmail: (String) -> Unit
) {
    val context = LocalContext.current
    val firebaseAuth = remember { Firebase.auth }
    var loading by remember { mutableStateOf(true) }
    var loggedIn by remember { mutableStateOf(false) }
    var userEmail by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        val alreadyLogged = sessionStore.hasLoggedOnce.first()
        val emailInDb = database.userEmailDao().getEmail()?.email
        loggedIn = alreadyLogged && !emailInDb.isNullOrBlank()
        userEmail = emailInDb.orEmpty()
        loading = false
    }

    val googleSignInClient = remember {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(context.getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        GoogleSignIn.getClient(context, gso)
    }

    val signInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        if (task.isSuccessful) {
            val account = task.result
            val token = account.idToken
            if (!token.isNullOrBlank()) {
                val credential = GoogleAuthProvider.getCredential(token, null)
                firebaseAuth.signInWithCredential(credential).addOnCompleteListener { authResult ->
                    if (authResult.isSuccessful) {
                        val email = authResult.result.user?.email.orEmpty()
                        if (email.isNotBlank()) {
                            onSaveEmail(email)
                            userEmail = email
                            loggedIn = true
                            errorMessage = null
                        } else {
                            errorMessage = "No se pudo recuperar el correo de Google."
                        }
                    } else {
                        errorMessage = authResult.exception?.message ?: "Falló la autenticación."
                    }
                }
            } else {
                errorMessage = "No se recibió token de Google."
            }
        } else {
            errorMessage = task.exception?.message ?: "No se pudo iniciar sesión con Google."
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets(0)
    ) { padding ->
        when {
            loading -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator()
                }
            }

            loggedIn -> {
                val recipe = remember { recipeFor(LocalDate.now()) }
                HomeContent(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    email = userEmail,
                    recipeTitle = recipe.title,
                    dayOfYear = recipe.dayOfYear
                )
            }

            else -> {
                LoginContent(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    errorMessage = errorMessage,
                    onGoogleLogin = {
                        signInLauncher.launch(googleSignInClient.signInIntent)
                    }
                )
            }
        }
    }
}

@Composable
private fun LoginContent(
    modifier: Modifier = Modifier,
    errorMessage: String?,
    onGoogleLogin: () -> Unit
) {
    Column(
        modifier = modifier.padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        ElevatedCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "Keto365",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Planifica tu alimentación keto diaria de forma simple.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(16.dp))
                HorizontalDivider()
                Spacer(Modifier.height(16.dp))
                Text(
                    text = "Inicia una sola vez con Google para guardar tu correo y continuar rápido en tus siguientes visitas.",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(Modifier.height(20.dp))
                ElevatedButton(
                    onClick = onGoogleLogin,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Entrar con Google")
                }
            }
        }

        if (!errorMessage.isNullOrBlank()) {
            Spacer(Modifier.height(16.dp))
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.errorContainer,
                shape = MaterialTheme.shapes.medium
            ) {
                Text(
                    text = errorMessage,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.padding(12.dp)
                )
            }
        }
    }
}

@Composable
private fun HomeContent(
    modifier: Modifier = Modifier,
    email: String,
    recipeTitle: String,
    dayOfYear: Int
) {
    Column(modifier = modifier.padding(24.dp)) {
        Text(
            text = "¡Bienvenido de nuevo!",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = email,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(24.dp))

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Receta keto del día $dayOfYear",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(8.dp))
                Text(text = recipeTitle, style = MaterialTheme.typography.bodyLarge)
            }
        }

        Spacer(Modifier.height(16.dp))
        Text(
            text = "Tip: guarda esta receta en tus favoritos para repetirla cuando quieras.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary
        )
    }
}
