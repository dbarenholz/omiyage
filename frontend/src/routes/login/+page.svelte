<script lang="ts">
import { goto } from '$app/navigation';
import { login } from '$lib/api';
import { userStore } from '$lib/stores';
import { base } from '$app/paths';

let accountNumber = '';
let error = '';
let loading = false;

async function handleLogin() {
error = '';
const trimmed = accountNumber.trim();
if (!trimmed) {
error = 'Please enter your account number';
return;
}
loading = true;
try {
const user = await login(trimmed);
userStore.set(user);
goto(`${base}/dashboard`);
} catch (e: unknown) {
error = e instanceof Error ? e.message : 'Login failed';
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

<form on:submit|preventDefault={handleLogin}>
<div class="form-group">
<label for="account-number">Account Number</label>
<input
id="account-number"
bind:value={accountNumber}
placeholder="XXXX-XXXX-XXXX-XXXX"
autocomplete="off"
spellcheck="false"
/>
<p class="field-hint">16-digit number from your signup, with or without dashes</p>
</div>
{#if error}
<p class="error-msg">{error}</p>
{/if}
<button type="submit" class="btn-primary full-width" disabled={loading}>
{loading ? 'Signing in…' : 'Sign In'}
</button>
</form>

<div class="divider"><span>or</span></div>

<a href="{base}/signup" class="btn-ghost create-btn">Create account</a>
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
content: '';
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
