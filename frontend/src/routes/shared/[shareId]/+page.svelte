<script lang="ts">
import { onMount } from 'svelte';
import { page } from '$app/stores';
import { ApiError, getSharedList, claimWish, unclaimWish } from '$lib/api';
import { userStore, loadUser } from '$lib/stores';
import WishCard from '$lib/components/WishCard.svelte';
import type { SharedListView } from '$lib/types';
import { base } from '$app/paths';

let shareId = '';
let view: SharedListView | null = null;
let loading = true;
let error = '';
let errorTitle = 'Shared List Unavailable';

$: shareId = $page.params.shareId as string;
$: canClaim = !!$userStore;

onMount(async () => {
await loadUser();
await fetchSharedList();
});

async function fetchSharedList() {
loading = true;
error = '';
try {
view = await getSharedList(shareId);
} catch (e: unknown) {
if (e instanceof ApiError) {
if (e.status === 404) {
errorTitle = 'List Not Found';
error = 'This shared link is invalid or has expired.';
} else if (e.status === 503) {
errorTitle = 'Service Unavailable';
error = 'The service is temporarily unavailable. Please try again in a moment.';
} else {
errorTitle = 'Could Not Load List';
error = e.message;
}
} else {
errorTitle = 'Could Not Load List';
error = e instanceof Error ? e.message : 'Failed to load shared list';
}
} finally {
loading = false;
}
}

async function handleClaim(e: CustomEvent<string>) {
if (!view) return;
try {
await claimWish(e.detail);
view = {
...view,
wishes: view.wishes.map((w) =>
w.id === e.detail ? { ...w, claimed: true, claimedByMe: true } : w
)
};
} catch (err: unknown) {
alert(err instanceof Error ? err.message : 'Failed to claim');
}
}

async function handleUnclaim(e: CustomEvent<string>) {
if (!view) return;
try {
await unclaimWish(e.detail);
view = {
...view,
wishes: view.wishes.map((w) =>
w.id === e.detail ? { ...w, claimed: false, claimedByMe: false } : w
)
};
} catch (err: unknown) {
alert(err instanceof Error ? err.message : 'Failed to unclaim');
}
}
</script>

<svelte:head>
<title>{view?.title || 'Shared List'} — Omiyage</title>
</svelte:head>

<div class="shared-page">
{#if !$userStore}
<div class="login-banner">
<span>👋 <a href="{base}/login">Log in</a> to claim wishes for {view?.title || 'this list'}!</span>
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
<h1>{view.title}</h1>
{#if view.ownerDisplayName}
<p class="owner-name">🎁 {view.ownerDisplayName}'s omiyage</p>
{/if}
{#if view.description}
<p class="list-desc">{view.description}</p>
{/if}
</div>

{#if view.wishes.length === 0}
<div class="empty-state">
<h3>No wishes yet</h3>
<p>This list is empty.</p>
</div>
{:else}
<div class="wish-list">
{#each view.wishes as wish (wish.id)}
<WishCard
{wish}
isOwner={false}
{canClaim}
on:claim={handleClaim}
on:unclaim={handleUnclaim}
/>
{/each}
</div>
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
margin-bottom: 1.5rem;

h1 {
font-size: 1.5rem;
margin-bottom: 0.25rem;
}
}

.owner-name {
color: var(--cyan);
font-size: 0.9rem;
margin-bottom: 0.4rem;
}

.list-desc {
color: var(--text);
font-size: 0.9rem;
}

.wish-list {
display: flex;
flex-direction: column;
gap: 0.75rem;
}
</style>
