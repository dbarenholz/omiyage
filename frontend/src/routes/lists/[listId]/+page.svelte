<script lang="ts">
	import { onMount } from 'svelte';
	import { page } from '$app/stores';
	import { goto } from '$app/navigation';
	import { getWishes, createWish, updateWish, deleteWish, getLists, updateList } from '$lib/api';
	import { userStore } from '$lib/stores';
	import WishCard from '$lib/components/WishCard.svelte';
	import WishForm from '$lib/components/WishForm.svelte';
	import Modal from '$lib/components/Modal.svelte';
	import type { Wish, WishListSummary, CreateWishData } from '$lib/types';
	import { resolve } from '$app/paths';

	let wishList: WishListSummary | null = $state(null);
	let wishes: Wish[] = $state([]);
	let loading = $state(true);
	let error = $state('');
	let showAddModal = $state(false);
	let editingWish: Wish | null = $state(null);
	let formLoading = $state(false);
	let shareCopied = $state(false);
	let showEditListModal = $state(false);
	let editListTitle = $state('');
	let editListDescription = $state('');
	let editListLoading = $state(false);
	let editListError = $state('');
	let editListTitleTouched = $state(false);

	let editListTitleError = $derived(editListTitleTouched && !editListTitle.trim() ? 'Title is required' : '');

	let listId = $derived($page.params.listId as string);

	onMount(async () => {
		if (!$userStore) {
			goto(resolve('/login'));
			return;
		}
		await fetchData();
	});

	async function fetchData() {
		loading = true;
		error = '';
		try {
			const [allLists, allWishes] = await Promise.all([getLists(), getWishes(listId)]);
			wishList = allLists.find((l) => l.id === listId) || null;
			wishes = allWishes;
			if (!wishList) error = 'List not found';
		} catch (e: unknown) {
			error = e instanceof Error ? e.message : 'Failed to load';
		} finally {
			loading = false;
		}
	}

	async function handleAddWish(data: CreateWishData) {
		formLoading = true;
		try {
			const w = await createWish(listId, data);
			wishes = [...wishes, w];
			showAddModal = false;
		} catch (err: unknown) {
			alert(err instanceof Error ? err.message : 'Failed to add wish');
		} finally {
			formLoading = false;
		}
	}

	async function handleEditWish(data: CreateWishData) {
		if (!editingWish) return;
		formLoading = true;
		try {
			const updated = await updateWish(listId, editingWish.id, data);
			wishes = wishes.map((w) => (w.id === updated.id ? updated : w));
			editingWish = null;
		} catch (err: unknown) {
			alert(err instanceof Error ? err.message : 'Failed to update wish');
		} finally {
			formLoading = false;
		}
	}

	async function handleDeleteWish(id: string) {
		if (!confirm('Delete this wish?')) return;
		try {
			await deleteWish(listId, id);
			wishes = wishes.filter((w) => w.id !== id);
		} catch (err: unknown) {
			alert(err instanceof Error ? err.message : 'Failed to delete');
		}
	}

	async function handleShare() {
		if (!wishList) return;
		const url = `${window.location.origin}${resolve(`/shared/${wishList.shareId}`)}`;
		await navigator.clipboard.writeText(url);
		shareCopied = true;
		setTimeout(() => (shareCopied = false), 2000);
	}

	function openEditListModal() {
		if (!wishList) return;
		editListTitle = wishList.title;
		editListDescription = wishList.description || '';
		editListError = '';
		editListTitleTouched = false;
		showEditListModal = true;
	}

	function closeEditListModal() {
		showEditListModal = false;
		editListError = '';
		editListTitleTouched = false;
	}

	async function handleEditListSave() {
		if (!wishList) return;
		editListTitleTouched = true;
		editListError = '';

		if (editListTitleError) {
			editListError = editListTitleError;
			return;
		}

		editListLoading = true;
		try {
			const updated = await updateList(wishList.id, {
				title: editListTitle.trim(),
				description: editListDescription.trim() || undefined
			});
			wishList = { ...wishList, ...updated };
			closeEditListModal();
		} catch (e: unknown) {
			editListError = e instanceof Error ? e.message : 'Failed to update list';
		} finally {
			editListLoading = false;
		}
	}
</script>

<svelte:head><title>{wishList?.title || 'List'} — Omiyage</title></svelte:head>

