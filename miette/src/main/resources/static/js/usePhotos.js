const { ref, computed, watch } = Vue;

export function usePhotos(recipe) {
    const uploadingPhoto = ref(false);
    const currentPhotoIndex = ref(0);

    const photos = computed(() => recipe.value?.assets?.filter(a => a.type === 'PHOTO') ?? []);

    watch(photos, () => {
        const coverIdx = photos.value.findIndex(p => p.cover);
        currentPhotoIndex.value = coverIdx >= 0 ? coverIdx : 0;
    });

    function prevPhoto() {
        currentPhotoIndex.value = (currentPhotoIndex.value - 1 + photos.value.length) % photos.value.length;
    }

    function nextPhoto() {
        currentPhotoIndex.value = (currentPhotoIndex.value + 1) % photos.value.length;
    }

    async function handleFileChange(e) {
        const file = e.target.files[0];
        if (!file) return;
        e.target.value = '';

        uploadingPhoto.value = true;
        try {
            const csrf = document.querySelector('meta[name="_csrf"]').content;
            const csrfHeader = document.querySelector('meta[name="_csrf_header"]').content;
            const formData = new FormData();
            formData.append('file', file);
            const response = await fetch(`/api/recipes/${recipe.value.id}/assets/upload`, {
                method: 'POST',
                headers: { [csrfHeader]: csrf },
                body: formData
            });
            if (response.ok) {
                const newAsset = await response.json();
                recipe.value.assets.unshift(newAsset);
                currentPhotoIndex.value = 0;
            } else {
                alert("Erreur lors de l'upload");
            }
        } finally {
            uploadingPhoto.value = false;
        }
    }

    async function setCover(photoId) {
        const csrf = document.querySelector('meta[name="_csrf"]').content;
        const csrfHeader = document.querySelector('meta[name="_csrf_header"]').content;
        await fetch(`/api/recipes/${recipe.value.id}/assets/${photoId}/cover`, {
            method: 'PATCH',
            headers: { [csrfHeader]: csrf }
        });
        recipe.value.assets.forEach(a => { a.cover = a.id === photoId; });
    }

    return { photos, uploadingPhoto, currentPhotoIndex, prevPhoto, nextPhoto, handleFileChange, setCover };
}
