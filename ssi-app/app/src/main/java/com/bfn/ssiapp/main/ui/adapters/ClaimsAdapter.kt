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

package com.bfn.ssiapp.main.ui.adapters

import android.graphics.BitmapFactory
import android.os.Build
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bfn.ssiapp.R
import com.bfn.ssiapp.main.data.ClaimAttribute
import com.bfn.ssiapp.main.utils.inflate
import io.realm.RealmChangeListener
import io.realm.RealmResults
import kotlinx.android.synthetic.main.item_claim_pic.view.iv_pic
import kotlinx.android.synthetic.main.item_claim_text.view.tv_value
import kotlinx.android.synthetic.main.item_claim_text.view.tv_verifier
import kotlinx.android.synthetic.main.item_claim_text.view.tv_name
import kotlinx.android.synthetic.main.item_claim_text.view.tv_schema_id
import org.apache.commons.lang3.StringUtils
import java.util.*

class ClaimsAdapter(private val claims: RealmResults<ClaimAttribute>) : RecyclerView.Adapter<ClaimsAdapter.ClaimViewHolder>() {
    private val TAG = this::class.simpleName

    private var realmChangeListener = RealmChangeListener<RealmResults<ClaimAttribute>> {
        Log.i(TAG, "Change occurred!")
        this.notifyDataSetChanged()
    }

    init {
        claims.addChangeListener(realmChangeListener)
    }

    //region ******************** OVERRIDE *************************************************************

    override fun onCreateViewHolder(viewGroup: ViewGroup, itemTypeOrdinal: Int): ClaimViewHolder {
        val itemType = ItemType.values().getOrElse(itemTypeOrdinal) { ordinal ->
            Log.e(TAG, "Unknown itemTypeOrdinal $ordinal")
            ItemType.TEXT_ITEM_VIEW
        }

        return when(itemType) {
            ItemType.TEXT_ITEM_VIEW -> TextClaimViewHolder(viewGroup.context.inflate(R.layout.item_claim_text, viewGroup))
            ItemType.PIC_ITEM_VIEW -> PicClaimViewHolder(viewGroup.context.inflate(R.layout.item_claim_pic, viewGroup))
        }
    }

    override fun onBindViewHolder(holder: ClaimViewHolder, position: Int) {
        claims[position]?.let { holder.bind(it) }
    }

    override fun getItemCount(): Int {
        return claims.size
    }

    override fun getItemViewType(position: Int): Int {
        val claim = claims[position]
        val valueSize = claim?.value?.length

        val itemType = if (valueSize == null || valueSize < MIN_PIC_VALUE_LEN)
            ItemType.TEXT_ITEM_VIEW
        else
            ItemType.PIC_ITEM_VIEW

        return itemType.ordinal
    }
//endregion OVERRIDE

    //region ******************** HOLDER ***********************************************************
    enum class ItemType { TEXT_ITEM_VIEW, PIC_ITEM_VIEW }

    val MIN_PIC_VALUE_LEN = 512;

    abstract class ClaimViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        abstract fun bind(item: ClaimAttribute?)
    }

    private inner class TextClaimViewHolder(itemView: View) : ClaimViewHolder(itemView) {
        var name: TextView = itemView.tv_name
        var value: TextView = itemView.tv_value
        var schemaId: TextView = itemView.tv_schema_id
        var issuerId: TextView = itemView.tv_verifier

        override fun bind(item: ClaimAttribute?) {
            item ?: return
            name.text = item.prettyKey()
            value.text = item.prettyValue()
            schemaId.text = item.prettySchema()
            issuerId.text = item.issuerDid
        }
    }

    private inner class PicClaimViewHolder(itemView: View) : ClaimViewHolder(itemView) {
        var name: TextView = itemView.tv_name
        var value: ImageView = itemView.iv_pic
        var schemaId: TextView = itemView.tv_schema_id
        var issuerId: TextView = itemView.tv_verifier

        override fun bind(item: ClaimAttribute?) {
            item ?: return
            name.text = item.prettyKey()
            schemaId.text = item.prettySchema()
            issuerId.text = item.issuerDid

            item.value ?: return
            val imageBytes = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                Base64.getDecoder().decode(item.value)
            } else {
                TODO("VERSION.SDK_INT < O")
            }

            val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
            value.setImageBitmap(bitmap)
        }
    }

    private fun ClaimAttribute.prettyKey(): String = StringUtils.abbreviate(key ?: "---", 30)
    private fun ClaimAttribute.prettyValue(): String = StringUtils.abbreviate(value ?: "null", 512)
    private fun ClaimAttribute.prettySchema(): String = "$schemaName:$schemaVersion"

    //endregion HOLDER
}
