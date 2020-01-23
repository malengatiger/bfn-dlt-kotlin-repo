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

package com.bfn.ssiapp.main.ui.fragments

import android.os.Bundle

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.bfn.ssiapp.R
import com.bfn.ssiapp.main.ui.activities.MainActivity.Companion.showAlertDialog
import com.bfn.ssiapp.main.ui.adapters.OrdersAdapter
import com.bfn.ssiapp.main.utils.showNotification
import com.bfn.ssiapp.main.communcations.SovrinAgentService
import com.bfn.ssiapp.main.data.PackageState
import com.bfn.ssiapp.main.data.Product
import com.bfn.ssiapp.main.data.ProductOperation
import io.realm.Realm
import io.realm.RealmResults
import kotlinx.android.synthetic.main.fragment_recycler.*
import org.koin.android.ext.android.inject
import rx.android.schedulers.AndroidSchedulers
import rx.schedulers.Schedulers

class OrdersFragment : Fragment() {

    private lateinit var adapter: OrdersAdapter
    private lateinit var layoutManager: LinearLayoutManager
    private val api: SovrinAgentService by inject()
    private val realm: Realm = Realm.getDefaultInstance()
    private val mTag = OrdersFragment::class.java.simpleName

    private lateinit var recyclerAdapter: OrdersAdapter
    private lateinit var mSwipeRefreshLayout: SwipeRefreshLayout

    private val productOperations: RealmResults<ProductOperation> = realm.where(ProductOperation::class.java).findAll()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        Log.d(mTag,"OrdersFragment \uD83C\uDF3F \uD83C\uDF3F \uD83C\uDF3F onCreateView")
        val v = inflater.inflate(R.layout.fragment_recycler, container, false)
        mSwipeRefreshLayout = v.findViewById(R.id.swipe_container)
        return v
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        Log.d(mTag,"OrdersFragment \uD83C\uDF3F \uD83C\uDF3F \uD83C\uDF3F onViewCreated")
        layoutManager = LinearLayoutManager(activity)
        recyclerAdapter = OrdersAdapter(Realm.getDefaultInstance())
        adapter = recyclerAdapter
        mSwipeRefreshLayout = view.findViewById(R.id.swipe_container)
        mSwipeRefreshLayout.setOnRefreshListener { updateMyOrders() }
    }

    override fun onResume() {
        super.onResume()
        updateMyOrders()
    }

    private fun updateMyOrders() {
        mSwipeRefreshLayout.isRefreshing = true
        api.getPackages().subscribeOn(Schedulers.newThread())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                loaded()
                saveOrders(it)
            }, { error ->
                Log.e("Get Packages Error: ", error.message, error)
                showAlertDialog(context!!, "Get Packages Error: ${error.message}") { loaded() }
            })
    }

    private fun loaded() {
        mSwipeRefreshLayout.isRefreshing = false
    }

    private fun saveOrders(offers: List<Product>) {
        if (offers.isNotEmpty()) {
            realm.beginTransaction()
            realm.copyToRealmOrUpdate(offers)
            realm.commitTransaction()
        }
        for (offer in offers) {
            if (offer.state.equals(PackageState.DELIVERED.name)) {
                var delivered = false
                for (productOperation in productOperations) {
                    if (offer.deliveredAt!! == productOperation.at
                        && productOperation.by.equals("delivered")) delivered = true
                }
                if (!delivered) {
                    showNotification(activity!!, getString(R.string.your_package_is_ready), getString(R.string.visit_your))
                    Realm.getDefaultInstance().executeTransaction {
//                        val productOperation = it.createObject(ProductOperation::class.java, offer.deliveredAt)
//                        productOperation.by = "delivered"
                    }
                }
            }
        }
    }
}
