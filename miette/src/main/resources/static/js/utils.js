let _keyCounter = 0;
export function nextTempKey() { return --_keyCounter; }

export function capitalize(str) {
    if (!str) return str;
    return str.charAt(0).toUpperCase() + str.slice(1);
}

export function formatDate(isoString) {
    if (!isoString) return '';
    return new Date(isoString).toLocaleDateString('fr-FR', { dateStyle: 'medium' });
}

export function formatQuantity(quantity) {
    if (quantity === null || quantity === undefined) return '';
    return quantity % 1 === 0 ? quantity.toFixed(0) : quantity.toFixed(1);
}

// --- Caractéristiques boulangerie ---

function allIngredients(recipe) {
    return recipe.phases.flatMap(p => p.ingredients);
}

// Retourne true si la recette contient au moins un ingrédient hors liste des standards.
// keywords : tableau de mots-clés (ex: ["farine", "eau", "sel", ...])
export function detectAjouts(recipe, keywords) {
    return allIngredients(recipe).some(ing => {
        const label = ing.label.toLowerCase();
        return !keywords.some(kw => label.includes(kw.trim().toLowerCase()));
    });
}

// Retourne l'hydratation en %, ou null si aucune farine détectée.
// Levain compté à 100% d'hydratation (50% eau / 50% farine).
// Lait et eau comptent comme liquides.
export function calcHydration(recipe) {
    let water = 0, flour = 0;
    for (const ing of allIngredients(recipe)) {
        if (!ing.quantity) continue;
        const label = ing.label.toLowerCase();
        if (label.includes('levain')) {
            water += ing.quantity * 0.5;
            flour += ing.quantity * 0.5;
        } else if (label.includes('farine')) {
            flour += ing.quantity;
        } else if (label.includes('eau') || label.includes('lait')) {
            water += ing.quantity;
        }
    }
    if (flour === 0) return null;
    return Math.round(water / flour * 100);
}
