package com.example.myrecycleview

import androidx.recyclerview.widget.RecyclerView
import android.util.Log
import android.view.View
import android.view.ViewGroup
import kotlinx.android.synthetic.main.item_default.view.*

class DefaultAdapter(private val data: String) :
    RecyclerView.Adapter<DefaultAdapter.DefaultHolder>(){

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DefaultAdapter.DefaultHolder {
        //Here you inflate the view from its layout and pass it in to a PersonHolder
        //Заполнение элементов RecyclerView
        //В inflate передаётся вёрстка, которая будет использоваться для построения каждого элемента
        // val inflatedView = parent.inflate(R.layout.item_person,false)
        val inflatedView = parent.inflate(R.layout.item_default,false)
        return DefaultHolder(inflatedView)
    }

    override fun getItemCount() = 1//сколько будет на дисплее

    override fun onBindViewHolder(holder: DefaultHolder, position: Int) {
        val item = data
        holder.bindDefault(item)
    }

    class DefaultHolder(v: View) : RecyclerView.ViewHolder(v), View.OnClickListener{
        private var view: View = v
        private var bus: String ?= null

        init {
            //инициализация листенера
            v.setOnClickListener(this)
        }

        override fun onClick(v: View?) {
            Log.d("RecyclerView","Click on default")
            //здесь будет открываться новая карта в localTransport
        }

        fun bindDefault(msg: String) {
            //Показываем какие именно данные закидываем в каждый элемент
            this.bus = bus
            view.msg.text = msg
        }

    }



}