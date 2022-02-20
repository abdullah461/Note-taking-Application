package com.example.myapplication.Fragments
mport androidx.lifecycle.LiveData
import com.omgodse.note_taker.R
import com.omgodse.note_taker.miscellaneous.Constants
import com.omgodse.note_taker.room.Item


class DisplayLabels {


    override fun getBackground() = R.drawable.label

    override fun getObservable(): LiveData<List<Item>> {
        val label = requireNotNull(requireArguments().getString(Constants.SelectedLabel))
        return model.getNotesByLabel(label)
    }
}
