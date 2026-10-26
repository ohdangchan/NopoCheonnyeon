package com.nopo.cheonnyeon.platform.auth

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential

/** 구글 로그인 결과 */
sealed interface GoogleResult {
    data class Token(val idToken: String) : GoogleResult
    data object Cancelled : GoogleResult
    /** 구글 로그인을 쓰려면 Firebase 콘솔에서 Google 로그인을 켜고 google-services.json 을 다시 받아야 한다 */
    data object NotConfigured : GoogleResult
    data object NoAccount : GoogleResult
    data class Failed(val reason: String) : GoogleResult
}

/**
 * Credential Manager 로 구글 계정을 골라 ID 토큰을 받는다. 서버 클라이언트 ID 는 google-services 플러그인이 만드는
 * `default_web_client_id` 문자열을 쓰므로 코드에 키가 들어가지 않는다.
 * [context] 는 화면을 띄울 수 있는 Activity 여야 한다.
 */
object GoogleSignIn {
    private fun webClientId(context: Context): String? {
        val id = context.resources.getIdentifier("default_web_client_id", "string", context.packageName)
        return if (id == 0) null else context.getString(id).takeIf { it.isNotBlank() }
    }

    fun isConfigured(context: Context) = webClientId(context) != null

    suspend fun getIdToken(context: Context): GoogleResult {
        val clientId = webClientId(context) ?: return GoogleResult.NotConfigured
        val manager = CredentialManager.create(context)
        fun request(filter: Boolean) = GetCredentialRequest.Builder()
            .addCredentialOption(
                GetGoogleIdOption.Builder().setFilterByAuthorizedAccounts(filter).setServerClientId(clientId)
                    .setAutoSelectEnabled(false).build()
            )
            .build()
        return try {
            val result = try {
                manager.getCredential(context, request(filter = true))
            } catch (_: NoCredentialException) {
                manager.getCredential(context, request(filter = false))
            }
            val cred = result.credential
            if (cred is CustomCredential && cred.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                GoogleResult.Token(GoogleIdTokenCredential.createFrom(cred.data).idToken)
            } else GoogleResult.Failed("지원하지 않는 계정 형식")
        } catch (_: GetCredentialCancellationException) {
            GoogleResult.Cancelled
        } catch (_: NoCredentialException) {
            GoogleResult.NoAccount
        } catch (e: GetCredentialException) {
            GoogleResult.Failed(e.javaClass.simpleName)
        } catch (e: Exception) {
            GoogleResult.Failed(e.javaClass.simpleName)
        }
    }
}
