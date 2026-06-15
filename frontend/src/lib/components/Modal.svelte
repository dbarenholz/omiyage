<script lang="ts">
	import { tick } from "svelte";
	import type { Snippet } from "svelte";

	let {
		title = "",
		open = false,
		onclose,
		children,
	}: {
		title?: string;
		open?: boolean;
		onclose?: () => void;
		children?: Snippet;
	} = $props();

	let modalEl: HTMLDivElement | null = $state(null);
	let previousFocus: HTMLElement | null = null;

	function close() {
		onclose?.();
	}

	function handleKeydown(e: KeyboardEvent) {
		if (!open || !modalEl) return;
		if (e.key === "Escape") {
			close();
			e.preventDefault();
			return;
		}
		if (e.key === "Tab") {
			const focusableElements = Array.from(
				modalEl.querySelectorAll<HTMLElement>(
					'button, [href], input:not([disabled]), select:not([disabled]), textarea:not([disabled]), [tabindex]:not([tabindex="-1"])',
				),
			);
			if (focusableElements.length === 0) {
				e.preventDefault();
				return;
			}
			// we know focusableElements != undefined, and that length > 0
			// since we use querySelectorAll, elements _must_ be of type HTMLElement, and therefor cannot be undefined
			// we can safely use `?` in the subsequent `.focus()` calls
			const firstElement = focusableElements[0];
			const lastElement = focusableElements[focusableElements.length - 1];

			if (e.shiftKey) {
				if (
					document.activeElement === firstElement ||
					document.activeElement === modalEl
				) {
					lastElement?.focus();
					e.preventDefault();
				}
			} else {
				if (
					document.activeElement === lastElement ||
					document.activeElement === modalEl
				) {
					firstElement?.focus();
					e.preventDefault();
				}
			}
		}
	}

	$effect(() => {
		if (open) {
			if (typeof document !== "undefined" && !previousFocus) {
				previousFocus = document.activeElement as HTMLElement;
			}
			tick().then(() => {
				if (modalEl) {
					const firstInput = modalEl.querySelector<HTMLElement>(
						'input:not([type="hidden"]):not([disabled]), textarea:not([disabled]), select:not([disabled])',
					);
					if (firstInput) {
						firstInput.focus();
					} else {
						const firstFocusable =
							modalEl.querySelector<HTMLElement>(
								'button:not([disabled]), [href], [tabindex]:not([tabindex="-1"])',
							);
						if (firstFocusable) firstFocusable.focus();
					}
				}
			});
		} else {
			if (previousFocus && typeof document !== "undefined") {
				previousFocus.focus();
				previousFocus = null;
			}
		}
	});
</script>

<svelte:window onkeydown={handleKeydown} />

{#if open}
	<!-- svelte-ignore a11y_click_events_have_key_events -->
	<!-- svelte-ignore a11y_no_noninteractive_element_interactions -->
	<div class="overlay" onclick={close} role="presentation">
		<!-- svelte-ignore a11y_no_noninteractive_element_interactions -->
		<div
			class="modal"
			bind:this={modalEl}
			tabindex="-1"
			onclick={(e) => e.stopPropagation()}
			role="dialog"
			aria-modal="true"
			aria-labelledby="modal-title"
		>
			<div class="modal-header">
				<h2 id="modal-title">{title}</h2>
				<button class="close-btn" onclick={close} aria-label="Close"
					>✕</button
				>
			</div>
			<div class="modal-body">
				{@render children?.()}
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
