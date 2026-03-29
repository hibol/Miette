const { ref, computed, watch, nextTick } = Vue;
import { groupTermsByLetter } from './glossary-utils.js';

export function useGlossaryHighlight(editMode) {
    const glossaryEnabled = ref(false);
    const glossaryTerms = ref([]);
    const activeModalLetter = ref(null);
    let _tooltips = [];
    let _modalObserver = null;

    const glossaryGrouped = computed(() => groupTermsByLetter(glossaryTerms.value));
    const glossaryLetters = computed(() => glossaryGrouped.value.map(([l]) => l));

    watch(editMode, (val) => { if (val && glossaryEnabled.value) removeHighlight(); });

    async function toggleGlossary() {
        if (!glossaryEnabled.value && glossaryTerms.value.length === 0) {
            const res = await fetch('/api/glossary');
            if (res.ok) glossaryTerms.value = await res.json();
        }
        glossaryEnabled.value = !glossaryEnabled.value;
        await nextTick();
        if (glossaryEnabled.value) applyHighlight();
        else removeHighlight();
    }

    function buildGlossary() {
        const entries = [];
        for (const t of glossaryTerms.value) {
            entries.push({ pattern: t.term, definition: t.definition });
            for (const a of t.aliases) {
                if (a.alias?.trim()) entries.push({ pattern: a.alias, definition: t.definition });
            }
        }
        entries.sort((a, b) => b.pattern.length - a.pattern.length);
        const lookup = Object.fromEntries(entries.map(e => [e.pattern.toLowerCase(), e.definition]));
        const escaped = entries.map(e => e.pattern.replace(/[.*+?^${}()|[\]\\]/g, '\\$&'));
        const regex = new RegExp(`(?<![a-zA-ZÀ-ÿ])(${escaped.join('|')})(?![a-zA-ZÀ-ÿ])`, 'gi');
        return { regex, lookup };
    }

    function applyHighlight() {
        const container = document.getElementById('phases-content');
        if (!container) return;
        const { regex, lookup } = buildGlossary();
        walkNodes(container, regex, lookup);
        _tooltips = [...document.querySelectorAll('.glossary-term')].map(el =>
            new bootstrap.Tooltip(el, { trigger: 'hover focus click' })
        );
    }

    function walkNodes(node, regex, lookup) {
        if (node.nodeType === Node.TEXT_NODE) {
            const text = node.textContent;
            regex.lastIndex = 0;
            if (!regex.test(text)) return;
            regex.lastIndex = 0;
            const fragment = document.createDocumentFragment();
            let last = 0, match;
            while ((match = regex.exec(text)) !== null) {
                if (match.index > last) fragment.appendChild(document.createTextNode(text.slice(last, match.index)));
                const span = document.createElement('span');
                span.className = 'glossary-term';
                span.textContent = match[0];
                span.title = lookup[match[0].toLowerCase()] || '';
                span.dataset.bsToggle = 'tooltip';
                fragment.appendChild(span);
                last = match.index + match[0].length;
            }
            if (last < text.length) fragment.appendChild(document.createTextNode(text.slice(last)));
            node.parentNode.replaceChild(fragment, node);
        } else if (node.nodeType === Node.ELEMENT_NODE && !['INPUT', 'TEXTAREA', 'SCRIPT', 'STYLE'].includes(node.tagName)) {
            [...node.childNodes].forEach(c => walkNodes(c, regex, lookup));
        }
    }

    function removeHighlight() {
        _tooltips.forEach(t => t.dispose());
        _tooltips = [];
        document.querySelectorAll('.glossary-term').forEach(span =>
            span.replaceWith(document.createTextNode(span.textContent))
        );
        document.getElementById('phases-content')?.normalize();
    }

    function scrollToInModal(letter) {
        const el = document.querySelector(`#glossaryModal .modal-letter[data-letter="${letter}"]`);
        const body = document.querySelector('#glossaryModal .modal-body');
        if (el && body) body.scrollTo({ top: el.offsetTop - body.offsetTop - 8, behavior: 'smooth' });
    }

    function setupModalObserver() {
        if (_modalObserver) _modalObserver.disconnect();
        const modalBody = document.querySelector('#glossaryModal .modal-body');
        if (!modalBody) return;
        _modalObserver = new IntersectionObserver(entries => {
            for (const entry of entries) {
                if (entry.isIntersecting) activeModalLetter.value = entry.target.dataset.letter;
            }
        }, { root: modalBody, rootMargin: '-10% 0px -60% 0px' });
        document.querySelectorAll('#glossaryModal .modal-letter').forEach(el => _modalObserver.observe(el));
    }

    document.addEventListener('shown.bs.modal', e => {
        if (e.target.id === 'glossaryModal') setupModalObserver();
    });
    document.addEventListener('hidden.bs.modal', e => {
        if (e.target.id === 'glossaryModal') {
            if (_modalObserver) _modalObserver.disconnect();
            activeModalLetter.value = null;
        }
    });

    return {
        glossaryEnabled, glossaryTerms, glossaryGrouped, glossaryLetters,
        activeModalLetter, toggleGlossary, scrollToInModal
    };
}
