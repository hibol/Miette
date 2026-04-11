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
