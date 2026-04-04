import { useNotes } from './useNotes.js';
import { usePhotos } from './usePhotos.js';
import { useValidation } from './useValidation.js';
import { nextTempKey } from './glossary-utils.js';
import { useGlossaryHighlight } from './useGlossaryHighlight.js';
import { vCuboSpinner } from './cubo.js';

const { createApp, ref, onMounted, computed, watch } = Vue;

const vTooltip = {
    mounted(el) {
        el._tooltip = new bootstrap.Tooltip(el, { trigger: 'hover' });
    },
    unmounted(el) {
        el._tooltip?.dispose();
    }
};

const vAutoResize = {
    mounted(el) {
        el.style.overflow = 'hidden';
        el.style.height = 'auto';
        el.style.height = el.scrollHeight + 'px';
        el._autoResize = () => { el.style.height = 'auto'; el.style.height = el.scrollHeight + 'px'; };
        el.addEventListener('input', el._autoResize);
    },
    unmounted(el) {
        el.removeEventListener('input', el._autoResize);
    }
};

const vSortableSteps = {
    mounted(el, binding) {
        Sortable.create(el, {
            animation: 150,
            handle: '.drag-handle',
            onEnd(evt) {
                const { items, clearErrors } = binding.value;
                const item = items.splice(evt.oldIndex, 1)[0];
                items.splice(evt.newIndex, 0, item);
                items.forEach((s, i) => s.position = i + 1);
                clearErrors();
            }
        });
    }
};

const vSortableIngredients = {
    mounted(el, binding) {
        Sortable.create(el, {
            animation: 150,
            handle: '.drag-handle',
            onEnd(evt) {
                const { items, clearErrors } = binding.value;
                const item = items.splice(evt.oldIndex, 1)[0];
                items.splice(evt.newIndex, 0, item);
                items.forEach((ing, i) => ing.position = i + 1);
                clearErrors();
            }
        });
    }
};

const vSortablePhases = {
    mounted(el, binding) {
        Sortable.create(el, {
            animation: 150,
            handle: '.phase-drag-handle',
            onStart() {
                binding.value.setDragging(true);
            },
            onEnd(evt) {
                binding.value.setDragging(false);
                binding.value.clearErrors();
                const phases = binding.value.phases;
                const item = phases.splice(evt.oldIndex, 1)[0];
                phases.splice(evt.newIndex, 0, item);
                phases.forEach((p, i) => p.position = i + 1);
            }
        });
    }
};

