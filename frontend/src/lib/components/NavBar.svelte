<script lang="ts">
	import { goto } from '$app/navigation';
	import { userStore } from '$lib/stores';
	import { page } from '$app/state';
	import { get } from 'svelte/store';
	import { logout, updateMe } from '$lib/api';
	import Modal from '$lib/components/Modal.svelte';
	import { resolve } from '$app/paths';
	import {Pencil} from "lucide-svelte";

	let showDisplayNameModal = $state(false);
	let displayNameInput = $state('');
	let displayNameTouched = $state(false);
	let savingDisplayName = $state(false);
	let displayNameError = $state('');

	let displayNameValidationError = $derived(
		displayNameTouched && !displayNameInput.trim() ? 'Display name is required' : ''
	);

	async function handleLogout() {
		await logout();
		userStore.set(null);
		const currentPath = page.url.pathname;
		if (!currentPath.startsWith(resolve('/shared/'))) {
			goto(resolve('/login'));
		}
	}

	function openDisplayNameModal() {
		displayNameInput = get(userStore)?.displayName || '';
		displayNameTouched = false;
		displayNameError = '';
		showDisplayNameModal = true;
	}

	function closeDisplayNameModal() {
		showDisplayNameModal = false;
		displayNameTouched = false;
		displayNameError = '';
	}

	async function handleDisplayNameSave() {
		displayNameTouched = true;
		displayNameError = '';

		if (displayNameValidationError) {
			displayNameError = displayNameValidationError;
			return;
		}

		savingDisplayName = true;
		try {
			const updatedUser = await updateMe(displayNameInput.trim());
			userStore.set(updatedUser);
			closeDisplayNameModal();
		} catch (e: unknown) {
			displayNameError = e instanceof Error ? e.message : 'Failed to update display name';
		} finally {
			savingDisplayName = false;
		}
	}
</script>

<nav class="navbar">
	<div class="navbar-inner container">
		<a href={resolve('/dashboard')} class="brand">🎁 Omiyage</a>
		<div class="nav-right">
			{#if $userStore}
				<button class="display-name-btn" onclick={openDisplayNameModal} title="Edit display name">
					<Pencil size={16} />
					<span class="display-name-text">{$userStore.displayName || 'Unnamed user'}</span>
				</button>
				<button class="btn-ghost logout-btn" onclick={handleLogout}>Logout</button>
			{/if}
		</div>
	</div>
</nav>

<Modal title="Update Display Name" open={showDisplayNameModal} onclose={closeDisplayNameModal}>
	<form onsubmit={(e) => { e.preventDefault(); handleDisplayNameSave(); }}>
		<div class="form-group">
			<label for="display-name">Display Name *</label>
			<input
				id="display-name"
				bind:value={displayNameInput}
				oninput={() => (displayNameTouched = true)}
				onblur={() => (displayNameTouched = true)}
				class:field-invalid={!!displayNameValidationError}
				class:field-valid={displayNameTouched && !displayNameValidationError}
				placeholder="How your name appears"
				required
			/>
		</div>
		{#if displayNameError}
			<p class="error-msg">{displayNameError}</p>
		{/if}
		<div class="modal-actions">
			<button type="button" class="btn-ghost" onclick={closeDisplayNameModal}>Cancel</button>
			<button type="submit" class="btn-primary" disabled={savingDisplayName}>
				{savingDisplayName ? 'Saving…' : 'Save'}
			</button>
		</div>
	</form>
</Modal>

<style lang="scss">
	.navbar {
		background-color: var(--surface);
		border-bottom: 1px solid var(--border);
		position: sticky;
		top: 0;
		z-index: 50;
	}

	.navbar-inner {
		display: flex;
		align-items: center;
		justify-content: space-between;
		height: 56px;
	}

	.brand {
		font-size: 1.1rem;
		font-weight: 700;
		color: var(--accent);
		text-decoration: none;
	}

	.nav-right {
		display: flex;
		align-items: center;
		gap: 0.75rem;
	}

	.display-name-btn {
		background: transparent;
		border: 1px solid transparent;
		color: var(--text);
		font-size: 0.85rem;
		max-width: 170px;
		padding: 0.2rem 0.4rem;
		display: inline-flex;
		align-items: center;
		gap: 0.3rem;

		.display-name-text {
			overflow: hidden;
			text-overflow: ellipsis;
			white-space: nowrap;
		}

		&:hover {
			color: var(--text-bright);
			background: var(--surface-2);
			border-color: var(--border);
			opacity: 1;
		}
	}

	.logout-btn {
		font-size: 0.8rem;
		padding: 0.35rem 0.75rem;
	}

	.modal-actions {
		display: flex;
		justify-content: flex-end;
		gap: 0.75rem;
		margin-top: 1rem;
	}
</style>
