package com.healthguidelab.keto365

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.res.painterResource
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
    var signingIn by remember { mutableStateOf(false) }
    var loggedIn by remember { mutableStateOf(false) }
    var showWelcome by remember { mutableStateOf(false) }
    var userEmail by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        val alreadyLogged = sessionStore.hasLoggedOnce.first()
        val emailInDb = database.userEmailDao().getEmail()?.email
        loggedIn = alreadyLogged && !emailInDb.isNullOrBlank()
        showWelcome = loggedIn
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
        signingIn = false

        if (result.resultCode != android.app.Activity.RESULT_OK) {
            errorMessage = "Inicio de sesión cancelado."
            return@rememberLauncherForActivityResult
        }

        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        if (!task.isSuccessful) {
            val lastAccount = GoogleSignIn.getLastSignedInAccount(context)
            if (!lastAccount?.email.isNullOrBlank()) {
                onSaveEmail(lastAccount?.email.orEmpty())
                userEmail = lastAccount?.email.orEmpty()
                loggedIn = true
                showWelcome = true
                errorMessage = "Ingresaste con la sesión de Google guardada en el dispositivo."
            } else {
                errorMessage = task.exception?.message ?: "No se pudo iniciar sesión con Google."
            }
            return@rememberLauncherForActivityResult
        }

        val account = task.result
        val accountEmail = account?.email.orEmpty()
        val token = account?.idToken

        fun finishLogin(email: String) {
            if (email.isNotBlank()) {
                onSaveEmail(email)
                userEmail = email
                loggedIn = true
                showWelcome = true
            } else {
                errorMessage = "No se pudo recuperar el correo de Google."
            }
        }

        if (token.isNullOrBlank()) {
            // Fallback: permite continuar con el correo de Google incluso si no hay token.
            finishLogin(accountEmail)
            if (loggedIn) {
                errorMessage = "Ingresaste sin validación Firebase (token ausente)."
            }
            return@rememberLauncherForActivityResult
        }

        val credential = GoogleAuthProvider.getCredential(token, null)
        firebaseAuth.signInWithCredential(credential).addOnCompleteListener { authResult ->
            if (authResult.isSuccessful) {
                finishLogin(authResult.result.user?.email ?: accountEmail)
                if (loggedIn) errorMessage = null
            } else {
                // Si Firebase está mal configurado (p.ej. code 10), continuamos con el correo de Google.
                finishLogin(accountEmail)
                if (loggedIn) {
                    errorMessage = "Ingresaste con Google, pero Firebase no está configurado en este build."
                } else {
                    errorMessage = authResult.exception?.message ?: "Falló la autenticación."
                }
            }
        }
    }

    Scaffold(modifier = Modifier.fillMaxSize()) { padding ->
        when {
            loading || signingIn -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator()
                    if (signingIn) {
                        Spacer(Modifier.height(12.dp))
                        Text("Validando acceso con Google...")
                    }
                }
            }

            loggedIn -> {
                val recipe = remember { recipeFor(LocalDate.now()) }
                if (showWelcome) {
                    WelcomeContent(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding),
                        email = userEmail,
                        onContinue = { showWelcome = false }
                    )
                } else {
                    HomeContent(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding),
                        email = userEmail,
                        recipeTitle = recipe.title,
                        dayOfYear = recipe.dayOfYear
                    )
                }
            }

            else -> {
                LoginContent(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    errorMessage = errorMessage,
                    onGoogleLogin = {
                        signingIn = true
                        errorMessage = null
                        signInLauncher.launch(googleSignInClient.signInIntent)
                    }
                )
            }
        }
    }
}

@Composable
private fun WelcomeContent(
    modifier: Modifier = Modifier,
    email: String,
    onContinue: () -> Unit
) {
    Column(
        modifier = modifier.padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Bienvenido a Keto365",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(12.dp))
        Text(text = "Sesión iniciada correctamente con:")
        Spacer(Modifier.height(8.dp))
        Text(text = email, style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.height(24.dp))
        Button(onClick = onContinue) {
            Text("Continuar")
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
        modifier = modifier.padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            painter = painterResource(id = R.drawable.keto365_logo),
            contentDescription = "Logo Keto365",
            modifier = Modifier.size(180.dp),
            contentScale = ContentScale.Fit
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "HealthGuideApp",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(12.dp))
        Text(text = "Inicia una vez con Google para guardar tu correo.")
        Spacer(Modifier.height(24.dp))
        Button(onClick = onGoogleLogin) {
            Text("Entrar con Google")
        }
        Spacer(Modifier.height(8.dp))
        TextButton(onClick = onGoogleLogin) {
            Text("Reintentar inicio con Google")
        }
        if (!errorMessage.isNullOrBlank()) {
            Spacer(Modifier.height(16.dp))
            Text(text = errorMessage, color = MaterialTheme.colorScheme.error)
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
            text = "¡Hola!",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Text(text = email)
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
    }
}
