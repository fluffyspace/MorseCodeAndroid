package com.ingokodba.morsecode

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.media.AudioAttributes
import android.media.SoundPool
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.NestedScrollView
import androidx.lifecycle.lifecycleScope
import com.ingokodba.morsecode.Constants.Companion.sharedPreferencesFile
import com.ingokodba.morsecode.MorseCodeService.Companion.ALPHANUM
import com.ingokodba.morsecode.MorseCodeService.Companion.prettifyMorse
import com.ingokodba.morsecode.MorseCodeService.Companion.stringToMorse
import com.ingokodba.morsecode.TutorialActivity.Companion.text_samples
import kotlinx.coroutines.*
import kotlin.random.Random


class PlaygroundActivity : AppCompatActivity(), PhysicalButtonsService.OnKeyListener {
    lateinit var tap_button: Button
    lateinit var tutorial_status_text:TextView
    lateinit var number_of_letters_label:TextView
    lateinit var letters_up:ImageButton
    lateinit var letters_down:ImageButton
    lateinit var speed_label:TextView
    lateinit var speed_up:ImageButton
    lateinit var speed_down:ImageButton
    lateinit var rjesenje_button:Button
    lateinit var rjesenje_label:TextView
    lateinit var tutorial_status_image:ImageView
    lateinit var speaker:ImageView
    lateinit var tutorial_number_view:TextView
    lateinit var visual_feedback_container:VisualFeedbackFragment
    var mAccessibilityService:MorseCodeService? = null
    var tutorial_number:Int = 0
    var tutorial_text:String = ""
    //var text_samples:MutableList<String> = mutableListOf("the","of","and","a","to","in","is","you","that","it","he","was","for","on","are","as","with","his","they","I","at","be","this","have","from","or","one","had","by","word","but","not","what","all","were","we","when","your","can","said","there","use","an","each","which","she","do","how","their","if","will","up","other","about","out","many","then","them","these","so","some","her","would","make","like","him","into","time","has","look","two","more","write","go","see","number","no","way","could","people","my","than","first","water","been","call","who","oil","its","now","find","long","down","day","did","get","come","made","may","part")
    lateinit var korutina_next_tutorial: Job
    lateinit var korutina_refresh_text: Job
    lateinit var korutina_play: Job
    var numberOfLetters = 1

    var soundPool: SoundPool? = null
    var sound_correct = 0
    var sound_wrong = 0
    var sound_buzz = 0
    var sound_clapping = 0
    var wrong: Boolean = false
    var playing_buzz: Boolean = false
    var buzz_id: Int = -1
    var buzz_id2: Int = -1
    lateinit var scroll_view: NestedScrollView
    lateinit var sharedPreferences: SharedPreferences
    var user_wins = 0
    var load_next_test = false
    var speed: Long = 0
    var award_interval: Int = 0
    var award_sound: Int = 0

