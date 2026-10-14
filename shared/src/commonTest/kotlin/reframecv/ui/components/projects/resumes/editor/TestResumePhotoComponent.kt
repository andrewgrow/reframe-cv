package reframecv.ui.components.projects.resumes.editor

import com.arkivanov.decompose.value.MutableValue
import reframecv.photos.PhotoCrop

class TestResumePhotoComponent(path: String? = null, private val changed: (String?) -> Unit = {}) :
    ResumePhotoComponent {
    override val state = MutableValue(ResumePhotoState(path))
    override val visible = MutableValue(false)
    override val confirmingDelete = MutableValue(false)
    override fun onOpen() {
        visible.value = true
    }
    override fun onChoose() = Unit
    override fun onRequestDelete() {
        confirmingDelete.value = true
    }
    override fun onCancelDelete() {
        confirmingDelete.value = false
    }
    override fun onConfirmDelete() {
        state.value = ResumePhotoState()
        changed(null)
        confirmingDelete.value = false
    }
    override fun onClose() {
        if (confirmingDelete.value) confirmingDelete.value = false else visible.value = false
    }
    override fun onCropChange(crop: PhotoCrop) {
        state.value = state.value.copy(crop = crop.constrained())
    }
    override fun prepareSave(finished: (Boolean) -> Unit) = finished(true)
    override fun onSaved() = Unit
}
