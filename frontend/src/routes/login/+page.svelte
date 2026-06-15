<script lang="ts">
	import { goto } from "$app/navigation";
	import { login } from "$lib/api";
	import { userStore } from "$lib/stores";
	import { resolve } from "$app/paths";

	let accountNumber = $state("");
	let error = $state("");
	let loading = $state(false);

	function handleAccountInput(e: Event) {
		const target = e.target as HTMLInputElement;
		const digits = target.value.replace(/\D/g, "").slice(0, 16);
		const formatted = digits.match(/.{1,4}/g)?.join("-") || "";
		accountNumber = formatted;
		target.value = formatted;
	}

	async function handleLogin() {
		error = "";
		const trimmed = accountNumber.trim();
		if (!trimmed) {
			error = "Please enter your account number";
			return;
		}

		const digitsOnly = trimmed.replace(/-/g, "");
		if (!/^\d{16}$/.test(digitsOnly)) {
			error = "Invalid account number format";
			return;
		}

		loading = true;
		try {
			const user = await login(trimmed);
			userStore.set(user);
			goto(resolve("/dashboard"));
		} catch (e: unknown) {
			error = e instanceof Error ? e.message : "Login failed";
		} finally {
			loading = false;
		}
	}
</script>

<svelte:head><title>Login — Omiyage</title></svelte:head>

<div class="auth-page">
	<div class="auth-card card">
		<h1>🎁 Omiyage</h1>
		<p class="subtitle">Sign in with your account number</p>

		<form
			onsubmit={(e) => {
				e.preventDefault();
				handleLogin();
			}}
		>
			<div class="form-group">
				<label for="account-number">Account Number</label>
				<input
					id="account-number"
					bind:value={accountNumber}
					oninput={handleAccountInput}
					placeholder="XXXX-XXXX-XXXX-XXXX"
					autocomplete="off"
					spellcheck="false"
				/>
				<p class="field-hint">
					16-digit number from your signup; invalid input ignored
				</p>
			</div>
			{#if error}
				<p class="error-msg">{error}</p>
			{/if}
			<button
				type="submit"
				class="btn-primary full-width"
				disabled={loading}
			>
				{loading ? "Logging in…" : "Login"}
			</button>
		</form>

		<div class="divider"><span>or</span></div>

		<a href={resolve("/signup")} class="btn-ghost create-btn">Sign up</a>
	</div>
</div>

<style lang="scss">
	.auth-page {
		min-height: 100vh;
		display: flex;
		align-items: center;
		justify-content: center;
		padding: 1rem;
	}

	.auth-card {
		width: 100%;
		max-width: 400px;
	}

	h1 {
		font-size: 1.6rem;
		margin-bottom: 0.25rem;
		text-align: center;
	}

	.subtitle {
		text-align: center;
		color: var(--text);
		margin-bottom: 1.5rem;
		font-size: 0.9rem;
	}

	.field-hint {
		font-size: 0.78rem;
		color: var(--text);
		margin-top: 0.3rem;
		opacity: 0.7;
	}

	.full-width {
		width: 100%;
		padding: 0.65rem;
		font-size: 1rem;
		margin-top: 0.25rem;
	}

	.divider {
		display: flex;
		align-items: center;
		gap: 0.75rem;
		margin: 1.25rem 0;
		color: var(--text);
		font-size: 0.8rem;

		&::before,
		&::after {
			content: "";
			flex: 1;
			height: 1px;
			background: var(--border);
		}
	}

	.create-btn {
		display: block;
		text-align: center;
		width: 100%;
		padding: 0.6rem;
		font-size: 0.9rem;
		text-decoration: none;
	}
</style>
