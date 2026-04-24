package app.dreamcue.sync

import android.content.Context
import app.dreamcue.DreamCueRepository
import app.dreamcue.R
import app.dreamcue.model.Memo
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

class FirebaseSyncCoordinator(
    private val repository: DreamCueRepository,
) {
    private val appContext: Context = repository.appContext
    private var listenerRegistration: ListenerRegistration? = null

    fun currentEmail(): String {
        val auth = authOrNull() ?: return ""
        return auth.currentUser?.email ?: ""
    }

    fun start(
        onStatus: (String) -> Unit,
        onRemoteChange: () -> Unit,
    ) {
        val auth = authOrNull()
        val firestore = firestoreOrNull()
        if (auth == null || firestore == null) {
            onStatus("Firebase sync is not configured.")
            return
        }

        val user = auth.currentUser
        if (user == null) {
            stop()
            onStatus("Sign in to sync across devices.")
            return
        }

        val path = FirebaseTenantPaths.memoCollectionPath(user.uid)
        listenerRegistration?.remove()
        listenerRegistration = firestore.collection(path).addSnapshotListener { snapshot, error ->
            if (error != null) {
                onStatus(error.message ?: "Sync listener failed.")
                return@addSnapshotListener
            }
            if (snapshot == null) {
                return@addSnapshotListener
            }

            for (change in snapshot.documentChanges) {
                runCatching {
                    repository.initialize().getOrThrow()
                    applyRemoteChange(change)
                }.onFailure { failure ->
                    onStatus(failure.message ?: "Remote sync failed.")
                }
            }
            onStatus("Syncing as ${user.email ?: user.uid}.")
            onRemoteChange()
        }
    }

    fun signIn(
        email: String,
        password: String,
        onStatus: (String) -> Unit,
        onRemoteChange: () -> Unit,
    ) {
        val auth = authOrNull()
        if (auth == null) {
            onStatus("Firebase sync is not configured.")
            return
        }
        auth.signInWithEmailAndPassword(email.trim(), password)
            .addOnSuccessListener {
                onStatus("Sync account signed in.")
                start(onStatus, onRemoteChange)
            }
            .addOnFailureListener { error ->
                onStatus(error.message ?: "Sync sign-in failed.")
            }
    }

    fun createAccount(
        email: String,
        password: String,
        onStatus: (String) -> Unit,
        onRemoteChange: () -> Unit,
    ) {
        val auth = authOrNull()
        if (auth == null) {
            onStatus("Firebase sync is not configured.")
            return
        }
        auth.createUserWithEmailAndPassword(email.trim(), password)
            .addOnSuccessListener {
                onStatus("Sync account created.")
                start(onStatus, onRemoteChange)
            }
            .addOnFailureListener { error ->
                onStatus(error.message ?: "Sync account creation failed.")
            }
    }

    fun signOut(onStatus: (String) -> Unit) {
        listenerRegistration?.remove()
        listenerRegistration = null
        authOrNull()?.signOut()
        onStatus("Sync account signed out.")
    }

    fun uploadAll(memos: List<Memo>) {
        for (memo in memos) {
            uploadMemo(memo)
        }
    }

    fun uploadDeletedMemo(memoId: String, deletedAtMs: Long = System.currentTimeMillis()) {
        val firestore = firestoreOrNull() ?: return
        val userId = authOrNull()?.currentUser?.uid ?: return
        val document = RemoteMemoDocument.deletedMemo(memoId, deletedAtMs)
        firestore.collection(FirebaseTenantPaths.memoCollectionPath(userId))
            .document(memoId)
            .set(document.toMap())
    }

    fun stop() {
        listenerRegistration?.remove()
        listenerRegistration = null
    }

    private fun uploadMemo(memo: Memo) {
        val firestore = firestoreOrNull() ?: return
        val userId = authOrNull()?.currentUser?.uid ?: return
        val reference = firestore.collection(FirebaseTenantPaths.memoCollectionPath(userId)).document(memo.id)
        val document = RemoteMemoDocument.fromMemo(memo)

        firestore.runTransaction { transaction ->
            val snapshot = transaction.get(reference)
            val remoteUpdatedAt = snapshot.getLong("updated_at_ms") ?: Long.MIN_VALUE
            if (!snapshot.exists() || memo.updatedAtMs >= remoteUpdatedAt) {
                transaction.set(reference, document.toMap())
            }
            null
        }
    }

    private fun applyRemoteChange(change: DocumentChange) {
        val memoId = change.document.id
        if (change.type == DocumentChange.Type.REMOVED) {
            repository.deleteRemoteMemo(memoId)
            return
        }

        val remote = RemoteMemoDocument.fromMap(memoId, change.document.data)
        if (remote.deleted) {
            repository.deleteRemoteMemo(memoId)
        } else {
            repository.applyRemoteMemo(remote.toMemo())
        }
    }

    private fun authOrNull(): FirebaseAuth? {
        return if (ensureFirebaseApp()) FirebaseAuth.getInstance() else null
    }

    private fun firestoreOrNull(): FirebaseFirestore? {
        return if (ensureFirebaseApp()) FirebaseFirestore.getInstance() else null
    }

    private fun ensureFirebaseApp(): Boolean {
        if (FirebaseApp.getApps(appContext).isNotEmpty()) {
            return true
        }

        val projectId = appContext.getString(R.string.firebase_project_id).trim()
        val applicationId = appContext.getString(R.string.firebase_application_id).trim()
        val apiKey = appContext.getString(R.string.firebase_api_key).trim()
        if (projectId.isEmpty() || applicationId.isEmpty() || apiKey.isEmpty()) {
            return false
        }

        val options = FirebaseOptions.Builder()
            .setProjectId(projectId)
            .setApplicationId(applicationId)
            .setApiKey(apiKey)
            .build()
        FirebaseApp.initializeApp(appContext, options)
        return true
    }
}
