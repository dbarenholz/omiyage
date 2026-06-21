<script lang="ts">
	import { onMount } from "svelte";
	import { page } from "$app/stores";
	import { goto } from "$app/navigation";
	import { ApiError, getSharedList, claimWish, unclaimWish } from "$lib/api";
	import { userStore, loadUser } from "$lib/stores";
	import WishCard from "$lib/components/WishCard.svelte";
	import type { WishList, Wish } from "$lib/types";
	import { resolve } from "$app/paths";

	let view: WishList | null = $state(null);
	let loading = $state(true);
	let error = $state("");
	let errorTitle = $state("Shared List Unavailable");

	let shareId = $derived($page.params.shareId as string);
	let canClaim = $derived(!!$userStore);

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

	let availableTags: string[] = $derived(
		view ? Array.from(new Set((view as WishList).wishes.flatMap(w => w.tags || []))).sort((a, b) => a.localeCompare(b, 'en', { sensitivity: 'base' })) : []
	);

	function compareWishes(a: Wish, b: Wish, mode: WishSortMode): number {
		if (mode === 'alphabetical') {
			const byTitle = a.title.localeCompare(b.title, 'en', { sensitivity: 'base' });
			return byTitle !== 0 ? byTitle : (new Date(a.createdAt).getTime() - new Date(b.createdAt).getTime());
		}
		if (mode === 'modified-date') {
			const byModifiedAt = new Date(b.updatedAt).getTime() - new Date(a.updatedAt).getTime();
			return byModifiedAt !== 0 ? byModifiedAt : (new Date(a.createdAt).getTime() - new Date(b.createdAt).getTime());
		}
		if (mode === 'price') {
			if (a.approximatePrice === null && b.approximatePrice !== null) return -1;
			if (b.approximatePrice === null && a.approximatePrice !== null) return 1;
			if (a.approximatePrice !== null && b.approximatePrice !== null) {
				const diff = a.approximatePrice - b.approximatePrice;
				if (diff !== 0) return diff;
			}
			return new Date(a.createdAt).getTime() - new Date(b.createdAt).getTime();
		}
		// 'created-date' or default
		return new Date(a.createdAt).getTime() - new Date(b.createdAt).getTime();
	}

	let filteredWishes = $derived(view ? (view as WishList).wishes.filter(w => {
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
	}) : []);

	let sortedWishes: Wish[] = $derived([...filteredWishes].sort((a, b) => compareWishes(a, b, sortMode)));

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

	onMount(async () => {
		await loadUser();
		await fetchSharedList();
	});

	async function fetchSharedList() {
		loading = true;
		error = "";
		try {
			view = await getSharedList(shareId);
		} catch (e: unknown) {
			if (e instanceof ApiError) {
				if (e.status === 404) {
					errorTitle = "List Not Found";
					error = "This shared link is invalid or has expired.";
				} else if (e.status === 503) {
					errorTitle = "Service Unavailable";
					error =
						"The service is temporarily unavailable. Please try again in a moment.";
				} else {
					errorTitle = "Could Not Load List";
					error = e.message;
				}
			} else {
				errorTitle = "Could Not Load List";
				error =
					e instanceof Error
						? e.message
						: "Failed to load shared list";
			}
		} finally {
			loading = false;
		}
	}

	async function handleClaim(id: string) {
		if (!view) return;
		try {
			await claimWish(id);
			view = {
				...view,
				wishes: view.wishes.map((w: Wish) =>
					w.id === id
						? { ...w, claimed: true, claimedByMe: true }
						: w,
				),
			};
		} catch (err: unknown) {
			alert(err instanceof Error ? err.message : "Failed to claim");
		}
	}

	async function handleUnclaim(id: string) {
		if (!view) return;
		try {
			await unclaimWish(id);
			view = {
				...view,
				wishes: view.wishes.map((w: Wish) =>
					w.id === id
						? { ...w, claimed: false, claimedByMe: false }
						: w,
				),
			};
		} catch (err: unknown) {
			alert(err instanceof Error ? err.message : "Failed to unclaim");
		}
	}
</script>

<svelte:head>
	<title>{view?.title || "Shared List"} — Omiyage</title>
</svelte:head>

<div class="shared-page">
	{#if !$userStore}
		<div class="login-banner">
			<span
				>👋 <a href={resolve("/login")}>Log in</a> to claim wishes for {view?.title ||
					"this list"}!</span
			>
		</div>
	{/if}

	<main class="container page">
		{#if loading}
			<div class="loading">Loading shared list…</div>
		{:else if error}
			<div class="empty-state">
				<h3>{errorTitle}</h3>
				<p>{error}</p>
			</div>
		{:else if view}
			<div class="list-header">
				<div class="header-left">
					<h1>{view.title}</h1>
					{#if view.description}
						<p class="description">{view.description}</p>
					{/if}
				</div>
				<div class="sort-control">
					<label for="sort-mode">Sort</label>
					<select id="sort-mode" value={sortMode} onchange={handleSortChange}>
						<option value="created-date">Creation Date</option>
						<option value="modified-date">Modified Date</option>
						<option value="alphabetical">Alphabetical</option>
						<option value="price">Price</option>
					</select>
				</div>
			</div>

			{#if view.wishes.length === 0}
				<div class="empty-state">
					<h3>No wishes yet</h3>
					<p>This list is empty.</p>
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
								isOwner={false}
								{canClaim}
								onclaim={handleClaim}
								onunclaim={handleUnclaim}
							/>
						{/each}
					</div>
				{/if}
			{/if}
		{/if}
	</main>
</div>

<style lang="scss">
	.shared-page {
		min-height: 100vh;
	}

	.login-banner {
		background: rgba(255, 204, 102, 0.1);
		border-bottom: 1px solid rgba(255, 204, 102, 0.3);
		padding: 0.75rem 1rem;
		text-align: center;
		font-size: 0.9rem;
		color: var(--accent);

		a {
			color: var(--accent);
			font-weight: 600;
			text-decoration: underline;
		}
	}

	.page {
		padding-top: 1.5rem;
		padding-bottom: 2rem;
	}

	.list-header {
		display: flex;
		align-items: flex-start;
		justify-content: space-between;
		gap: 1rem;
		margin-bottom: 1.5rem;
		flex-wrap: wrap;

		.header-left {
			flex: 1;
			min-width: 0;
		}

		h1 {
			font-size: 1.5rem;
			margin-bottom: 0.25rem;
		}
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
</style>
