<script lang="ts">
	import { enhance } from "$app/forms";
	import { resolve } from "$app/paths";
	import { page } from "$app/stores";
	import { goto } from "$app/navigation";
	import WishCard from "$lib/components/WishCard.svelte";
	import WishForm from "$lib/components/WishForm.svelte";
	import Modal from "$lib/components/Modal.svelte";
	import type { Wish } from "$lib/types";

	let { data, form } = $props();

	let wishList = $derived(data.wishList);
	let wishes = $derived(data.wishes);

	type WishSortMode = 'alphabetical' | 'modified-date' | 'created-date' | 'price';

	let sortMode: WishSortMode = $derived(
		($page.url.searchParams.get('sort') as WishSortMode) || 'created-date'
	);

	let searchQuery = $derived($page.url.searchParams.get('q') || '');
	let selectedTags = $derived(
		$page.url.searchParams.get('tags') ? $page.url.searchParams.get('tags')!.split(',').filter(Boolean) : []
	);
	let tagMatchMode = $derived(
		$page.url.searchParams.get('match') === 'any' ? 'any' : 'all'
	);

	let availableTags = $derived(
		Array.from(new Set(wishes.flatMap(w => w.tags || []))).sort((a, b) => a.localeCompare(b, 'en', { sensitivity: 'base' }))
	);

	function compareWishes(a: Wish, b: Wish, mode: WishSortMode): number {
		if (mode === 'alphabetical') {
			const byTitle = a.title.localeCompare(b.title, 'en', { sensitivity: 'base' });
			return byTitle !== 0 ? byTitle : (a.createdAt.getTime() - b.createdAt.getTime());
		}
		if (mode === 'modified-date') {
			const byModifiedAt = b.updatedAt.getTime() - a.updatedAt.getTime();
			return byModifiedAt !== 0 ? byModifiedAt : (a.createdAt.getTime() - b.createdAt.getTime());
		}
		if (mode === 'price') {
			if (a.approximatePrice === null && b.approximatePrice !== null) return -1;
			if (b.approximatePrice === null && a.approximatePrice !== null) return 1;
			if (a.approximatePrice !== null && b.approximatePrice !== null) {
				const diff = a.approximatePrice - b.approximatePrice;
				if (diff !== 0) return diff;
			}
			return a.createdAt.getTime() - b.createdAt.getTime();
		}
		// 'created-date' or default
		return a.createdAt.getTime() - b.createdAt.getTime();
	}

	let filteredWishes = $derived(wishes.filter(w => {
		if (searchQuery) {
			const q = searchQuery.toLowerCase();
			const titleMatch = w.title.toLowerCase().includes(q);
			const descMatch = (w.description || '').toLowerCase().includes(q);
			if (!titleMatch && !descMatch) return false;
		}
		
		if (selectedTags.length > 0) {
			const wishTags = w.tags || [];
			if (tagMatchMode === 'all') {
				const hasAll = selectedTags.every(t => wishTags.includes(t));
				if (!hasAll) return false;
			} else {
				const hasAny = selectedTags.some(t => wishTags.includes(t));
				if (!hasAny) return false;
			}
		}

		return true;
	}));

	let sortedWishes = $derived([...filteredWishes].sort((a, b) => compareWishes(a, b, sortMode)));

	function updateFilters(updates: Record<string, string | null>) {
		const url = new URL($page.url);
		for (const [key, value] of Object.entries(updates)) {
			if (value === null || value === '') {
				url.searchParams.delete(key);
			} else {
				url.searchParams.set(key, value);
			}
		}
		goto(url, { replaceState: true, keepFocus: true });
	}

	function handleSortChange(e: Event) {
		const target = e.target as HTMLSelectElement;
		updateFilters({ sort: target.value === 'created-date' ? null : target.value });
	}

	function handleSearchInput(e: Event) {
		const target = e.target as HTMLInputElement;
		updateFilters({ q: target.value });
	}

	function toggleTag(tag: string) {
		const current = new Set(selectedTags);
		if (current.has(tag)) {
			current.delete(tag);
		} else {
			current.add(tag);
		}
		updateFilters({ tags: current.size > 0 ? Array.from(current).join(',') : null });
	}

	function handleTagMatchChange(e: Event) {
		const target = e.target as HTMLSelectElement;
		updateFilters({ match: target.value === 'any' ? 'any' : null });
	}

	let showAddModal = $state(false);
	let formLoading = $state(false);

	let editingWish: Wish | null = $state(null);

	let shareCopied = $state(false);

	let showEditListModal = $state(false);
	let editListLoading = $state(false);

	let deleteWishId = $state("");
	let deleteWishForm: HTMLFormElement;

	let createWishData = $derived(form?.createWishError ? (form as Record<string, any>) : null);
	let updateWishData = $derived(form?.updateWishError ? (form as Record<string, any>) : null);
	let updateListData = $derived(form?.updateListError ? (form as Record<string, any>) : null);

	$effect(() => {
		if (form?.createWishSuccess) showAddModal = false;
		if (form?.updateWishSuccess) editingWish = null;
		if (form?.updateListSuccess) showEditListModal = false;
	});

	async function handleShare() {
		if (!wishList) return;
		const url = `${window.location.origin}${resolve(`/shared/${wishList.shareId}`)}`;
		await navigator.clipboard.writeText(url);
		shareCopied = true;
		setTimeout(() => (shareCopied = false), 2000);
	}

	function handleDeleteWish(id: string) {
		if (!confirm("Delete this wish?")) return;
		deleteWishId = id;
		setTimeout(() => deleteWishForm.requestSubmit(), 0);
	}
