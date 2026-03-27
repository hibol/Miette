const { ref } = Vue;

export function useValidation(draft) {
    const errors = ref({});

    function validateDraft() {
        const errs = {};
        if (!draft.value.title?.trim()) {
            errs['title'] = 'Le titre est obligatoire.';
        }
        draft.value.phases.forEach((phase, pi) => {
            if (draft.value.phases.length > 1 && !phase.label?.trim()) {
                errs[`phases[${pi}].label`] = 'Le nom de la phase est obligatoire.';
            }
            phase.ingredients.forEach((ing, ii) => {
                const hasContent = ing.quantity || ing.unit?.trim();
                if (hasContent && !ing.label?.trim()) {
                    errs[`phases[${pi}].ingredients[${ii}].label`] = "Le nom de l'ingrédient est obligatoire.";
                }
                if (ing.label?.trim() && !ing.quantity) {
                    errs[`phases[${pi}].ingredients[${ii}].quantity`] = 'La quantité est obligatoire.';
                }
            });
        });
        errors.value = errs;
        return Object.keys(errs).length === 0;
    }

    function getError(key) {
        return errors.value[key] ?? null;
    }

    function clearErrors() {
        errors.value = {};
    }

    return { errors, validateDraft, getError, clearErrors };
}
