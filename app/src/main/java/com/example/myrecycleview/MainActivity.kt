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
import android.util.Log
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.tasks.Tasks

import com.google.android.material.bottomsheet.BottomSheetBehavior
import kotlinx.android.synthetic.main.bottom_sheet.*
import kotlinx.android.synthetic.main.main_content.*
import com.google.firebase.Timestamp
import com.google.firebase.firestore.*
import kotlinx.android.synthetic.main.item_bus_stop.view.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import java.util.*


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
    private lateinit var db : FirebaseFirestore
    private val busStops = mutableListOf<Busstop>()

    //Считывание данных из БД
    private fun readBusstops(myCallback: (MutableList<Busstop>) -> Unit)
    {
        db = FirebaseFirestore.getInstance()
        val busstop = db.collection("busstop")
        var buff : Busstop
//.whereEqualTo("town", "Zel")
        busstop.get().addOnSuccessListener {
            it.forEach { it1 ->
                buff = Busstop(it1.get("name").toString(), it1.get("cords") as GeoPoint, mutableListOf<Bus>())
                setBusstopOnMap(buff)//ставим остановку на карту
                busStops.add(buff)//добавляем остановку в лист

                Log.d("PPP", "In list Cords of ${busStops.last().name} is ${busStops.last().cords}")
            }
            myCallback(busStops)//сообщаем о том, что мы считали данные
        }
    }
    private fun readBus(bufstop: Busstop)
    {
        db.collection("actualtime").whereEqualTo("name", bufstop.name).get().addOnSuccessListener {
            it.forEach { it1 ->
                bufstop.buses.add(
                    Bus(it1.get("rows").toString(), (it1.getTimestamp("time") as Timestamp).toDate())
                )
                Log.d("KKK", bufstop.name)
                Log.d("KKK", bufstop.buses.size.toString())
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        //Считывание данных из БД
        readBusstops(){
            for (bs in busStops) {
                readBus(bs)
            }
            mMap.setOnMarkerClickListener(this)
        }
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
        //Получение запроса о последнем известном местоположении
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        linearLayoutManager = LinearLayoutManager(this)
        rv.apply {
            setHasFixedSize(true)//размер RecyclerView не будет изменяться
            layoutManager = linearLayoutManager
           //if (busStops.first().buses != null)
            // adapter = MyRecyclerAdapter(busStops.first().buses)
        }
        initBottomSheet()

    }


    //Работа с картами
    //Функция загрузки нужных данных в recyclerView
    //вызывается при нажатии на остановку
    override fun onMarkerClick(p0: Marker?) : Boolean {
        if (p0 != null) {
            val buff = busStops.find { it.name == p0.title }
            if (buff != null)
            {
                linearLayoutManager = LinearLayoutManager(this)
                rv.apply {
                    name_busstop.text = buff.name
                    setHasFixedSize(true)//размер RecyclerView не будет изменяться
                    layoutManager = linearLayoutManager
                    val b = actualBuses(buff.buses)//("сортировка списка автобусов по времени прибытия")
                    if (b != null)
                        adapter = MyRecyclerAdapter(b)
                    else
                        adapter = DefaultAdapter("Пока что автобусов нет :с")
                }
                initBottomSheet()
            }
            slideUpDownBottomSheet()
            return true
        }
        return false
    }
    //Находит актуальные автобусы для текущего запроса, чтобы отсправить их в RV
    private fun actualBuses(busInStop: MutableList<Bus> ) : MutableList<BusForAdapter>? {
        val bu = mutableListOf<BusForAdapter>()
        var timeNow = Date()
        var buf = Date()
        var timeArrival : Int
        var timeNoww : Int
        var bufTime : Int
        var b: BusForAdapter?
        for (bus in busInStop) {
                timeArrival = bus.arrivalTime.hours * 60 + bus.arrivalTime.hours
                timeNoww = timeNow.hours * 60 + timeNow.minutes
                bufTime = timeArrival - timeNoww
                if (bufTime > 0) {
                    buf.minutes = bufTime % 60
                    b = bu.find { it.busNumber == bus.busNumber }
                    if (bufTime <= 15) {
                        if (b == null)
                            bu.add(BusForAdapter(bus.busNumber, buf.minutes.toString().plus(" мин")))
                        else if ( b != null)
                            b.arrivalTime =
                                b.arrivalTime + "\n" +  buf.minutes.toString() + " мин"
                    }
                    else if (bufTime < 120)
                    {
                        if (b != null) {
                            if (bus.arrivalTime.minutes < 10)
                                b.arrivalTime =
                                    b.arrivalTime + "\n" + bus.arrivalTime.hours.toString() + ":0" + bus.arrivalTime.minutes.toString()
                            else
                                b.arrivalTime =
                                    b.arrivalTime + "\n" + bus.arrivalTime.hours.toString() + ":" + bus.arrivalTime.minutes.toString()
                        }
                        else {
                            if (bus.arrivalTime.minutes < 10)
                                bu.add(
                                    BusForAdapter(
                                        bus.busNumber,
                                        bus.arrivalTime.hours.toString() + ":0" + bus.arrivalTime.minutes.toString()
                                    ))
                            else
                                bu.add(
                                    BusForAdapter(
                                        bus.busNumber,
                                        bus.arrivalTime.hours.toString() + ":" + bus.arrivalTime.minutes.toString()
                                    ))
                        }
                    }
                }
        }
        if (bu.isNotEmpty())
            return bu
        return null
    }
    //Подключение карты
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
    //Добавление маркера остановки на карту
    private fun setBusstopOnMap (bStop: Busstop)
    {
        val b = LatLng(bStop.cords.latitude, bStop.cords.longitude)
        mMap.addMarker(MarkerOptions().position(b).title(bStop.name))
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        //Добавление маркера остановки МИЭТ
        for (busstop in busStops)
            setBusstopOnMap(busstop)
        /**
        val miet = LatLng(55.983174, 37.210626)
        mMap.addMarker(MarkerOptions().position(miet).title("Остановка МИЭТ"))
        val qwe = LatLng(55.983374, 37.210926)
        mMap.addMarker(MarkerOptions().position(qwe).title("Остановка qwe"))
        //Наведение камеры на маркер и приближение
        //mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(sydney, 12.0f))
        */

        //Подключение элементов управления масштабом на карте
        mMap.uiSettings.isZoomControlsEnabled = true
        //Подключение листенера при клике на маркер на карте
        //mMap.setOnMarkerClickListener(this)
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

    //Работа с футором
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
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(LatLng(56.363715, 41.312183), 12f))
                //Toast.makeText(this, "Open Kovrov", Toast.LENGTH_SHORT).show()
                return true
            }
            R.id.action_ulsk ->{
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(LatLng(55.983284, 37.209307), 12f))
                //Toast.makeText(this, "Open Ulyanovsk", Toast.LENGTH_SHORT).show()
                return true
            }
            null -> return false
        }
        return super.onOptionsItemSelected(item)
    }
}
