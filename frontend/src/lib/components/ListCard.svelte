<script lang="ts">
	import type { WishList } from '$lib/types';
	import { base } from '$app/paths';

	export let list: WishList;
</script>

<a href="{base}/lists/{list.id}" class="list-card">
	<div class="list-card-header">
		<h3>{list.title}</h3>
		<span class="count">{list.wishCount ?? 0} {list.wishCount === 1 ? 'wish' : 'wishes'}</span>
	</div>
	{#if list.description}
		<p class="description">{list.description}</p>
	{/if}
</a>

<style lang="scss">
	.list-card {
		display: block;
		background-color: var(--surface);
		border-radius: var(--radius);
		padding: 1rem;
		box-shadow: var(--shadow);
		text-decoration: none;
		transition: background-color 0.15s, transform 0.1s;
		border: 1px solid var(--border);
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
		flex-wrap: wrap;
	}

	h3 {
		font-size: 1rem;
		color: var(--text-bright);
		margin: 0;
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
		-webkit-box-orient: vertical;
	}
</style>
