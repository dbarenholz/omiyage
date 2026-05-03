<script lang="ts">
	import '../app.scss';
	import NavBar from '$lib/components/NavBar.svelte';
	import { userStore, loadUser } from '$lib/stores';
	import { onMount } from 'svelte';
	import { page } from '$app/stores';

	const publicRoutes = ['/login', '/signup'];

	onMount(async () => {
		if ($userStore === null) {
			await loadUser();
		}
	});

	$: showNav = !publicRoutes.some((r) => $page.url.pathname.endsWith(r));
</script>

{#if showNav && $userStore}
	<NavBar />
{/if}
<slot />
