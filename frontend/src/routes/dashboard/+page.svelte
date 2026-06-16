<script lang="ts">
	import { enhance } from '$app/forms';
	import { page } from '$app/stores';
	import { goto } from '$app/navigation';
	import ListCard from '$lib/components/ListCard.svelte';
	import Modal from '$lib/components/Modal.svelte';
	import type { WishListSummary } from '$lib/types';

	type ListSortMode = 'alphabetical' | 'wish-count' | 'created-date' | 'modified-date';

	let { data, form } = $props();

	let lists = $derived(data.lists);
	
	let showCreateModal = $state(false);
	let createLoading = $state(false);

	let showEditModal = $state(false);
	let editingList: WishListSummary | null = $state(null);
	let editLoading = $state(false);

	let sortMode: ListSortMode = $derived(
		($page.url.searchParams.get('sort') as ListSortMode) || 'modified-date'
	);

	let sortedLists = $derived([...lists].sort((a, b) => compareLists(a, b, sortMode)));

	function handleSortChange(e: Event) {
		const target = e.target as HTMLSelectElement;
		const url = new URL($page.url);
		url.searchParams.set('sort', target.value);
		goto(url, { replaceState: true, keepFocus: true });
	}

	function openEditModal(list: WishListSummary) {
		editingList = list;
		showEditModal = true;
	}

	let deleteForm: HTMLFormElement;
	let deleteId = $state('');

	function handleDelete(id: string) {
		if (!confirm('Delete this list and all its wishes?')) return;
		deleteId = id;
		setTimeout(() => deleteForm.requestSubmit(), 0);
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
				<select id="sort-mode" value={sortMode} onchange={handleSortChange}>
					<option value="modified-date">Modified Date</option>
					<option value="created-date">Creation Date</option>
					<option value="wish-count"># Wishes</option>
					<option value="alphabetical">Alphabetical</option>
				</select>
			</div>
			<button class="btn-primary" onclick={() => (showCreateModal = true)}>+ New List</button>
		</div>
	</div>

	{#if lists.length === 0}
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

<form method="POST" action="?/delete" use:enhance bind:this={deleteForm} style="display:none">
	<input type="hidden" name="id" bind:value={deleteId} />
</form>

<Modal title="New Omiyage" open={showCreateModal} onclose={() => (showCreateModal = false)}>
	<form method="POST" action="?/create" use:enhance={() => {
		createLoading = true;
		return async ({ result, update }) => {
			createLoading = false;
			if (result.type === 'success') showCreateModal = false;
			await update();
		};
	}}>
		<div class="form-group">
			<label for="list-title">Title *</label>
			<input
				id="list-title"
				name="title"
				value={form?.createError ? form.title : ''}
				placeholder="e.g. Birthday 2025"
				required
			/>
		</div>
		<div class="form-group">
			<label for="list-desc">Description</label>
			<input 
				id="list-desc" 
				name="description" 
				value={form?.createError ? form.description : ''} 
				placeholder="Optional description" 
			/>
		</div>
		{#if form?.createError}
			<p class="error-msg">{form.createError}</p>
		{/if}
		<div class="modal-actions">
			<button
				type="button"
				class="btn-ghost"
				onclick={() => { showCreateModal = false; }}
			>Cancel</button>
			<button type="submit" class="btn-primary" disabled={createLoading}>
				{createLoading ? 'Creating…' : 'Create'}
			</button>
		</div>
	</form>
</Modal>

<Modal title="Edit Omiyage" open={showEditModal} onclose={() => (showEditModal = false)}>
	<form method="POST" action="?/update" use:enhance={() => {
		editLoading = true;
		return async ({ result, update }) => {
			editLoading = false;
			if (result.type === 'success') showEditModal = false;
			await update();
		};
	}}>
		<input type="hidden" name="id" value={editingList?.id || ''} />
		<div class="form-group">
			<label for="edit-list-title">Title *</label>
			<input
				id="edit-list-title"
				name="title"
				value={form?.editError ? form.title : (editingList?.title || '')}
				placeholder="e.g. Birthday 2025"
				required
			/>
		</div>
		<div class="form-group">
			<label for="edit-list-desc">Description</label>
			<input 
				id="edit-list-desc" 
				name="description" 
				value={form?.editError ? form.description : (editingList?.description || '')} 
				placeholder="Optional description" 
			/>
		</div>
		{#if form?.editError}
			<p class="error-msg">{form.editError}</p>
		{/if}
		<div class="modal-actions">
			<button type="button" class="btn-ghost" onclick={() => (showEditModal = false)}>Cancel</button>
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
