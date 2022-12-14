package com.example.morsecode.Adapters

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat.startActivity
import androidx.recyclerview.widget.RecyclerView
import com.example.morsecode.ChatActivity
import com.example.morsecode.R
import com.example.morsecode.models.OpenedFile

class PreviouslyOpenedFilesAdapter(c: Context, longClickListener: OpenedFilesAdapterClickListener) :
    RecyclerView.Adapter<PreviouslyOpenedFilesAdapter.ViewHolder>() {
    var filesList: List<OpenedFile> = listOf()
    var context: Context
    var longClickListener: OpenedFilesAdapterClickListener
    var selectedContact:Int = -1

    private val SELECTED = 1
    private val NOT_SELECTED = 2

    init {
        context = c
        this.longClickListener = longClickListener
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView),
        View.OnClickListener {
        val fileNameTV: TextView
        //val contactImageIV: ImageView
        val background: ConstraintLayout
        //var textView: TextView
        //var img: ImageView

        //This is the subclass ViewHolder which simply
        //'holds the views' for us to show on each row
        init {
            //Finds the views from our row.xml
            fileNameTV = itemView.findViewById(R.id.fileNameTV)
            //contactImageIV = itemView.findViewById(R.id.contactImageIV)
            background = itemView.findViewById(R.id.background)
            //textView = itemView.findViewById<View>(R.id.text) as TextView
            //img = itemView.findViewById<View>(R.id.img) as ImageView

        }

        override fun onClick(v: View?) {
            TODO("Not yet implemented")
            //context.startActivity(messages[adapterPosition].intent)
        }
    }

    fun selectContact(position: Int){
        val oldSelectedContact = selectedContact
        selectedContact = position
        if(oldSelectedContact != -1) notifyItemChanged(oldSelectedContact)
        if(selectedContact != -1) notifyItemChanged(selectedContact)
    }

    override fun getItemViewType(position: Int): Int {
        if(selectedContact == position) {
            return SELECTED;
        } else {
            return NOT_SELECTED;
        }
    }

    override fun onBindViewHolder(viewHolder: ViewHolder, i: Int) {
        val ime = viewHolder.fileNameTV
        val imeText = "${filesList[i].filename} (${filesList[i].lastOpened})"
        ime.text = imeText

        viewHolder.itemView.setOnClickListener{
            longClickListener.click(filesList[i].id!!.toInt(), filesList[i].uri)
        }

        viewHolder.itemView.setOnLongClickListener {
            longClickListener.longHold(filesList[i].id!!.toInt(), i)
            true
        }
        if(viewHolder.itemViewType == SELECTED){
            viewHolder.background.setBackgroundColor(Color.YELLOW)
            viewHolder.fileNameTV.setTextColor(Color.BLACK)
        }
    }

    override fun getItemCount(): Int {
        //This method needs to be overridden so that Androids knows how many items
        //will be making it into the list
        Log.d("stjepan", filesList.size.toString())
        return filesList.size
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ViewHolder {

        //This is what adds the code we've written in here to our target view
        val inflater = LayoutInflater.from(parent.context)
        val view: View = inflater.inflate(R.layout.recyclerview_files, parent, false)
        return ViewHolder(view)
    }

    init {

        //This is where we build our list of app details, using the app
        //object we created to store the label, package name and icon

    }
/*
    fun pass(){
        val activity = viewHolder.itemView.context as Activity
        val intent = Intent(activity, ChatActivity::class.java)
        intent.putExtra("username", kontakt[i].username)
        intent.putExtra("id", kontakt[i].id.toString())
        startActivity(activity,intent,null)
    }
*/
}