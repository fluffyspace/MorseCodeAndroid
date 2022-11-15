package com.example.morsecode

import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.provider.DocumentsContract
import android.provider.OpenableColumns
import android.util.Log
import android.view.*
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri
import androidx.core.view.isVisible
import androidx.fragment.app.FragmentContainerView
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.morsecode.Adapters.OnLongClickListener
import com.example.morsecode.Adapters.OpenedFilesAdapterClickListener
import com.example.morsecode.Adapters.PreviouslyOpenedFilesAdapter
import com.example.morsecode.baza.AppDatabase
import com.example.morsecode.baza.LegProfileDao
import com.example.morsecode.baza.PreviouslyOpenedFilesDao
import com.example.morsecode.models.OpenedFile
import com.itextpdf.text.pdf.PdfReader
import com.itextpdf.text.pdf.parser.PdfTextExtractor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.*
import java.text.SimpleDateFormat
import java.util.*


class ReadFilesActivity : AppCompatActivity(), OpenedFilesAdapterClickListener {

    lateinit var tapButton: Button
    lateinit var sendButton: Button
    lateinit var morseButton: Button
    lateinit var textEditMessage: EditText
    lateinit var editTextToReadFrom: EditText
    lateinit var currentlyOpenedLabel: TextView
    var extractedText: String = ""
    var currentUri: Uri? = null

    lateinit var visual_feedback_container: VisualFeedbackFragment
    private lateinit var accelerometer: Accelerometer
    private lateinit var gyroscope: Gyroscope
    private lateinit var handsFree: HandsFreeFile
    private lateinit var sharedPreferences: SharedPreferences
    private var handsFreeOnChat = false
    lateinit var string: String
    lateinit var lines:List<String>
    var index = 0
    //var fileModified: Boolean = false // for check before losing data accidentally

