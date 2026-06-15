<script lang="ts">
	import type { WishListSummary } from "$lib/types";
	import { resolve } from "$app/paths";
	import { Pencil, X } from "lucide-svelte";

	let {
		list,
		onedit,
		ondelete,
	}: {
		list: WishListSummary;
		onedit?: (list: WishListSummary) => void;
		ondelete?: (id: string) => void;
	} = $props();
</script>

<div class="list-card">
	<a
		href={resolve(`/lists/${list.id}`)}
		class="card-link"
		aria-label="View list {list.title}"
	></a>
	<div class="list-card-content">
		<div class="list-card-header">
			<h3>{list.title}</h3>
			<span class="count"
				>{list.wishCount ?? 0}
				{list.wishCount === 1 ? "wish" : "wishes"}</span
			>
		</div>
		{#if list.description}
			<p class="description">{list.description}</p>
		{/if}
	</div>
	<div class="list-card-actions">
		<button
			class="btn-ghost list-action-btn"
			onclick={(e) => {
				e.preventDefault();
				e.stopPropagation();
				onedit?.(list);
			}}
			title="Edit list"><Pencil class="icon" size={16} /></button
		>
		<button
			class="btn-ghost list-action-btn"
			onclick={(e) => {
				e.preventDefault();
				e.stopPropagation();
				ondelete?.(list.id);
			}}
			title="Delete list"><X class="icon danger" size={16} /></button
		>
	</div>
</div>

<style lang="scss">
	.list-card {
		position: relative;
		display: flex;
		flex-direction: column;
		height: 100%;
		background-color: var(--surface);
		border-radius: var(--radius);
		padding: 1rem;
		box-shadow: var(--shadow);
		transition:
			background-color 0.15s,
			transform 0.1s;
		border: 1px solid var(--border);
	}

	.card-link {
		position: absolute;
		inset: 0;
		z-index: 1;
		border-radius: var(--radius);
	}

	.list-card-content {
		flex: 1;
	}

	.list-card:hover {
		background-color: var(--surface-2);
		transform: translateY(-1px);
	}

	.list-card-header {
		display: flex;
		align-items: center;
		justify-content: space-between;
		gap: 0.5rem;
		flex-wrap: nowrap;
	}

	h3 {
		font-size: 1rem;
		color: var(--text-bright);
		margin: 0;
		white-space: nowrap;
		overflow: hidden;
		text-overflow: ellipsis;
		flex: 1;
		min-width: 0;
	}

	.count {
		font-size: 0.78rem;
		color: var(--cyan);
		background: rgba(92, 207, 230, 0.15);
		padding: 0.15rem 0.5rem;
		border-radius: 999px;
		white-space: nowrap;
	}

	.description {
		margin-top: 0.4rem;
		font-size: 0.85rem;
		color: var(--text);
		overflow: hidden;
		display: -webkit-box;
		-webkit-line-clamp: 2;
		line-clamp: 2;
		-webkit-box-orient: vertical;
		overflow-wrap: anywhere;
		word-break: break-word;
	}

	.list-card-actions {
		display: flex;
		justify-content: flex-end;
		gap: 0.35rem;
		margin-top: 1rem;
		position: relative;
		z-index: 2;
	}

	.list-action-btn {
		display: inline-flex;
		align-items: center;
		justify-content: center;
		width: 2rem;
		height: 2rem;
		padding: 0;
	}

	.list-action-btn :global(.icon) {
		stroke-width: 2;
	}

	.list-action-btn :global(.icon.danger) {
		color: var(--error);
	}
</style>
