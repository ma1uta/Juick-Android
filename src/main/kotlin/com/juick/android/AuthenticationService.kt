/*
 * Juick
 * Copyright (C) 2008-2012, Ugnich Anton
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.juick.android

import android.accounts.AbstractAccountAuthenticator
import android.accounts.Account
import android.accounts.AccountAuthenticatorResponse
import android.accounts.AccountManager
import android.accounts.NetworkErrorException
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.IBinder
import com.juick.R


/**

 * @author Ugnich Anton
 */
class AuthenticationService : Service() {

    override fun onBind(intent: Intent): IBinder? {
        var ret: IBinder? = null
        if (intent.action.equals(android.accounts.AccountManager.ACTION_AUTHENTICATOR_INTENT)) {
            ret = authenticator?.iBinder
        }
        return ret
    }

    private val authenticator: AccountAuthenticatorImpl?
        get() {
            if (sAccountAuthenticator == null) {
                sAccountAuthenticator = AccountAuthenticatorImpl(this)
            }
            return sAccountAuthenticator
        }

    private class AccountAuthenticatorImpl(private val mContext: Context) : AbstractAccountAuthenticator(mContext) {

        override @Throws(NetworkErrorException::class)
        fun addAccount(response: AccountAuthenticatorResponse, accountType: String, authTokenType: String, requiredFeatures: Array<String>, options: Bundle): Bundle {
            val i = Intent(mContext, SignInActivity::class.java)
            i.putExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE, response)
            val reply = Bundle()
            reply.putParcelable(AccountManager.KEY_INTENT, i)
            return reply
        }

        override @Throws(NetworkErrorException::class)
        fun getAuthToken(response: AccountAuthenticatorResponse, account: Account, authTokenType: String, options: Bundle): Bundle {
            val result = Bundle()
            result.putString(AccountManager.KEY_ACCOUNT_NAME, account.name)
            result.putString(AccountManager.KEY_ACCOUNT_TYPE, mContext.getString(R.string.com_juick))
            result.putString(AccountManager.KEY_AUTHTOKEN, AccountManager.get(mContext).getPassword(account))
            return result
        }

        override @Throws(NetworkErrorException::class)
        fun hasFeatures(arg0: AccountAuthenticatorResponse, arg1: Account, arg2: Array<String>): Bundle? {
            return null
        }

        override @Throws(NetworkErrorException::class)
        fun updateCredentials(arg0: AccountAuthenticatorResponse, arg1: Account, arg2: String, arg3: Bundle): Bundle? {
            return null
        }

        override fun getAuthTokenLabel(arg0: String): String? {
            return null
        }

        override @Throws(NetworkErrorException::class)
        fun confirmCredentials(arg0: AccountAuthenticatorResponse, arg1: Account, arg2: Bundle): Bundle? {
            return null
        }

        override fun editProperties(arg0: AccountAuthenticatorResponse, arg1: String): Bundle? {
            return null
        }
    }

    companion object {

        private var sAccountAuthenticator: AccountAuthenticatorImpl? = null
    }
}
