package com.ingokodba.morsecode

import android.graphics.Typeface
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.TextView
import androidx.lifecycle.lifecycleScope
import com.ingokodba.morsecode.Adapters.CommandListener
import com.ingokodba.morsecode.MorseCodeServiceCommands.*
import kotlinx.coroutines.*

class MorseServiceVisualised : AppCompatActivity(), CommandListener {
    val textViewMap = HashMap<MorseCodeServiceCommands, TextView>()
    var last_command: MorseCodeServiceCommands = MAIN
    var new_command: MorseCodeServiceCommands = MAIN
    var mAccessibilityService: MorseCodeService? = null
    var physicalButtonsService: PhysicalButtonsService? = null
    lateinit var korutina: Job
    lateinit var last_entered_characters:TextView
    lateinit var last_characters_vibrated:TextView
    lateinit var go_back:TextView
    lateinit var repeat:TextView
    lateinit var howtousephysicalbuttons:TextView
    var last_bottom_command: MorseCodeServiceCommands? = null
    var new_bottom_command: MorseCodeServiceCommands? = null
    var last_characters_entered = ""
    var last_vibrated_characters = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_morse_service_visualised)
        textViewMap[MAIN] = findViewById<TextView>(R.id.main)
        textViewMap[CONTACTS] = findViewById<TextView>(R.id.contacts)
        textViewMap[CONTACTS_NEXT] = findViewById<TextView>(R.id.contacts_next)
        textViewMap[CONTACTS_PREVIOUS] = findViewById<TextView>(R.id.contacts_previous)
        textViewMap[CONTACTS_CHOOSE] = findViewById<TextView>(R.id.contacts_choose)
        textViewMap[CHAT_LAST_MESSAGE] = findViewById<TextView>(R.id.chat_last_message)
        textViewMap[CHAT_PREVIOUS] = findViewById<TextView>(R.id.chat_message_before)
        textViewMap[CHAT_NEXT] = findViewById<TextView>(R.id.chat_message_after)
        textViewMap[CHAT_SEND_NEW_MESSAGE] = findViewById<TextView>(R.id.chat_send_message)
        textViewMap[FILES] = findViewById<TextView>(R.id.files_read)
        textViewMap[FILES_PICKER] = findViewById<TextView>(R.id.files_picker)
        textViewMap[FILES_CHOOSE_NEXT] = findViewById<TextView>(R.id.files_open_next)
        textViewMap[FILES_CHOOSE_PREVIOUS] = findViewById<TextView>(R.id.files_open_previous)
        textViewMap[FILES_OPEN] = findViewById<TextView>(R.id.files_open_current)
        textViewMap[FILES_SEARCH] = findViewById<TextView>(R.id.files_search)
        textViewMap[FILES_SEARCH_NEXT] = findViewById<TextView>(R.id.files_search_next)
        textViewMap[FILES_SEARCH_PREVIOUS] = findViewById<TextView>(R.id.files_search_previous)
        textViewMap[FILES_NEW] = findViewById<TextView>(R.id.files_new)
        textViewMap[FILES_SAVE] = findViewById<TextView>(R.id.files_save)
        textViewMap[FILES_WRITE] = findViewById<TextView>(R.id.files_insert)
        textViewMap[FILES_NEXT_LINE] = findViewById<TextView>(R.id.files_next_line)
        textViewMap[FILES_PREVIOUS_LINE] = findViewById<TextView>(R.id.files_previous_line)
        textViewMap[INTERNET] = findViewById<TextView>(R.id.internet_main)
        textViewMap[INTERNET_SWITCH_ENGINE] = findViewById<TextView>(R.id.internet_engine_switch)
        textViewMap[INTERNET_FEELING_LUCKY] = findViewById<TextView>(R.id.internet_feeling_lucky)
        textViewMap[INTERNET_SEARCH] = findViewById<TextView>(R.id.internet_search)
        textViewMap[INTERNET_SEARCH_NEXT] = findViewById<TextView>(R.id.internet_search_next)
        textViewMap[INTERNET_SEARCH_PREVIOUS] = findViewById<TextView>(R.id.internet_search_previous)
        textViewMap[INTERNET_SEARCH_SKIP_SENTENCE] = findViewById<TextView>(R.id.internet_search_skip_sentence)

        mAccessibilityService = MorseCodeService.getSharedInstance()
        mAccessibilityService?.setListener(this)
        if(mAccessibilityService?.lastCommand != null){
            last_command = mAccessibilityService?.lastCommand!!
            new_command = last_command
        }
        textViewMap[last_command]?.setTypeface(null, Typeface.BOLD);

        val textMap = HashMap<MorseCodeServiceCommands, String>()
        textMap.apply {
            put(MorseCodeServiceCommands.MAIN, getString(R.string.main_menu))
            put(MorseCodeServiceCommands.CONTACTS, getString(R.string.contacts))
            put(MorseCodeServiceCommands.CONTACTS_NEXT, getString(R.string.next_contact))
            put(MorseCodeServiceCommands.CONTACTS_PREVIOUS, getString(R.string.previous_contact))
            put(MorseCodeServiceCommands.CONTACTS_CHOOSE, getString(R.string.open_contact))
            put(MorseCodeServiceCommands.CHAT_LAST_MESSAGE, getString(R.string.listen_to_last_received_message))
            put(MorseCodeServiceCommands.CHAT_PREVIOUS, getString(R.string.listen_to_message_before))
            put(MorseCodeServiceCommands.CHAT_NEXT, getString(R.string.listen_to_message_after))
            put(MorseCodeServiceCommands.CHAT_SEND_NEW_MESSAGE, getString(R.string.send_a_new_message))
            put(MorseCodeServiceCommands.FILES, getString(R.string.read_from_files))
            put(MorseCodeServiceCommands.FILES_PICKER, getString(R.string.choose_file))
            put(MorseCodeServiceCommands.FILES_CHOOSE_NEXT, getString(R.string.next_file))
            put(MorseCodeServiceCommands.FILES_CHOOSE_PREVIOUS, getString(R.string.previous_file))
            put(MorseCodeServiceCommands.FILES_OPEN, getString(R.string.open_file))
            put(MorseCodeServiceCommands.FILES_SEARCH, getString(R.string.search_in_file))
            put(MorseCodeServiceCommands.FILES_SEARCH_NEXT, getString(R.string.next_occurence))
            put(MorseCodeServiceCommands.FILES_SEARCH_PREVIOUS, getString(R.string.previous_occurence))
            put(MorseCodeServiceCommands.FILES_NEW, getString(R.string.new_file))
            put(MorseCodeServiceCommands.FILES_SAVE, getString(R.string.save_file))
            put(MorseCodeServiceCommands.FILES_WRITE, getString(R.string.write_to_file))
            put(MorseCodeServiceCommands.FILES_NEXT_LINE, getString(R.string.files_next_line))
            put(MorseCodeServiceCommands.FILES_PREVIOUS_LINE, getString(R.string.files_previous_line))
            put(MorseCodeServiceCommands.INTERNET, getString(R.string.search_internet))
            put(MorseCodeServiceCommands.INTERNET_SWITCH_ENGINE, getString(R.string.switch_search_engine))
            put(MorseCodeServiceCommands.INTERNET_FEELING_LUCKY, getString(R.string.feeling_lucky_on_off))
            put(MorseCodeServiceCommands.INTERNET_SEARCH, getString(R.string.search))
            put(MorseCodeServiceCommands.INTERNET_SEARCH_NEXT, getString(R.string.next_result))
            put(MorseCodeServiceCommands.INTERNET_SEARCH_PREVIOUS, getString(R.string.previous_result))
            put(MorseCodeServiceCommands.INTERNET_SEARCH_SKIP_SENTENCE, getString(R.string.skip_sentence))
            put(MorseCodeServiceCommands.GO_BACK, getString(R.string.go_back))
            put(MorseCodeServiceCommands.REPEAT, getString(R.string.repeat))
        }

        textViewMap[CONTACTS]?.text = getString(R.string.contacts) + " (${MorseCodeService.Companion.CONTACTS_KEY})"
        textViewMap[INTERNET]?.text = getString(R.string.search_internet) + " (${MorseCodeService.Companion.INTERNET_KEY})"
        textViewMap[FILES]?.text = getString(R.string.read_from_files) + " (${MorseCodeService.Companion.FILES_KEY})"

        howtousephysicalbuttons = findViewById(R.id.howtousephysicalbuttons)
        howtousephysicalbuttons.text = StringBuilder("Navigation:\nNEXT = ${MorseCodeService.SHORTCUT_NEXT}\n" +
                "PREVIOUS = ${MorseCodeService.SHORTCUT_PREVIOUS}\n" +
                "CONFIRM = ${MorseCodeService.SHORTCUT_CONFIRM}\n" +
                "EXTRA = ${MorseCodeService.SHORTCUT_EXTRA}\n" +
                "REPEAT = ${MorseCodeService.SHORTCUT_REPEAT}\n" +
                "BACK = ${MorseCodeService.SHORTCUT_BACK}\n" +
                "VIBRATION OK = ${MorseCodeService.VIBRATION_OK}\n" +
                "VIBRATION NOT OK = ${MorseCodeService.VIBRATION_NOT_OK}"
        ).toString()

        last_entered_characters = findViewById(R.id.last_entered_characters)
        go_back = findViewById(R.id.go_back)
        repeat = findViewById(R.id.repeat)
        last_characters_vibrated = findViewById(R.id.last_characters_vibrated)

        physicalButtonsService = PhysicalButtonsService.getSharedInstance()

        korutina = lifecycleScope.launch(Dispatchers.Default) {
            while(true) {
                delay(500) // non-blocking delay for one dot duration (default time unit is ms)

                withContext(Dispatchers.Main){
                    if(last_command != new_command) {
                        textViewMap[last_command]?.setTypeface(null, Typeface.NORMAL);
                        textViewMap[new_command]?.setTypeface(null, Typeface.BOLD);
                        last_command = new_command
                        if(last_bottom_command == new_bottom_command){
                            repeat?.setTypeface(null, Typeface.NORMAL);
                            go_back?.setTypeface(null, Typeface.NORMAL);
                        }
                    }
                    last_entered_characters.text = "Last entered: $last_characters_entered"
                    last_characters_vibrated.text = "Last vibrated: $last_vibrated_characters"
                    if(last_bottom_command != new_bottom_command){
                        if(new_bottom_command == GO_BACK){
                            repeat?.setTypeface(null, Typeface.NORMAL);
                            go_back?.setTypeface(null, Typeface.BOLD);
                        } else if(new_bottom_command == REPEAT){
                            go_back?.setTypeface(null, Typeface.NORMAL);
                            repeat?.setTypeface(null, Typeface.BOLD);
                        }
                        last_bottom_command = new_bottom_command
                    }
                }
            }
        }
    }

    override fun commandChanged(command: MorseCodeServiceCommands){
        new_command = command
    }

    override fun bottomCommand(command: MorseCodeServiceCommands) {
        new_bottom_command = command
    }

    override fun lastCharactersEntered(characters: String) {
        last_characters_entered = characters
    }

    override fun lastCharactersVibrated(characters: String) {
        last_vibrated_characters = characters
    }

    override fun onDestroy() {
        korutina.cancel()
        mAccessibilityService?.removeListener()
        super.onDestroy()
    }
}