    var keyIsDown = false

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_playground)
        tap_button = findViewById(R.id.tap)
        tutorial_status_text = findViewById(R.id.tutorial_status_text)
        tutorial_status_image = findViewById(R.id.tutorial_status_image)
        speaker = findViewById(R.id.speaker)
        tutorial_number_view = findViewById(R.id.tutorial_number_view)
        scroll_view = findViewById<NestedScrollView>(R.id.scrollView)
        number_of_letters_label = findViewById(R.id.number_of_letters_label)
        rjesenje_button = findViewById(R.id.rjesenje_button)
        rjesenje_label = findViewById(R.id.rjesenje_label)
        letters_up = findViewById(R.id.letters_up)
        letters_down = findViewById(R.id.letters_down)
        speed_up = findViewById(R.id.speed_up)
        speed_down = findViewById(R.id.speed_down)
        speed_label = findViewById(R.id.speed_label)

        mAccessibilityService = MorseCodeService.getSharedInstance();

        number_of_letters_label.text = getString(R.string.broj_slova) + numberOfLetters

        letters_up.setOnClickListener {
            numberOfLetters++
            number_of_letters_label.text = getString(R.string.broj_slova) + numberOfLetters
            nextTutorial()
        }
        letters_down.setOnClickListener {
            if(numberOfLetters > 1) numberOfLetters--
            number_of_letters_label.text = getString(R.string.broj_slova) + numberOfLetters
            nextTutorial()
        }
        speed = mAccessibilityService!!.servicePostavke.oneTimeUnit
        speed_label.text = getString(R.string.brzina_koda) + speed

        speed_up.setOnClickListener {
            speed += 50
            speed_label.text = getString(R.string.brzina_koda) + speed
        }
        speed_down.setOnClickListener {
            if(speed > 50) speed -= 50
            speed_label.text = getString(R.string.brzina_koda) + speed
        }

        findViewById<Button>(R.id.upute).setOnClickListener {
            startActivity(Intent(this, PlaygroundInstructions::class.java))
        }

        visual_feedback_container = VisualFeedbackFragment()
        visual_feedback_container.gameMode = true
        visual_feedback_container.smaller = false
        supportFragmentManager
            .beginTransaction()
            .add(R.id.visual_feedback_container, visual_feedback_container, "main")
            .commitNow()

        /*findViewById<Button>(R.id.tablica).setOnClickListener {
            val intent = Intent(this, MorseTable::class.java)
            startActivity(intent)
        }*/
        sharedPreferences =
            this.getSharedPreferences(sharedPreferencesFile, Context.MODE_PRIVATE)
        user_wins = sharedPreferences.getInt(Constants.USER_WINS, 0)
        award_interval = sharedPreferences.getInt(Constants.AWARD_INTERVAL, 20)

        findViewById<Button>(R.id.btn_broj_slova).setOnClickListener {
            // ovdje otvoriti alert dialog za unos broja slova
            alertDialogWithMultipleOptions("Odaberi maksimalan broj slova", arrayOf("1", "2", "3", "4", "5", "6", "7")) { index ->
                numberOfLetters = index + 1
                number_of_letters_label.text = getString(R.string.broj_slova) + numberOfLetters
                nextTutorial()
            }
        }

        findViewById<Button>(R.id.btn_trajanje_tocke).setOnClickListener {
            // ovdje otvoriti alert dialog za trajanje tocke
            alertDialogWithMultipleOptions("Odaberi brzinu primanja koda", arrayOf("Jako brzo", "Brzo", "Normalno", "Sporo", "Jako sporo")) { index ->
                speed = (index+1)*200L
                speed_label.text = getString(R.string.brzina_koda) + speed
                nextTutorial()
            }
        }

        speaker.imageTintList = getColorStateList(R.color.obrub)
        speaker.setOnClickListener {
            //speaker.setImageDrawable(getDrawable(R.drawable.volume_up_svg_yellow))
            if(::korutina_play.isInitialized && korutina_play.isActive) {
                korutina_play.cancel()
                soundPool!!.stop(buzz_id2)
                speaker.imageTintList = getColorStateList(R.color.obrub)
            } else {
                speaker.imageTintList = getColorStateList(R.color.orange)
                korutina_play = lifecycleScope.launch(Dispatchers.Default) {
                    tutorial_text.forEach { slovo ->
                        val index: Int = ALPHANUM.indexOfFirst { it == slovo }
                        val morse: String = MorseCodeService.MORSE.get(index)
                        for (i: Int in morse.indices) {
                            withContext(Dispatchers.Main) {
                                buzz_id2 = soundPool!!.play(sound_buzz, 1F, 1F, 0, -1, 1F)
                            }
                            if (morse[i] == '.') {
                                delay(speed)
                            } else if (morse[i] == '-') {
                                delay(speed * 3)
                            }
                            withContext(Dispatchers.Main) {
                                soundPool!!.stop(buzz_id2)
                            }
                            delay(speed)
                        }
                        delay(speed * 2)
                    }
                    withContext(Dispatchers.Main) {
                        //speaker.setImageDrawable(getDrawable(R.drawable.volume_up_svg))
                        speaker.imageTintList = getColorStateList(R.color.obrub)
                    }
                }
            }
        }

        rjesenje_button.setOnClickListener {
            it.visibility = View.GONE
            rjesenje_label.visibility = View.VISIBLE
        }

        tap_button.setOnTouchListener { v, event ->
            when (event?.action) {
                MotionEvent.ACTION_DOWN -> {
                    downButton()
                    visual_feedback_container.down()
                    scroll_view.requestDisallowInterceptTouchEvent(true);
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_OUTSIDE, MotionEvent.ACTION_CANCEL -> {
                    upButton()
                    visual_feedback_container.up()
                    v.performClick()
                    scroll_view.requestDisallowInterceptTouchEvent(false);
                }
            }
            return@setOnTouchListener true
        }
        generateNewTest()

        val audioAttributes = AudioAttributes.Builder()
            .setUsage(
                AudioAttributes.USAGE_GAME
            )
            .setContentType(
                AudioAttributes.CONTENT_TYPE_SONIFICATION
            )
            .build()
        soundPool = SoundPool.Builder()
                    .setMaxStreams(4)
                    .setAudioAttributes(
                        audioAttributes
                    )
                    .build()
        sound_correct = soundPool!!.load(
                this,
                R.raw.sound_correct,
                1
            )
        sound_wrong = soundPool!!.load(
            this,
            R.raw.sound_wrong,
            1
        )

        sound_buzz = soundPool!!.load(
            this,
            R.raw.sound_morse,
            2
        )
        sound_clapping = soundPool!!.load(
            this,
            R.raw.sound_clapping,
            1
        )
    }

    fun downButton(){
        if(!playing_buzz) {
            buzz_id = soundPool!!.play(sound_buzz, 1F, 1F, 1, -1, 1F);
            playing_buzz = true
        }
        refreshText()
    }

    fun upButton(){
        playing_buzz = false
        soundPool!!.stop(buzz_id)
        refreshText()
    }

    override fun onKey(pressed: Boolean) {
        if(pressed){
            downButton()
        } else {
            upButton()
        }
    }

    override fun keyAddedOrRemoved() {

    }

    fun chooseNumberOfLetters(number: Int){
        numberOfLetters = number
        generateNewTest()
    }

    fun alertDialogWithMultipleOptions(title: String, options: Array<String>, callback: (Int) -> Unit) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle(title)
        builder.setItems(options) { dialog, which ->
            callback(which)
            dialog.dismiss()
        }
        builder.show()
    }

    fun nextTutorial() { // this: CoroutineScope
            load_next_test = false
            generateNewTest()
            tutorial_status_image.setImageResource(R.drawable.ic_baseline_check_circle_24)
            tutorial_status_text.text = getString(R.string.start_tapping)
    }

    suspend fun refreshTextJob() { // this: CoroutineScope
        while(true) {
            delay(100)
            withContext(Dispatchers.Main) {
                refreshText()
            }
        }
    }

    fun refreshText(){
        if(mAccessibilityService?.buttonHistory?.size!! >= 2) {
            var tekst = mAccessibilityService?.getMessage()
            tekst = tekst?.replace("\\s".toRegex(), "")
            if(tekst != null){
                if(tutorial_text == tekst){
                    tutorial_status_image.setImageResource(R.drawable.ic_baseline_check_circle_24)
                    tutorial_status_text.text = tekst + " - bravo! Učitavam novi test..."
                    load_next_test = true
                    soundPool!!.play(sound_correct, 1F, 1F, 0, 0, 1F);
                    if(::korutina_next_tutorial.isInitialized && korutina_next_tutorial.isActive) {
                        korutina_next_tutorial.cancel()
                    }
                    korutina_next_tutorial = lifecycleScope.launch(Dispatchers.Default){
                        delay(3000)
                        withContext(Dispatchers.Main) {
                            nextTutorial()
                        }
                    }
                    visual_feedback_container.reset()
                    wrong = false
                    user_wins += 1
                    val editor = sharedPreferences.edit()
                    editor.putInt(Constants.USER_WINS, user_wins)
                    editor.apply()
                    editor.commit()
                    mozdaNagradi(user_wins)
                } else if(tutorial_text.startsWith(tekst)){
                    tutorial_status_image.setImageResource(R.drawable.ic_baseline_check_circle_24)
                    tutorial_status_text.text = tekst + " - ${getString(R.string.so_far_so_good)}"
                    wrong = false
                } else if(tekst.length > 1 && tutorial_text.startsWith(tekst.slice(IntRange(0, tekst.length-2))) && mAccessibilityService?.isCharacterFinished() == false){
                    tutorial_status_image.setImageResource(R.drawable.ic_baseline_check_circle_24)
                    tutorial_status_text.text = tekst.slice(IntRange(0, tekst.length-2)) + " - ${getString(R.string.so_far_so_good)}"
                    wrong = false
                } else if(tekst.length > 0 && mAccessibilityService?.isCharacterFinished() == true){
                    tutorial_status_image.setImageResource(R.drawable.ic_baseline_cancel_24)
                    tutorial_status_text.text = tekst + " - ${getString(R.string.wrong)}"
                    visual_feedback_container.reset()
                    if(!wrong) {
                        soundPool!!.play(sound_wrong, 1F, 1F, 0, 0, 1F);
                    }
                    wrong = true
                }
            }
        }
    }

    fun mozdaNagradi(broj_pobjede: Int){
        if(broj_pobjede % award_interval == 0){
            award_sound = soundPool!!.play(sound_clapping, 1F, 1F, 0, 0, 1F)
            val intent = Intent(this, AwardForWins::class.java)
            intent.putExtra("bigText", "Bravo!");
            intent.putExtra("smallText", "Do sad si riješio ${broj_pobjede} zadatka!");
            intent.putExtra("award_image_id", R.drawable.ic_baseline_help_24);
            startActivity(intent)
        }
    }

    fun generateNewTest(){
        rjesenje_button.visibility = View.VISIBLE
        rjesenje_label.visibility = View.GONE
        tutorial_number++
        val random_sample_text_index = Random.nextInt(0, text_samples.size)
        tutorial_text = text_samples[random_sample_text_index].lowercase().take(numberOfLetters)
        text_samples.removeAt(random_sample_text_index)

        tutorial_number_view.text = "${getString(R.string.tutorial)}: " + (user_wins+1)
        rjesenje_label.text = "${prettifyMorse(stringToMorse(tutorial_text))} (${tutorial_text})"
    }

    fun cancelKorutina(){
        if(::korutina_next_tutorial.isInitialized && korutina_next_tutorial.isActive) {
            korutina_next_tutorial.cancel()
        }
        if(::korutina_refresh_text.isInitialized && korutina_refresh_text.isActive) {
            korutina_refresh_text.cancel()
        }
        if(::korutina_play.isInitialized && korutina_play.isActive) {
            korutina_play.cancel()
            soundPool!!.stop(buzz_id2)
        }
    }

    override fun onResume() {
        soundPool?.stop(award_sound)
        toggleTesting(true)
        korutina_refresh_text = lifecycleScope.launch(Dispatchers.Default){
            refreshTextJob()
        }
        if(load_next_test){
            generateNewTest()
            tutorial_status_image.setImageResource(R.drawable.ic_baseline_check_circle_24)
            tutorial_status_text.text = getString(R.string.start_tapping)
            load_next_test = false
        }
        PhysicalButtonsService.getSharedInstance()?.addListener(this)
        MorseCodeService.getSharedInstance()?.dont_check_input = true
        super.onResume()
    }

    override fun onPause() {
        super.onPause()
        toggleTesting(false)
        PhysicalButtonsService.getSharedInstance()?.removeListener(this)
        MorseCodeService.getSharedInstance()?.dont_check_input = false
        cancelKorutina()
    }

    override fun onDestroy() {
        super.onDestroy()
        soundPool?.release();
        soundPool = null;
        Log.d("ingo", "playground activity ondestroy")
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if(mAccessibilityService?.servicePostavke?.physicalButtons?.contains(keyCode) == true){
            if(!keyIsDown) {
                keyIsDown = true
                downButton()
                visual_feedback_container.down()
            }
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean {
        if(mAccessibilityService?.servicePostavke?.physicalButtons?.contains(keyCode) == true){
            if(keyIsDown) {
                keyIsDown = false
                upButton()
                visual_feedback_container.up()
            }
            return true
        }
        return super.onKeyUp(keyCode, event)
    }

    fun toggleTesting(testing:Boolean){
        mAccessibilityService?.toggleTesting(testing)
    }
}