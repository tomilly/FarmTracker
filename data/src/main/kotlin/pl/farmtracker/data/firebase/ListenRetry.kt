package pl.farmtracker.data.firebase

import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.retryWhen

/**
 * Słuchanie danych zbioru ponawiane po odmowie dostępu. Tuż po założeniu zbioru albo dołączeniu kodem telefon
 * już widzi swoje członkostwo (zapis w kopii na telefonie), ale serwer jeszcze go nie dostał – i odrzuca
 * nasłuch na zawsze. Po chwili (zapis dotarł) ta sama prośba przechodzi.
 */
internal fun <T> Flow<T>.retryWhenNotYetMember(attempts: Int = 6): Flow<T> = retryWhen { cause, attempt ->
    val retry = cause is FirebaseFirestoreException &&
        cause.code == FirebaseFirestoreException.Code.PERMISSION_DENIED &&
        attempt < attempts
    if (retry) delay(RETRY_STEP_MILLIS * (attempt + 1))
    retry
}

private const val RETRY_STEP_MILLIS = 1_000L
