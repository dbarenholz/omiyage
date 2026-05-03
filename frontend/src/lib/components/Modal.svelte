<script lang="ts">
	import { createEventDispatcher } from 'svelte';

	export let title = '';
	export let open = false;

	const dispatch = createEventDispatcher();

	function close() {
		dispatch('close');
	}

	function handleKeydown(e: KeyboardEvent) {
		if (e.key === 'Escape') close();
	}
</script>

<svelte:window on:keydown={handleKeydown} />

{#if open}
	<div class="overlay" on:click={close} role="presentation">
		<div class="modal" on:click|stopPropagation role="dialog" aria-modal="true" aria-labelledby="modal-title">
			<div class="modal-header">
				<h2 id="modal-title">{title}</h2>
				<button class="close-btn" on:click={close} aria-label="Close">✕</button>
			</div>
			<div class="modal-body">
				<slot />
			</div>
		</div>
	</div>
{/if}

<style lang="scss">
	.overlay {
		position: fixed;
		inset: 0;
		background: rgba(0, 0, 0, 0.6);
		display: flex;
		align-items: center;
		justify-content: center;
		z-index: 100;
		padding: 1rem;
	}

	.modal {
		background: var(--surface);
		border-radius: var(--radius);
		width: 100%;
		max-width: 520px;
		max-height: 90vh;
		overflow-y: auto;
		box-shadow: 0 8px 32px rgba(0, 0, 0, 0.5);
	}

	.modal-header {
		display: flex;
		align-items: center;
		justify-content: space-between;
		padding: 1rem 1.25rem;
		border-bottom: 1px solid var(--border);
	}

	.modal-header h2 {
		font-size: 1.1rem;
		margin: 0;
	}

	.close-btn {
		background: transparent;
		color: var(--text);
		font-size: 1rem;
		padding: 0.25rem 0.5rem;
		line-height: 1;
	}

	.close-btn:hover {
		color: var(--text-bright);
		opacity: 1;
	}

	.modal-body {
		padding: 1.25rem;
	}
</style>
