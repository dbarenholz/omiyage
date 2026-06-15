<script lang="ts">
	import { onMount } from 'svelte';
	import { goto } from '$app/navigation';
	import { Pencil, X } from 'lucide-svelte';
	import { getLists, createList, updateList, deleteList } from '$lib/api';
	import { userStore } from '$lib/stores';
	import ListCard from '$lib/components/ListCard.svelte';
	import Modal from '$lib/components/Modal.svelte';
	import type { WishList, WishListSummary } from '$lib/types';
	import { resolve } from '$app/paths';
	import { browser } from '$app/environment';

	type ListSortMode = 'alphabetical' | 'wish-count' | 'created-date' | 'modified-date';

	let lists: WishListSummary[] = $state([]);
	let loading = $state(true);
	let error = $state('');
	let showCreateModal = $state(false);
	let createTitle = $state('');
	let createDesc = $state('');
	let createLoading = $state(false);
	let createError = $state('');
	let createTitleTouched = $state(false);
	let showEditModal = $state(false);
	let editingList: WishListSummary | null = $state(null);
	let editTitle = $state('');
	let editDesc = $state('');
	let editLoading = $state(false);
	let editError = $state('');
	let editTitleTouched = $state(false);
	let sortMode: ListSortMode = $state((browser && localStorage.getItem('omiyage-sort-mode') as ListSortMode) || 'modified-date');

	let createTitleError = $derived(createTitleTouched && !createTitle.trim() ? 'Title is required' : '');
	let editTitleError = $derived(editTitleTouched && !editTitle.trim() ? 'Title is required' : '');
	let sortedLists = $derived([...lists].sort((a, b) => compareLists(a, b, sortMode)));

	onMount(async () => {
		if (!$userStore) {
			goto(resolve('/login'));
			return;
		}
		
		await fetchLists();
	});

	$effect(() => {
		if (browser) {
			localStorage.setItem('omiyage-sort-mode', sortMode);
		}
	});

	async function fetchLists() {
		loading = true;
		error = '';
		try {
			lists = await getLists();
		} catch (e: unknown) {
			error = e instanceof Error ? e.message : 'Failed to load lists';
		} finally {
			loading = false;
		}
	}

	async function handleCreate() {
		createError = '';
		createTitleTouched = true;
		if (createTitleError) {
			createError = 'Title is required';
			return;
		}
		createLoading = true;
		try {
			const newList = await createList(createTitle.trim(), createDesc.trim() || undefined);
			lists = [{ ...newList, wishCount: 0, updatedAt: newList.createdAt }, ...lists];
			showCreateModal = false;
			createTitle = '';
			createDesc = '';
			createTitleTouched = false;
		} catch (e: unknown) {
			createError = e instanceof Error ? e.message : 'Failed to create list';
		} finally {
			createLoading = false;
		}
	}

	function openEditModal(list: WishListSummary) {
		editingList = list;
		editTitle = list.title;
		editDesc = list.description || '';
		editError = '';
		editTitleTouched = false;
		showEditModal = true;
	}

	function closeEditModal() {
		showEditModal = false;
		editingList = null;
		editTitle = '';
		editDesc = '';
		editError = '';
		editTitleTouched = false;
	}

	async function handleUpdate() {
		if (!editingList) return;
		editError = '';
		editTitleTouched = true;

		if (editTitleError) {
			editError = 'Title is required';
			return;
		}

		editLoading = true;
		try {
			const updated = await updateList(editingList.id, {
				title: editTitle.trim(),
				description: editDesc.trim() || undefined
			});
			lists = lists.map((list) => (list.id === updated.id ? { ...list, title: updated.title, description: updated.description } : list));
			closeEditModal();
		} catch (e: unknown) {
			editError = e instanceof Error ? e.message : 'Failed to update list';
		} finally {
			editLoading = false;
		}
	}

	async function handleDelete(listId: string) {
		if (!confirm('Delete this list and all its wishes?')) return;
		try {
			await deleteList(listId);
			lists = lists.filter((l) => l.id !== listId);
		} catch (e: unknown) {
			alert(e instanceof Error ? e.message : 'Failed to delete');
		}
	}

	function compareLists(a: WishListSummary, b: WishListSummary, mode: ListSortMode): number {
		if (mode === 'alphabetical') {
			const byTitle = a.title.localeCompare(b.title, 'en', { sensitivity: 'base' });
			return byTitle !== 0 ? byTitle : compareByNewestDate(a, b);
		}

		if (mode === 'wish-count') {
			const byWishCount = (b.wishCount ?? 0) - (a.wishCount ?? 0);
			return byWishCount !== 0 ? byWishCount : compareByNewestDate(a, b);
		}

		if (mode === 'created-date') {
			const byCreatedAt = a.createdAt.getTime() - b.createdAt.getTime();
			return byCreatedAt !== 0 ? byCreatedAt : a.title.localeCompare(b.title, 'en');
		}

		return compareByNewestDate(a, b);
	}

	function compareByNewestDate(a: WishListSummary, b: WishListSummary): number {
		const byModifiedAt =
			(b.updatedAt || b.createdAt).getTime() - (a.updatedAt || a.createdAt).getTime();
		return byModifiedAt !== 0 ? byModifiedAt : a.title.localeCompare(b.title, 'en');
	}
