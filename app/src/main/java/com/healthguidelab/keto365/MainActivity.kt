package com.healthguidelab.keto365

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import androidx.room.Room
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.auth.api.signin.GoogleSignInStatusCodes
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.healthguidelab.keto365.data.AppDatabase
import com.healthguidelab.keto365.data.SessionStore
import com.healthguidelab.keto365.data.UserEmailEntity
import com.healthguidelab.keto365.data.recipeFor
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate

private enum class LoggedInFlowStep {
    DAY_ANIMATION,
    FREE_RECIPE,
    PREMIUM_PREVIEW,
    SUBSCRIPTION_PLANS
}

private val BrandGreen = Color(0xFF0F2418)
private val BrandGray = Color(0xFF6E6565)
private val BrandBackground = Color(0xFFFCF4F4)

private val AppColorScheme = lightColorScheme(
    primary = BrandGreen,
    onPrimary = Color.White,
    background = BrandBackground,
    onBackground = BrandGreen,
    surface = Color.White,
    onSurface = BrandGreen,
    surfaceVariant = Color(0xFFF3ECEC),
    onSurfaceVariant = BrandGray
)

@Composable
private fun Keto365Theme(content: @Composable () -> Unit) {
    val provider = GoogleFont.Provider(
        providerAuthority = "com.google.android.gms.fonts",
        providerPackage = "com.google.android.gms",
        certificates = androidx.compose.ui.R.array.com_google_android_gms_fonts_certs
    )
    val poppins = FontFamily(
        Font(googleFont = GoogleFont("Poppins"), fontProvider = provider)
    )

    val typography = Typography(
        bodyLarge = TextStyle(fontFamily = poppins),
        bodyMedium = TextStyle(fontFamily = poppins),
        titleLarge = TextStyle(fontFamily = poppins, fontWeight = FontWeight.SemiBold),
        titleMedium = TextStyle(fontFamily = poppins, fontWeight = FontWeight.Medium),
        headlineSmall = TextStyle(fontFamily = poppins, fontWeight = FontWeight.Bold),
        displayMedium = TextStyle(fontFamily = poppins, fontWeight = FontWeight.Bold)
    )

    MaterialTheme(colorScheme = AppColorScheme, typography = typography, content = content)
}

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
            Keto365Theme {
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

private const val TAG = "Keto365Login"

private fun googleSignInErrorMessage(exception: Exception?): String {
    val apiException = exception as? ApiException
    val code = apiException?.statusCode

    return when (code) {
        GoogleSignInStatusCodes.SIGN_IN_CANCELLED -> "Sign-in was canceled."
        GoogleSignInStatusCodes.SIGN_IN_REQUIRED -> "Choose a Google account to continue."
        GoogleSignInStatusCodes.NETWORK_ERROR -> "No connection. Check your internet and try again."
        GoogleSignInStatusCodes.DEVELOPER_ERROR -> "Invalid Google Sign-In configuration (SHA-1 or client ID)."
        GoogleSignInStatusCodes.INTERNAL_ERROR -> "Google Sign-In internal error. Try again."
        else -> exception?.message ?: "Could not sign in with Google."
    }
}

private fun firebaseAuthErrorMessage(exception: Exception?): String {
    val firebaseCode = (exception as? FirebaseAuthException)?.errorCode

    return when (firebaseCode) {
        "ERROR_INVALID_CREDENTIAL" -> "Invalid credential. Verify SHA-1/SHA-256 and google-services.json."
        "ERROR_NETWORK_REQUEST_FAILED" -> "Network error while validating with Firebase."
        else -> exception?.message ?: "Firebase authentication failed."
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
    var userEmail by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var flowStep by remember { mutableStateOf(LoggedInFlowStep.DAY_ANIMATION) }

    LaunchedEffect(Unit) {
        val alreadyLogged = sessionStore.hasLoggedOnce.first()
        val emailInDb = database.userEmailDao().getEmail()?.email
        loggedIn = alreadyLogged && !emailInDb.isNullOrBlank()
        userEmail = emailInDb.orEmpty()
        if (loggedIn) flowStep = LoggedInFlowStep.DAY_ANIMATION
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

        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        if (!task.isSuccessful) {
            val apiException = task.exception as? ApiException
            val wasCancelled =
                result.resultCode != android.app.Activity.RESULT_OK &&
                    apiException?.statusCode == GoogleSignInStatusCodes.SIGN_IN_CANCELLED

            if (wasCancelled) {
                errorMessage = "Sign-in was canceled."
                return@rememberLauncherForActivityResult
            }

            val lastAccount = GoogleSignIn.getLastSignedInAccount(context)
            if (!lastAccount?.email.isNullOrBlank()) {
                onSaveEmail(lastAccount?.email.orEmpty())
                userEmail = lastAccount?.email.orEmpty()
                loggedIn = true
                flowStep = LoggedInFlowStep.DAY_ANIMATION
                errorMessage = "You signed in with the Google session saved on this device."
            } else {
                val friendlyError = googleSignInErrorMessage(task.exception)
                Log.w(TAG, "Google Sign-In failed", task.exception)
                errorMessage = friendlyError
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
                flowStep = LoggedInFlowStep.DAY_ANIMATION
            } else {
                errorMessage = "Could not retrieve Google email."
            }
        }

        if (token.isNullOrBlank()) {
            errorMessage = "No Google token was received. Check default_web_client_id and SHA in Firebase."
            return@rememberLauncherForActivityResult
        }

        val credential = GoogleAuthProvider.getCredential(token, null)
        firebaseAuth.signInWithCredential(credential).addOnCompleteListener { authResult ->
            if (authResult.isSuccessful) {
                finishLogin(authResult.result.user?.email ?: accountEmail)
                if (loggedIn) errorMessage = null
            } else {
                Log.w(TAG, "Firebase auth failed", authResult.exception)
                errorMessage = firebaseAuthErrorMessage(authResult.exception)
            }
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets(0),
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        when {
            loading || signingIn -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    if (signingIn) {
                        Spacer(Modifier.height(12.dp))
                        Text("Validating access with Google...")
                    }
                }
            }

            loggedIn -> {
                val recipe = remember { recipeFor(LocalDate.now()) }
                when (flowStep) {
                    LoggedInFlowStep.DAY_ANIMATION -> DayAnimationContent(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding),
                        dayOfYear = recipe.dayOfYear,
                        onAnimationDone = { flowStep = LoggedInFlowStep.FREE_RECIPE }
                    )

                    LoggedInFlowStep.FREE_RECIPE -> FreeRecipeContent(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding),
                        email = userEmail,
                        recipeTitle = recipe.title,
                        dayOfYear = recipe.dayOfYear,
                        onContinue = { flowStep = LoggedInFlowStep.PREMIUM_PREVIEW }
                    )

                    LoggedInFlowStep.PREMIUM_PREVIEW -> PremiumPreparationContent(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding),
                        onUnlockPaidAccess = { flowStep = LoggedInFlowStep.SUBSCRIPTION_PLANS }
                    )

                    LoggedInFlowStep.SUBSCRIPTION_PLANS -> SubscriptionPlansContent(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding),
                        onBack = { flowStep = LoggedInFlowStep.PREMIUM_PREVIEW }
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
                        runCatching { signInLauncher.launch(googleSignInClient.signInIntent) }
                            .onFailure { launchError ->
                                Log.e(TAG, "Could not open Google Sign-In", launchError)
                                signingIn = false
                                errorMessage = "Could not open Google Sign-In on this device."
                            }
                    }
                )
            }
        }
    }
}