</script>

<svelte:head><title>{wishList?.title || "List"} — Omiyage</title></svelte:head>

<main class="container page">
	<div class="page-header">
		<div class="header-left">
			<a href={resolve("/dashboard")} class="back-link">← My Lists</a>
			<h1>{wishList.title}</h1>
			{#if wishList.description}
				<p class="list-desc">{wishList.description}</p>
			{/if}
		</div>
		<div class="header-actions">
			<div class="sort-control">
				<label for="sort-mode">Sort</label>
				<select id="sort-mode" value={sortMode} onchange={handleSortChange}>
					<option value="created-date">Creation Date</option>
					<option value="modified-date">Modified Date</option>
					<option value="alphabetical">Alphabetical</option>
					<option value="price">Price</option>
				</select>
			</div>
			<button
				class="btn-ghost header-action-btn"
				onclick={() => (showEditListModal = true)}>Edit</button
			>
			<button class="btn-ghost header-action-btn" onclick={handleShare}>
				{shareCopied ? "Copied!" : "Share"}
			</button>
			<button
				class="btn-primary header-action-btn"
				onclick={() => (showAddModal = true)}
			>
				+ Add Wish
			</button>
		</div>
	</div>

	{#if wishes.length === 0}
		<div class="empty-state">
			<h3>No wishes yet</h3>
			<p>Add your first wish to this list!</p>
			<button
				class="btn-primary"
				style="margin-top:1rem"
				onclick={() => (showAddModal = true)}
			>
				+ Add Wish
			</button>
		</div>
	{:else}
		<div class="filter-bar">
			<div class="search-input-wrapper">
				<input 
					type="search" 
					class="search-input" 
					placeholder="Search wishes…" 
					value={searchQuery}
					oninput={handleSearchInput}
				/>
			</div>
			<div class="tag-filters">
				<div class="tag-logic">
					<select value={tagMatchMode} onchange={handleTagMatchChange}>
						<option value="all">All of</option>
						<option value="any">Any of</option>
					</select>
				</div>
				{#if availableTags.length > 0}
					<div class="tag-pills">
						{#each availableTags as tag}
							<button 
								class="tag-pill" 
								class:active={selectedTags.includes(tag)}
								onclick={() => toggleTag(tag)}
							>
								#{tag}
							</button>
						{/each}
					</div>
				{:else}
					<span class="no-tags-msg">No tags in this list</span>
				{/if}
			</div>
		</div>

		{#if sortedWishes.length === 0}
			<div class="empty-state">
				<h3>No matches found</h3>
				<p>Try adjusting your search or filters.</p>
			</div>
		{:else}
			<div class="wish-list">
				{#each sortedWishes as wish (wish.id)}
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

<form
	method="POST"
	action="?/deleteWish"
	use:enhance
	bind:this={deleteWishForm}
	style="display:none"
>
	<input type="hidden" name="id" bind:value={deleteWishId} />
</form>

<Modal
	title="Add Wish"
	open={showAddModal}
	onclose={() => (showAddModal = false)}
>
	<WishForm
		loading={formLoading}
		action="?/createWish"
		formError={form?.createWishError}
		initial={{
			title: createWishData ? createWishData.title : "",
			description: createWishData ? createWishData.description : "",
			approximatePrice: createWishData
				? createWishData.approximatePrice
				: undefined,
			currencyCode: createWishData ? createWishData.currencyCode : "EUR",
			imageUrl: createWishData ? createWishData.imageUrl : "",
		}}
		enhanceHandler={() => {
			formLoading = true;
			return async ({ update }: any) => {
				formLoading = false;
				await update();
			};
		}}
		oncancel={() => (showAddModal = false)}
	/>
</Modal>

<Modal
	title="Edit Wish"
	open={!!editingWish}
	onclose={() => (editingWish = null)}
>
	{#if editingWish}
		<WishForm
			loading={formLoading}
			action="?/updateWish"
			formError={form?.updateWishError}
			id={editingWish.id}
			initial={{
				title: updateWishData ? updateWishData.title : editingWish.title,
				description: updateWishData
					? updateWishData.description
					: editingWish.description || undefined,
				approximatePrice: updateWishData
					? updateWishData.approximatePrice
					: editingWish.approximatePrice || undefined,
				currencyCode: updateWishData
					? updateWishData.currencyCode
					: editingWish.currencyCode || undefined,
				imageUrl: updateWishData
					? updateWishData.imageUrl
					: editingWish.imageUrl || undefined,
				tags: editingWish.tags,
				links: editingWish.links.map((l) => ({
					url: l.url,
					label: l.label || null,
				})),
			}}
			enhanceHandler={() => {
				formLoading = true;
				return async ({ update }: any) => {
					formLoading = false;
					await update();
				};
			}}
			oncancel={() => (editingWish = null)}
		/>
	{/if}
</Modal>

<Modal
	title="Edit List"
	open={showEditListModal}
	onclose={() => (showEditListModal = false)}
>
	<form
		method="POST"
		action="?/updateList"
		use:enhance={() => {
			editListLoading = true;
			return async ({ update }: any) => {
				editListLoading = false;
				await update();
			};
		}}
	>
		<div class="form-group">
			<label for="edit-list-title">Title *</label>
			<input
				id="edit-list-title"
				name="title"
				value={updateListData ? updateListData.title : wishList.title}
				required
			/>
		</div>
		<div class="form-group">
			<label for="edit-list-description">Description</label>
			<input
				id="edit-list-description"
				name="description"
				value={updateListData
					? updateListData.description
					: wishList.description || ""}
			/>
		</div>
		{#if form?.updateListError}
			<p class="error-msg">{form.updateListError}</p>
		{/if}
		<div class="modal-actions">
			<button
				type="button"
				class="btn-ghost"
				onclick={() => (showEditListModal = false)}>Cancel</button
			>
			<button
				type="submit"
				class="btn-primary"
				disabled={editListLoading}
			>
				{editListLoading ? "Saving…" : "Save"}
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

	.sort-control {
		display: flex;
		align-items: center;
		gap: 0.4rem;
		min-height: 2.35rem;

		label {
			margin: 0;
			font-size: 0.78rem;
		}

		select {
			min-width: 10rem;
			width: auto;
		}
	}

	.filter-bar {
		display: flex;
		flex-direction: column;
		gap: 1rem;
		margin-bottom: 1.5rem;
		padding: 1rem;
		background: rgba(255, 255, 255, 0.03);
		border: 1px solid var(--border);
		border-radius: 0.5rem;
	}

	.search-input-wrapper {
		width: 100%;
	}

	.search-input {
		width: 100%;
		padding: 0.6rem 1rem;
		border-radius: 0.5rem;
		border: 1px solid var(--border);
		background: rgba(0, 0, 0, 0.2);
		color: var(--text-bright);
		font-family: inherit;

		&:focus {
			outline: 2px solid var(--accent);
			border-color: transparent;
		}
	}

	.tag-filters {
		display: flex;
		align-items: center;
		gap: 0.75rem;
		flex-wrap: wrap;
	}

	.tag-logic select {
		font-size: 0.85rem;
		width: auto;
	}

	.no-tags-msg {
		font-size: 0.85rem;
		color: rgba(255, 255, 255, 0.4);
		font-style: italic;
	}

	.tag-pills {
		display: flex;
		flex-wrap: wrap;
		gap: 0.5rem;
		align-items: center;
	}

	.tag-pill {
		background: rgba(255, 255, 255, 0.05);
		border: 1px solid var(--border);
		border-radius: 1rem;
		padding: 0.3rem 0.8rem;
		font-size: 0.85rem;
		color: var(--text);
		cursor: pointer;
		transition: all 0.2s;

		&:hover {
			background: rgba(255, 255, 255, 0.1);
			color: var(--text-bright);
		}

		&.active {
			background: var(--accent);
			color: var(--bg);
			border-color: var(--accent);
		}
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
