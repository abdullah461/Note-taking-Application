package com.example.myapplication.Activities

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.viewbinding.ViewBinding
import com.google.android.material.chip.ChipGroup
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class NoteActivity {
}
abstract class NotallyActivity : AppCompatActivity(), OperationsParent {

    internal abstract val model: NotallyModel
    internal abstract val binding: ViewBinding

    override fun onBackPressed() {
        model.saveNote {
            super.onBackPressed()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        model.saveNote()
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        binding.root.isSaveFromParentEnabled = false

        if (model.isFirstInstance) {
            val selectedBaseNote = intent.getParcelableExtra<BaseNote>(Constants.SelectedBaseNote)
            if (selectedBaseNote != null) {
                model.isNewNote = false
                model.setStateFromBaseNote(selectedBaseNote)
            } else model.isNewNote = true

            if (intent.action == Intent.ACTION_SEND) {
                receiveSharedNote()
            }

            model.isFirstInstance = false
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        val menuId = when (model.folder) {
            Folder.NOTES -> R.menu.notes
            Folder.DELETED -> R.menu.deleted
            Folder.ARCHIVED -> R.menu.archived
        }

        menuInflater.inflate(menuId, menu)
        //  bindPinned(menu?.findItem(R.id.Pin))
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> onBackPressed()
            R.id.Share -> shareNote()
            R.id.Labels -> labelNote()
            //    R.id.Pin -> pinNote(item)
            R.id.Delete -> deleteNote()
            //  R.id.Archive -> archiveNote()
            R.id.Restore -> restoreNote()
            R.id.Unarchive -> restoreNote()
            R.id.DeleteForever -> deleteNoteForever()
        }
        return super.onOptionsItemSelected(item)
    }


    override fun accessContext(): Context {
        return this
    }

    override fun insertLabel(label: Label, onComplete: (success: Boolean) -> Unit) {
        model.insertLabel(label, onComplete)
    }


    abstract fun shareNote()

    abstract fun getLabelGroup(): ChipGroup

    //   abstract fun getPinnedIndicator(): TextView

    abstract fun getPinnedParent(): LinearLayout


    open fun receiveSharedNote() {}


    private fun labelNote() {
        lifecycleScope.launch {
            val labels = withContext(Dispatchers.IO) { model.getAllLabelsAsList() }
            labelNote(labels, model.labels) { updatedLabels ->
                model.labels = updatedLabels
                getLabelGroup().bindLabels(updatedLabels)
            }
        }
    }

    private fun deleteNote() {
        model.moveBaseNoteToDeleted()
        onBackPressed()
    }

    private fun restoreNote() {
        model.restoreBaseNote()
        onBackPressed()
    }

    /*private fun archiveNote() {
        model.moveBaseNoteToArchive()
        onBackPressed()
    }*/

    private fun deleteNoteForever() {
        MaterialAlertDialogBuilder(this)
            .setMessage(R.string.delete_note_forever)
            .setPositiveButton(R.string.delete) { dialog, which ->
                model.deleteBaseNoteForever {
                    super.onBackPressed()
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

/*private fun pinNote(item: MenuItem) {
    model.pinned = !model.pinned
    bindPinned(item, true)
}*/
