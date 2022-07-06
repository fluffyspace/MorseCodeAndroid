package com.example.morsecode.Adapters

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat.startActivity
import androidx.recyclerview.widget.RecyclerView
import com.example.morsecode.ChatActivity
import com.example.morsecode.R
import com.example.morsecode.models.Contact

class KontaktiAdapter(c: Context, kontakt: List<Contact>, longClickListener: OnLongClickListener) :
    RecyclerView.Adapter<KontaktiAdapter.ViewHolder>() {
    var kontakt: List<Contact>
    var context: Context
    var longClickListener: OnLongClickListener
    var selectedContact:Int = -1

    private val SELECTED = 1
    private val NOT_SELECTED = 2

    init {
        this.kontakt = kontakt
        context = c
        this.longClickListener = longClickListener
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView),
        View.OnClickListener {
        val imePrezimeTV: TextView
        //val contactImageIV: ImageView
        val background: ConstraintLayout
        //var textView: TextView
        //var img: ImageView

        //This is the subclass ViewHolder which simply
        //'holds the views' for us to show on each row
        init {
            //Finds the views from our row.xml
            imePrezimeTV = itemView.findViewById(R.id.imePrezimeTV)
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
        val ime = viewHolder.imePrezimeTV
        val imeText = "${kontakt[i].username} (id ${kontakt[i].id})"
        ime.text = imeText

        viewHolder.itemView.setOnClickListener{
            val activity = viewHolder.itemView.context as Activity
            val intent = Intent(activity, ChatActivity::class.java)
            intent.putExtra("username", kontakt[i].username)
            intent.putExtra("id", kontakt[i].id)
            startActivity(activity,intent,null)
        }

        viewHolder.itemView.setOnLongClickListener {
            longClickListener.longHold(kontakt[i].id!!.toInt(), kontakt[i].username)
            /*val intent = Intent(viewHolder.itemView.context, ContactActivity::class.java)
            intent.putExtra("idFriend", kontakt[i].id.toString())
            intent.putExtra("nameFriend", kontakt[i].username)
            viewHolder.itemView.context.startActivity(intent)*/
            true
        }
        if(viewHolder.itemViewType == SELECTED){
            viewHolder.background.setBackgroundColor(Color.YELLOW)
            viewHolder.imePrezimeTV.setTextColor(Color.BLACK)
        }
    }

    override fun getItemCount(): Int {
        //This method needs to be overridden so that Androids knows how many items
        //will be making it into the list
        Log.d("stjepan", kontakt.size.toString())
        return kontakt.size
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ViewHolder {

        //This is what adds the code we've written in here to our target view
        val inflater = LayoutInflater.from(parent.context)
        val view: View = inflater.inflate(R.layout.recyclerview_contacts, parent, false)
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
