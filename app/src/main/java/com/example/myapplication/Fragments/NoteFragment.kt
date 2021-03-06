package com.example.myapplication.Fragments

class NoteFragment {
    private lateinit var settingsHelper: SettingsHelper

    private var adapter: BaseNoteAdapter? = null
    private var binding: FragmentNotesBinding? = null

    internal val model: BaseNoteModel by activityViewModels()

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
        adapter = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        settingsHelper = SettingsHelper(requireContext())

        adapter = BaseNoteAdapter(settingsHelper, model.formatter, this)
        adapter?.registerAdapterDataObserver(object : RecyclerView.AdapterDataObserver() {

            override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
                if (itemCount > 0) {
                    binding?.RecyclerView?.scrollToPosition(positionStart)
                }
            }
        })
        binding?.RecyclerView?.adapter = adapter
        binding?.RecyclerView?.setHasFixedSize(true)

        binding?.ImageView?.setImageResource(getBackground())

        setupRecyclerView()
        setupObserver()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentNotesBinding.inflate(inflater)
        return binding?.root
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == Constants.RequestCodeExportFile && resultCode == Activity.RESULT_OK) {
            data?.data?.let { uri ->
                model.writeCurrentFileToUri(uri)
            }
        }
    }


    override fun onClick(position: Int) {
        adapter?.currentList?.get(position)?.let { item ->
            if (item is BaseNote) {
                when (item.type) {
                    Type.NOTE -> goToActivity(TakeNote::class.java, item)
                   // Type.LIST -> goToActivity(MakeList::class.java, item)
                }
            }
        }
    }

    override fun onLongClick(position: Int) {
        adapter?.currentList?.get(position)?.let { item ->
            if (item is BaseNote) {
                showOperations(item)
            }
        }
    }


    override fun accessContext(): Context {
        return requireContext()
    }

    override fun insertLabel(label: Label, onComplete: (success: Boolean) -> Unit) {
        model.insertLabel(label, onComplete)
    }


    private fun setupObserver() {
        getObservable().observe(viewLifecycleOwner, { list ->
            adapter?.submitList(list)
            binding?.RecyclerView?.isVisible = list.isNotEmpty()
        })
    }

    private fun setupRecyclerView() {
        binding?.RecyclerView?.layoutManager = if (settingsHelper.getView() == getString(R.string.gridKey)) {
            StaggeredGridLayoutManager(2, RecyclerView.VERTICAL)
        } else LinearLayoutManager(requireContext())
    }


    private fun showOperations(baseNote: BaseNote) {
        val operations = when (baseNote.folder) {
            Folder.NOTES -> {
              /*  val pin = if (baseNote.pinned) {
                    Operation(R.string.unpin, R.drawable.pin) { model.unpinBaseNote(baseNote.id) }
                } else Operation(R.string.pin, R.drawable.pin) { model.pinBaseNote(baseNote.id) }
                val share = Operation(R.string.share, R.drawable.share) { shareBaseNote(baseNote) }
                val labels = Operation(R.string.labels, R.drawable.label) { labelBaseNote(baseNote) }
                val export = Operation(R.string.export, R.drawable.export) { exportBaseNote(baseNote) }

                val delete = Operation(R.string.delete, R.drawable.delete) { model.moveBaseNoteToDeleted(baseNote.id) }
                val archive = Operation(R.string.archive, R.drawable.archive) { model.moveBaseNoteToArchive(baseNote.id) }
                val moreOptions = Operation(R.string.more_options, R.drawable.more_options) { showMenu(delete, archive) } */
                arrayOf(/*pin, share, labels, export, moreOptions*/)
            }
            Folder.DELETED -> {
                val restore = Operation(R.string.restore, R.drawable.restore) { model.restoreBaseNote(baseNote.id) }
                val deleteForever = Operation(R.string.delete_forever, R.drawable.delete) { deleteBaseNote(baseNote) }
                arrayOf(restore, deleteForever)
            }
            Folder.ARCHIVED -> {
                val unarchive = Operation(R.string.unarchive, R.drawable.unarchive) { model.restoreBaseNote(baseNote.id) }
                arrayOf(unarchive)
            }
        }
        showMenu(*operations)
    }

    internal fun goToActivity(activity: Class<*>, baseNote: BaseNote? = null) {
        val intent = Intent(requireContext(), activity)
        intent.putExtra(Constants.SelectedBaseNote, baseNote)
        startActivity(intent)
    }


    /*private fun shareBaseNote(baseNote: BaseNote) {
        when (baseNote.type) {
            Type.NOTE -> shareNote(baseNote.title, baseNote.body.applySpans(baseNote.spans))
            Type.LIST -> shareNote(baseNote.title, baseNote.items)
        }
    }*/

    private fun labelBaseNote(baseNote: BaseNote) {
        lifecycleScope.launch {
            val labels = model.getAllLabelsAsList()
            labelNote(labels, baseNote.labels) { updatedLabels ->
                model.updateBaseNoteLabels(updatedLabels, baseNote.id)
            }
        }
    }

   private fun exportBaseNote(baseNote: BaseNote) {
        val pdf = Operation(R.string.pdf, R.drawable.pdf) { exportBaseNoteToPDF(baseNote) }
        val txt = Operation(R.string.txt, R.drawable.txt) { exportBaseNoteToTXT(baseNote) }
        val xml = Operation(R.string.xml, R.drawable.xml) { exportBaseNoteToXML(baseNote) }
        val json = Operation(R.string.json, R.drawable.json) { exportBaseNoteToJSON(baseNote) }
        val html = Operation(R.string.html, R.drawable.html) { exportBaseNoteToHTML(baseNote) }
        showMenu(pdf, txt, xml, json, html)
    }

    private fun deleteBaseNote(baseNote: BaseNote) {
        MaterialAlertDialogBuilder(requireContext())
            .setMessage(R.string.delete_note_forever)
            .setPositiveButton(R.string.delete) { dialog, which ->
                model.deleteBaseNoteForever(baseNote)
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }


    private fun exportBaseNoteToPDF(baseNote: BaseNote) {
        model.getPDFFile(baseNote, settingsHelper.showDateCreated(), object : PostPDFGenerator.OnResult {

            override fun onSuccess(file: File) {
                showFileOptionsDialog(file, "application/pdf")
            }

            override fun onFailure(message: String?) {
                Toast.makeText(requireContext(), R.string.something_went_wrong, Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun exportBaseNoteToTXT(baseNote: BaseNote) {
        lifecycleScope.launch {
            val file = model.getTXTFile(baseNote, settingsHelper.showDateCreated())
            showFileOptionsDialog(file, "text/plain")
        }
    }

    private fun exportBaseNoteToXML(baseNote: BaseNote) {
        lifecycleScope.launch {
            val file = model.getXMLFile(baseNote)
            showFileOptionsDialog(file, "text/xml")
        }
    }

    private fun exportBaseNoteToJSON(baseNote: BaseNote) {
        lifecycleScope.launch {
            val file = model.getJSONFile(baseNote)
            showFileOptionsDialog(file, "application/json")
        }
    }

    private fun exportBaseNoteToHTML(baseNote: BaseNote) {
        lifecycleScope.launch {
            val file = model.getHTMLFile(baseNote, settingsHelper.showDateCreated())
            showFileOptionsDialog(file, "text/html")
        }
    }

    private fun showFileOptionsDialog(file: File, mimeType: String) {
        val uri = FileProvider.getUriForFile(requireContext(), "${requireContext().packageName}.provider", file)

        val share = Operation(R.string.share, R.drawable.share) { shareFile(uri, mimeType) }
        val viewFile = Operation(R.string.view_file, R.drawable.view) { viewFile(uri, mimeType) }
        val saveToDevice = Operation(R.string.save_to_device, R.drawable.save) { saveFileToDevice(file, mimeType) }
        showMenu(share, viewFile, saveToDevice)
    }


    private fun viewFile(uri: Uri, mimeType: String) {
        val intent = Intent(Intent.ACTION_VIEW)
        intent.setDataAndType(uri, mimeType)
        intent.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION

        val chooser = Intent.createChooser(intent, getString(R.string.view_note))
        startActivity(chooser)
    }

    private fun shareFile(uri: Uri, mimeType: String) {
        val intent = Intent(Intent.ACTION_SEND)
        intent.type = mimeType
        intent.putExtra(Intent.EXTRA_STREAM, uri)

        val chooser = Intent.createChooser(intent, getString(R.string.share_note))
        startActivity(chooser)
    }

    private fun saveFileToDevice(file: File, mimeType: String) {
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT)
        intent.type = mimeType
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        intent.putExtra(Intent.EXTRA_TITLE, file.nameWithoutExtension)

        model.currentFile = file
        startActivityForResult(intent, Constants.RequestCodeExportFile)
    }


    abstract fun getBackground(): Int

    abstract fun getObservable(): LiveData<List<Item>>
}
}