@Composable
private fun DayAnimationContent(
    modifier: Modifier = Modifier,
    dayOfYear: Int,
    onAnimationDone: () -> Unit
) {
    var startAnimation by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0.7f,
        animationSpec = tween(durationMillis = 700),
        label = "dayScale"
    )
    val alpha by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0f,
        animationSpec = tween(durationMillis = 700),
        label = "dayAlpha"
    )

    LaunchedEffect(Unit) {
        startAnimation = true
        delay(1800)
        onAnimationDone()
    }

    Column(
        modifier = modifier.padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Your free recipe of the day",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(Modifier.height(24.dp))
        Text(
            text = "DAY $dayOfYear",
            style = MaterialTheme.typography.displayMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .scale(scale)
                .alpha(alpha),
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = "Preparing your recipe for today...",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun FreeRecipeContent(
    modifier: Modifier = Modifier,
    email: String,
    recipeTitle: String,
    dayOfYear: Int,
    onContinue: () -> Unit
) {
    Column(
        modifier = modifier
            .padding(24.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Image(
            painter = painterResource(id = R.drawable.keto365_logo),
            contentDescription = "Keto365 logo",
            modifier = Modifier.size(120.dp),
            contentScale = ContentScale.Fit
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = "Today's free recipe",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Signed in as $email",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "DAY $dayOfYear",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(Modifier.height(16.dp))
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Image(
                    painter = painterResource(id = R.drawable.free_recipe_result),
                    contentDescription = "Final recipe result",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(190.dp),
                    contentScale = ContentScale.Crop
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    text = recipeTitle,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Free recipe unlocked today. Enjoy it and continue to view the full healthy preparation section.",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        Spacer(Modifier.height(20.dp))
        ElevatedButton(
            onClick = onContinue,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Continue")
        }
    }
}

@Composable
private fun PremiumPreparationContent(
    modifier: Modifier = Modifier,
    onUnlockPaidAccess: () -> Unit
) {
    Column(
        modifier = modifier
            .padding(24.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = "Healthy recipe preparation",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = "Here you will access full plans, cooking techniques, and step-by-step premium recipes.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(20.dp))
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Premium Content",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "• Guided video preparations\n• Personalized weekly menus\n• Healthy substitutions by goal",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(Modifier.height(12.dp))
                ElevatedButton(
                    onClick = onUnlockPaidAccess,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Unlock paid access")
                }
            }
        }
    }
}

@Composable
private fun SubscriptionPlansContent(
    modifier: Modifier = Modifier,
    onBack: () -> Unit
) {
    Column(
        modifier = modifier
            .padding(24.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = "Subscription Plans",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Choose a plan to unlock premium recipes, advanced meal prep, and exclusive wellness content.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(16.dp))
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Monthly Plan", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(6.dp))
                Text("$4.99 / month", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(6.dp))
                Text("• Cancel anytime\n• New premium recipes every week")
            }
        }

        Spacer(Modifier.height(12.dp))
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Yearly Plan", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(6.dp))
                Text("$39.99 / year", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(6.dp))
                Text("• Save over 30%\n• Includes all premium recipe collections")
            }
        }

        Spacer(Modifier.height(20.dp))
        ElevatedButton(
            onClick = onBack,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Back")
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
        Image(
            painter = painterResource(id = R.drawable.keto365_logo),
            contentDescription = "Keto365 logo",
            modifier = Modifier.size(120.dp),
            contentScale = ContentScale.Fit
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = "Sign in to continue",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Use your Google account to save your progress and view your daily recipe.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(24.dp))
        ElevatedButton(
            onClick = onGoogleLogin,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Continue with Google")
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
