package com.example.morsecode

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.morsecode.models.EntitetKontakt

class KontaktiAdapter(c: Context, kontakt: List<EntitetKontakt>) : RecyclerView.Adapter<KontaktiAdapter.ViewHolder>() {
    var kontakt:List<EntitetKontakt>
    var context:Context
    init {
        this.kontakt = kontakt
        context = c
    }
    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView),
        View.OnClickListener {
        var textView: TextView
        //var img: ImageView


        //This is the subclass ViewHolder which simply
        //'holds the views' for us to show on each row
        init {
            //Finds the views from our row.xml
            textView = itemView.findViewById<View>(R.id.text) as TextView
            //img = itemView.findViewById<View>(R.id.img) as ImageView

        }

        override fun onClick(v: View?) {
            TODO("Not yet implemented")
            //context.startActivity(messages[adapterPosition].intent)
        }
    }

    override fun onBindViewHolder(viewHolder: ViewHolder, i: Int) {
        val textView = viewHolder.textView
        textView.text = kontakt[i].username + "bok"
        //val imageView = viewHolder.img
        //val launcherApps:LauncherApps = context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
        //imageView.setImageDrawable(launcherApps.getShortcutIconDrawable(shortcuts[i],
        //    context.resources.displayMetrics.densityDpi))
    }

    override fun getItemCount(): Int {

        //This method needs to be overridden so that Androids knows how many items
        //will be making it into the list
        Log.d("stjepan" ,  kontakt.size.toString())
        return kontakt.size
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ViewHolder {

        //This is what adds the code we've written in here to our target view
        val inflater = LayoutInflater.from(parent.context)
        val view: View = inflater.inflate(R.layout.message_row, parent, false)
        return ViewHolder(view)
    }

    init {

        //This is where we build our list of app details, using the app
        //object we created to store the label, package name and icon

    }
}