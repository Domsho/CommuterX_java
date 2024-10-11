package com.example.commuterx_java

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.mapbox.search.result.SearchSuggestion

class SearchResultsAdapter(private val onItemClick: (SearchSuggestion) -> Unit) :
    RecyclerView.Adapter<SearchResultsAdapter.ViewHolder>() {

    private var searchSuggestions: List<SearchSuggestion> = emptyList()

    fun updateResults(newSuggestions: List<SearchSuggestion>) {
        searchSuggestions = newSuggestions
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_search_suggestion, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val suggestion = searchSuggestions[position]
        holder.bind(suggestion)
    }

    override fun getItemCount(): Int = searchSuggestions.size

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val titleTextView: TextView = itemView.findViewById(R.id.suggestion_title)
        private val addressTextView: TextView = itemView.findViewById(R.id.suggestion_address)

        fun bind(suggestion: SearchSuggestion) {
            titleTextView.text = suggestion.name
            addressTextView.text = suggestion.address?.formattedAddress() ?: ""
            itemView.setOnClickListener { onItemClick(suggestion) }
        }
    }
}