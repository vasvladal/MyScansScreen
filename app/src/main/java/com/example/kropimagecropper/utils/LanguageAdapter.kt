package com.example.kropimagecropper.utils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.TextView
import com.example.kropimagecropper.R
//package com.example.kropimagecropper.utils.Language

class LanguageAdapter(
    private val languages: List<Language>,
    private val onLanguageSelected: (Language) -> Unit
) : BaseAdapter() {

    override fun getCount(): Int = languages.size
    override fun getItem(position: Int): Language = languages[position]
    override fun getItemId(position: Int): Long = position.toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: LayoutInflater.from(parent.context)
            .inflate(R.layout.ui_component_language_item, parent, false)

        val language = getItem(position)
        view.apply {
            findViewById<TextView>(R.id.language_name).text = language.name
            findViewById<ImageView>(R.id.language_flag).setImageResource(language.iconRes?.toInt()!!)

            setOnClickListener { onLanguageSelected(language) }
        }
        return view
    }
}