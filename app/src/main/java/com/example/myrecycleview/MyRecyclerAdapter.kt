package com.example.myrecycleview

import android.content.Intent
import androidx.recyclerview.widget.RecyclerView
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import kotlinx.android.synthetic.main.item_person.view.*


class MyRecyclerAdapter(private val data: ArrayList<Person>) :
RecyclerView.Adapter<MyRecyclerAdapter.PersonHolder>(){

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyRecyclerAdapter.PersonHolder {
        //Here you inflate the view from its layout and pass it in to a PersonHolder
        //Заполнение элементов RecyclerView
        //В inflate передаётся вёрстка, которая будет использоваться для построения каждого элемента
        val inflatedView = parent.inflate(R.layout.item_person,false)
        return PersonHolder(inflatedView)
    }

    override fun getItemCount() = data.size//сколько будет на дисплее

    override fun onBindViewHolder(holder: MyRecyclerAdapter.PersonHolder, position: Int) {
        //крч тут инициализируем наши данные
        // - get element from your dataset at this position
        // - replace the contents of the view with that element
        val itemPhoto = data[position]
        holder.bindPerson(itemPhoto)
    }

    class PersonHolder(v: View) : RecyclerView.ViewHolder(v), View.OnClickListener{
        private var view: View = v
        private var person: Person ?= null

        init {
            //инициализация листенера
            v.setOnClickListener(this)
        }

        override fun onClick(v: View?) {
            Log.d("RecyclerView","CLICK!")
            //здесь будет открываться новая карта в localTransport
        }

       /* companion object {
            private val PHOTO_KEY = "PHOTO"
        }*/

        fun bindPerson(person: Person) {
            //Показываем какие именно данные закидываем в каждый элемент
            this.person = person
            //Picasso.with(view.context).load(photo.url).into(view.itemImage)
            view.itemImage.setImageResource(person.photoId)
            view.itemDate.text = person.name
            view.itemDescription.text = person.age
        }
    }

}