import { nextTempKey } from './utils.js';
import { groupTermsByLetter } from './glossary-utils.js';
import { vCuboSpinner } from './cubo.js';

const { createApp, ref, computed, watch, onMounted, nextTick } = Vue;

createApp({
    directives: { cuboSpinner: vCuboSpinner },
    setup() {
        const terms = ref([]);
        const loading = ref(true);
        const error = ref(null);
        const isAdmin = ref(false);
        const editMode = ref(false);
        const saving = ref(false);
        const activeLetter = ref(null);
        let observer = null;
        const draft = ref([]);
        const newTerms = ref([]);
        const fieldErrors = ref({});

        // --- Lecture ---

        const grouped = computed(() => {
            const source = editMode.value ? draft.value : terms.value;
            return groupTermsByLetter(source);
        });

        const letters = computed(() => grouped.value.map(([l]) => l));

        onMounted(async () => {
            isAdmin.value = document.getElementById('app').dataset.isAdmin === 'true';
            try {
                const res = await fetch('/api/glossary');
                if (!res.ok) throw new Error('Erreur de chargement');
                terms.value = await res.json();
            } catch (e) {
                error.value = e.message;
            } finally {
                window._stopCuboPreloader?.();
                loading.value = false;
            }
            await nextTick();
            setupObserver();
        });

        watch(editMode, async () => { await nextTick(); setupObserver(); });

        function setupObserver() {
            if (observer) observer.disconnect();
            observer = new IntersectionObserver(entries => {
                for (const entry of entries) {
                    if (entry.isIntersecting) activeLetter.value = entry.target.dataset.letter;
                }
            }, { rootMargin: '-20% 0px -70% 0px' });
            document.querySelectorAll('.letter-heading').forEach(el => observer.observe(el));
        }

        function scrollTo(letter) {
            const el = document.querySelector(`.letter-heading[data-letter="${letter}"]`);
            if (el) el.scrollIntoView({ behavior: 'smooth', block: 'start' });
        }

        // --- Édition ---

        function startEdit() {
            draft.value = terms.value.map(t => ({
                ...t,
                aliases: t.aliases.map(a => ({ ...a }))
            }));
            newTerms.value = [];
            fieldErrors.value = {};
            editMode.value = true;
        }

        function cancelEdit() {
            if (hasChanges()) {
                if (!confirm('Annuler les modifications ? Les changements non sauvegardés seront perdus.')) return;
            }
            editMode.value = false;
            fieldErrors.value = {};
        }

        function hasChanges() {
            if (newTerms.value.length > 0) return true;
            return JSON.stringify(draft.value) !== JSON.stringify(terms.value);
        }

        window.addEventListener('beforeunload', e => { if (editMode.value && hasChanges()) e.preventDefault(); });

        function addNewTerm() {
            newTerms.value.push({ _key: nextTempKey(), term: '', definition: '', aliases: [] });
        }

        function removeNewTerm(index) {
            newTerms.value.splice(index, 1);
        }

        function addAlias(termObj) {
            termObj.aliases.push({ id: null, _key: nextTempKey(), alias: '' });
        }

        function removeAlias(termObj, index) {
            termObj.aliases.splice(index, 1);
        }

        function validate() {
            fieldErrors.value = {};
            const all = [...draft.value, ...newTerms.value];
            let valid = true;
            all.forEach(t => {
                const prefix = t.id ? `term_${t.id}` : `new_${t._key}`;
                if (!t.term?.trim()) { fieldErrors.value[`${prefix}_term`] = 'Le terme est obligatoire'; valid = false; }
                if (!t.definition?.trim()) { fieldErrors.value[`${prefix}_def`] = 'La définition est obligatoire'; valid = false; }
            });
            return valid;
        }

        async function saveAll() {
            if (!validate()) return;
            saving.value = true;
            const csrf = document.querySelector('meta[name="_csrf"]').content;
            const csrfHeader = document.querySelector('meta[name="_csrf_header"]').content;
            const headers = { 'Content-Type': 'application/json', [csrfHeader]: csrf };

            try {
                // Termes existants modifiés
                for (const t of draft.value) {
                    const original = terms.value.find(o => o.id === t.id);
                    if (JSON.stringify(t) !== JSON.stringify(original)) {
                        const res = await fetch(`/api/glossary/${t.id}`, {
                            method: 'PUT',
                            headers,
                            body: JSON.stringify({ term: t.term, definition: t.definition, aliases: t.aliases.map(a => a.alias).filter(a => a.trim()) })
                        });
                        if (!res.ok) throw new Error('Erreur lors de la sauvegarde');
                    }
                }
                // Nouveaux termes
                for (const t of newTerms.value) {
                    const res = await fetch('/api/glossary', {
                        method: 'POST',
                        headers,
                        body: JSON.stringify({ term: t.term, definition: t.definition, aliases: t.aliases.map(a => a.alias).filter(a => a.trim()) })
                    });
                    if (!res.ok) throw new Error('Erreur lors de la sauvegarde');
                }
                // Recharger
                const res = await fetch('/api/glossary');
                terms.value = await res.json();
                editMode.value = false;
                newTerms.value = [];
                fieldErrors.value = {};
            } catch (e) {
                alert(e.message);
            } finally {
                saving.value = false;
            }
        }

        async function deleteTerm(id) {
            if (!confirm('Supprimer ce terme et tous ses alias ?')) return;
            const csrf = document.querySelector('meta[name="_csrf"]').content;
            const csrfHeader = document.querySelector('meta[name="_csrf_header"]').content;
            const res = await fetch(`/api/glossary/${id}`, { method: 'DELETE', headers: { [csrfHeader]: csrf } });
            if (res.ok) {
                draft.value = draft.value.filter(t => t.id !== id);
                terms.value = terms.value.filter(t => t.id !== id);
            }
        }

        function termError(t, field) {
            const prefix = t.id ? `term_${t.id}` : `new_${t._key}`;
            return fieldErrors.value[`${prefix}_${field}`];
        }

        return {
            terms, grouped, letters, loading, error, isAdmin,
            activeLetter, scrollTo,
            editMode, saving, draft, newTerms, fieldErrors,
            startEdit, cancelEdit, saveAll, deleteTerm,
            addNewTerm, removeNewTerm, addAlias, removeAlias, termError
        };
    },

    template: `
        <div v-if="loading"></div>
        <div v-else-if="error" class="alert alert-danger">{{ error }}</div>
        <div v-else class="position-relative">

            <!-- Index alphabétique latéral -->
            <div class="alphabet-index d-none d-md-flex">
                <span v-for="letter in letters" :key="letter"
                      @click="scrollTo(letter)"
                      :class="['alphabet-letter', { active: letter === activeLetter }]">
                    {{ letter }}
                </span>
            </div>

            <!-- En-tête -->
            <div class="d-flex justify-content-between align-items-baseline mb-4">
                <h1>Ça veut dire quoi ?</h1>
                <div v-if="isAdmin" class="d-flex gap-2">
                    <a v-if="!editMode" href="/admin" class="btn btn-outline-secondary btn-sm">
                        <i class="bi bi-arrow-left"></i><span class="d-none d-md-inline"> Admin</span>
                    </a>
                    <button v-if="!editMode" @click="startEdit" class="btn btn-success btn-sm">
                        <i class="bi bi-pencil"></i><span class="d-none d-md-inline"> Modifier</span>
                    </button>
                    <template v-else>
                        <button @click="saveAll" :disabled="saving" class="btn btn-primary btn-sm">
                            <span v-if="saving" v-cubo-spinner="{ color: '#fff' }" class="me-1"></span>
                            <i v-else class="bi bi-floppy"></i><span class="d-none d-md-inline">{{ saving ? ' Enregistrement...' : ' Enregistrer' }}</span>
                        </button>
                        <button @click="cancelEdit" class="btn btn-outline-secondary btn-sm">
                            <i class="bi bi-arrow-left"></i><span class="d-none d-md-inline"> Annuler</span>
                        </button>
                    </template>
                </div>
            </div>

            <!-- Index horizontal mobile -->
            <div class="d-flex flex-wrap gap-2 mb-3 d-md-none alphabet-mobile-bar">
                <span v-for="letter in letters" :key="letter"
                      @click="scrollTo(letter)"
                      :class="['alphabet-letter-mobile', { active: letter === activeLetter }]">
                    {{ letter }}
                </span>
            </div>

            <!-- Nouveaux termes (édition) -->
            <template v-if="editMode">
                <div class="mb-4">
                    <button @click="addNewTerm" class="btn btn-outline-secondary btn-sm mb-3">
                        <i class="bi bi-plus"></i> Nouveau terme
                    </button>
                    <div v-for="(t, i) in newTerms" :key="t._key" class="card mb-3 border-success">
                        <div class="card-body">
                            <div class="d-flex gap-2 mb-2">
                                <div class="flex-grow-1">
                                    <input v-model="t.term" class="form-control form-control-sm"
                                           :class="{'is-invalid': termError(t, 'term')}" placeholder="Terme" />
                                    <div v-if="termError(t, 'term')" class="text-danger small mt-1">{{ termError(t, 'term') }}</div>
                                </div>
                                <button @click="removeNewTerm(i)" class="btn btn-outline-danger btn-sm">
                                    <i class="bi bi-x"></i>
                                </button>
                            </div>
                            <textarea v-model="t.definition" class="form-control form-control-sm mb-2" rows="2"
                                      :class="{'is-invalid': termError(t, 'def')}" placeholder="Définition"></textarea>
                            <div v-if="termError(t, 'def')" class="text-danger small mb-2">{{ termError(t, 'def') }}</div>
                            <div v-for="(a, ai) in t.aliases" :key="a._key" class="d-flex gap-1 mb-1">
                                <input v-model="a.alias" class="form-control form-control-sm" placeholder="Alias" />
                                <button @click="removeAlias(t, ai)" class="btn btn-outline-danger btn-sm"><i class="bi bi-x"></i></button>
                            </div>
                            <button @click="addAlias(t)" class="btn btn-outline-secondary btn-sm mt-1">
                                <i class="bi bi-plus"></i> Alias
                            </button>
                        </div>
                    </div>
                </div>
            </template>

            <!-- Liste des termes -->
            <div v-for="[letter, group] in grouped" :key="letter">
                <h4 class="letter-heading text-muted mt-4 mb-3 pb-1 border-bottom" :data-letter="letter">{{ letter }}</h4>

                <!-- Mode lecture -->
                <template v-if="!editMode">
                    <div v-for="term in group" :key="term.id" class="mb-4">
                        <div class="d-flex align-items-baseline gap-2 mb-1 flex-wrap">
                            <h5 class="mb-0">{{ term.term }}</h5>
                            <span v-for="alias in term.aliases" :key="alias.id"
                                  class="badge bg-light text-muted border small fw-normal">{{ alias.alias }}</span>
                        </div>
                        <p class="mb-0">{{ term.definition }}</p>
                    </div>
                </template>

                <!-- Mode édition -->
                <template v-else>
                    <div v-for="term in group" :key="term.id" class="card mb-3">
                        <div class="card-body">
                            <div class="d-flex gap-2 mb-2">
                                <div class="flex-grow-1">
                                    <input v-model="term.term" class="form-control form-control-sm"
                                           :class="{'is-invalid': termError(term, 'term')}" placeholder="Terme" />
                                    <div v-if="termError(term, 'term')" class="text-danger small mt-1">{{ termError(term, 'term') }}</div>
                                </div>
                                <button @click="deleteTerm(term.id)" class="btn btn-outline-danger btn-sm">
                                    <i class="bi bi-trash"></i>
                                </button>
                            </div>
                            <textarea v-model="term.definition" class="form-control form-control-sm mb-2" rows="2"
                                      :class="{'is-invalid': termError(term, 'def')}" placeholder="Définition"></textarea>
                            <div v-if="termError(term, 'def')" class="text-danger small mb-2">{{ termError(term, 'def') }}</div>
                            <div v-for="(a, ai) in term.aliases" :key="a.id ?? a._key" class="d-flex gap-1 mb-1">
                                <input v-model="a.alias" class="form-control form-control-sm" placeholder="Alias" />
                                <button @click="removeAlias(term, ai)" class="btn btn-outline-danger btn-sm"><i class="bi bi-x"></i></button>
                            </div>
                            <button @click="addAlias(term)" class="btn btn-outline-secondary btn-sm mt-1">
                                <i class="bi bi-plus"></i> Alias
                            </button>
                        </div>
                    </div>
                </template>
            </div>
        </div>
    `
}).mount('#app');
