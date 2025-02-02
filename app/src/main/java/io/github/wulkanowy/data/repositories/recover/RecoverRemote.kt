package io.github.wulkanowy.data.repositories.recover

import io.github.wulkanowy.sdk.Sdk
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RecoverRemote @Inject constructor(private val sdk: Sdk) {

    suspend fun getReCaptchaSiteKey(host: String, symbol: String): Pair<String, String> {
        return sdk.getPasswordResetCaptchaCode(host, symbol)
    }

    suspend fun sendRecoverRequest(url: String, symbol: String, email: String, reCaptchaResponse: String): String {
        return sdk.sendPasswordResetRequest(url, symbol, email, reCaptchaResponse)
    }
}

