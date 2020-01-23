package com.bfn.ssiapp.main.ui.adapters

import android.content.Context
import com.bfn.ssiapp.main.data.Product

interface OrderClickListener {
    fun click(order: Product, context: Context)
}
