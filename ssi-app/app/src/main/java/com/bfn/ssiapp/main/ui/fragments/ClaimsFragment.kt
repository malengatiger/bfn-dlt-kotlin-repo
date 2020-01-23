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
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.bfn.ssiapp.R
import com.bfn.ssiapp.main.data.ClaimAttribute
import com.bfn.ssiapp.main.utils.updateCredentialsInRealm
import com.luxoft.blockchainlab.hyperledger.indy.IndyUser
import com.bfn.ssiapp.main.ui.adapters.ClaimsAdapter
import com.luxoft.supplychain.sovrinagentapp.application.AUTHORITIES
import com.luxoft.supplychain.sovrinagentapp.application.EXTRA_SERIAL
import com.luxoft.supplychain.sovrinagentapp.application.FIELD_KEY
import com.luxoft.supplychain.sovrinagentapp.application.TIME
import io.realm.Realm
import org.koin.android.ext.android.inject

class ClaimsFragment : Fragment() {

    private lateinit var tvClaims: TextView
    private lateinit var adapterRecycler: ClaimsAdapter
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout
    private lateinit var recycler: RecyclerView

    private val realm: Realm = Realm.getDefaultInstance()
    private val indyUser: IndyUser by inject()
    private val mTag = ClaimsFragment::class.java.simpleName

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_claims, container, false)
        Log.d(mTag,"\uD83D\uDC7D ClaimsFragment onCreateView")
        tvClaims = view.findViewById(R.id.tvClaims)
        recycler = view.findViewById(R.id.recycler)
        swipeRefreshLayout = view.findViewById(R.id.swipe_container)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        Log.d(mTag,"\uD83D\uDC7D ClaimsFragment onViewCreated")
        val claims = realm.where(ClaimAttribute::class.java)
            .sort(FIELD_KEY)
            .notEqualTo(FIELD_KEY, AUTHORITIES)
            .notEqualTo(FIELD_KEY, TIME)
            .notEqualTo(FIELD_KEY, EXTRA_SERIAL)
            .findAll()

        claims.addChangeListener { result ->
            val numCredRefs = result.distinctBy { it.credRefSeqNo }.size
            tvClaims.text = getString(R.string.verified_credentials, numCredRefs)
        }

        val linearLayoutManager = LinearLayoutManager(activity)
        recycler.layoutManager = linearLayoutManager
        recycler.addItemDecoration(DividerItemDecoration(recycler.context, linearLayoutManager.orientation))

        adapterRecycler = ClaimsAdapter(claims)
        recycler.adapter = adapterRecycler

//        swipeRefreshLayout = swipe_container
        swipeRefreshLayout.setOnRefreshListener { updateMyClaims() }

        updateMyClaims()
    }

    private fun updateMyClaims() {
        swipeRefreshLayout.isRefreshing = true
        indyUser.walletUser.updateCredentialsInRealm()
        swipeRefreshLayout.isRefreshing = false
    }
}