    private lateinit var animalAdapter: PreviouslyOpenedFilesAdapter
    private lateinit var dialogBox: Dialog

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_read_files)

        val vibrator = this.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator

        accelerometer = Accelerometer(this)
        gyroscope = Gyroscope(this)
        handsFree = HandsFreeFile()

        sendButton = findViewById(R.id.sendButton)
        morseButton = findViewById(R.id.sendMorseButton)
        textEditMessage = findViewById(R.id.enter_message_edittext)
        editTextToReadFrom = findViewById(R.id.editTextToReadFrom)
        currentlyOpenedLabel = findViewById(R.id.currentlyOpenedLabel)
        findViewById<TextView>(R.id.open_file_icon_label).setOnClickListener {
            // ukoliko je trenutna datoteka izmijenjena, pitaj žele li se odbaciti promjene
            // otvori biranje datoteke, pročitaj ju i zapiši rezultat u
            openFile(null)
        }
        findViewById<TextView>(R.id.new_file_icon_label).setOnClickListener {
            editTextToReadFrom.setText("")
            clearCurrentUri()
            setCurrentlyOpenedText(null)
        }
        findViewById<TextView>(R.id.save_file_as_icon_label).setOnClickListener {
            createFile(null)
        }
        findViewById<TextView>(R.id.save_icon_label).setOnClickListener {
            if(currentUri != null){
                Log.d("ingo", currentUri.toString())
                Log.d("ingo", getUriMimeType(currentUri!!).toString())
                if(getUriMimeType(currentUri!!) == "text/plain")
                {
                    alterDocument(currentUri!!, editTextToReadFrom.text.toString())
                    return@setOnClickListener
                }
            }
            createFile(null)
        }
        findViewById<TextView>(R.id.history_icon_label).setOnClickListener {
            this.dialogBox = Dialog(this)
            this.dialogBox.setContentView(R.layout.previously_opened_file_dialog)
            this.animalAdapter = PreviouslyOpenedFilesAdapter(this, this)
            val animalsRecyclerView: RecyclerView = this.dialogBox.findViewById(R.id.dialog_recycler_view)//recycler view is defined in dialog box View
            animalsRecyclerView.layoutManager = LinearLayoutManager(this)
            animalsRecyclerView.adapter = this.animalAdapter
            addDataToAdapter()
            dialogBox.show()
        }

        sharedPreferences =
            this.getSharedPreferences(Constants.sharedPreferencesFile, Context.MODE_PRIVATE)
        val lastFileUri = sharedPreferences.getString("file", "")
        if(lastFileUri !== ""){
            readDocumentByUri(Uri.parse(Uri.decode(lastFileUri.toString())))
        } else {
            setCurrentlyOpenedText(null)
        }
        handsFreeOnChat = sharedPreferences.getBoolean("hands_free", false)

        visual_feedback_container = VisualFeedbackFragment()
        visual_feedback_container.testing = true
        visual_feedback_container.layout1 = true
        supportFragmentManager
            .beginTransaction()
            .add(R.id.visual_feedback_container, visual_feedback_container, "main")
            .commitNow()

        visual_feedback_container.setListener(object : VisualFeedbackFragment.Listener {
            override fun onTranslation(changeText: String) {
                visual_feedback_container.setMessage(changeText)
                textEditMessage.setText(changeText)
            }

            override fun finish(gotovo: Boolean) {
                if (gotovo) {
                    sendButton.performClick()
                    //vibrator.vibrate(100)
                } else {
                    vibrator.vibrate(100)
                }
            }
        })

        sendButton.setOnClickListener {
            Log.e("Stjepan ", editTextToReadFrom.text.toString())
            if (textEditMessage.text.isNotEmpty()) {
                lines =
                    searchFile(editTextToReadFrom.text.toString(), textEditMessage.text.toString())
                Log.d("ingo", lines.toString())
                Log.d("ingo", getLines(editTextToReadFrom.text.toString()).toString())
                index = 0
            }
            //searchFileLine()
        }

        morseButton.setOnClickListener {
            val fra: FragmentContainerView = findViewById(R.id.visual_feedback_container)
            fra.isVisible = !(fra.isVisible)
        }

        tapButton = findViewById(R.id.tap)
        tapButton.setOnTouchListener { v, event ->
            when (event?.action) {
                MotionEvent.ACTION_DOWN ->
                    visual_feedback_container.down()//Do Something
                MotionEvent.ACTION_UP -> {
                    visual_feedback_container.up()
                    v.performClick()
                }
            }
            true
        }

        accelerometer.setListener { x, y, z, xG, yG, zG ->
            handsFree.followAccelerometer(x, y, z, xG, yG, zG)
        }

        gyroscope.setListener { rx, ry, rz ->
            handsFree.followGyroscope(rx, ry, rz)
        }
        
        handsFree.setListener(object : HandsFreeFile.Listener {
            override fun onTranslation(tap: Int) {
                if (tap == 1) {
                    visual_feedback_container.down()
                } else if (tap == 2) {
                    visual_feedback_container.up()
                } else if (tap == 3) {
                    visual_feedback_container.reset()
                    vibrateLine()
                } else if (tap == 4) {
                    onBackPressed()
                }else if (tap == 5) {
                    visual_feedback_container.reset()
                    vibrator.vibrate(1)
                }
            }
        })
    }

    fun getUriMimeType(uri: Uri): String? {
        return contentResolver.getType(uri)
    }

    fun addDataToAdapter(){
        // load previous files
        lifecycleScope.launch(Dispatchers.Default) {
            val db = AppDatabase.getInstance(this@ReadFilesActivity)
            val previouslyOpenedFilesDao: PreviouslyOpenedFilesDao = db.previouslyOpenedFilesDao()
            var previouslyOpenedFiles = previouslyOpenedFilesDao.getAll().toMutableList()
            withContext(Dispatchers.Main) {
                animalAdapter.filesList = previouslyOpenedFiles
                animalAdapter.notifyDataSetChanged()
                if(previouslyOpenedFiles.isEmpty()){
                    dialogBox.cancel()
                    Toast.makeText(this@ReadFilesActivity, "History is empty.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    fun clearCurrentUri(){
        val editor = sharedPreferences.edit()
        editor.putString("file", "")
        editor.apply()
        currentUri = null
    }

    fun setCurrentUri2(uri: Uri){
        val editor = sharedPreferences.edit()
        editor.putString("file", uri.toString())
        editor.apply()
        currentUri = uri
    }

    private fun vibrateLine() {
        if(lines.isNotEmpty()){
            Log.e("Stjepan" , index.toString())
            if(index > lines.size-1) index = 0
            if(index < 0) index = lines.size-1
            vibrate(lines[index])
            index++
        }
    }

    private fun createFile(pickerInitialUri: Uri?) {
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "text/plain"
            putExtra(Intent.EXTRA_TITLE, "file.txt")

            // Optionally, specify a URI for the directory that should be opened in
            // the system file picker before your app creates the document.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                putExtra(DocumentsContract.EXTRA_INITIAL_URI, pickerInitialUri)
            }
        }
        startActivityForResult(intent, Companion.CREATE_FILE)
    }

    private fun readDocumentByUri(uri: Uri) {
        try {
            readFromFile(uri)
            setCurrentlyOpenedText(uri)
            editTextToReadFrom.setText(extractedText)
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
        } catch (e: IOException) {
            e.printStackTrace()
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }

    fun readFromFile(uri: Uri){
        val inputStream: InputStream? = contentResolver.openInputStream(uri)
        val mime = getUriMimeType(uri)
        if(mime == "text/plain"){
            val r = BufferedReader(InputStreamReader(inputStream))
            val total = StringBuilder()
            var line: String?
            while (r.readLine().also { line = it } != null) {
                total.append(line).append('\n')
            }
            extractedText = total.toString()
            Log.d("ingo", extractedText)
        } else {
            // if it's pdf
            val reader = PdfReader(inputStream)
            // below line is for getting number
            // of pages of PDF file.
            val n = reader.numberOfPages
            // running a for loop to get the data from PDF
            // we are storing that data inside our string.
            val total = StringBuilder()
            for (i in 0 until n) {
                total.append(PdfTextExtractor.getTextFromPage(
                    reader,
                    i + 1
                ).trim { it <= ' ' }).append("\n")
                // to extract the PDF content from the different pages
            }
            extractedText = total.toString()
            // below line is used for closing reader.
            reader.close()
            inputStream?.close()
        }
    }

    private fun alterDocument(uri: Uri, text: String) {
        try {
            contentResolver.openFileDescriptor(uri, "w")?.use { pfd ->
                FileOutputStream(pfd.fileDescriptor).use { fos ->
                    fos.write(text.toByteArray())
                    Toast.makeText(this, "Saved", Toast.LENGTH_SHORT).show()
                }
            }
            setCurrentlyOpenedText(uri)
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    fun setCurrentlyOpenedText(uri: Uri?){
        if(uri == null){
            currentlyOpenedLabel.text = ""
            currentlyOpenedLabel.visibility = View.GONE
            return
        }
        currentlyOpenedLabel.visibility = View.VISIBLE
        val cursor: Cursor? = contentResolver.query(uri, null, null, null, null)
        if(cursor != null){
            val nameIndex: Int = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            val sizeIndex: Int = cursor.getColumnIndex(OpenableColumns.SIZE)
            cursor.moveToFirst()
            currentlyOpenedLabel.text = StringBuilder("Currently opened: " + cursor.getString(nameIndex) + " (" + cursor.getString(sizeIndex) + " bytes)").toString()
            lifecycleScope.launch(Dispatchers.Default) {
                val date=System.currentTimeMillis() //here the date comes in 13 digits
                val dtlong = Date(date)
                val sdfdate = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(dtlong)
                val db = AppDatabase.getInstance(this@ReadFilesActivity)
                val previouslyOpenedFilesDao: PreviouslyOpenedFilesDao = db.previouslyOpenedFilesDao()
                val exists = previouslyOpenedFilesDao.getByUri(uri.toString())
                if(exists.isEmpty()){
                    var newFile = OpenedFile(id = 0, filename = cursor.getString(nameIndex), uri=uri.toString(), lastOpened = sdfdate)
                    previouslyOpenedFilesDao.insertAll(newFile)
                } else {
                    var oldFile = OpenedFile(id = exists[0].id, filename = cursor.getString(nameIndex), uri=uri.toString(), lastOpened = sdfdate)
                    previouslyOpenedFilesDao.update(oldFile)
                }
            }
        }
    }

    override fun onActivityResult(
        requestCode: Int, resultCode: Int, resultData: Intent?) {
        super.onActivityResult(requestCode, resultCode, resultData)
        if(requestCode == CREATE_FILE && resultCode == RESULT_OK){
            val takeFlags: Int = Intent.FLAG_GRANT_READ_URI_PERMISSION or
                    Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            // Check for the freshest data.
            contentResolver.takePersistableUriPermission(resultData!!.data!!, takeFlags)

            resultData?.data?.also { uri ->
                setCurrentUri2(uri)
                setCurrentlyOpenedText(uri)
                alterDocument(uri, editTextToReadFrom.text.toString())
            }
        }
        if (requestCode == PICK_PDF_FILE
            && resultCode == RESULT_OK
        ) {
            val takeFlags: Int = Intent.FLAG_GRANT_READ_URI_PERMISSION or
                    Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            // Check for the freshest data.
            contentResolver.takePersistableUriPermission(resultData!!.data!!, takeFlags)
            // The result data contains a URI for the document or directory that
            // the user selected.
            resultData?.data?.also { uri ->
                setCurrentUri2(uri)
                // Perform operations on the document using its URI.
                try {
                    setCurrentlyOpenedText(uri)
                    readFromFile(uri)
                    // after extracting all the data we are
                    // setting that string value to our text view.
                    editTextToReadFrom.setText(extractedText)
                } catch (e: java.lang.Exception) {
                    Log.e("ingo", e.toString())
                }
            }
        }
    }

    fun openFile(pickerInitialUri: Uri?) {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            type = "*/*"
            //putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("text/plain"))
            // Optionally, specify a URI for the file that should appear in the
            // system file picker when it loads.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                putExtra(DocumentsContract.EXTRA_INITIAL_URI, pickerInitialUri)
            }
        }
        startActivityForResult(intent, Companion.PICK_PDF_FILE)
    }

    private fun getLines(file: String): MutableList<String>{
        val lines = mutableListOf<String>()
        var pocetak = 0
        for (i in 0 until file.length) {
            if (file[i] == '\n') {
                lines.add(file.substring(pocetak, i))
                pocetak = i+1
            }
        }
        lines.add(file.substring(pocetak, file.length))
        return lines
    }

    private fun getLineByIndex(file: String, index: Int): String {
        var pocetak = 0
        var kraj = file.length
        for (i in index downTo 0) {
            if (file[i] == '\n') {
                pocetak = i+1
                break
            }
        }
        for (i in index until file.length) {
            if (file[i] == '\n') {
                kraj = i
                break
            }
        }
        Log.e("stjepan", "$pocetak, $kraj")
        return file.substring(pocetak, kraj)
    }

    private fun searchFile(file: String, query: String): List<String> {
        val matches = query.toRegex().findAll(file).toList()
        val lines = mutableListOf<String>()
        for(match in matches) {

            val linja = getLineByIndex(file, match.range.first)

            Log.e("Stjepan", "$linja")
            lines.add(linja)
        }
        return lines.distinct()
    }


    private fun searchFile(string: String) {

        val st = string.toRegex()
        val match = st.find(this.string)
        if (match != null) {
            Log.e("Stjepan", match.range.first.toString())
            Log.e("Stjepan", match.range.last.toString())

            var pocetak = 0
            var kraj = 0
            for (i in match.range.first downTo 0) {
                if (this.string[i] == '\n' || i == 0) {
                    pocetak = i
                    break
                }

            }
            for (i in match.range.last until this.string.length) {
                if (this.string[i] == '\n') {
                    kraj = i
                    break
                }
            }
            Log.e("Stjepan",this.string.substring(pocetak, kraj))
            vibrate(this.string.substring(pocetak, kraj))

        }
    }

    private fun vibrate(str: String) {
        var mAccessibilityService = MorseCodeService.getSharedInstance();

        mAccessibilityService?.vibrateWithPWM(
            mAccessibilityService!!.makeWaveformFromText(
                str
            )
        )
    }


    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater: MenuInflater = menuInflater
        inflater.inflate(R.menu.chat_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.hands_free -> {
                try {
                    val vibrator = this.getSystemService(VIBRATOR_SERVICE) as Vibrator
                    if (Build.VERSION.SDK_INT >= 26) {
                        vibrator.vibrate(
                            VibrationEffect.createOneShot(
                                200,
                                VibrationEffect.DEFAULT_AMPLITUDE
                            )
                        )
                    } else {
                        vibrator.vibrate(200)
                    }

                    morseButton.performClick()

                    if (handsFreeOnChat) {
                        handsFreeOnChat = false
                        accelerometer.unregister()
                        gyroscope.unregister()
                        handsFreeOnChatSet(false)
                    } else if (!handsFreeOnChat) {
                        handsFreeOnChat = true
                        accelerometer.register()
                        gyroscope.register()
                        handsFreeOnChatSet(true)
                    }

                    Toast.makeText(
                        this,
                        "vibration" + Toast.LENGTH_SHORT.toString(),
                        Toast.LENGTH_SHORT
                    ).show()
                } catch (e: Exception) {

                }
                true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun handsFreeOnChatSet(b: Boolean) {
        val editor = sharedPreferences.edit()
        editor.putBoolean("hands_free", b)
        editor.apply()
    }

    override fun onResume() {
        if (handsFreeOnChat) {
            accelerometer.register()
            gyroscope.register()
        }
        super.onResume()
    }

    override fun onPause() {
        gyroscope.unregister()
        accelerometer.unregister()
        super.onPause()
    }

    companion object {
        const val CREATE_FILE = 1
        const val PICK_PDF_FILE = 2
        const val SAVE_FILE_AS = 3
    }

    override fun longHold(id: Int, position: Int) {
        lifecycleScope.launch(Dispatchers.Default) {
            val db = AppDatabase.getInstance(this@ReadFilesActivity)
            val previouslyOpenedFilesDao: PreviouslyOpenedFilesDao = db.previouslyOpenedFilesDao()
            val file = previouslyOpenedFilesDao.getById(id).toList()[0]
            previouslyOpenedFilesDao.delete(file)
            withContext(Dispatchers.Main){
                animalAdapter.filesList = animalAdapter.filesList.filter { file -> file.id.toInt() != id }
                animalAdapter.notifyItemRemoved(position)
                Toast.makeText(this@ReadFilesActivity, "File removed from history", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun click(id: Int, uri: String) {
        readDocumentByUri(Uri.parse(Uri.decode(uri)))
        dialogBox.cancel()
    }
}