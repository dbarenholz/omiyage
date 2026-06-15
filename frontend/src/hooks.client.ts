import { dev } from '$app/environment';
import { env } from '$env/dynamic/public';

if (dev || env.PUBLIC_E2E_TEST === 'true') {
	const { worker } = await import('./mocks/browser');
	await worker.start({
		onUnhandledRequest: 'bypass'
	});
}
