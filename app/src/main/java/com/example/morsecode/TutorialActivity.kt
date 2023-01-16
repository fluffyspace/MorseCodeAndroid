package com.example.morsecode

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.SoundPool
import android.os.Build
import android.os.Bundle
import android.view.MotionEvent
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintSet.Motion
import androidx.core.widget.NestedScrollView
import androidx.lifecycle.lifecycleScope
import com.example.morsecode.Constants.Companion.sharedPreferencesFile
import kotlinx.coroutines.*
import kotlin.random.Random


class TutorialActivity : AppCompatActivity() {
    lateinit var tap_button: Button
    lateinit var tutorial_status_text:TextView
    lateinit var wins_label:TextView
    lateinit var tutorial_status_image:ImageView
    lateinit var tutorial_text_view:TextView
    lateinit var tutorial_number_view:TextView
    lateinit var visual_feedback_container:VisualFeedbackFragment
    var mAccessibilityService:MorseCodeService? = null
    var tutorial_number:Int = 0
    var tutorial_text:String = ""
    //var text_samples:MutableList<String> = mutableListOf("the","of","and","a","to","in","is","you","that","it","he","was","for","on","are","as","with","his","they","I","at","be","this","have","from","or","one","had","by","word","but","not","what","all","were","we","when","your","can","said","there","use","an","each","which","she","do","how","their","if","will","up","other","about","out","many","then","them","these","so","some","her","would","make","like","him","into","time","has","look","two","more","write","go","see","number","no","way","could","people","my","than","first","water","been","call","who","oil","its","now","find","long","down","day","did","get","come","made","may","part")

    lateinit var korutina_next_tutorial: Job
    lateinit var korutina_refresh_text: Job

    var soundPool: SoundPool? = null
    var sound_correct = 0
    var sound_wrong = 0
    var sound_buzz = 0
    var sound_clapping = 0
    var wrong: Boolean = false
    var playing_buzz: Boolean = false
    var buzz_id: Int = -1
    lateinit var scroll_view: NestedScrollView
    lateinit var sharedPreferences: SharedPreferences
    var user_wins = 0
    var load_next_test = false

    companion object{
        var text_samples:MutableList<String> = mutableListOf("bok","sunce","trava","livada","ljubav","snijeg","more","brod","auto","voda","sat","papir","ptica","drvo","pjesma","krov","pas","konj","glazba","stol","planina","brijeg","kamion","motor","mobitel","kompjuter","novine","prozor","terasa","balkon","truba","vatra","led","mir","smijeh","nebo","zvijezda","svemir","planet","struja","poruka","poziv","internet","broj","jezik","cipela","tepih","jakna","cesta","piknik","mraz","brat","sestra","otac","majka","deda","baka","gitara","violina","svjetlo","ples","sport","nogomet","karate","judo","gimnastika","gluma","pjevanje","avion","jedrilica","sok","krema","pita","prijatelj","vjetar","put","hvala","molim","oprosti","stablo","ogledalo","kupaona","kuhinja","hodnik","vrata","radijator","okno","zid","kabel","spavanje","krevet","kupka","stup","beba","dijete","tanjur","vilica","kruh","mlijeko","jabuka")
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tutorial)
        tap_button = findViewById(R.id.tap)
        tutorial_status_text = findViewById(R.id.tutorial_status_text)
        tutorial_status_image = findViewById(R.id.tutorial_status_image)
        tutorial_text_view = findViewById(R.id.tutorial_text_view)
        tutorial_number_view = findViewById(R.id.tutorial_number_view)
        scroll_view = findViewById<NestedScrollView>(R.id.scrollView)

        visual_feedback_container = VisualFeedbackFragment()
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
        wins_label = findViewById(R.id.wins_label)
        wins_label.text = java.lang.StringBuilder("${getString(R.string.sveukupno_pogodeno_rijeci)} ${user_wins}").toString()

