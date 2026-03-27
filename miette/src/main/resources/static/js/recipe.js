import { useNotes } from './useNotes.js';
import { useValidation } from './useValidation.js';

const { createApp, ref, onMounted, computed } = Vue;

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
    directives: { sortableSteps: vSortableSteps, sortableIngredients: vSortableIngredients, sortablePhases: vSortablePhases },
    setup() {
        let _keyCounter = 0;
        function nextTempKey() { return --_keyCounter; }

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
        const returnUrl = document.getElementById('app').dataset.returnUrl;

        const { notesOpen, showNoteForm, noteDate, noteDescription, savingNote, openNoteForm, addNote, deleteNote, formatNoteDate } = useNotes(recipe);
        const { errors, validateDraft, getError, clearErrors } = useValidation(draft);

        const filteredTags = computed(() => {
            if (!newTag.value.trim()) return [];
            return availableTags.value.filter(t =>
                t.toLowerCase().includes(newTag.value.toLowerCase()) &&
                !draft.value.tags.includes(t)
            );
        });

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
            isSinglePhase, nextTempKey, formatQuantity,
            startEdit, cancelEdit, saveRecipe, deleteRecipe,
            addTag, addPhase, removePhase,
            notesOpen, showNoteForm, noteDate, noteDescription, savingNote, openNoteForm, addNote, deleteNote, formatNoteDate,
            errors, validateDraft, getError, clearErrors
        };
    },

    template: `
        <div v-if="loading">Chargement...</div>
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
                        <span v-if="saving" class="spinner-border spinner-border-sm me-1"></span>
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
                            @keydown.enter.prevent="addTag"
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
                            <li v-for="tag in filteredTags" :key="tag"
                                @click="draft.tags.push(tag); newTag = ''"
                                class="list-group-item list-group-item-action py-1 px-2"
                                style="cursor: pointer; font-size: 0.85rem">
                                {{ tag }}
                            </li>
                        </ul>
                    </div>
                </div>
            </div>

            <!-- Notes -->
            <div v-if="recipe.id !== null && (recipe.assets && recipe.assets.length > 0 || isAdmin)" class="mb-4">
                <button @click="notesOpen = !notesOpen"
                    class="btn btn-link text-decoration-none px-0 text-muted d-flex align-items-center gap-2">
                    <i :class="notesOpen ? 'bi bi-chevron-down' : 'bi bi-chevron-right'"></i>
                    <span>Notes <span v-if="recipe.assets && recipe.assets.length > 0" class="badge bg-secondary">{{ recipe.assets.length }}</span></span>
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
                                    <span v-if="savingNote" class="spinner-border spinner-border-sm me-1"></span>
                                    Enregistrer
                                </button>
                                <button @click="showNoteForm = false" class="btn btn-outline-secondary btn-sm">Annuler</button>
                            </div>
                        </div>
                    </div>

                    <!-- Liste des notes -->
                    <div v-if="recipe.assets && recipe.assets.length > 0">
                        <div v-for="asset in recipe.assets" :key="asset.id"
                            class="border-start border-2 ps-3 mb-3 position-relative">
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

            <!-- Phases en lecture -->
            <div v-if="!editMode && isSinglePhase">
                <section class="mb-5">
                    <h3><i class="bi bi-egg-fried text-warning"></i> Ingrédients</h3>
                    <ul class="list-group list-group-flush">
                        <li v-for="ing in recipe.phases[0].ingredients" :key="ing.id"
                            class="list-group-item px-0 border-0 py-2">
                            {{ ing.label }} : {{ formatQuantity(ing.quantity) }} {{ ing.unit || '' }}
                        </li>
                    </ul>
                </section>
                <section>
                    <h3><i class="bi bi-list-numbered text-info"></i> Étapes</h3>
                    <ol class="list-group list-group-numbered list-group-flush">
                        <li v-for="step in recipe.phases[0].steps" :key="step.id"
                            class="list-group-item px-0 border-0 py-3">{{ step.label }}</li>
                    </ol>
                </section>
            </div>

            <div v-else-if="!editMode && !isSinglePhase">
                <div v-for="phase in recipe.phases" :key="phase.id" class="mb-5">
                    <h3 class="fw-bold text-primary mb-4">{{ phase.label }}</h3>
                    <section v-if="phase.ingredients.length > 0">
                        <h5><i class="bi bi-egg-fried text-warning"></i> Ingrédients</h5>
                        <ul class="list-group list-group-flush">
                            <li v-for="ing in phase.ingredients" :key="ing.id"
                                class="list-group-item px-0 border-0 py-2">
                                {{ ing.label }} : {{ formatQuantity(ing.quantity) }} {{ ing.unit || '' }}
                            </li>
                        </ul>
                    </section>
                    <section v-if="phase.steps.length > 0" class="mt-4">
                        <h5><i class="bi bi-list-numbered text-info"></i> Étapes</h5>
                        <ol class="list-group list-group-numbered list-group-flush">
                            <li v-for="step in phase.steps" :key="step.id"
                                class="list-group-item px-0 border-0 py-3">{{ step.label }}</li>
                        </ol>
                    </section>
                </div>
            </div>

            <!-- Phases en édition -->
            <div v-if="editMode" v-sortable-phases="{ phases: draft.phases, setDragging: (val) => isDraggingPhase = val, clearErrors }">
                <div v-for="(phase, phaseIndex) in draft.phases" :key="phase.id ?? phase._key" class="mb-5">

                    <!-- Label phase (seulement si multiples phases) -->
                    <div class="d-flex justify-content-between align-items-center mb-4">
                        <i v-if="draft.phases.length > 1"
                            class="bi bi-grip-vertical phase-drag-handle text-muted me-2"
                            style="cursor: grab"></i>
                        <div v-if="draft.phases.length > 1" class="flex-grow-1">
                            <input v-model="phase.label"
                                class="form-control fw-bold text-primary" placeholder="Nom de la phase"
                                :class="{'is-invalid': getError('phases[' + phaseIndex + '].label')}" />
                            <div v-if="getError('phases[' + phaseIndex + '].label')" class="text-danger small mt-1">
                                {{ getError('phases[' + phaseIndex + '].label') }}
                            </div>
                        </div>
                        <h5 v-else class="mb-0 text-muted">Phase unique</h5>
                        <button v-if="draft.phases.length > 1"
                                @click="removePhase(phaseIndex)"
                                class="btn btn-outline-danger btn-sm ms-2">
                            <i class="bi bi-x"></i>
                        </button>
                    </div>

                    <div v-show="!isDraggingPhase" >
                        <!-- Ingrédients -->
                        <section class="mb-4">
                            <h5><i class="bi bi-egg-fried text-warning"></i> Ingrédients</h5>
                            <ul v-sortable-ingredients="{ items: phase.ingredients, clearErrors }" class="list-group list-group-flush">
                                <li v-for="(ing, ingIndex) in phase.ingredients" :key="ing.id ?? ing._key"
                                    class="list-group-item px-0 border-0 py-2">
                                    <div class="row g-2 align-items-start">
                                        <div class="col-auto pt-1">
                                            <i class="bi bi-grip-vertical drag-handle text-muted" style="cursor: grab"></i>
                                        </div>
                                        <div class="col">
                                            <input v-model="ing.label" class="form-control" placeholder="Ingrédient"
                                                :class="{'is-invalid': getError('phases[' + phaseIndex + '].ingredients[' + ingIndex + '].label')}" />
                                            <div v-if="getError('phases[' + phaseIndex + '].ingredients[' + ingIndex + '].label')" class="text-danger small">
                                                {{ getError('phases[' + phaseIndex + '].ingredients[' + ingIndex + '].label') }}
                                            </div>
                                        </div>
                                        <div class="col-3 col-md-2">
                                            <input v-model="ing.quantity" type="number" class="form-control"
                                                :class="{'is-invalid': getError('phases[' + phaseIndex + '].ingredients[' + ingIndex + '].quantity')}" />
                                            <div v-if="getError('phases[' + phaseIndex + '].ingredients[' + ingIndex + '].quantity')" class="text-danger small">
                                                {{ getError('phases[' + phaseIndex + '].ingredients[' + ingIndex + '].quantity') }}
                                            </div>
                                        </div>
                                        <div class="col-3 col-md-2">
                                            <input v-model="ing.unit" class="form-control" placeholder="unité" />
                                        </div>
                                        <div class="col-auto pt-1">
                                            <button @click="phase.ingredients.splice(ingIndex, 1)" class="btn btn-outline-danger btn-sm">
                                                <i class="bi bi-x"></i>
                                            </button>
                                        </div>
                                    </div>
                                </li>
                            </ul>
                            <button @click="phase.ingredients.push({id: null, _key: nextTempKey(), label: '', quantity: null, unit: '', position: phase.ingredients.length + 1})"
                                class="btn btn-outline-secondary btn-sm mt-2">
                                <i class="bi bi-plus"></i> Ajouter un ingrédient
                            </button>
                        </section>

                        <!-- Étapes -->
                        <section>
                            <h5><i class="bi bi-list-numbered text-info"></i> Étapes</h5>
                            <ol v-sortable-steps="{ items: phase.steps, clearErrors }" class="list-group list-group-flush">
                                <li v-for="(step, stepIndex) in phase.steps" :key="step.id ?? step._key"
                                    class="list-group-item px-0 border-0 py-2 d-flex gap-2 align-items-center">
                                    <i class="bi bi-grip-vertical drag-handle text-muted" style="cursor: grab"></i>
                                    <span class="text-muted me-1">{{ stepIndex + 1 }}.</span>
                                    <input v-model="step.label" class="form-control" />
                                    <button @click="phase.steps.splice(stepIndex, 1)" class="btn btn-outline-danger btn-sm">
                                        <i class="bi bi-x"></i>
                                    </button>
                                </li>
                            </ol>
                            <button @click="phase.steps.push({id: null, _key: nextTempKey(), label: '', position: phase.steps.length + 1})"
                                class="btn btn-outline-secondary btn-sm mt-2">
                                <i class="bi bi-plus"></i> Ajouter une étape
                            </button>
                        </section>
                    </div>
                </div>
                <!-- Ajouter une phase -->
                <button @click="addPhase" class="btn btn-outline-secondary btn-sm mt-2">
                    <i class="bi bi-plus"></i> Ajouter une phase
                </button>
            </div>

        </div>
    `
}).mount('#app');
