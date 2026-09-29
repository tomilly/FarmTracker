package pl.farmtracker.data.firebase

import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/*
 * Nasłuch zmian zamiast `snapshots()` z Firebase: tamten przy odmowie serwera kończy się
 * CancellationException – `retryWhen`/`catch` go nie widzą, a ekran po cichu przestaje się odświeżać.
 * Tu błąd dochodzi jako [com.google.firebase.firestore.FirebaseFirestoreException].
 */

internal fun DocumentReference.changes(): Flow<DocumentSnapshot> = callbackFlow {
    val registration = addSnapshotListener { snapshot, error ->
        when {
            error != null -> close(error)
            snapshot != null -> trySend(snapshot)
        }
    }
    awaitClose { registration.remove() }
}

internal fun Query.changes(): Flow<QuerySnapshot> = callbackFlow {
    val registration = addSnapshotListener { snapshot, error ->
        when {
            error != null -> close(error)
            snapshot != null -> trySend(snapshot)
        }
    }
    awaitClose { registration.remove() }
}
