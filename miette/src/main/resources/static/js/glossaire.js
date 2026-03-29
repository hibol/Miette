const { createApp, ref, computed, onMounted } = Vue;

createApp({
    setup() {
        const terms = ref([]);
        const loading = ref(true);
        const error = ref(null);
        const isAdmin = ref(false);
        const activeLetter = ref(null);

        const grouped = computed(() => {
            const map = {};
            for (const term of terms.value) {
                const letter = term.term[0].normalize('NFD').replace(/[\u0300-\u036f]/g, '').toUpperCase();
                if (!map[letter]) map[letter] = [];
                map[letter].push(term);
            }
            return Object.entries(map).sort(([a], [b]) => a.localeCompare(b));
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
                loading.value = false;
            }

            // IntersectionObserver après rendu
            setTimeout(() => {
                const observer = new IntersectionObserver(entries => {
                    for (const entry of entries) {
                        if (entry.isIntersecting) {
                            activeLetter.value = entry.target.dataset.letter;
                        }
                    }
                }, { rootMargin: '-20% 0px -70% 0px' });

                document.querySelectorAll('.letter-heading').forEach(el => observer.observe(el));
            }, 100);
        });

        function scrollTo(letter) {
            const el = document.querySelector(`.letter-heading[data-letter="${letter}"]`);
            if (el) el.scrollIntoView({ behavior: 'smooth', block: 'start' });
        }

        return { terms, grouped, letters, loading, error, isAdmin, activeLetter, scrollTo };
    },

    template: `
        <div v-if="loading">Chargement...</div>
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

            <h1 class="mb-4">Ça veut dire quoi ?</h1>

            <!-- Index horizontal mobile -->
            <div class="d-flex flex-wrap gap-2 mb-4 d-md-none">
                <span v-for="letter in letters" :key="letter"
                      @click="scrollTo(letter)"
                      :class="['alphabet-letter-mobile', { active: letter === activeLetter }]">
                    {{ letter }}
                </span>
            </div>

            <div v-for="[letter, group] in grouped" :key="letter">
                <h4 class="letter-heading text-muted mt-4 mb-3 pb-1 border-bottom"
                    :data-letter="letter">{{ letter }}</h4>
                <div v-for="term in group" :key="term.id" class="mb-4">
                    <div class="d-flex align-items-baseline gap-2 mb-1 flex-wrap">
                        <h5 class="mb-0">{{ term.term }}</h5>
                        <span v-for="alias in term.aliases" :key="alias.id"
                              class="badge bg-light text-muted border small fw-normal">{{ alias.alias }}</span>
                    </div>
                    <p class="mb-0">{{ term.definition }}</p>
                </div>
            </div>
        </div>
    `
}).mount('#app');
