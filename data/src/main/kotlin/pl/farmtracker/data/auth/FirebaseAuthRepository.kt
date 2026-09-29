package pl.farmtracker.data.auth

import com.google.firebase.FirebaseException
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await
import pl.farmtracker.data.firebase.CurrentActivity
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

/** Logowanie numerem telefonu przez Firebase Auth (kod SMS). */
@Singleton
class FirebaseAuthRepository @Inject constructor(
    private val currentActivity: CurrentActivity,
) : AuthRepository {

    private val auth: FirebaseAuth get() = FirebaseAuth.getInstance()

    private var verificationId: String? = null
    private var resendToken: PhoneAuthProvider.ForceResendingToken? = null
    private var lastPhone: String? = null

    override val state: Flow<AuthState> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            val user = firebaseAuth.currentUser
            trySend(if (user == null) AuthState.SignedOut else AuthState.SignedIn(user.uid, user.phoneNumber.orEmpty()))
        }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }

    override suspend fun sendCode(phone: String): SendCodeResult {
        val activity = currentActivity.activity ?: return SendCodeResult.Unavailable
        return suspendCancellableCoroutine { continuation ->
            val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                override fun onCodeSent(id: String, token: PhoneAuthProvider.ForceResendingToken) {
                    verificationId = id
                    resendToken = token
                    lastPhone = phone
                    if (continuation.isActive) continuation.resume(SendCodeResult.CodeSent)
                }

                // Telefon sam odczytał SMS (albo numer zweryfikowano od razu) – logujemy bez wpisywania kodu.
                // Bywa wołane już po onCodeSent – wtedy stan logowania zmieni się sam i ekran pójdzie dalej.
                override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                    auth.signInWithCredential(credential)
                    if (continuation.isActive) continuation.resume(SendCodeResult.SignedIn)
                }

                override fun onVerificationFailed(error: FirebaseException) {
                    if (continuation.isActive) continuation.resume(error.toSendCodeResult())
                }
            }
            val options = PhoneAuthOptions.newBuilder(auth)
                .setPhoneNumber(phone)
                .setTimeout(AUTO_RETRIEVAL_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .setActivity(activity)
                .setCallbacks(callbacks)
                .apply { resendToken?.takeIf { phone == lastPhone }?.let(::setForceResendingToken) }
                .build()
            PhoneAuthProvider.verifyPhoneNumber(options)
        }
    }

    override suspend fun verifyCode(code: String): VerifyCodeResult {
        val id = verificationId ?: return VerifyCodeResult.Expired
        return try {
            auth.signInWithCredential(PhoneAuthProvider.getCredential(id, code)).await()
            VerifyCodeResult.SignedIn
        } catch (error: FirebaseAuthInvalidCredentialsException) {
            if (error.errorCode == "ERROR_SESSION_EXPIRED") VerifyCodeResult.Expired else VerifyCodeResult.WrongCode
        } catch (error: FirebaseException) {
            VerifyCodeResult.Unavailable
        }
    }

    override suspend fun signOut() {
        auth.signOut()
        verificationId = null
        resendToken = null
    }

    private fun FirebaseException.toSendCodeResult(): SendCodeResult = when (this) {
        is FirebaseAuthInvalidCredentialsException -> SendCodeResult.InvalidNumber
        is FirebaseTooManyRequestsException -> SendCodeResult.TooManyAttempts
        is FirebaseNetworkException -> SendCodeResult.Unavailable
        else -> SendCodeResult.Unavailable
    }

    private companion object {
        /** Jak długo telefon czeka na SMS, żeby odczytać go sam. */
        const val AUTO_RETRIEVAL_TIMEOUT_SECONDS = 60L
    }
}
