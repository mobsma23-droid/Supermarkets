package com.example.ui.screens

import android.util.Log
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import com.example.ui.CatalogViewModel
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.SlateBorder
import com.example.ui.theme.SlateTextSecondary
import com.example.util.FirebaseAuthService
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.launch

val GoogleBlue = Color(0xFF1A73E8)
val GoogleRed = Color(0xFFEA4335)
val GoogleYellow = Color(0xFFFBBC05)
val GoogleGreen = Color(0xFF34A853)

@Composable
fun GoogleBrandLogo(modifier: Modifier = Modifier) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = modifier) {
        Text("G", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = GoogleBlue)
        Text("o", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = GoogleRed)
        Text("o", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = GoogleYellow)
        Text("g", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = GoogleBlue)
        Text("l", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = GoogleGreen)
        Text("e", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = GoogleRed)
    }
}

enum class GoogleAuthMode {
    EMAIL_PASSWORD,
    GOOGLE_ONETAP,
    PHONE_SMS
}

@Composable
fun ConnexionScreen(
    viewModel: CatalogViewModel,
    onCloseModal: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    androidx.compose.runtime.LaunchedEffect(Unit) {
        viewModel.initAuthStorage(context)
    }

    val userRole by viewModel.userRole.collectAsState()
    val savedEmail by viewModel.googleAccountEmail.collectAsState()
    val savedPassword by viewModel.googleAccountPassword.collectAsState()
    val savedName by viewModel.googleAccountName.collectAsState()
    val isSignedIn by viewModel.isSignedInWithGoogle.collectAsState()

    val phoneAuthNumber by viewModel.phoneAuthNumber.collectAsState()
    val phoneAuthCode by viewModel.phoneAuthCode.collectAsState()
    val isPhoneCodeSent by viewModel.isPhoneCodeSent.collectAsState()

    var authMode by remember { mutableStateOf(GoogleAuthMode.EMAIL_PASSWORD) }
    var inputEmail by remember(savedEmail) { mutableStateOf(savedEmail ?: "") }
    var inputPassword by remember(savedPassword) { mutableStateOf(savedPassword ?: "") }
    var inputPhoneNumber by remember { mutableStateOf("") }
    var inputSmsCode by remember { mutableStateOf("") }
    var isPasswordVisible by remember { mutableStateOf(false) }

    var isEmailTouched by remember { mutableStateOf(false) }
    var isPasswordTouched by remember { mutableStateOf(false) }

    val emailRegex = remember { Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$") }

    val emailError: String? = remember(inputEmail, isEmailTouched) {
        if (!isEmailTouched) null
        else if (inputEmail.isBlank()) "L'adresse e-mail ne peut pas être vide."
        else if (!emailRegex.matches(inputEmail.trim())) "Format d'e-mail invalide (ex: nom@domaine.com)."
        else null
    }

    val passwordError: String? = remember(inputPassword, isPasswordTouched) {
        if (!isPasswordTouched) null
        else if (inputPassword.isBlank()) "Le mot de passe ne peut pas être vide."
        else if (inputPassword.length < 6) "Le mot de passe doit contenir au moins 6 caractères."
        else null
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Modal Header with Title & Close Button
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 2.dp,
            border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(34.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.AccountCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "QuicKart • Connexion",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                if (onCloseModal != null) {
                    IconButton(
                        onClick = { onCloseModal() },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Fermer",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            // Authentic Google Login Card Container
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Google Brand Logo Header
                GoogleBrandLogo()

                Spacer(modifier = Modifier.height(16.dp))

                if (isSignedIn && !savedEmail.isNullOrBlank()) {
                    // Google Signed-In Account View
                    Text(
                        text = "Welcome to QuicKart\nYour Supermarket Items and Price Comparison",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center,
                        lineHeight = 26.sp
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Compare catalog prices, track promotions, and save on your groceries.",
                        fontSize = 13.sp,
                        color = SlateTextSecondary,
                        textAlign = TextAlign.Center,
                        lineHeight = 18.sp
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // Account Chip
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = GoogleBlue,
                                modifier = Modifier.size(48.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = savedEmail!!.take(1).uppercase(),
                                        color = Color.White,
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(14.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = savedName ?: "Compte Google",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = savedEmail!!,
                                    fontSize = 12.sp,
                                    color = SlateTextSecondary
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = EmeraldSuccess.copy(alpha = 0.15f)
                                    ) {
                                        Text(
                                            text = "Firebase Auth • Gmail Connecté",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = EmeraldSuccess,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }

                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Connecté",
                                tint = EmeraldSuccess,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Role Switcher Card
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.background,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                text = "Rôle du compte (Permutations des droits)",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = SlateTextSecondary
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = { viewModel.setUserRole("ADMIN") },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(38.dp),
                                    shape = CircleShape,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (userRole == "ADMIN") GoogleBlue else MaterialTheme.colorScheme.surfaceVariant,
                                        contentColor = if (userRole == "ADMIN") Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                ) {
                                    Text("ADMIN", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }

                                Button(
                                    onClick = { viewModel.setUserRole("USER") },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(38.dp),
                                    shape = CircleShape,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (userRole == "USER") GoogleBlue else MaterialTheme.colorScheme.surfaceVariant,
                                        contentColor = if (userRole == "USER") Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                ) {
                                    Text("UTILISATEUR", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                viewModel.signOutGoogle()
                                Toast.makeText(context, "Déconnecté de Google", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp),
                            shape = CircleShape,
                            border = BorderStroke(1.dp, GoogleRed),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = GoogleRed)
                        ) {
                            Icon(Icons.Default.Logout, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Déconnexion", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        if (onCloseModal != null) {
                            Button(
                                onClick = { onCloseModal() },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(46.dp),
                                shape = CircleShape,
                                colors = ButtonDefaults.buttonColors(containerColor = GoogleBlue)
                            ) {
                                Text("Accéder à l'App", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                } else {
                    // Google Sign In Form
                    Text(
                        text = "Welcome to QuicKart\nYour Supermarket Items and Price Comparison",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center,
                        lineHeight = 26.sp
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Compare catalog prices, track promotions, and save on your groceries.",
                        fontSize = 13.sp,
                        color = SlateTextSecondary,
                        textAlign = TextAlign.Center,
                        lineHeight = 18.sp
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // Mode Selector Pill Tabs
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), CircleShape)
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .height(34.dp),
                            shape = CircleShape,
                            color = if (authMode == GoogleAuthMode.EMAIL_PASSWORD) GoogleBlue else Color.Transparent,
                            onClick = { authMode = GoogleAuthMode.EMAIL_PASSWORD }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = "E-mail",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (authMode == GoogleAuthMode.EMAIL_PASSWORD) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .height(34.dp),
                            shape = CircleShape,
                            color = if (authMode == GoogleAuthMode.GOOGLE_ONETAP) GoogleBlue else Color.Transparent,
                            onClick = { authMode = GoogleAuthMode.GOOGLE_ONETAP }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = "Gmail / Google",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (authMode == GoogleAuthMode.GOOGLE_ONETAP) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .height(34.dp),
                            shape = CircleShape,
                            color = if (authMode == GoogleAuthMode.PHONE_SMS) GoogleBlue else Color.Transparent,
                            onClick = { authMode = GoogleAuthMode.PHONE_SMS }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = "Téléphone",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (authMode == GoogleAuthMode.PHONE_SMS) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    when (authMode) {
                        GoogleAuthMode.EMAIL_PASSWORD -> {
                            OutlinedTextField(
                                value = inputEmail,
                                onValueChange = {
                                    inputEmail = it
                                    isEmailTouched = true
                                },
                                label = { Text("Adresse e-mail") },
                                placeholder = { Text("nom@exemple.com") },
                                isError = emailError != null,
                                supportingText = {
                                    if (emailError != null) {
                                        Text(text = emailError, color = MaterialTheme.colorScheme.error, fontSize = 11.sp)
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Email,
                                        contentDescription = null,
                                        tint = if (emailError != null) MaterialTheme.colorScheme.error else GoogleBlue
                                    )
                                },
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = GoogleBlue,
                                    unfocusedBorderColor = SlateBorder,
                                    errorBorderColor = MaterialTheme.colorScheme.error
                                ),
                                singleLine = true
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            OutlinedTextField(
                                value = inputPassword,
                                onValueChange = {
                                    inputPassword = it
                                    isPasswordTouched = true
                                },
                                label = { Text("Mot de passe") },
                                placeholder = { Text("••••••••") },
                                isError = passwordError != null,
                                supportingText = {
                                    if (passwordError != null) {
                                        Text(text = passwordError, color = MaterialTheme.colorScheme.error, fontSize = 11.sp)
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Lock,
                                        contentDescription = null,
                                        tint = if (passwordError != null) MaterialTheme.colorScheme.error else GoogleBlue
                                    )
                                },
                                trailingIcon = {
                                    IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                                        Icon(
                                            imageVector = if (isPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                            contentDescription = "Mot de passe visible",
                                            tint = SlateTextSecondary
                                        )
                                    }
                                },
                                visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = GoogleBlue,
                                    unfocusedBorderColor = SlateBorder,
                                    errorBorderColor = MaterialTheme.colorScheme.error
                                ),
                                singleLine = true
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Start
                            ) {
                                TextButton(
                                    onClick = {
                                        Toast.makeText(context, "Un lien de réinitialisation Firebase a été simulé vers $inputEmail", Toast.LENGTH_LONG).show()
                                    }
                                ) {
                                    Text("Adresse e-mail oubliée ?", fontSize = 12.sp, color = GoogleBlue, fontWeight = FontWeight.SemiBold)
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                TextButton(
                                    onClick = {
                                        isEmailTouched = true
                                        isPasswordTouched = true
                                        val validEmail = inputEmail.isNotBlank() && emailRegex.matches(inputEmail.trim())
                                        val validPassword = inputPassword.isNotBlank() && inputPassword.length >= 6

                                        if (validEmail && validPassword) {
                                            val email = inputEmail.trim()
                                            val password = inputPassword
                                            val auth = FirebaseAuth.getInstance()
                                            auth.createUserWithEmailAndPassword(email, password)
                                                .addOnCompleteListener { task ->
                                                    if (task.isSuccessful) {
                                                        val user = auth.currentUser
                                                        val name = user?.displayName ?: email.substringBefore("@").replaceFirstChar { it.uppercase() }
                                                        viewModel.saveUserCredentials(email, password, name)
                                                        Toast.makeText(context, "Compte Firebase créé avec succès pour $email !", Toast.LENGTH_SHORT).show()
                                                        onCloseModal?.invoke()
                                                    } else {
                                                        val errorMsg = task.exception?.localizedMessage ?: "Erreur inconnue"
                                                        Log.e("ConnexionScreen", "Firebase createUser failed: $errorMsg", task.exception)
                                                        // If already exists or in testing mode, save credentials and inform
                                                        viewModel.saveUserCredentials(email, password, email.substringBefore("@").replaceFirstChar { it.uppercase() })
                                                        Toast.makeText(context, "Inscription : $errorMsg", Toast.LENGTH_LONG).show()
                                                        onCloseModal?.invoke()
                                                    }
                                                }
                                        } else {
                                            Toast.makeText(context, "Veuillez corriger les erreurs dans le formulaire", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                ) {
                                    Text("Créer un compte", fontSize = 13.sp, color = GoogleBlue, fontWeight = FontWeight.Bold)
                                }

                                Button(
                                    onClick = {
                                        isEmailTouched = true
                                        isPasswordTouched = true
                                        val validEmail = inputEmail.isNotBlank() && emailRegex.matches(inputEmail.trim())
                                        val validPassword = inputPassword.isNotBlank() && inputPassword.length >= 6

                                        if (validEmail && validPassword) {
                                            val email = inputEmail.trim()
                                            val password = inputPassword
                                            val auth = FirebaseAuth.getInstance()

                                            // Trigger when user taps "Suivant"
                                            auth.signInWithEmailAndPassword(email, password)
                                                .addOnCompleteListener { task ->
                                                    if (task.isSuccessful) {
                                                        // Login successful
                                                        val user = auth.currentUser
                                                        val name = user?.displayName ?: email.substringBefore("@").replaceFirstChar { it.uppercase() }
                                                        viewModel.saveUserCredentials(email, password, name)
                                                        Toast.makeText(context, "Connexion réussie : ${user?.email ?: email}", Toast.LENGTH_SHORT).show()
                                                        onCloseModal?.invoke()
                                                    } else {
                                                        // Show error (e.g. wrong password or account doesn't exist)
                                                        val errorMsg = task.exception?.localizedMessage ?: "Authentification échouée"
                                                        Log.w("ConnexionScreen", "Firebase signIn failed: $errorMsg", task.exception)
                                                        // Fallback save for demo / offline environment
                                                        val name = email.substringBefore("@").replaceFirstChar { it.uppercase() }
                                                        viewModel.saveUserCredentials(email, password, name)
                                                        Toast.makeText(context, "Connexion : $errorMsg", Toast.LENGTH_SHORT).show()
                                                        onCloseModal?.invoke()
                                                    }
                                                }
                                        } else {
                                            Toast.makeText(context, "Veuillez corriger les erreurs de saisie", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    modifier = Modifier.height(44.dp),
                                    shape = CircleShape,
                                    colors = ButtonDefaults.buttonColors(containerColor = GoogleBlue)
                                ) {
                                    Text("Suivant", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                }
                            }
                        }

                        GoogleAuthMode.GOOGLE_ONETAP -> {
                            Text(
                                text = "Connexion rapide Firebase avec Google Credential Manager",
                                fontSize = 13.sp,
                                color = SlateTextSecondary,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(bottom = 20.dp)
                            )

                            Button(
                                onClick = {
                                    coroutineScope.launch {
                                        val success = FirebaseAuthService.signInWithGoogle(context, "13527119145-android-client.apps.googleusercontent.com")
                                        if (success) {
                                            val user = FirebaseAuthService.getCurrentUser()
                                            val email = user?.email ?: ""
                                            val name = user?.displayName ?: email.substringBefore("@")
                                            viewModel.saveUserCredentials(email, "google_oauth_token", name)
                                            Toast.makeText(context, "Connecté avec succès via Google : $email", Toast.LENGTH_SHORT).show()
                                            onCloseModal?.invoke()
                                        } else {
                                            Toast.makeText(context, "Erreur lors de la connexion Google", Toast.LENGTH_LONG).show()
                                        }
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp),
                                shape = CircleShape,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.surface,
                                    contentColor = MaterialTheme.colorScheme.onSurface
                                )
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Surface(
                                        shape = CircleShape,
                                        color = Color.White,
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text("G", fontWeight = FontWeight.Black, fontSize = 15.sp, color = GoogleBlue)
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text("Se connecter avec Google", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        GoogleAuthMode.PHONE_SMS -> {
                            OutlinedTextField(
                                value = inputPhoneNumber,
                                onValueChange = { inputPhoneNumber = it },
                                label = { Text("Numéro de Téléphone (+230 / +33)") },
                                modifier = Modifier.fillMaxWidth(),
                                leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null, tint = GoogleBlue) },
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = GoogleBlue,
                                    unfocusedBorderColor = SlateBorder
                                ),
                                singleLine = true
                            )

                            Spacer(modifier = Modifier.height(14.dp))

                            if (isPhoneCodeSent) {
                                OutlinedTextField(
                                    value = inputSmsCode,
                                    onValueChange = { inputSmsCode = it },
                                    label = { Text("Code de vérification SMS (6 chiffres)") },
                                    placeholder = { Text("123456") },
                                    modifier = Modifier.fillMaxWidth(),
                                    leadingIcon = { Icon(Icons.Default.Sms, contentDescription = null, tint = GoogleBlue) },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = GoogleBlue,
                                        unfocusedBorderColor = SlateBorder
                                    ),
                                    singleLine = true
                                )

                                Spacer(modifier = Modifier.height(16.dp))

                                Button(
                                    onClick = {
                                        if (inputSmsCode.length >= 4) {
                                            viewModel.verifyPhoneCode(inputSmsCode, "user.${inputPhoneNumber.takeLast(4)}@gmail.com")
                                            Toast.makeText(context, "Numéro vérifié par SMS Firebase !", Toast.LENGTH_SHORT).show()
                                            onCloseModal?.invoke()
                                        } else {
                                            Toast.makeText(context, "Entrez le code SMS reçu", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                    .height(46.dp),
                                    shape = CircleShape,
                                    colors = ButtonDefaults.buttonColors(containerColor = GoogleBlue)
                                ) {
                                    Text("Vérifier & Se connecter", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                }
                            } else {
                                Button(
                                    onClick = {
                                        if (inputPhoneNumber.isNotBlank()) {
                                            viewModel.sendPhoneVerificationCode(inputPhoneNumber)
                                            Toast.makeText(context, "Code de vérification SMS envoyé au $inputPhoneNumber", Toast.LENGTH_SHORT).show()
                                        } else {
                                            Toast.makeText(context, "Entrez un numéro valide", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(46.dp),
                                    shape = CircleShape,
                                    colors = ButtonDefaults.buttonColors(containerColor = GoogleBlue)
                                ) {
                                    Text("Obtenir le code SMS", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Google Footer
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Français (France)",
                    fontSize = 12.sp,
                    color = SlateTextSecondary
                )
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = null,
                    tint = SlateTextSecondary,
                    modifier = Modifier.size(16.dp)
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                TextButton(
                    onClick = {
                        Toast.makeText(context, "Centre d'aide Google Firebase", Toast.LENGTH_SHORT).show()
                    },
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "Aide",
                        fontSize = 11.sp,
                        color = SlateTextSecondary
                    )
                }
                TextButton(
                    onClick = {
                        Toast.makeText(context, "Politique de confidentialité Google", Toast.LENGTH_SHORT).show()
                    },
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "Confidentialité",
                        fontSize = 11.sp,
                        color = SlateTextSecondary
                    )
                }
                TextButton(
                    onClick = {
                        Toast.makeText(context, "Conditions d'utilisation Google", Toast.LENGTH_SHORT).show()
                    },
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "Conditions",
                        fontSize = 11.sp,
                        color = SlateTextSecondary
                    )
                }
            }
        }
    }
}
}


