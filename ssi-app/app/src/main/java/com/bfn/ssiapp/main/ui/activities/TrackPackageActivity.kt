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

package com.bfn.ssiapp.main.ui.activities

import android.os.Bundle
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.bfn.ssiapp.R
import com.luxoft.supplychain.sovrinagentapp.application.EXTRA_SERIAL
import com.bfn.ssiapp.main.data.Product
import io.realm.Realm

class TrackPackageActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_track_product)

        val product = Realm.getDefaultInstance().where(Product::class.java).equalTo(EXTRA_SERIAL, intent.getStringExtra(EXTRA_SERIAL)).findFirst()

        if (product != null) {
//            with(recyclerView) {
//                adapter = TimeLineAdapter(product)
//                layoutManager = LinearLayoutManager(this@TrackPackageActivity, LinearLayoutManager.VERTICAL, false)
//                setHasFixedSize(true)
//            }
            title = product.medicineName
            supportActionBar?.let {
                it.setDisplayHomeAsUpEnabled(true)
                it.setHomeAsUpIndicator(R.drawable.ic_back)
            }
        } else {
            Toast.makeText(this, "Item not found", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Kids, never try it at home
        onBackPressed()
        return true
    }
}
