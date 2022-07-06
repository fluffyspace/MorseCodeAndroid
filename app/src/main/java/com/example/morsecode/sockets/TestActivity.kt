package com.example.morsecode.sockets

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.morsecode.*
import com.example.morsecode.baza.AppDatabase
import com.example.morsecode.baza.LegProfileDao
import com.example.morsecode.models.LegProfile
import com.google.android.material.slider.Slider
import com.google.android.material.snackbar.Snackbar
import com.google.gson.Gson
import kotlinx.coroutines.*
import java.util.Collections.max
import kotlin.math.abs
import kotlin.math.round
import kotlin.reflect.typeOf


class TestActivity : AppCompatActivity() {
    private lateinit var accelerometer: Accelerometer
    private lateinit var handsFree: HandsFree
    var coroutine: Job? = null
    lateinit var mAccessibilityService: MorseCodeService

    var legDownValues = mutableListOf<Float>()
    var legUpValues = mutableListOf<Float>()
    lateinit var calibrationTimer: TextView
    lateinit var enter_message_edittext: EditText
    lateinit var bottomText: TextView
    lateinit var valuesText: TextView
    lateinit var legStatus: TextView
    lateinit var currentXYZ: TextView
    lateinit var threshold_slider: Slider
    lateinit var threshold_value: TextView
    lateinit var spinner: Spinner
    var legProfiles: MutableList<LegProfile> = mutableListOf()
    var creatingNewProfile = true
    var selectedProfile: LegProfile? = null
    var calibrationWait = 3

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_test)

        accelerometer = Accelerometer(this)
        handsFree = HandsFree()
        enter_message_edittext = findViewById(R.id.enter_message_edittext)
        calibrationTimer = findViewById(R.id.calibrationTimer)
        bottomText = findViewById(R.id.bottomText)
        valuesText = findViewById(R.id.valuesText)
        currentXYZ = findViewById(R.id.currentXYZ)
        legStatus = findViewById(R.id.legStatus)
        threshold_value = findViewById(R.id.threshold_value)
        threshold_slider = findViewById(R.id.slider)
        bottomText.setText("To create new profile, select 'NEW' from dropdown menu, choose its name and click 'CALIBRATE' button to start calibration.\n\nOr select existing profile to recalibrate, rename or delete it.")
        spinner = findViewById(R.id.spinner)

        accelerometer.setListener { x, y, z, xG, yG, zG ->
            handsFree.followAccelerometer(x, y, z, xG, yG, zG)
        }
        accelerometer.register()
        mAccessibilityService = MorseCodeService.getSharedInstance()!!

        threshold_slider.addOnChangeListener { slider, value, fromUser ->
            threshold_value.setText(value.toString())
        }
        handsFree.setListener(object : HandsFree.Listener {
            override fun onTranslation(tap: Int) {
                legStatus.setText(if(tap == HandsFree.UP) "UP" else "DOWN")
            }

            override fun onNewData(x: Float, y: Float, z: Float) {
                if(x == 0f || y == 0f || z == 0f) return
                currentXYZ.setText("${round(x*10)/10}, ${round(y*10)/10}, ${round(z*10)/10}")

                /*if(selectedProfile != null){
                    val xDiff = abs(selectedProfile!!.downX - selectedProfile!!.upX)
                    val yDiff = abs(selectedProfile!!.downY - selectedProfile!!.upY)
                    val zDiff = abs(selectedProfile!!.downZ - selectedProfile!!.upZ)
                    val maxDiff = max(listOf(xDiff, yDiff, zDiff))
                    if(maxDiff == xDiff){
                        if(abs(selectedProfile!!.downX - x) > abs(selectedProfile!!.upX - x)){
                            legStatus.setText("UP")
                        } else if(abs(selectedProfile!!.downX - x) < abs(selectedProfile!!.upX - x)){
                            legStatus.setText("DOWN")
                        } else {
                            legStatus.setText("??")
                        }
                    } else if(maxDiff == yDiff){
                        if(abs(selectedProfile!!.downY - y) > abs(selectedProfile!!.upY - y)){
                            legStatus.setText("UP")
                        } else if(abs(selectedProfile!!.downY - y) < abs(selectedProfile!!.upY - y)){
                            legStatus.setText("DOWN")
                        } else {
                            legStatus.setText("??")
                        }
                    } else if(maxDiff == zDiff){
                        if(abs(selectedProfile!!.downZ - z) > abs(selectedProfile!!.upZ - z)){
                            legStatus.setText("UP")
                        } else if(abs(selectedProfile!!.downZ - z) < abs(selectedProfile!!.upZ - z)){
                            legStatus.setText("DOWN")
                        } else {
                            legStatus.setText("??")
                        }
                    }
                } else {
                    legStatus.setText("")
                }*/

                /*if(selectedProfile != null){
                    if(
                        abs(selectedProfile!!.downX - x) > abs(selectedProfile!!.upX - x) &&
                        abs(selectedProfile!!.downY - y) > abs(selectedProfile!!.upY - y) &&
                        abs(selectedProfile!!.downZ - z) > abs(selectedProfile!!.upZ - z)
                            ){
                        legStatus.setText("UP")
                    } else if(
                        abs(selectedProfile!!.downX - x) < abs(selectedProfile!!.upX - x) &&
                        abs(selectedProfile!!.downY - y) < abs(selectedProfile!!.upY - y) &&
                        abs(selectedProfile!!.downZ - z) < abs(selectedProfile!!.upZ - z)
                    ){
                        legStatus.setText("DOWN")
                    }
                } else {
                    legStatus.setText("")
                }*/
            }
        })

        //create a list of items for the spinner.
        lifecycleScope.launch(Dispatchers.Default) {
            val db = AppDatabase.getInstance(this@TestActivity)
            val legProfileDao: LegProfileDao = db.legProfileDao()
            legProfiles = legProfileDao.getAll().toMutableList()
            withContext(Dispatchers.Main){
                spinner.adapter = getSpinnerItems()
                spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                    override fun onNothingSelected(parent: AdapterView<*>?) {}
                    override fun onItemSelected(
                        parent: AdapterView<*>?,
                        view: View?,
                        position: Int,
                        id: Long
                    ) {
                        val selected = parent?.getItemAtPosition(position)
                        if(selected == "New"){
                            creatingNewProfile = true
                            enter_message_edittext.setText("")
                            selectedProfile = null
                            valuesText.setText("")
                        } else {
                            creatingNewProfile = false
                            selectedProfile = legProfiles.find{ it.name == selected }
                            Log.d("ingo", "selected -> " + Gson().toJson(selectedProfile))
                            enter_message_edittext.setText(selected.toString())
                            showProfileValues()
                            handsFree.profile = selectedProfile
                        }
                        Log.d("ingo", selected.toString())
                    }
                }
            }
        }
        findViewById<Button>(R.id.start).setOnClickListener{
            if(enter_message_edittext.text.toString() != "") {
                coroutine = startCalibration()
            }
        }

        findViewById<Button>(R.id.delete).setOnClickListener{
            if(selectedProfile != null) {
                lifecycleScope.launch(Dispatchers.Default) {
                    val db = AppDatabase.getInstance(this@TestActivity)
                    val legProfileDao: LegProfileDao = db.legProfileDao()
                    Log.d("ingo", legProfileDao.delete(selectedProfile!!).toString())
                    withContext(Dispatchers.Main){
                        Snackbar.make(
                            bottomText,
                            "Profile '${selectedProfile!!.name}' deleted.",
                            Snackbar.LENGTH_LONG
                        ).show()
                        legProfiles.remove(selectedProfile)
                        selectedProfile = null
                        spinner.adapter = getSpinnerItems()
                    }
                }

            }
        }

        findViewById<Button>(R.id.rename).setOnClickListener{
            if(selectedProfile != null) {
                lifecycleScope.launch(Dispatchers.Default) {
                    selectedProfile!!.name = enter_message_edittext.text.toString()
                    selectedProfile!!.threshold = threshold_slider.value
                    val db = AppDatabase.getInstance(this@TestActivity)
                    val legProfileDao: LegProfileDao = db.legProfileDao()
                    Log.d("ingo", legProfileDao.update(selectedProfile!!).toString())
                    withContext(Dispatchers.Main){
                        Snackbar.make(
                            bottomText,
                            "Profile renamed.",
                            Snackbar.LENGTH_LONG
                        ).show()
                        spinner.adapter = getSpinnerItems()
                    }
                }

            }
        }
    }

    fun disableInputs(){

    }

    fun enableInputs(){

    }

    fun showProfileValues(){
        selectedProfile?.let{
            val text = "Down: ${it.downX}, ${it.downY}, ${it.downZ}.\nUp: ${it.upX}, ${it.upY}, ${it.upZ}.\nThreshold: ${round(it.threshold*10)/10}"
            valuesText.setText(text)
        }
    }

    fun getSpinnerItems(): ArrayAdapter<String>{
        val items = arrayOf("New", *legProfiles.map{ it.name }.toTypedArray())
        return ArrayAdapter(this@TestActivity, android.R.layout.simple_spinner_dropdown_item, items)
    }

    fun startCalibration(): Job{
        spinner.isEnabled = false
        enter_message_edittext.isEnabled = false
        return lifecycleScope.launch(Dispatchers.Default) {
            for(downUp in 0..1) {
                for (i in calibrationWait downTo 1) {
                    Log.d(
                        "ingo",
                        "${handsFree.lastXRot} ${handsFree.lastYRot} ${handsFree.lastZRot} "
                    )
                    withContext(Dispatchers.Main) {
                        mAccessibilityService.vibrate(100)
                        if(downUp == 0) {
                            calibrationTimer.setText("Calibration for leg down in $i seconds.")
                        } else {
                            calibrationTimer.setText("Calibration for leg up in $i seconds.")
                        }
                    }
                    delay(1000)
                }
                var lista = listOf(
                    handsFree.lastXRot,
                    handsFree.lastYRot,
                    handsFree.lastZRot
                )
                if (downUp == 0) {
                    legDownValues.clear()
                    legDownValues.addAll(lista)
                    withContext(Dispatchers.Main) {
                        calibrationTimer.setText("Calibrating down position...")
                        mAccessibilityService.vibrate(1000)
                    }
                    delay(1000)
                } else {
                    legUpValues.clear()
                    legUpValues.addAll(lista)
                    var newProfile = LegProfile(
                        id = 0,
                        downX = legDownValues[0],
                        downY = legDownValues[1],
                        downZ = legDownValues[2],
                        upX = legUpValues[0],
                        upY = legUpValues[1],
                        upZ = legUpValues[2],
                        name = enter_message_edittext.text.toString(),
                        threshold = threshold_slider.value
                    )
                    withContext(Dispatchers.Main) {
                        calibrationTimer.setText("Calibrating up position...")
                        mAccessibilityService.vibrate(1000)
                    }
                    delay(1000)
                    if(creatingNewProfile) {
                        val db = AppDatabase.getInstance(this@TestActivity)
                        val legProfileDao: LegProfileDao = db.legProfileDao()
                        val newProfileId = legProfileDao.insertAll(newProfile)
                        newProfile.id = newProfileId[0]
                        legProfiles.add(newProfile)
                        selectedProfile = newProfile
                        withContext(Dispatchers.Main) {
                            showProfileValues()
                            spinner.adapter = getSpinnerItems()
                            spinner.setSelection(legProfiles.size)
                            calibrationTimer.setText("")
                            enter_message_edittext.setText(newProfile.name)
                            mAccessibilityService.vibrate(longArrayOf(100, 300, 100, 300))
                            Snackbar.make(
                                bottomText,
                                "New leg profile '${enter_message_edittext.text}' added!",
                                Snackbar.LENGTH_LONG
                            ).show()
                        }
                    } else {
                        newProfile.id = selectedProfile!!.id
                        selectedProfile = newProfile
                        val db = AppDatabase.getInstance(this@TestActivity)
                        val legProfileDao: LegProfileDao = db.legProfileDao()
                        legProfileDao.update(selectedProfile!!)
                        withContext(Dispatchers.Main) {
                            calibrationTimer.setText("")
                            showProfileValues()
                            Snackbar.make(
                                bottomText,
                                "Profile '${enter_message_edittext.text}' recalibrated.",
                                Snackbar.LENGTH_LONG
                            ).show()
                        }
                    }
                    withContext(Dispatchers.Main){
                        spinner.isEnabled = true
                        enter_message_edittext.isEnabled = true
                    }
                }

                delay(1000)
            }
        }
    }

    override fun onDestroy() {
        coroutine?.cancel()
        accelerometer.unregister()
        super.onDestroy()
    }
}