package com.example.myrecycleview

import com.google.firebase.firestore.GeoPoint

class Busstop (
    val name: String,
    val cords: GeoPoint,
    val buses: MutableList<Bus>
)