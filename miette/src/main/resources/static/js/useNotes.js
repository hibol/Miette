const { ref } = Vue;

export function useNotes(recipe) {
    const notesOpen = ref(false);
    const showNoteForm = ref(false);
    const noteDate = ref('');
    const noteDescription = ref('');
    const savingNote = ref(false);

    function formatNoteDate(isoString) {
        if (!isoString) return '';
        const d = new Date(isoString);
        return d.toLocaleString('fr-FR', { dateStyle: 'medium', timeStyle: 'short' });
    }

    function openNoteForm() {
        const now = new Date();
        const pad = n => String(n).padStart(2, '0');
        noteDate.value = `${now.getFullYear()}-${pad(now.getMonth()+1)}-${pad(now.getDate())}T${pad(now.getHours())}:${pad(now.getMinutes())}`;
        noteDescription.value = '';
        showNoteForm.value = true;
    }

    async function addNote() {
        savingNote.value = true;
        try {
            const csrf = document.querySelector('meta[name="_csrf"]').content;
            const csrfHeader = document.querySelector('meta[name="_csrf_header"]').content;
            const response = await fetch(`/api/recipes/${recipe.value.id}/assets`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json', [csrfHeader]: csrf },
                body: JSON.stringify({ date: noteDate.value, description: noteDescription.value })
            });
            if (response.ok) {
                const newAsset = await response.json();
                recipe.value.assets.unshift(newAsset);
                showNoteForm.value = false;
            } else {
                alert("Erreur lors de l'ajout de la note");
            }
        } finally {
            savingNote.value = false;
        }
    }

    async function deleteNote(assetId) {
        if (!confirm('Supprimer cette note ?')) return;
        const csrf = document.querySelector('meta[name="_csrf"]').content;
        const csrfHeader = document.querySelector('meta[name="_csrf_header"]').content;
        const response = await fetch(`/api/recipes/${recipe.value.id}/assets/${assetId}`, {
            method: 'DELETE',
            headers: { [csrfHeader]: csrf }
        });
        if (response.ok) {
            recipe.value.assets = recipe.value.assets.filter(a => a.id !== assetId);
        } else {
            alert('Erreur lors de la suppression');
        }
    }

    return { notesOpen, showNoteForm, noteDate, noteDescription, savingNote, openNoteForm, addNote, deleteNote, formatNoteDate };
}
