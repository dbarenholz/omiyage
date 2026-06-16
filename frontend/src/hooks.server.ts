import { redirect, type Handle, type HandleFetch } from '@sveltejs/kit';
import { env as privateEnv } from '$env/dynamic/private';
import { getMe } from '$lib/api';
import { dev } from '$app/environment';

if (dev) {
	if (!(globalThis as any).__msw_server_started) {
		const { server } = await import('./mocks/server');
		server.listen({
			onUnhandledRequest: 'bypass'
		});
		(globalThis as any).__msw_server_started = true;
	}
}

const publicRoutes = ['/login', '/signup', '/shared'];

export const handleFetch: HandleFetch = async ({ event, request, fetch }) => {
	const cookie = event.request.headers.get('cookie');
	if (cookie) {
		request.headers.set('cookie', cookie);
	}

	// In Docker, route requests to the internal backend container instead of localhost
	if (privateEnv.INTERNAL_API_URL && request.url.startsWith('http://localhost:8080/')) {
		request = new Request(request.url.replace('http://localhost:8080/', privateEnv.INTERNAL_API_URL), request);
	}

	return fetch(request);
};

export const handle: Handle = async ({ event, resolve }) => {
	event.locals.user = await getMe({ fetch: event.fetch });

	const isPublic = publicRoutes.some((route) => event.url.pathname.startsWith(route));
	const isApi = event.url.pathname.startsWith('/api/');

	if (!event.locals.user && !isPublic && !isApi && event.url.pathname !== '/') {
		throw redirect(302, '/login');
	}

	return resolve(event);
};
