package com.bfn.ssiapp

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.annotation.RequiresApi
import com.google.android.material.bottomnavigation.BottomNavigationView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.PermissionChecker
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.bfn.ssiapp.main.ui.activities.MainActivity
import com.luxoft.blockchainlab.hyperledger.indy.IndyUser
import com.luxoft.supplychain.sovrinagentapp.application.GENESIS_CONTENT
import com.luxoft.supplychain.sovrinagentapp.application.GENESIS_PATH
import org.koin.android.ext.android.inject
import rx.Completable
import rx.schedulers.Schedulers
import java.io.File

class NewMainActivity : AppCompatActivity() {

    private val permissionRequestCode = 101
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("\uD83C\uDF4E \uD83C\uDF4E NewMain", "\uD83C\uDF4E \uD83C\uDF4E \uD83C\uDF4E \uD83C\uDF4E NewMainActivity onCreate")
        setContentView(R.layout.activity_new_main)
        val navView: BottomNavigationView = findViewById(R.id.nav_view)

        val navController = findNavController(R.id.nav_host_fragment)
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        val appBarConfiguration = AppBarConfiguration(setOf(
                R.id.navigation_home, R.id.navigation_dashboard, R.id.navigation_notifications))
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)
        Log.d("NewMain", "\uD83C\uDF4E \uD83C\uDF4E \uD83C\uDF4E \uD83C\uDF4E NewMainActivity requestPermissions")

        requestPermissions(
            arrayOf(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.INTERNET,
                Manifest.permission.CAMERA,
                Manifest.permission.ACCESS_NETWORK_STATE),
            permissionRequestCode)
    }
    //private val indyUser: IndyUser by inject()
    @RequiresApi(Build.VERSION_CODES.N)
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        when (requestCode) {
            permissionRequestCode -> {
                if (grantResults.any { it != PermissionChecker.PERMISSION_GRANTED })
                    throw RuntimeException("You should grant permissions if you want to use vcx")
                else {
                    initGenesis()
                    //startMain()
//                    Completable.complete().observeOn(Schedulers.io()).subscribe {
////                        indyUser.walletUser.getCredentials().forEachRemaining {
////                            Log.d("User", "\uD83E\uDDE9 \uD83E\uDDE9 User $it")
////                        }
////                        startMain()
//                    }
                }
            }
        }
    }

    private fun startMain() {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
    }

    private fun initGenesis() {
        Log.d("NewMain", "\uD83E\uDD66 \uD83E\uDD66  - initGenesis ... what is this??")

        val genesis = File(GENESIS_PATH)
        if (genesis.exists()) genesis.delete()
        genesis.createNewFile()
        genesis.writeText(GENESIS_CONTENT)

        Log.d("NewMain", "\uD83E\uDD66 \uD83E\uDD66  - initGenesis ...\uD83E\uDDE9 " +
                "\uD83E\uDDE9 \uD83E\uDDE9  GENESIS_PATH: $GENESIS_PATH")
        Log.d("NewMain", "\uD83E\uDD66 \uD83E\uDD66  - initGenesis ...\uD83E\uDDE9 " +
                "GENESIS_CONTENT: $GENESIS_CONTENT \uD83E\uDDE9 \uD83E\uDDE9 ")
    }
}
