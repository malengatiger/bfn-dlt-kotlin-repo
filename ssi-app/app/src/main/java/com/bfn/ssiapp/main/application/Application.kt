/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.bfn.ssiapp.main.application

import android.app.Application
import android.os.Environment
import com.bfn.ssiapp.main.di.myModule
import io.realm.Realm
import io.realm.RealmConfiguration
import org.koin.android.ext.android.startKoin
import org.slf4j.LoggerFactory
import java.io.File

class Application : Application() {
    private val logger = LoggerFactory.getLogger(Application::class.java)

    override fun onCreate() {
        super.onCreate()
        logger.info("\uD83E\uDDA0 \uD83E\uDDA0 \uD83E\uDDA0 \uD83E\uDDA0 \uD83E\uDDA0 " +
                "SSIApp starting, will set up Realm ... ")
        System.setProperty("INDY_HOME", Environment.getDataDirectory().absolutePath)
        startKoin(this, listOf(myModule))
        Realm.init(this)
        val c = RealmConfiguration.Builder()
        c.name("student")
        c.deleteRealmIfMigrationNeeded()
        Realm.setDefaultConfiguration(c.build())
        val realm = Realm.getDefaultInstance()
        logger.info("❄️ ❄️ ❄️ ❄️ REALM instance, path: ${realm.path} ❄️ schema: ${realm.schema}")
//        realm.executeTransaction {
////            it.where(ClaimAttribute::class.java).findAll().deleteAllFromRealm()
////            it.where(Product::class.java).findAll().deleteAllFromRealm()
////
////            val product2 = it..createObject(Product::class.java, "N/A")
////            product2.state = PackageState.NEW.name
////            product2.medicineName = "Santorium Plus"
////            product2.requestedAt = Long.MAX_VALUE
//        }

    }
}
