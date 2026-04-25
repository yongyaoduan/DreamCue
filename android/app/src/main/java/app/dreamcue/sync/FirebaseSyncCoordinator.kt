package app.dreamcue.sync

import android.content.Context
import app.dreamcue.BuildConfig
import app.dreamcue.DreamCueRepository
import app.dreamcue.R
import app.dreamcue.model.Memo
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

interface MemoSyncCoordinator {
    fun currentEmail(): String
    fun start(
        onStatus: (String) -> Unit,
        onRemoteChange: () -> Unit,
    )
    fun signIn(
        email: String,
        password: String,
        onStatus: (String) -> Unit,
        onRemoteChange: () -> Unit,
    )
    fun createAccount(
        email: String,
        password: String,
        onStatus: (String) -> Unit,
        onRemoteChange: () -> Unit,
    )
    fun signOut(onStatus: (String) -> Unit)
    fun uploadAll(memos: List<Memo>)
    fun uploadDeletedMemo(memoId: String, deletedAtMs: Long = System.currentTimeMillis())
    fun stop()
}

class FirebaseSyncCoordinator(
    private val repository: DreamCueRepository,
) : MemoSyncCoordinator {
    private val appContext: Context = repository.appContext
    private var memoReference: DatabaseReference? = null
    private var valueEventListener: ValueEventListener? = null

    override fun currentEmail(): String {
        val auth = authOrNull() ?: return ""
        return auth.currentUser?.email ?: ""
    }

    override fun start(
        onStatus: (String) -> Unit,
        onRemoteChange: () -> Unit,
    ) {
        val auth = authOrNull()
        val database = databaseOrNull()
        if (auth == null || database == null) {
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
        stop()
        val reference = database.getReference(path)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                runCatching {
                    repository.initialize().getOrThrow()
                    applyRemoteSnapshot(snapshot)
                }.onFailure { failure ->
                    onStatus(failure.message ?: "Remote sync failed.")
                    return
                }
                onStatus("Syncing as ${user.email ?: user.uid}.")
                onRemoteChange()
            }

            override fun onCancelled(error: DatabaseError) {
                onStatus(error.message.ifBlank { "Sync listener failed." })
            }
        }
        reference.addValueEventListener(listener)
        memoReference = reference
        valueEventListener = listener
    }

    override fun signIn(
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
                onStatus(firebaseSyncFailureMessage(error, "Sync sign-in failed."))
            }
    }

    override fun createAccount(
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
                onStatus(firebaseSyncFailureMessage(error, "Sync account creation failed."))
            }
    }

    override fun signOut(onStatus: (String) -> Unit) {
        stop()
        authOrNull()?.signOut()
        onStatus("Sync account signed out.")
    }

    override fun uploadAll(memos: List<Memo>) {
        for (memo in memos) {
            uploadMemo(memo)
        }
    }

    override fun uploadDeletedMemo(memoId: String, deletedAtMs: Long) {
        val database = databaseOrNull() ?: return
        val userId = authOrNull()?.currentUser?.uid ?: return
        val document = RemoteMemoDocument.deletedMemo(memoId, deletedAtMs)
        database.getReference(FirebaseTenantPaths.memoDocumentPath(userId, memoId))
            .setValue(document.toMap())
    }

    override fun stop() {
        val listener = valueEventListener
        val reference = memoReference
        if (listener != null && reference != null) {
            reference.removeEventListener(listener)
        }
        valueEventListener = null
        memoReference = null
    }

    private fun uploadMemo(memo: Memo) {
        val database = databaseOrNull() ?: return
        val userId = authOrNull()?.currentUser?.uid ?: return
        val document = RemoteMemoDocument.fromMemo(memo).toMap()
        database.getReference(FirebaseTenantPaths.memoDocumentPath(userId, memo.id))
            .setValue(document)
    }

    private fun applyRemoteSnapshot(snapshot: DataSnapshot) {
        for (child in snapshot.children) {
            val memoId = child.key ?: continue
            val values = child.value as? Map<String, Any?> ?: continue
            val remote = RemoteMemoDocument.fromMap(memoId, values)
            if (remote.deleted) {
                repository.deleteRemoteMemo(memoId)
            } else {
                repository.applyRemoteMemo(remote.toMemo())
            }
        }
    }

    private fun authOrNull(): FirebaseAuth? {
        return if (ensureFirebaseApp()) FirebaseAuth.getInstance() else null
    }

    private fun databaseOrNull(): FirebaseDatabase? {
        return if (ensureFirebaseApp()) FirebaseDatabase.getInstance() else null
    }

    private fun ensureFirebaseApp(): Boolean {
        if (FirebaseApp.getApps(appContext).isNotEmpty()) {
            return true
        }

        val projectId = firebaseConfigValue(R.string.firebase_project_id, BuildConfig.FIREBASE_PROJECT_ID)
        val applicationId = firebaseConfigValue(R.string.firebase_application_id, BuildConfig.FIREBASE_APPLICATION_ID)
        val apiKey = firebaseConfigValue(R.string.firebase_api_key, BuildConfig.FIREBASE_API_KEY)
        val databaseUrl = firebaseConfigValue(R.string.firebase_database_url, BuildConfig.FIREBASE_DATABASE_URL)
        if (projectId.isEmpty() || applicationId.isEmpty() || apiKey.isEmpty() || databaseUrl.isEmpty()) {
            return false
        }

        val options = FirebaseOptions.Builder()
            .setProjectId(projectId)
            .setApplicationId(applicationId)
            .setApiKey(apiKey)
            .setDatabaseUrl(databaseUrl)
            .build()
        FirebaseApp.initializeApp(appContext, options)
        return true
    }

    private fun firebaseConfigValue(resourceId: Int, fallback: String): String {
        return appContext.getString(resourceId).trim().ifEmpty { fallback.trim() }
    }
}

internal fun firebaseSyncFailureMessage(error: Throwable, fallback: String): String {
    val rawMessage = error.message.orEmpty()
    return when {
        rawMessage.contains("CONFIGURATION_NOT_FOUND") ->
            "Sync account setup is not available yet."
        rawMessage.contains("EMAIL_EXISTS") ->
            "An account already exists for this email."
        rawMessage.contains("INVALID_EMAIL") ->
            "Enter a valid email address."
        rawMessage.contains("WEAK_PASSWORD") ->
            "Use a password with at least 6 characters."
        rawMessage.contains("INVALID_LOGIN_CREDENTIALS") ||
            rawMessage.contains("INVALID_PASSWORD") ||
            rawMessage.contains("EMAIL_NOT_FOUND") ->
            "Email or password is incorrect."
        rawMessage.isNotBlank() -> rawMessage
        else -> fallback
    }
}
