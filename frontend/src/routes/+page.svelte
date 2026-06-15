<script lang="ts">
	import { goto } from '$app/navigation';
	import { onMount } from 'svelte';
	import { userStore, loadUser } from '$lib/stores';
	import { resolve } from '$app/paths';

	onMount(async () => {
		await loadUser();
		if ($userStore) {
			goto(resolve('/dashboard'), { replaceState: true });
		} else {
			goto(resolve('/login'), { replaceState: true });
		}
	});
</script>

<div class="loading">Loading…</div>