</script>

<svelte:head><title>Dashboard — Omiyage</title></svelte:head>

<main class="container page">
	<div class="page-header">
		<h1>My Wishlists</h1>
		<div class="header-controls">
			<div class="sort-control">
				<label for="sort-mode">Sort</label>
				<select id="sort-mode" bind:value={sortMode}>
					<option value="modified-date">Modified Date</option>
					<option value="created-date">Creation Date</option>
					<option value="wish-count"># Wishes</option>
					<option value="alphabetical">Alphabetical</option>
				</select>
			</div>
			<button class="btn-primary" onclick={() => (showCreateModal = true)}>+ New List</button>
		</div>
	</div>

	{#if loading}
		<div class="loading">Loading your lists…</div>
	{:else if error}
		<p class="error-msg">{error}</p>
	{:else if lists.length === 0}
		<div class="empty-state">
			<h3>No wishlists yet</h3>
			<p>Create your first list to get started!</p>
			<button class="btn-primary" style="margin-top:1rem" onclick={() => (showCreateModal = true)}>
				+ New List
			</button>
		</div>
	{:else}
		<div class="list-grid">
			{#each sortedLists as list (list.id)}
				<div class="list-item">
					<ListCard 
						{list} 
						onedit={(l) => openEditModal(l)}
						ondelete={(id) => handleDelete(id)}
					/>
				</div>
			{/each}
		</div>
	{/if}
</main>

<Modal title="New Omiyage" open={showCreateModal} onclose={() => (showCreateModal = false)}>
	<form onsubmit={(e) => { e.preventDefault(); handleCreate(); }}>
		<div class="form-group">
			<label for="list-title">Title *</label>
			<input
				id="list-title"
				bind:value={createTitle}
				oninput={() => (createTitleTouched = true)}
				onblur={() => (createTitleTouched = true)}
				class:field-invalid={!!createTitleError}
				class:field-valid={createTitleTouched && !createTitleError}
				placeholder="e.g. Birthday 2025"
				required
			/>
		</div>
		<div class="form-group">
			<label for="list-desc">Description</label>
			<input id="list-desc" bind:value={createDesc} placeholder="Optional description" />
		</div>
		{#if createError}
			<p class="error-msg">{createError}</p>
		{/if}
		<div class="modal-actions">
			<button
				type="button"
				class="btn-ghost"
				onclick={() => {
					showCreateModal = false;
					createTitleTouched = false;
					createError = '';
				}}
			>Cancel</button>
			<button type="submit" class="btn-primary" disabled={createLoading}>
				{createLoading ? 'Creating…' : 'Create'}
			</button>
		</div>
	</form>
</Modal>

<Modal title="Edit Omiyage" open={showEditModal} onclose={closeEditModal}>
	<form onsubmit={(e) => { e.preventDefault(); handleUpdate(); }}>
		<div class="form-group">
			<label for="edit-list-title">Title *</label>
			<input
				id="edit-list-title"
				bind:value={editTitle}
				oninput={() => (editTitleTouched = true)}
				onblur={() => (editTitleTouched = true)}
				class:field-invalid={!!editTitleError}
				class:field-valid={editTitleTouched && !editTitleError}
				placeholder="e.g. Birthday 2025"
				required
			/>
		</div>
		<div class="form-group">
			<label for="edit-list-desc">Description</label>
			<input id="edit-list-desc" bind:value={editDesc} placeholder="Optional description" />
		</div>
		{#if editError}
			<p class="error-msg">{editError}</p>
		{/if}
		<div class="modal-actions">
			<button type="button" class="btn-ghost" onclick={closeEditModal}>Cancel</button>
			<button type="submit" class="btn-primary" disabled={editLoading}>
				{editLoading ? 'Saving…' : 'Save'}
			</button>
		</div>
	</form>
</Modal>

<style lang="scss">
	.page {
		padding-top: 1.5rem;
		padding-bottom: 2rem;
	}

	.page-header {
		display: flex;
		align-items: center;
		justify-content: space-between;
		margin-bottom: 1.5rem;
		gap: 0.75rem;
		flex-wrap: wrap;
	}

	.header-controls {
		display: flex;
		align-items: center;
		gap: 0.6rem;
		flex-wrap: wrap;
	}

	.sort-control {
		display: flex;
		align-items: center;
		gap: 0.4rem;

		label {
			margin: 0;
			font-size: 0.78rem;
		}

		select {
			min-width: 10rem;
			width: auto;
		}
	}

	.page-header h1 {
		font-size: 1.4rem;
	}

	.list-grid {
		display: flex;
		flex-direction: column;
		gap: 0.75rem;
	}

	.list-item {
		display: flex;
		align-items: stretch;
		gap: 0.5rem;
	}

	.list-item > :global(.list-card) {
		flex: 1;
		min-width: 0;
	}

	.modal-actions {
		display: flex;
		justify-content: flex-end;
		gap: 0.75rem;
		margin-top: 1rem;
	}

	@media (min-width: 600px) {
		.list-grid {
			display: grid;
			grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
			gap: 1rem;
		}

		.list-item {
			flex-direction: column;
			height: 100%;
		}

		.list-item > :global(.list-card) {
			flex: 1;
		}
	}
</style>
