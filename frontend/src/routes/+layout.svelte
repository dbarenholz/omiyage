<script lang="ts">
	import "../app.scss";
	import NavBar from "$lib/components/NavBar.svelte";
	import { userStore } from "$lib/stores";
	import { page } from "$app/state";

	let { data, children } = $props();

	// Initialize store from server data
	$effect(() => {
		if (data.user !== undefined) {
			userStore.set(data.user);
		}
	});

	const publicRoutes = ["/login", "/signup"];

	let showNav = $derived(
		!publicRoutes.some((r) => page.url.pathname.endsWith(r)),
	);
</script>

{#if showNav && $userStore}
	<NavBar />
{/if}
{@render children?.()}
