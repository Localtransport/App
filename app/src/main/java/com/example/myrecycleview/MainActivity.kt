package com.example.myrecycleview

import android.content.pm.PackageManager
import android.os.Bundle
//import android.support.constraint.ConstraintLayout

//import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.recyclerview.widget.LinearLayoutManager
//Для карты
import android.location.Location
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices

import com.google.android.material.bottomsheet.BottomSheetBehavior
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.bottom_sheet.*
import kotlinx.android.synthetic.main.main_content.*

class MainActivity : AppCompatActivity(), View.OnClickListener,
    //Интерфейсы для карт
    OnMapReadyCallback, GoogleMap.OnMarkerClickListener {
    //Для футбара
    private lateinit var bottomSheetBehavior: BottomSheetBehavior<LinearLayout>
    //Для карты
    private lateinit var mMap: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var lastLocation: Location
    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE  = 1
    }
    //Для данных
    private lateinit var linearLayoutManager: LinearLayoutManager
    private val persons = arrayListOf<Person>()
    private fun initializeData() {

        persons.add(Person("Хаски из Аляски","Ему 5 лет,а тебе?))",R.drawable.husky))
        persons.add(Person("Помираниан (как помираниум, только шпиц)","Ему 6 лет,а тебе?))",R.drawable.pomeranian))
        persons.add(Person("Шипдог (как шиппер,только собака; или как корабль ","Ему 7 лет,а тебе?))",R.drawable.sheepdog))
        persons.add(Person("Это спрингер как машина,вжух,вжух","Ему 5 лет,а тебе?))",R.drawable.springer))
        persons.add(Person("Хаски из Аляски","Ему 5 лет",R.drawable.husky))
        persons.add(Person("Помираниан ","Ему 6 лет,а тебе?))",R.drawable.pomeranian))
        persons.add(Person("Шипдог","Ему 7 лет",R.drawable.sheepdog))
        persons.add(Person("Это спрингер как машина, вжух,вжух","Ему 5 лет",R.drawable.springer))
        persons.add(Person("Кекерики","Ему 5 лет",R.drawable.husky))
        persons.add(Person("Бадум-тсс ","Ему 6 лет)",R.drawable.pomeranian))
        persons.add(Person("Шип-шип","Ему 7 лет",R.drawable.sheepdog))
        persons.add(Person("Вжух,вжух","Ему 5 лет",R.drawable.springer))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initializeData()
        linearLayoutManager = LinearLayoutManager(this)
        rv.apply {
            setHasFixedSize(true)//размер RecyclerView не будет изменяться
            layoutManager = linearLayoutManager
            adapter = MyRecyclerAdapter(persons)
        }
        //Вызов функции,которая собирает футор - в разработке
        initBottomSheet()

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
        //Получение запроса о последнем известном местоположении
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

    }
    //Работа с картами
    override fun onMarkerClick(p0: Marker?) : Boolean {
        Toast.makeText(this, "yea", Toast.LENGTH_SHORT).show()
        slideUpDownBottomSheet()
        return false
    }
    private fun setUpMap() {
        //Проверка, есть ли у нас права на доступ к местоположению пользователя
        if (ActivityCompat.checkSelfPermission(this,
                android.Manifest.permission.ACCESS_FINE_LOCATION) !=
            PackageManager.PERMISSION_GRANTED) {
            //Если нет, то запрашиваем разрешение
            ActivityCompat.requestPermissions(
                this,
                arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
            return
        }
    }
    private fun placeMarkerOnMap(location: LatLng){
        //Создание объекта MarkerOptions и установка текущего
        //местоположения как позиция для маркера
        val markerOptions = MarkerOptions().position(location)
        //Добавление маркера на карту
        mMap.addMarker(markerOptions)
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        //Добавление маркера остановки МИЭТ
        val miet = LatLng(55.983174, 37.210626)
        mMap.addMarker(MarkerOptions().position(miet).title("Остановка МИЭТ"))
        //Наведение камеры на маркер и приближение
        //mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(sydney, 12.0f))

        //Подключение элементов управления масштабом на карте
        mMap.uiSettings.isZoomControlsEnabled = true
        //Подключение листенера при клике на маркер на карте
        mMap.setOnMarkerClickListener(this)
        setUpMap()

        //добавляет синюю точку на user’s location
        //и кнопку на карте,при нажатии на которую карта центрируется относительно пользователя
        mMap.isMyLocationEnabled = true

        //Даёт самое последнее местоположение,доступное в текущее время
        fusedLocationClient.lastLocation.addOnSuccessListener(this) { location ->
            // Got last known location. In some rare situations this can be null.
            // Если удалось получить самое последнее местоположение
            if (location != null) {
                //переместить камеру в текущее местоположеие пользователя
                lastLocation = location
                val currentLatLng = LatLng(location.latitude, location.longitude)
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 12f))
            }
        }
    }

    private fun initBottomSheet() {
        //Устанавливаем листенер (обработчик событий) на кнопку Не надо
        //buttonMain.setOnClickListener(this)

        //подключаем дейстия на BottomSheet
        bottomSheetBehavior = BottomSheetBehavior.from<LinearLayout>(bottom_sheet)
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
        //управление поведением BottomSheet
        bottomSheetBehavior.setBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
            override fun onSlide(bottomSheet: View, slideOffset: Float) {
                // Реакция на события перетаскивания (свайп BottomSheet)
            }

            // Реакция на изменение состояния
            override fun onStateChanged(bottomSheet: View, newState: Int) {
                when (newState) {
                    BottomSheetBehavior.STATE_COLLAPSED -> {
                        //buttonMain.visibility = View.VISIBLE
                        //buttonMain.text = "Slide Up"
                    }
                    BottomSheetBehavior.STATE_HIDDEN -> {
                        //buttonMain.visibility = View.VISIBLE
                        //buttonMain.text = "Slide Down"
                    }
                    BottomSheetBehavior.STATE_EXPANDED -> {
                        //buttonMain.visibility = View.INVISIBLE
                        // buttonMain.text = "Slide Down"
                    }
                    BottomSheetBehavior.STATE_DRAGGING -> {

                    }
                    BottomSheetBehavior.STATE_SETTLING -> {

                    }
                }
            }
        })
    }
    //Листенер для того, на что можно кликать
    override fun onClick(clickView: View?) {
        when (clickView) {//Если нажали на какую-то кнопку
            /*buttonMain -> {//нажали на кнопку с id button
                slideUpDownBottomSheet()
            }*/
        }
    }
    private fun slideUpDownBottomSheet() {
        //Проверяем состояние BottomSheet
        if (bottomSheetBehavior.state == BottomSheetBehavior.STATE_HIDDEN) {
            //Если BottomSheet не открыт, открываем его
            bottomSheetBehavior.peekHeight = 600
            bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
            //buttonMain.text = "Slide Down"
        } else {
            //Сворачиваем BottomSheet
            bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
            //buttonMain.text = "Slide Up"
        }
    }
    //Подключение меню
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        val inflater = menuInflater
        inflater.inflate(R.menu.menu, menu)
        return super.onCreateOptionsMenu(menu)
    }
    //Обработка нажатий элементов меню
    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when (item?.itemId){
            R.id.action_kovrov ->{
                Toast.makeText(this, "Open Kovrov", Toast.LENGTH_SHORT).show()
                return true
            }
            R.id.action_ulsk ->{
                Toast.makeText(this, "Open Ulyanovsk", Toast.LENGTH_SHORT).show()
                return true
            }
            null -> return false
        }
        return super.onOptionsItemSelected(item)
    }
}