        mAccessibilityService = MorseCodeService.getSharedInstance();
        tap_button.setOnTouchListener { v, event ->
            when (event?.action) {
                MotionEvent.ACTION_DOWN -> {
                    if(!playing_buzz) {
                        buzz_id = soundPool!!.play(sound_buzz, 1F, 1F, 0, -1, 1F);
                        playing_buzz = true
                    }
                    visual_feedback_container.down()
                    refreshText()
                    scroll_view.requestDisallowInterceptTouchEvent(true);
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_OUTSIDE, MotionEvent.ACTION_CANCEL -> {
                    playing_buzz = false
                    soundPool!!.stop(buzz_id)
                    visual_feedback_container.up()
                    refreshText()
                    v.performClick()
                    scroll_view.requestDisallowInterceptTouchEvent(false);
                }
            }
            return@setOnTouchListener true
        }
        generateNewTest()

        soundPool = if (Build.VERSION.SDK_INT
            >= Build.VERSION_CODES.LOLLIPOP
        ) {
            val audioAttributes = AudioAttributes.Builder()
                .setUsage(
                    AudioAttributes.USAGE_ASSISTANCE_SONIFICATION
                )
                .setContentType(
                    AudioAttributes.CONTENT_TYPE_SONIFICATION
                )
                .build()
            SoundPool.Builder()
                .setMaxStreams(3)
                .setAudioAttributes(
                    audioAttributes
                )
                .build()
        } else {
            SoundPool(
                4,
                AudioManager.STREAM_MUSIC,
                0
            )
        }
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
            1
        )
        sound_clapping = soundPool!!.load(
            this,
            R.raw.sound_clapping,
            1
        )
    }

    suspend fun nextTutorial() { // this: CoroutineScope
        delay(3000)
        withContext(Dispatchers.Main){
            load_next_test = false
            generateNewTest()
            tutorial_status_image.setImageResource(R.drawable.ic_baseline_check_circle_24)
            tutorial_status_text.text = getString(R.string.so_far_so_good)
        }
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
                        nextTutorial()
                    }
                    visual_feedback_container.reset()
                    wrong = false
                    user_wins += 1
                    wins_label.text = java.lang.StringBuilder("${getString(R.string.sveukupno_pogodeno_rijeci)} ${user_wins}").toString()
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
        if(broj_pobjede % 1 == 0){
            soundPool!!.play(sound_clapping, 1F, 1F, 0, 0, 1F)
            val intent = Intent(this, AwardForWins::class.java)
            intent.putExtra("bigText", "Bravo!");
            intent.putExtra("smallText", "Do sad si riješio ${broj_pobjede} zadataka!");
            intent.putExtra("award_image_id", R.drawable.ic_baseline_help_24);
            startActivity(intent)
        }
    }

    fun generateNewTest(){
        tutorial_number++
        val random_sample_text_index = Random.nextInt(0, text_samples.size)
        tutorial_text = text_samples[random_sample_text_index].lowercase()
        text_samples.removeAt(random_sample_text_index)

        tutorial_number_view.text = "${getString(R.string.tutorial)}: " + (user_wins+1)
        tutorial_text_view.text = "Pokušajte upisati: " + tutorial_text
    }

    fun cancelKorutina(){
        if(::korutina_next_tutorial.isInitialized && korutina_next_tutorial.isActive) {
            korutina_next_tutorial.cancel()
        }
        if(::korutina_refresh_text.isInitialized && korutina_refresh_text.isActive) {
            korutina_refresh_text.cancel()
        }
    }

    override fun onResume() {
        toggleTesting(true)
        korutina_refresh_text = lifecycleScope.launch(Dispatchers.Default){
            refreshTextJob()
        }
        if(load_next_test){
            generateNewTest()
            tutorial_status_image.setImageResource(R.drawable.ic_baseline_check_circle_24)
            tutorial_status_text.text = getString(R.string.so_far_so_good)
            load_next_test = false
        }
        super.onResume()
    }

    override fun onPause() {
        super.onPause()
        toggleTesting(false)
        cancelKorutina()
    }

    fun toggleTesting(testing:Boolean){
        mAccessibilityService?.toggleTesting(testing)
    }
}