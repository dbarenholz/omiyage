<script lang="ts">
	import { goto } from '$app/navigation';
	import { onMount } from 'svelte';
	import { userStore, loadUser } from '$lib/stores';
	import { base } from '$app/paths';

	onMount(async () => {
		await loadUser();
		if ($userStore) {
			goto(`${base}/dashboard`, { replaceState: true });
		} else {
			goto(`${base}/login`, { replaceState: true });
		}
	});
</script>

<div class="loading">Loading…</div>
