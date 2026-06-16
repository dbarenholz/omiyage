<script lang="ts">
	import { enhance } from "$app/forms";
	import { resolve } from "$app/paths";
	import WishCard from "$lib/components/WishCard.svelte";
	import WishForm from "$lib/components/WishForm.svelte";
	import Modal from "$lib/components/Modal.svelte";
	import type { Wish } from "$lib/types";

	let { data, form } = $props();

	let wishList = $derived(data.wishList);
	let wishes = $derived(data.wishes);

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