createApp({
    directives: { sortableSteps: vSortableSteps, sortableIngredients: vSortableIngredients, sortablePhases: vSortablePhases, cuboSpinner: vCuboSpinner, autoResize: vAutoResize, tooltip: vTooltip },
    setup() {
        const recipe = ref(null);
        const loading = ref(true);
        const saving = ref(false);
        const isDraggingPhase = ref(false);
        const error = ref(null);
        const isAdmin = ref(false);
        const editMode = ref(false);
        const draft = ref(null);
        const availableTags = ref([]);
        const newTag = ref('');
        const selectedTagIndex = ref(-1);
        const returnUrl = document.getElementById('app').dataset.returnUrl;

        const { notesOpen, showNoteForm, noteDate, noteDescription, savingNote, openNoteForm, addNote, deleteNote, formatNoteDate } = useNotes(recipe);
        const { photos, uploadingPhoto, currentPhotoIndex, prevPhoto, nextPhoto, handleFileChange } = usePhotos(recipe);
        const { errors, validateDraft, getError, clearErrors } = useValidation(draft);

        const filteredTags = computed(() => {
            if (!newTag.value.trim()) return [];
            return availableTags.value.filter(t =>
                t.toLowerCase().includes(newTag.value.toLowerCase()) &&
                !draft.value.tags.includes(t)
            );
        });

        watch(filteredTags, () => { selectedTagIndex.value = -1; });

        function handleTagKeydown(e) {
            if (e.key === 'ArrowDown') {
                e.preventDefault();
                selectedTagIndex.value = Math.min(selectedTagIndex.value + 1, filteredTags.value.length - 1);
            } else if (e.key === 'ArrowUp') {
                e.preventDefault();
                selectedTagIndex.value = Math.max(selectedTagIndex.value - 1, -1);
            } else if (e.key === 'Enter') {
                e.preventDefault();
                if (selectedTagIndex.value >= 0) {
                    const tag = filteredTags.value[selectedTagIndex.value];
                    if (!draft.value.tags.includes(tag)) draft.value.tags.push(tag);
                    newTag.value = '';
                } else if (filteredTags.value.length === 1) {
                    const tag = filteredTags.value[0];
                    if (!draft.value.tags.includes(tag)) draft.value.tags.push(tag);
                    newTag.value = '';
                } else {
                    addTag();
                }
            }
        }

        window.addEventListener('beforeunload', (e) => {
            if (editMode.value) e.preventDefault();
        });

        onMounted(async () => {
            const appEl = document.getElementById('app');
            isAdmin.value = appEl.dataset.isAdmin === 'true';
            const recipeId = appEl.dataset.recipeId;
            try {
                if (!recipeId) {
                    recipe.value = { id: null, title: '', tags: [], assets: [], phases: [
                        { id: null, _key: nextTempKey(), label: '', position: 1, ingredients: [], steps: [] }
                    ]};
                    startEdit();
                } else {
                    const response = await fetch(`/api/recipes/${recipeId}`);
                    if (!response.ok) throw new Error('Recette introuvable');
                    recipe.value = await response.json();
                    if (appEl.dataset.editMode === 'true') {
                        startEdit();
                    }
                }
                const tagsResponse = await fetch('/api/tags');
                availableTags.value = await tagsResponse.json();
            } catch (e) {
                error.value = e.message;
            } finally {
                window._stopCuboPreloader?.();
                loading.value = false;
            }
        });

        function startEdit() {
            clearErrors();
            draft.value = JSON.parse(JSON.stringify(recipe.value));
            editMode.value = true;
        }

        function cancelEdit() {
            clearErrors();
            if (recipe.value.id === null) {
                window.location.href = returnUrl;
                return;
            }
            draft.value = null;
            editMode.value = false;
        }

        function capitalize(str) {
            if (!str) return str;
            return str.charAt(0).toUpperCase() + str.slice(1);
        }

        function formatDate(isoString) {
            if (!isoString) return '';
            return new Date(isoString).toLocaleDateString('fr-FR', { dateStyle: 'medium' });
        }

        const copied = ref(false);

        function copyToClipboard() {
            if (!recipe.value) return;
            const lines = [];
            lines.push(recipe.value.title);
            lines.push('');
            recipe.value.phases.forEach(phase => {
                if (recipe.value.phases.length > 1) {
                    lines.push(`— ${phase.label} —`);
                    lines.push('');
                }
                if (phase.ingredients.length > 0) {
                    lines.push('Ingrédients :');
                    phase.ingredients.forEach(ing => {
                        const qty = ing.quantity ? `${formatQuantity(ing.quantity)} ${ing.unit || ''}`.trim() + ' ' : '';
                        lines.push(`  • ${qty}${capitalize(ing.label)}`);
                    });
                    lines.push('');
                }
                if (phase.steps.length > 0) {
                    lines.push('Étapes :');
                    phase.steps.forEach((step, i) => lines.push(`  ${i + 1}. ${step.label}`));
                    lines.push('');
                }
            });
            lines.push(`Consulter la recette : https://chez-miette.xyz/recette/${recipe.value.id}`);
            navigator.clipboard.writeText(lines.join('\n')).then(() => {
                copied.value = true;
                setTimeout(() => copied.value = false, 2000);
            });
        }

        function formatQuantity(quantity) {
            if (quantity === null || quantity === undefined) return '';
            return quantity % 1 === 0 ? quantity.toFixed(0) : quantity.toFixed(1);
        }

        function addTag() {
            const tag = newTag.value.trim();
            if (tag && !draft.value.tags.includes(tag)) {
                draft.value.tags.push(tag);
            }
            newTag.value = '';
        }

        const noteAssets = computed(() => recipe.value?.assets?.filter(a => a.type === 'NOTE') ?? []);

        // --- Glossaire ---
        const { glossaryEnabled, glossaryTerms, glossaryLoading, glossaryGrouped, glossaryLetters,
                activeModalLetter, toggleGlossary, scrollToInModal } = useGlossaryHighlight(editMode);

        const isSinglePhase = computed(() => {
            const data = recipe.value;
            if (!data || !data.phases || data.phases.length === 0) return false;
            return data.phases.length === 1;
        });

        function addPhase() {
            draft.value.phases.push({
                id: null,
                _key: nextTempKey(),
                label: 'Nouvelle phase',
                position: draft.value.phases.length + 1,
                ingredients: [],
                steps: []
            });
        }

        function removePhase(phaseIndex) {
            if (draft.value.phases.length <= 1) return;
            const phase = draft.value.phases[phaseIndex];
            if (phase.ingredients.length > 0 || phase.steps.length > 0) {
                if (!confirm(`⚠️ La phase "${phase.label}" contient des données. Confirmer la suppression ?`)) return;
            }
            draft.value.phases.splice(phaseIndex, 1);
            draft.value.phases.forEach((p, i) => p.position = i + 1);
        }

        async function saveRecipe() {
            if (!validateDraft()) return;
            saving.value = true;
            try {
                const csrf = document.querySelector('meta[name="_csrf"]').content;
                const csrfHeader = document.querySelector('meta[name="_csrf_header"]').content;
                const isNew = recipe.value.id === null;
                const url = isNew ? '/api/recipes' : `/api/recipes/${recipe.value.id}`;
                const method = isNew ? 'POST' : 'PUT';
                const response = await fetch(url, {
                    method,
                    headers: { 'Content-Type': 'application/json', [csrfHeader]: csrf },
                    body: JSON.stringify(draft.value, (key, value) => key === '_key' ? undefined : value)
                });
                if (response.ok) {
                    clearErrors();
                    recipe.value = await response.json();
                    editMode.value = false;
                    draft.value = null;
                    if (isNew) {
                        window.history.replaceState(null, '', `/recette/${recipe.value.id}`);
                    }
                } else if (response.status === 400) {
                    const body = await response.json();
                    errors.value = body.errors ?? {};
                } else {
                    alert('Erreur lors de la sauvegarde');
                }
            } finally {
                saving.value = false;
            }
        }

        async function deleteRecipe() {
            if (!confirm('⚠️ Confirmer la suppression ?')) return;
            const csrf = document.querySelector('meta[name="_csrf"]').content;
            const csrfHeader = document.querySelector('meta[name="_csrf_header"]').content;
            const response = await fetch(`/api/recipes/${recipe.value.id}`, {
                method: 'DELETE',
                headers: { [csrfHeader]: csrf }
            });
            if (response.ok) {
                window.location.href = returnUrl;
            } else {
                alert('Erreur lors de la suppression');
            }
        }

        return {
            recipe, loading, saving, isDraggingPhase, error, isAdmin, editMode, draft,
            returnUrl, availableTags, filteredTags, newTag,
            photos, currentPhotoIndex, prevPhoto, nextPhoto,
            noteAssets, isSinglePhase, nextTempKey, formatQuantity, capitalize, formatDate,
            startEdit, cancelEdit, saveRecipe, deleteRecipe,
            addTag, handleTagKeydown, selectedTagIndex, addPhase, removePhase,
            notesOpen, showNoteForm, noteDate, noteDescription, savingNote, openNoteForm, addNote, deleteNote, formatNoteDate,
            uploadingPhoto, handleFileChange,
            errors, validateDraft, getError, clearErrors,
            glossaryEnabled, glossaryTerms, glossaryLoading, glossaryGrouped, glossaryLetters,
            activeModalLetter, toggleGlossary, scrollToInModal,
            copied, copyToClipboard
        };
    },

    template: `
        <div v-if="loading"></div>
        <div v-else-if="error" class="alert alert-danger">{{ error }}</div>
        <div v-else>

            <!-- Barre de boutons -->
            <div class="row mb-4">
                <div class="col-auto">
                    <a v-if="!editMode" :href="returnUrl" class="btn btn-outline-secondary">
                        <i class="bi bi-arrow-left"></i>
                        <span class="d-none d-md-inline"> Retour</span>
                    </a>
                    <button v-else @click="cancelEdit" class="btn btn-outline-secondary">
                        <i class="bi bi-arrow-left"></i>
                        <span class="d-none d-md-inline"> Annuler</span>
                    </button>
                </div>
                <div class="col d-flex gap-2 justify-content-end">
                    <button v-if="!editMode && isAdmin" @click="startEdit" class="btn btn-success">
                        <i class="bi bi-pencil"></i>
                        <span class="d-none d-md-inline"> Modifier</span>
                    </button>
                    <button v-if="!editMode && isAdmin" @click="deleteRecipe" class="btn btn-danger">
                        <i class="bi bi-trash"></i>
                        <span class="d-none d-md-inline"> Supprimer</span>
                    </button>
                    <button v-if="editMode" @click="saveRecipe" :disabled="saving" class="btn btn-primary">
                        <span v-if="saving" v-cubo-spinner class="me-1"></span>
                        <i v-else class="bi bi-floppy"></i>
                        <span class="d-none d-md-inline">{{ saving ? ' Enregistrement...' : ' Enregistrer' }}</span>
                    </button>
                </div>
            </div>

            <!-- Titre -->
            <h1 v-if="!editMode" class="mb-4">{{ recipe.title }}</h1>
            <input v-else v-model="draft.title" class="form-control form-control-lg" :class="{'is-invalid': errors.title, 'mb-4': !errors.title, 'mb-1': errors.title}" />
            <div v-if="errors.title" class="text-danger small mb-3">{{ errors.title }}</div>

            <!-- Tags -->
            <div class="mb-4">
                <span v-if="!editMode" v-for="tag in recipe.tags" :key="tag"
                    class="badge bg-primary me-2 mb-2">{{ tag }}</span>
                <div v-else class="d-flex flex-wrap gap-2 align-items-center">
                    <span v-for="(tag, index) in draft.tags" :key="index"
                        class="badge bg-primary d-flex align-items-center gap-1">
                        {{ tag }}
                        <button @click="draft.tags.splice(index, 1)"
                            class="btn-close btn-close-white" style="font-size: 0.6rem;"></button>
                    </span>
                    <div class="d-flex gap-1 position-relative">
                        <input v-model="newTag"
                            @keydown="handleTagKeydown"
                            class="form-control form-control-sm"
                            style="width: 120px"
                            placeholder="+ tag..." />
                        <button @click="addTag" class="btn btn-outline-secondary btn-sm">
                            <i class="bi bi-plus"></i>
                        </button>
                        <!-- Dropdown suggestions -->
                        <ul v-if="filteredTags.length > 0"
                            class="list-group position-absolute shadow-sm"
                            style="top: 100%; left: 0; z-index: 1000; min-width: 150px">
                            <li v-for="(tag, index) in filteredTags" :key="tag"
                                @mousedown.prevent="draft.tags.push(tag); newTag = ''"
                                :class="['list-group-item', 'list-group-item-action', 'py-1', 'px-2', { active: index === selectedTagIndex }]"
                                style="cursor: pointer; font-size: 0.85rem">
                                {{ tag }}
                            </li>
                        </ul>
                    </div>
                </div>
            </div>

            <!-- Notes -->
            <div v-if="recipe.id !== null && (noteAssets.length > 0 || isAdmin)" class="mb-4">
                <button @click="notesOpen = !notesOpen"
                    class="btn btn-link text-decoration-none px-0 text-muted d-flex align-items-center gap-2">
                    <i :class="notesOpen ? 'bi bi-chevron-down' : 'bi bi-chevron-right'"></i>
                    <span>Notes <span v-if="noteAssets.length > 0" class="badge bg-secondary">{{ noteAssets.length }}</span></span>
                </button>

                <div v-if="notesOpen" class="mt-2">
                    <!-- Formulaire ajout note (admin) -->
                    <div v-if="isAdmin && !editMode && !showNoteForm" class="mb-3">
                        <button @click="openNoteForm" class="btn btn-outline-secondary btn-sm">
                            <i class="bi bi-plus"></i> Nouvelle note
                        </button>
                    </div>
                    <div v-if="isAdmin && showNoteForm" class="card mb-3">
                        <div class="card-body">
                            <div class="mb-2">
                                <label class="form-label small text-muted">Date</label>
                                <input v-model="noteDate" type="datetime-local" class="form-control form-control-sm" />
                            </div>
                            <div class="mb-2">
                                <label class="form-label small text-muted">Description</label>
                                <textarea v-model="noteDescription" class="form-control form-control-sm" rows="3"></textarea>
                            </div>
                            <div class="d-flex gap-2">
                                <button @click="addNote" :disabled="savingNote" class="btn btn-primary btn-sm">
                                    <span v-if="savingNote" v-cubo-spinner class="me-1"></span>
                                    Enregistrer
                                </button>
                                <button @click="showNoteForm = false" class="btn btn-outline-secondary btn-sm">Annuler</button>
                            </div>
                        </div>
                    </div>
                    <div v-if="noteAssets.length > 0">
                        <div v-for="asset in noteAssets" :key="asset.id"
                            class="border-start border-2 ps-3 mb-3">
                            <div class="d-flex justify-content-between align-items-start">
                                <small class="text-muted">{{ formatNoteDate(asset.date) }}</small>
                                <button v-if="isAdmin && !editMode" @click="deleteNote(asset.id)"
                                    class="btn btn-outline-danger btn-sm ms-2">
                                    <i class="bi bi-x"></i>
                                </button>
                            </div>
                            <p class="mb-0 mt-1" style="white-space: pre-wrap">{{ asset.description }}</p>
                        </div>
                    </div>
                    <p v-else class="text-muted small">Aucune note.</p>
                </div>
            </div>

            <!-- Photos -->
            <div v-if="recipe.id !== null && !editMode && (photos.length > 0 || isAdmin)" class="row justify-content-center mb-4">
                <div class="col-12 col-lg-10">
                    <div v-if="photos.length > 0" class="card shadow-sm">
                        <img :src="photos[currentPhotoIndex].url"
                            class="card-img-top"
                            style="object-fit: cover; max-height: 420px;">
                        <div class="card-footer d-flex justify-content-between align-items-center py-2">
                            <button @click="prevPhoto" class="btn btn-outline-secondary btn-sm">
                                <i class="bi bi-chevron-left"></i>
                            </button>
                            <div class="text-center">
                                <small class="text-muted d-block">{{ formatNoteDate(photos[currentPhotoIndex].date) }}</small>
                                <small v-if="photos.length > 1" class="text-muted">{{ currentPhotoIndex + 1 }} / {{ photos.length }}</small>
                            </div>
                            <div class="d-flex gap-1 align-items-center">
                                <button v-if="isAdmin && !editMode" @click="deleteNote(photos[currentPhotoIndex].id)"
                                    class="btn btn-outline-danger btn-sm">
                                    <i class="bi bi-trash"></i>
                                </button>
                                <button @click="nextPhoto" class="btn btn-outline-secondary btn-sm">
                                    <i class="bi bi-chevron-right"></i>
                                </button>
                            </div>
                        </div>
                    </div>
                    <label v-if="isAdmin && !editMode"
                        class="btn btn-outline-secondary btn-sm mt-2" :class="{ disabled: uploadingPhoto }">
                        <span v-if="uploadingPhoto" v-cubo-spinner class="me-1"></span>
                        <i v-else class="bi bi-image"></i> Ajouter une photo
                        <input type="file" accept="image/*" style="display:none" @change="handleFileChange">
                    </label>
                </div>
            </div>

            <!-- Phases en lecture -->
            <div id="phases-content">

            <!-- Phase unique -->
            <div v-if="!editMode && isSinglePhase" class="card shadow-sm mb-4">
                <div class="card-body">
                    <div class="row g-4">
                        <div class="col-12 col-md-4">
                            <h5><i class="bi text-warning"></i> Ingrédients</h5>
                            <ul class="list-group list-group-flush">
                                <li v-for="ing in recipe.phases[0].ingredients" :key="ing.id"
                                    class="list-group-item px-0 border-0 py-2">
                                    {{ capitalize(ing.label) }} : {{ formatQuantity(ing.quantity) }} {{ ing.unit || '' }}
                                </li>
                            </ul>
                        </div>
                        <div class="col-12 col-md-8">
                            <h5><i class="bi bi-list-numbered text-info"></i> Étapes</h5>
                            <ol class="list-group list-group-numbered list-group-flush">
                                <li v-for="step in recipe.phases[0].steps" :key="step.id"
                                    class="list-group-item px-0 border-0 py-3">{{ step.label }}</li>
                            </ol>
                        </div>
                    </div>
                </div>
                <div class="card-footer py-2 d-flex justify-content-between align-items-center bg-transparent border-top">
                    <div class="text-muted lh-sm" style="font-size: 0.7rem;">
                        <div>Créé le {{ formatDate(recipe.createdAt) }}<span v-if="recipe.createdBy"> par {{ recipe.createdBy }}</span></div>
                        <div v-if="recipe.updatedAt">Modifié le {{ formatDate(recipe.updatedAt) }}<span v-if="recipe.updatedBy"> par {{ recipe.updatedBy }}</span></div>
                    </div>
                    <div class="d-flex gap-2">
                        <button @click="copyToClipboard" v-tooltip data-bs-title="Copier la recette" class="btn btn-outline-secondary btn-sm">
                            <i :class="copied ? 'bi bi-check2' : 'bi bi-clipboard'"></i>
                        </button>
                        <button v-if="glossaryEnabled" v-tooltip data-bs-title="Voir le glossaire" class="btn btn-outline-secondary btn-sm"
                            data-bs-toggle="modal" data-bs-target="#glossaryModal">
                            <i class="bi bi-book"></i><span class="d-none d-md-inline"> Glossaire</span>
                        </button>
                        <button @click="toggleGlossary" :disabled="glossaryLoading" v-tooltip data-bs-title="Surligner les termes techniques"
                            :class="['btn', 'btn-sm', 'btn-glossary-toggle', glossaryEnabled ? 'btn-secondary' : 'btn-outline-secondary']">
                            <span v-if="glossaryLoading" v-cubo-spinner></span>
                            <i v-else class="bi bi-question-circle"></i>
                        </button>
                    </div>
                </div>
            </div>

            <!-- Phases multiples -->
            <div v-else-if="!editMode && !isSinglePhase">
                <div v-for="(phase, phaseIndex) in recipe.phases" :key="phase.id" class="card shadow-sm mb-4">
                    <div class="card-header">
                        <h5 class="mb-0 fw-bold text-primary">{{ phase.label }}</h5>
                    </div>
                    <div class="card-body">
                        <div class="row g-4">
                            <div v-if="phase.ingredients.length > 0" class="col-12 col-md-4">
                                <h6>Ingrédients</h6>
                                <ul class="list-group list-group-flush">
                                    <li v-for="ing in phase.ingredients" :key="ing.id"
                                        class="list-group-item px-0 border-0 py-2">
                                        {{ capitalize(ing.label) }} : {{ formatQuantity(ing.quantity) }} {{ ing.unit || '' }}
                                    </li>
                                </ul>
                            </div>
                            <div v-if="phase.steps.length > 0" class="col-12 col-md-8">
                                <h6><i class="bi bi-list-numbered text-info"></i> Étapes</h6>
                                <ol class="list-group list-group-numbered list-group-flush">
                                    <li v-for="step in phase.steps" :key="step.id"
                                        class="list-group-item px-0 border-0 py-3">{{ step.label }}</li>
                                </ol>
                            </div>
                        </div>
                    </div>
                    <div v-if="phaseIndex === recipe.phases.length - 1" class="card-footer py-2 d-flex justify-content-between align-items-center bg-transparent border-top">
                        <div class="text-muted lh-sm" style="font-size: 0.7rem;">
                            <div>Créé le {{ formatNoteDate(recipe.createdAt) }}<span v-if="recipe.createdBy"> par {{ recipe.createdBy }}</span></div>
                            <div v-if="recipe.updatedAt">Modifié le {{ formatNoteDate(recipe.updatedAt) }}<span v-if="recipe.updatedBy"> par {{ recipe.updatedBy }}</span></div>
                        </div>
                        <div class="d-flex gap-2">
                            <button @click="copyToClipboard" v-tooltip data-bs-title="Copier la recette" class="btn btn-outline-secondary btn-sm">
                                <i :class="copied ? 'bi bi-check2' : 'bi bi-clipboard'"></i>
                            </button>
                            <button v-if="glossaryEnabled" v-tooltip data-bs-title="Voir le glossaire" class="btn btn-outline-secondary btn-sm"
                                data-bs-toggle="modal" data-bs-target="#glossaryModal">
                                <i class="bi bi-book"></i><span class="d-none d-md-inline"> Glossaire</span>
                            </button>
                            <button @click="toggleGlossary" v-tooltip data-bs-title="Surligner les termes techniques"
                                :class="['btn', 'btn-sm', 'btn-glossary-toggle', glossaryEnabled ? 'btn-secondary' : 'btn-outline-secondary']">
                                <i class="bi bi-question-circle"></i>
                            </button>
                        </div>
                    </div>
                </div>
            </div>

            </div><!-- fin phases-content -->

            <!-- Phases en édition -->
            <div v-if="editMode" v-sortable-phases="{ phases: draft.phases, setDragging: (val) => isDraggingPhase = val, clearErrors }">
                <div v-for="(phase, phaseIndex) in draft.phases" :key="phase.id ?? phase._key" class="card shadow-sm mb-4">

                    <!-- Card header : label de phase (multiples phases uniquement) -->
                    <div v-if="draft.phases.length > 1" class="card-header d-flex align-items-center gap-2">
                        <i class="bi bi-grip-vertical phase-drag-handle text-muted" style="cursor: grab"></i>
                        <div class="flex-grow-1">
                            <input v-model="phase.label"
                                class="form-control fw-bold text-primary" placeholder="Nom de la phase"
                                :class="{'is-invalid': getError('phases[' + phaseIndex + '].label')}" />
                            <div v-if="getError('phases[' + phaseIndex + '].label')" class="text-danger small mt-1">
                                {{ getError('phases[' + phaseIndex + '].label') }}
                            </div>
                        </div>
                        <button @click="removePhase(phaseIndex)" class="btn btn-outline-danger btn-sm ms-2">
                            <i class="bi bi-x"></i>
                        </button>
                    </div>

                    <div class="card-body" v-show="!isDraggingPhase">
                        <div class="row g-4">
                            <!-- Ingrédients -->
                            <div class="col-12 col-md-4">
                                <h5>Ingrédients</h5>
                                <ul v-sortable-ingredients="{ items: phase.ingredients, clearErrors }" class="list-group list-group-flush">
                                    <li v-for="(ing, ingIndex) in phase.ingredients" :key="ing.id ?? ing._key"
                                        class="list-group-item px-0 border-0 py-2">
                                        <div class="d-flex gap-2 align-items-start">
                                            <i class="bi bi-grip-vertical drag-handle text-muted mt-2" style="cursor: grab; flex-shrink: 0"></i>
                                            <div class="flex-grow-1">
                                                <input v-model="ing.label" class="form-control form-control-sm mb-1" placeholder="Ingrédient"
                                                    :class="{'is-invalid': getError('phases[' + phaseIndex + '].ingredients[' + ingIndex + '].label')}" />
                                                <div v-if="getError('phases[' + phaseIndex + '].ingredients[' + ingIndex + '].label')" class="text-danger small">
                                                    {{ getError('phases[' + phaseIndex + '].ingredients[' + ingIndex + '].label') }}
                                                </div>
                                                <div class="d-flex gap-1">
                                                    <input v-model="ing.quantity" type="number" class="form-control form-control-sm" placeholder="Qté"
                                                        style="width: 70px"
                                                        :class="{'is-invalid': getError('phases[' + phaseIndex + '].ingredients[' + ingIndex + '].quantity')}" />
                                                    <input v-model="ing.unit" class="form-control form-control-sm" placeholder="unité" />
                                                </div>
                                                <div v-if="getError('phases[' + phaseIndex + '].ingredients[' + ingIndex + '].quantity')" class="text-danger small">
                                                    {{ getError('phases[' + phaseIndex + '].ingredients[' + ingIndex + '].quantity') }}
                                                </div>
                                            </div>
                                            <button @click="phase.ingredients.splice(ingIndex, 1)" class="btn btn-outline-danger btn-sm mt-1" style="flex-shrink: 0">
                                                <i class="bi bi-x"></i>
                                            </button>
                                        </div>
                                    </li>
                                </ul>
                                <button @click="phase.ingredients.push({id: null, _key: nextTempKey(), label: '', quantity: null, unit: '', position: phase.ingredients.length + 1})"
                                    class="btn btn-outline-secondary btn-sm mt-2">
                                    <i class="bi bi-plus"></i> Ajouter un ingrédient
                                </button>
                            </div>

                            <!-- Étapes -->
                            <div class="col-12 col-md-8">
                                <h5><i class="bi bi-list-numbered text-info"></i> Étapes</h5>
                                <ol v-sortable-steps="{ items: phase.steps, clearErrors }" class="list-group list-group-flush">
                                    <li v-for="(step, stepIndex) in phase.steps" :key="step.id ?? step._key"
                                        class="list-group-item px-0 border-0 py-2 d-flex gap-2 align-items-center">
                                        <i class="bi bi-grip-vertical drag-handle text-muted" style="cursor: grab"></i>
                                        <span class="text-muted me-1">{{ stepIndex + 1 }}.</span>
                                        <textarea v-model="step.label" v-auto-resize class="form-control" rows="1" style="resize: none;"></textarea>
                                        <button @click="phase.steps.splice(stepIndex, 1)" class="btn btn-outline-danger btn-sm">
                                            <i class="bi bi-x"></i>
                                        </button>
                                    </li>
                                </ol>
                                <button @click="phase.steps.push({id: null, _key: nextTempKey(), label: '', position: phase.steps.length + 1})"
                                    class="btn btn-outline-secondary btn-sm mt-2">
                                    <i class="bi bi-plus"></i> Ajouter une étape
                                </button>
                            </div>
                        </div>
                    </div>
                </div>
                <!-- Ajouter une phase -->
                <button @click="addPhase" class="btn btn-outline-secondary btn-sm mt-2">
                    <i class="bi bi-plus"></i> Ajouter une phase
                </button>
            </div>

        <!-- Modal glossaire -->
        <div class="modal fade" id="glossaryModal" tabindex="-1">
            <div class="modal-dialog modal-lg modal-dialog-scrollable">
                <div class="modal-content">
                    <div class="modal-header">
                        <h5 class="modal-title"><i class="bi bi-book me-2"></i>Ça veut dire quoi ?</h5>
                        <button type="button" class="btn-close" data-bs-dismiss="modal"></button>
                    </div>
                    <div class="modal-body pt-0">
                        <!-- Index horizontal mobile (direct child = sticky fonctionne sur tout le scroll) -->
                        <div class="d-flex flex-wrap gap-1 mb-3 d-md-none alphabet-mobile-bar">
                            <span v-for="letter in glossaryLetters" :key="letter"
                                  @click="scrollToInModal(letter)"
                                  :class="['alphabet-letter-mobile', { active: letter === activeModalLetter }]">{{ letter }}</span>
                        </div>
                        <!-- Layout desktop : barre verticale + contenu -->
                        <div class="d-flex gap-3">
                        <!-- Index alphabétique vertical -->
                        <div class="d-none d-md-flex flex-column align-items-center gap-1 flex-shrink-0 pt-1" style="position: sticky; top: 0; align-self: flex-start;">
                            <span v-for="letter in glossaryLetters" :key="letter"
                                  @click="scrollToInModal(letter)"
                                  :class="['alphabet-letter', { active: letter === activeModalLetter }]">{{ letter }}</span>
                        </div>
                        <!-- Contenu -->
                        <div class="flex-grow-1">
                            <!-- Termes groupés -->
                            <div v-for="[letter, group] in glossaryGrouped" :key="letter">
                                <h6 class="modal-letter text-muted pb-1 border-bottom mb-3"
                                    :data-letter="letter">{{ letter }}</h6>
                                <div v-for="term in group" :key="term.id" class="mb-4">
                                    <div class="d-flex align-items-baseline gap-2 mb-1 flex-wrap">
                                        <span class="fw-bold">{{ term.term }}</span>
                                        <span v-for="alias in term.aliases" :key="alias.id"
                                              class="badge bg-light text-muted border small fw-normal">{{ alias.alias }}</span>
                                    </div>
                                    <p class="mb-0 small">{{ term.definition }}</p>
                                </div>
                            </div>
                        </div>
                        </div><!-- fin d-flex gap-3 -->
                    </div><!-- fin modal-body -->
                </div>
            </div>
        </div>

        </div>
    `
}).mount('#app');
