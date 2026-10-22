import { useEffect, useMemo, useState } from "react";
import { createCategory, deleteCategory, getCategories, renameCategory } from "../services/categoryService";

export default function CategoryManagementPage({ onBack }) {
    const [categories, setCategories] = useState([]);
    const [search, setSearch] = useState("");
    const [adding, setAdding] = useState(false);
    const [newName, setNewName] = useState("");
    const [editingId, setEditingId] = useState(null);
    const [editingName, setEditingName] = useState("");
    const [categoryToDelete, setCategoryToDelete] = useState(null);
    const [isLoading, setIsLoading] = useState(true);
    const [busy, setBusy] = useState(false);
    const [error, setError] = useState("");
    const [notice, setNotice] = useState("");

    useEffect(() => {
        let active = true;
        getCategories()
            .then(data => { if (active) setCategories(data); })
            .catch(loadError => { if (active) setError(loadError.message); })
            .finally(() => { if (active) setIsLoading(false); });
        return () => { active = false; };
    }, []);

    const visibleCategories = useMemo(() => {
        const term = search.trim().toLocaleLowerCase("es");
        return categories.filter(category => category.name.toLocaleLowerCase("es").includes(term));
    }, [categories, search]);

    async function handleAdd(event) {
        event.preventDefault();
        if (!newName.trim()) return;
        setBusy(true); setError(""); setNotice("");
        try {
            const created = await createCategory(newName.trim());
            setCategories(current => [...current, created].sort((a, b) => a.name.localeCompare(b.name, "es")));
            setNewName(""); setAdding(false); setNotice("La categoría se agregó correctamente.");
        } catch (requestError) { setError(requestError.message); }
        finally { setBusy(false); }
    }

    async function handleRename(event, id) {
        event.preventDefault();
        if (!editingName.trim()) return;
        setBusy(true); setError(""); setNotice("");
        try {
            const updated = await renameCategory(id, editingName.trim());
            setCategories(current => current.map(category => category.id === id ? updated : category)
                .sort((a, b) => a.name.localeCompare(b.name, "es")));
            setEditingId(null); setNotice("La categoría se modificó correctamente.");
        } catch (requestError) { setError(requestError.message); }
        finally { setBusy(false); }
    }

    async function confirmDelete() {
        if (!categoryToDelete) return;
        setBusy(true); setError(""); setNotice("");
        try {
            await deleteCategory(categoryToDelete.id);
            setCategories(current => current.filter(item => item.id !== categoryToDelete.id));
            setNotice("La categoría se eliminó.");
            setCategoryToDelete(null);
        } catch (requestError) { setError(requestError.message); }
        finally { setBusy(false); }
    }

    return (
        <main className="registration-page form-page">
            <button className="back-button" type="button" onClick={onBack}>← Volver</button>
            <section className="form-card category-management-page">
                <div className="form-header">
                    <span className="page-label">Administración</span>
                    <h1>Editar categorías</h1>
                    <p>Organizá las categorías que podrán asignarse a las actividades.</p>
                </div>
                <label className="category-search-label">
                    <span>Buscar categoría</span>
                    <input type="search" value={search} onChange={event => setSearch(event.target.value)}
                        placeholder="Escribí el nombre de una categoría" />
                </label>
                {adding ? (
                    <form className="category-add-row" onSubmit={handleAdd}>
                        <input autoFocus maxLength={60} value={newName} onChange={event => setNewName(event.target.value)}
                            placeholder="Nombre de la nueva categoría" aria-label="Nombre de la nueva categoría" required />
                        <button className="category-action primary" type="submit" disabled={busy}>{busy ? "Agregando…" : "Agregar"}</button>
                        <button className="category-cancel" type="button" onClick={() => { setAdding(false); setNewName(""); setError(""); }}>Cancelar</button>
                    </form>
                ) : (
                    <button className="category-add-trigger" type="button" onClick={() => { setAdding(true); setError(""); setNotice(""); }}>Agregar</button>
                )}
                {notice && <div className="form-message success" role="status">{notice}</div>}
                {error && <div className="form-message error" role="alert">{error}</div>}
                <section className="category-list-section">
                    <h2>Categorías existentes</h2>
                    {isLoading ? <p className="provider-empty-state">Cargando categorías...</p> : visibleCategories.length === 0 ? (
                        <p className="provider-empty-state">{categories.length === 0 ? "Todavía no hay categorías. Agregá la primera." : "No hay categorías que coincidan con la búsqueda."}</p>
                    ) : (
                        <div className="category-list">
                            {visibleCategories.map(category => (
                                <article className="category-card" key={category.id}>
                                    <div className="category-main">
                                        {editingId === category.id ? (
                                            <form className="category-edit-form" onSubmit={event => handleRename(event, category.id)}>
                                                <input autoFocus maxLength={60} value={editingName}
                                                    onChange={event => setEditingName(event.target.value)} aria-label="Modificar nombre de categoría" required />
                                                <button className="category-action primary" type="submit" disabled={busy}>Guardar</button>
                                                <button className="category-action secondary" type="button" onClick={() => setEditingId(null)}>Cancelar</button>
                                            </form>
                                        ) : <h3>{category.name}</h3>}
                                    </div>
                                    {editingId !== category.id && <div className="category-actions">
                                        <button className="category-action secondary" type="button" disabled={busy}
                                            onClick={() => { setEditingId(category.id); setEditingName(category.name); setError(""); setNotice(""); }}>Modificar</button>
                                        <button className="category-action danger" type="button" disabled={busy}
                                            onClick={() => { setCategoryToDelete(category); setError(""); setNotice(""); }}>Eliminar</button>
                                    </div>}
                                </article>
                            ))}
                        </div>
                    )}
                </section>
            </section>
            {categoryToDelete && <div className="category-dialog-backdrop" role="presentation"
                onMouseDown={event => { if (event.target === event.currentTarget && !busy) setCategoryToDelete(null); }}>
                <section className="category-confirm-dialog" role="alertdialog" aria-modal="true"
                    aria-labelledby="category-confirm-title" aria-describedby="category-confirm-description">
                    <span className="category-dialog-icon" aria-hidden="true">!</span>
                    <h2 id="category-confirm-title">¿Eliminar esta categoría?</h2>
                    <p id="category-confirm-description">Se va a eliminar <strong>{categoryToDelete.name}</strong> de la lista de categorías.</p>
                    <div className="category-dialog-actions">
                        <button className="category-action secondary" type="button" disabled={busy}
                            onClick={() => setCategoryToDelete(null)}>Cancelar</button>
                        <button className="category-action danger" type="button" disabled={busy}
                            onClick={confirmDelete}>{busy ? "Eliminando…" : "Sí, eliminar"}</button>
                    </div>
                </section>
            </div>}
        </main>
    );
}
