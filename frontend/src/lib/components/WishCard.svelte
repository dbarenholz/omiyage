<script lang="ts">
	import { Pencil, X } from 'lucide-svelte';
	import type { Wish } from '$lib/types';

	let {
		wish,
		isOwner = false,
		canClaim = false,
		onedit,
		ondelete,
		onclaim,
		onunclaim
	}: {
		wish: Wish;
		isOwner?: boolean;
		canClaim?: boolean;
		onedit?: (wish: Wish) => void;
		ondelete?: (id: string) => void;
		onclaim?: (id: string) => void;
		onunclaim?: (id: string) => void;
	} = $props();

	const tagColors = ['tag-yellow', 'tag-cyan', 'tag-purple', 'tag-green'];

	function tagColor(index: number): string {
		return tagColors[index % tagColors.length] ?? '#3b82f6';
	}

	function formatPrice(price: number, currencyCode?: string): string {
		const normalizedCurrency = (currencyCode || 'EUR').toUpperCase();

		try {
			return new Intl.NumberFormat('en-US', {
				style: 'currency',
				currency: normalizedCurrency
			}).format(price);
		} catch {
			return new Intl.NumberFormat('en-US', { style: 'currency', currency: 'EUR' }).format(price);
		}
	}

	function sanitizeUrl(url: string): string {
		try {
			const parsed = new URL(url);
			if (parsed.protocol === 'http:' || parsed.protocol === 'https:') {
				return url;
			}
			return 'about:blank';
		} catch {
			return 'about:blank';
		}
	}

	function getHostname(url: string): string {
		try {
			return new URL(url).hostname;
		} catch {
			return 'Link';
		}
	}
</script>

<div class="wish-card" class:claimed={wish.claimed}>
	<div class="wish-top">
		<div class="wish-title-row">
			<h3 class="wish-title">{wish.title}</h3>
			{#if wish.approximatePrice != null}
				<span class="price">{formatPrice(wish.approximatePrice, wish.currencyCode ?? undefined)}</span>
			{/if}
		</div>
		{#if isOwner}
			<div class="owner-actions">
				<button class="btn-ghost action-btn" onclick={() => onedit?.(wish)} title="Edit"><Pencil class="icon" size={18} /></button>
				<button class="btn-ghost action-btn danger" onclick={() => ondelete?.(wish.id)} title="Delete"><X class="icon danger" size={18} /></button>
			</div>
		{/if}
	</div>

	{#if wish.description}
		<p class="description">{wish.description}</p>
	{/if}

	{#if wish.imageUrl}
		<div class="image-wrap">
			<img src={sanitizeUrl(wish.imageUrl)} alt={wish.title} loading="lazy" />
		</div>
	{/if}

	{#if wish.tags.length > 0}
		<div class="tags">
			{#each wish.tags as tag, i}
				<span class="tag {tagColor(i)}">{tag}</span>
			{/each}
		</div>
	{/if}

	{#if wish.links.length > 0}
		<div class="links">
			{#each wish.links as link}
				<a href={sanitizeUrl(link.url)} target="_blank" rel="noopener noreferrer" class="link-pill">
					🔗 {link.label || getHostname(link.url)}
				</a>
			{/each}
		</div>
	{/if}

	{#if canClaim && !isOwner}
		<div class="claim-row">
			{#if wish.claimed && !wish.claimedByMe}
				<span class="claimed-badge">✓ Claimed</span>
			{:else if wish.claimedByMe}
				<button class="btn-ghost unclaim-btn" onclick={() => onunclaim?.(wish.id)}>Unclaim</button>
			{:else}
				<button class="btn-primary claim-btn" onclick={() => onclaim?.(wish.id)}>Claim 🎁</button>
			{/if}
		</div>
	{/if}
</div>

<style lang="scss">
	.wish-card {
		background-color: var(--surface);
		border-radius: var(--radius);
		padding: 1rem;
		box-shadow: var(--shadow);
		border: 1px solid var(--border);
		transition: border-color 0.15s;
	}

	.wish-card.claimed {
		border-color: rgba(186, 230, 126, 0.4);
		opacity: 0.85;
	}

	.wish-top {
		display: flex;
		align-items: flex-start;
		justify-content: space-between;
		gap: 0.5rem;
	}

	.wish-title-row {
		display: flex;
		align-items: baseline;
		gap: 0.75rem;
		flex-wrap: wrap;
		flex: 1;
		min-width: 0;
	}

	.wish-title {
		font-size: 0.95rem;
		color: var(--text-bright);
		margin: 0;
	}

	.price {
		font-size: 0.8rem;
		color: var(--success);
		font-weight: 600;
		white-space: nowrap;
	}

	.owner-actions {
		display: flex;
		gap: 0.25rem;
		flex-shrink: 0;
	}

	.action-btn {
		padding: 0.2rem 0.4rem;
		border-color: transparent;
		display: flex;
		align-items: center;
		justify-content: center;
	}

	.action-btn :global(.icon) {
		stroke-width: 2;
	}

	.action-btn :global(.icon.danger) {
		color: var(--error);
	}

	.description {
		font-size: 0.85rem;
		color: var(--text);
		margin-top: 0.5rem;
		line-height: 1.5;
	}

	.image-wrap {
		margin-top: 0.65rem;
		border-radius: var(--radius-sm);
		overflow: hidden;
		border: 1px solid var(--border);

		img {
			display: block;
			width: 100%;
			max-height: 220px;
			object-fit: cover;
		}
	}

	.tags {
		display: flex;
		flex-wrap: wrap;
		gap: 0.35rem;
		margin-top: 0.6rem;
	}

	.links {
		display: flex;
		flex-wrap: wrap;
		gap: 0.4rem;
		margin-top: 0.6rem;
	}

	.link-pill {
		display: inline-flex;
		align-items: center;
		gap: 0.25rem;
		background: rgba(92, 207, 230, 0.1);
		color: var(--cyan);
		padding: 0.2rem 0.6rem;
		border-radius: 999px;
		font-size: 0.78rem;
		text-decoration: none;
		border: 1px solid rgba(92, 207, 230, 0.2);
		transition: background 0.15s;
	}

	.link-pill:hover {
		background: rgba(92, 207, 230, 0.2);
		text-decoration: none;
	}

	.claim-row {
		margin-top: 0.75rem;
		display: flex;
		align-items: center;
	}

	.claim-btn {
		font-size: 0.85rem;
		padding: 0.4rem 0.9rem;
	}

	.unclaim-btn {
		font-size: 0.85rem;
		padding: 0.4rem 0.9rem;
		color: var(--text);
	}

	.claimed-badge {
		font-size: 0.82rem;
		color: var(--success);
	}
</style>
