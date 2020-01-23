package com.bfn.ssiapp.main.ui.activities

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.PermissionChecker
import com.bfn.ssiapp.R
import com.luxoft.blockchainlab.hyperledger.indy.IndyUser
import com.luxoft.supplychain.sovrinagentapp.application.GENESIS_CONTENT
import com.luxoft.supplychain.sovrinagentapp.application.GENESIS_PATH
import org.koin.android.ext.android.inject
import rx.Completable
import rx.schedulers.Schedulers
import java.io.File

lateinit var splashScreen: SplashActivity

class SplashActivity : AppCompatActivity() {

    private val permissionRequestCode = 101

    private val indyUser: IndyUser by inject()

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        requestPermissions(
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.INTERNET,
                Manifest.permission.CAMERA,
                Manifest.permission.ACCESS_NETWORK_STATE),
            permissionRequestCode
        )
        splashScreen = this
    }

    @RequiresApi(Build.VERSION_CODES.N)
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        when (requestCode) {
            permissionRequestCode -> {
                if (grantResults.any { it != PermissionChecker.PERMISSION_GRANTED })
                    throw RuntimeException("You should grant permissions if you want to use vcx")
                else {
                    initGenesis()
                    Completable.complete().observeOn(Schedulers.io()).subscribe {
                        indyUser.walletUser.getCredentials().forEachRemaining {
                            Log.d("User", "User $it")
                        }
                        val intent = Intent(this, MainActivity::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        startActivity(intent)
                    }
                }
            }
        }
    }

    private fun initGenesis() {
        val genesis = File(GENESIS_PATH)
        if (genesis.exists()) genesis.delete()
        genesis.createNewFile()
        genesis.writeText(GENESIS_CONTENT)
    }
}
