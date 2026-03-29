let _keyCounter = 0;
export function nextTempKey() { return --_keyCounter; }

export function groupTermsByLetter(terms) {
    const map = {};
    for (const term of terms) {
        if (!term.term) continue;
        const letter = term.term[0].normalize('NFD').replace(/[\u0300-\u036f]/g, '').toUpperCase();
        if (!map[letter]) map[letter] = [];
        map[letter].push(term);
    }
    return Object.entries(map).sort(([a], [b]) => a.localeCompare(b));
}