<main class="container page">
	{#if loading}
		<div class="loading">Loading…</div>
	{:else if error}
		<p class="error-msg">{error}</p>
	{:else if wishList}
		<div class="page-header">
			<div class="header-left">
				<a href={resolve('/dashboard')} class="back-link">← My Lists</a>
				<h1>{wishList.title}</h1>
				{#if wishList.description}
					<p class="list-desc">{wishList.description}</p>
				{/if}
			</div>
			<div class="header-actions">
				<button class="btn-ghost header-action-btn" onclick={openEditListModal}>Edit</button>
				<button class="btn-ghost header-action-btn" onclick={handleShare}>
					{shareCopied ? '✓ Copied!' : '🔗 Share'}
				</button>
				<button class="btn-primary header-action-btn" onclick={() => (showAddModal = true)}>
					+ Add Wish
				</button>
			</div>
		</div>

		{#if wishes.length === 0}
			<div class="empty-state">
				<h3>No wishes yet</h3>
				<p>Add your first wish to this list!</p>
				<button class="btn-primary" style="margin-top:1rem" onclick={() => (showAddModal = true)}>
					+ Add Wish
				</button>
			</div>
		{:else}
			<div class="wish-list">
				{#each wishes as wish (wish.id)}
					<WishCard
						{wish}
						isOwner={true}
						canClaim={false}
						onedit={(w) => (editingWish = w)}
						ondelete={handleDeleteWish}
					/>
				{/each}
			</div>
		{/if}
	{/if}
</main>

<Modal title="Add Wish" open={showAddModal} onclose={() => (showAddModal = false)}>
	<WishForm
		loading={formLoading}
		onsubmit={handleAddWish}
		oncancel={() => (showAddModal = false)}
	/>
</Modal>

<Modal title="Edit Wish" open={!!editingWish} onclose={() => (editingWish = null)}>
	{#if editingWish}
		<WishForm
			initial={{
				title: editingWish.title,
				description: editingWish.description || undefined,
				approximatePrice: editingWish.approximatePrice || undefined,
				currencyCode: editingWish.currencyCode || undefined,
				imageUrl: editingWish.imageUrl || undefined,
				tags: editingWish.tags,
				links: editingWish.links.map(l => ({ url: l.url, label: l.label || null }))
			}}
			loading={formLoading}
			onsubmit={handleEditWish}
			oncancel={() => (editingWish = null)}
		/>
	{/if}
</Modal>

<Modal title="Edit List" open={showEditListModal} onclose={closeEditListModal}>
	<form onsubmit={(e) => { e.preventDefault(); handleEditListSave(); }}>
		<div class="form-group">
			<label for="edit-list-title">Title *</label>
			<input
				id="edit-list-title"
				bind:value={editListTitle}
				oninput={() => (editListTitleTouched = true)}
				onblur={() => (editListTitleTouched = true)}
				class:field-invalid={!!editListTitleError}
				class:field-valid={editListTitleTouched && !editListTitleError}
				required
			/>
		</div>
		<div class="form-group">
			<label for="edit-list-description">Description</label>
			<input id="edit-list-description" bind:value={editListDescription} />
		</div>
		{#if editListError}
			<p class="error-msg">{editListError}</p>
		{/if}
		<div class="modal-actions">
			<button type="button" class="btn-ghost" onclick={closeEditListModal}>Cancel</button>
			<button type="submit" class="btn-primary" disabled={editListLoading}>
				{editListLoading ? 'Saving…' : 'Save'}
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
		align-items: flex-start;
		justify-content: space-between;
		gap: 1rem;
		margin-bottom: 1.5rem;
		flex-wrap: wrap;
	}

	.back-link {
		display: block;
		font-size: 0.85rem;
		color: var(--text);
		margin-bottom: 0.25rem;

		&:hover {
			color: var(--text-bright);
			text-decoration: none;
		}
	}

	h1 {
		font-size: 1.4rem;
		margin-bottom: 0.25rem;
	}

	.list-desc {
		color: var(--text);
		font-size: 0.9rem;
	}

	.header-actions {
		display: flex;
		gap: 0.5rem;
		flex-shrink: 0;
		align-items: flex-start;
	}

	.header-action-btn {
		min-height: 2.35rem;
		display: inline-flex;
		align-items: center;
		justify-content: center;
	}

	.wish-list {
		display: flex;
		flex-direction: column;
		gap: 0.75rem;
	}

	.modal-actions {
		display: flex;
		justify-content: flex-end;
		gap: 0.75rem;
		margin-top: 1rem;
	}
</style>
