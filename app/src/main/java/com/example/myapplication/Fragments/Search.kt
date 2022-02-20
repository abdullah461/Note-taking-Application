package com.example.myapplication.Fragments

class Search {
    import com.omgodse.note_taker.R

class Search : NotallyFragment() {

    override fun getBackground() = R.drawable.search

    override fun getObservable() = model.searchResults
}
}