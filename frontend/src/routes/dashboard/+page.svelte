<script lang="ts">
	import { onMount } from 'svelte';
	import { goto } from '$app/navigation';
	import { Pencil, X } from 'lucide-svelte';
	import { getLists, createList, updateList, deleteList } from '$lib/api';
	import { userStore } from '$lib/stores';
	import ListCard from '$lib/components/ListCard.svelte';
	import Modal from '$lib/components/Modal.svelte';
	import type { WishList } from '$lib/types';
	import { base } from '$app/paths';

	type ListSortMode = 'alphabetical' | 'wish-count' | 'created-date' | 'modified-date';

	let lists: WishList[] = [];
	let loading = true;
	let error = '';
	let showCreateModal = false;
	let createTitle = '';
	let createDesc = '';
	let createLoading = false;
	let createError = '';
	let createTitleTouched = false;
	let showEditModal = false;
	let editingList: WishList | null = null;
	let editTitle = '';
	let editDesc = '';
	let editLoading = false;
	let editError = '';
	let editTitleTouched = false;
	let sortMode: ListSortMode = 'modified-date';

	$: createTitleError = createTitleTouched && !createTitle.trim() ? 'Title is required' : '';
	$: editTitleError = editTitleTouched && !editTitle.trim() ? 'Title is required' : '';
	$: sortedLists = [...lists].sort((a, b) => compareLists(a, b, sortMode));

	onMount(async () => {
		if (!$userStore) {
			goto(`${base}/login`);
			return;
		}
		await fetchLists();
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
			lists = [newList, ...lists];
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

	function openEditModal(list: WishList) {
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
			lists = lists.map((list) => (list.id === updated.id ? { ...list, ...updated } : list));
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

	function compareLists(a: WishList, b: WishList, mode: ListSortMode): number {
		if (mode === 'alphabetical') {
			const byTitle = a.title.localeCompare(b.title, 'en', { sensitivity: 'base' });
			return byTitle !== 0 ? byTitle : compareByNewestDate(a, b);
		}

		if (mode === 'wish-count') {
			const byWishCount = (b.wishCount ?? 0) - (a.wishCount ?? 0);
			return byWishCount !== 0 ? byWishCount : compareByNewestDate(a, b);
		}

		if (mode === 'created-date') {
			const byCreatedAt = toTimestamp(b.createdAt) - toTimestamp(a.createdAt);
			return byCreatedAt !== 0 ? byCreatedAt : a.title.localeCompare(b.title, 'en');
		}

		return compareByNewestDate(a, b);
	}

	function compareByNewestDate(a: WishList, b: WishList): number {
		const byModifiedAt =
			toTimestamp(b.updatedAt || b.createdAt) - toTimestamp(a.updatedAt || a.createdAt);
		return byModifiedAt !== 0 ? byModifiedAt : a.title.localeCompare(b.title, 'en');
	}

	function toTimestamp(value: string): number {
		return Number.isNaN(Date.parse(value)) ? 0 : Date.parse(value);
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
			<button class="btn-primary" on:click={() => (showCreateModal = true)}>+ New List</button>
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
			<button class="btn-primary" style="margin-top:1rem" on:click={() => (showCreateModal = true)}>
				+ New List
			</button>
		</div>
	{:else}
		<div class="list-grid">
			{#each sortedLists as list (list.id)}
				<div class="list-item">
					<ListCard {list} />
					<div class="list-actions">
						<button
							class="btn-ghost list-action-btn"
							on:click={() => openEditModal(list)}
							title="Edit list"
					><Pencil class="icon" size={18} /></button>
					<button
						class="btn-ghost list-action-btn"
						on:click={() => handleDelete(list.id)}
						title="Delete list"
					><X class="icon danger" size={18} /></button>
					</div>
				</div>
			{/each}
		</div>
	{/if}
</main>

<Modal title="New Omiyage" open={showCreateModal} on:close={() => (showCreateModal = false)}>
	<form on:submit|preventDefault={handleCreate}>
		<div class="form-group">
			<label for="list-title">Title *</label>
			<input
				id="list-title"
				bind:value={createTitle}
				on:input={() => (createTitleTouched = true)}
				on:blur={() => (createTitleTouched = true)}
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
				on:click={() => {
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

<Modal title="Edit Omiyage" open={showEditModal} on:close={closeEditModal}>
	<form on:submit|preventDefault={handleUpdate}>
		<div class="form-group">
			<label for="edit-list-title">Title *</label>
			<input
				id="edit-list-title"
				bind:value={editTitle}
				on:input={() => (editTitleTouched = true)}
				on:blur={() => (editTitleTouched = true)}
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
			<button type="button" class="btn-ghost" on:click={closeEditModal}>Cancel</button>
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

	.list-item > :global(a) {
		flex: 1;
		min-width: 0;
	}

	.list-actions {
		display: flex;
		gap: 0.35rem;
		align-self: center;
	}

	.list-action-btn {
		flex-shrink: 0;
		align-self: center;
		display: inline-flex;
		align-items: center;
		justify-content: center;
		width: 2.25rem;
		height: 2.25rem;
		padding: 0;
	}

	.list-action-btn :global(.icon) {
		stroke-width: 2;
	}

	.list-action-btn :global(.icon.danger) {
		color: var(--error);
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
		}

		.list-item > :global(a) {
			flex: unset;
		}

		.list-actions {
			align-self: flex-end;
		}

		.list-action-btn {
			align-self: flex-end;
		}
	}
</style>
